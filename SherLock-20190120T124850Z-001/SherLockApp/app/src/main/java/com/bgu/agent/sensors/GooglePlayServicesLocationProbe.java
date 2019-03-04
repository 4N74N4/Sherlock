package com.bgu.agent.sensors;

import android.location.Location;
import android.os.Bundle;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.TimeUtil;

import java.math.BigDecimal;

//com.bgu.agent.sensors.GooglePlayServicesLocationProbe

/**
 * Created by BittonRon on 2/21/14.
 */

@Probe.DisplayName("GooglePlayServicesLocationProbe")
@Probe.RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION})
@Probe.RequiredFeatures("android.hardware.location")
@Schedule.DefaultSchedule(interval = 10 * Constants.MINUTE, duration = 30 * Constants.SECOND)
public class GooglePlayServicesLocationProbe extends ContinuousDataProbe implements ProbeKeys.LocationKeys,
        ConnectionCallbacks, OnConnectionFailedListener
{

    // Milliseconds per second
    public static final int MILLISECONDS_PER_SECOND = 1000;

    // The update interval
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

    // A fast interval ceiling
    public static final int FAST_CEILING_IN_SECONDS = 8;

    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS = MILLISECONDS_PER_SECOND * FAST_CEILING_IN_SECONDS;

    @Configurable
    private BigDecimal maxAge = BigDecimal.valueOf(900);

    @Configurable
    private BigDecimal goodEnoughAccuracy = BigDecimal.valueOf(50);

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    // Listener to Location updates
    private LocationListener locationListener;

    // The best Location for this sample
    private Location bestLocation;

    private long currentTime = 0;

    public GooglePlayServicesLocationProbe()
    {
        super();
        maxWaitTime = BigDecimal.valueOf(25 * Constants.SECOND);
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        if (servicesConnected())
        {
            // Create a new global location parameters object
            mLocationRequest = LocationRequest.create();

            //Set the update interval
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

            // Use high accuracy
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            // Set the interval ceiling
            mLocationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);

            //Create a new location client,
            mLocationClient = new LocationClient(getContext(), this, this);

            // using the enclosing class to handle callbacks.
            locationListener = new LocationListener()
            {

                @Override
                public void onLocationChanged(Location location)
                {
                    Logger.i(GooglePlayServicesLocationProbe.class, "GetLocation " + location);
                    try
                    {
                        if (getState() != State.RUNNING && mLocationClient != null && mLocationClient.isConnected())
                        {
                            Logger.i(GooglePlayServicesLocationProbe.class, "GetLocation when Sensor Not Running");
                            mLocationClient.removeLocationUpdates(this);
                            return;
                        }

                        try
                        {
                            if (Utils.isBetterThenBestLocation(bestLocation, location, maxAge, currentTime))
                            {
                                bestLocation = location;
                            }
                        }
                        catch (Throwable t)
                        {
                            sendErrorLog(t);
                        }
                        if (bestLocation != null && bestLocation.getAccuracy() < goodEnoughAccuracy.longValue() && (currentTime -
                                (bestLocation.getTime()) / 1000) < maxAge.longValue() && getState() == State.RUNNING)
                        {

                            if (getState() == State.RUNNING)
                            {
                                endOfTimeSendData();
                                stop();
                            }

                        }
                    }
                    catch (Throwable t)
                    {
                        sendErrorLog(t);
                    }

                }
            };
        }
        else
        {
            Logger.e(getClass(), "Google Play Services are not connected");
        }
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        if (servicesConnected() && mLocationClient != null)
        {
            currentTime = TimeUtil.getTimestamp().longValue();
            mLocationClient.connect();
        }
        else
        {
            Logger.e(getClass(), "Location Client is not Enabled");
            stop();
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Logger.i(getClass(), "Location Client connected");
        startPeriodicUpdates();
    }

    @Override
    protected void secureOnStop()
    {
        super.secureOnStop();
        if (servicesConnected() && mLocationClient != null)
        {
            // If the client is connected
            if (mLocationClient.isConnected())
            {
                stopPeriodicUpdates();
                mLocationClient.disconnect();
            }
        }
        else
        {
            Logger.e(getClass(), "Location Client is not Enabled");
        }

    }

    /**
     * changed sent data to fit purpose! only lat long alt is sent
     */
    @Override
    protected void endOfTimeSendData()
    {
        if (bestLocation != null)
        {
            JsonObject data = new JsonObject();
            data.addProperty(Constants.LATITUDE, bestLocation.getLatitude());
            data.addProperty(Constants.LONGITUDE, bestLocation.getLongitude());
            data.addProperty(Constants.ALTITUDE, bestLocation.getAltitude());
            data.addProperty(Constants.ACCURACY, bestLocation.getAccuracy());
            //data.addProperty(Constants.PROVIDER, bestLocation.getProvider());
            data.addProperty(Constants.SPEED, bestLocation.getSpeed());
            //data.addProperty(Constants.TIME, bestLocation.getTime());

            Logger.i(getClass(), "Location Selected " + bestLocation);
            sendData(data);
        }
        else
        {
            Logger.e(getClass(), "Google Play Services Cant get Location");
        }

    }

    @Override
    public void onDisconnected()
    {
        mLocationClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Logger.e(getClass(), "Google Play Services Connection Failed");
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        if (servicesConnected())
        {
            bestLocation = null;
            currentTime = 0;
            mLocationRequest = null;
            locationListener = null;
        }
    }


    private void startPeriodicUpdates()
    {
        if (mLocationRequest != null && locationListener != null && mLocationClient != null && mLocationClient
                .isConnected())
        {
            Logger.i(getClass(), "Location Client Request Location Updates");
            mLocationClient.requestLocationUpdates(mLocationRequest, locationListener);
        }
    }

    private void stopPeriodicUpdates()
    {
        if (locationListener != null && mLocationClient != null && mLocationClient.isConnected())
        {
            Logger.i(getClass(), "Location Client Remove Location Updates");
            mLocationClient.removeLocationUpdates(locationListener);
        }
    }

    private boolean servicesConnected()
    {
        return (ConnectionResult.SUCCESS == GooglePlayServicesUtil.isGooglePlayServicesAvailable(getContext()));
    }

}
