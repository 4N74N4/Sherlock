package com.bgu.agent.sensors;

import android.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.LocationProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

import java.math.BigDecimal;


// com.bgu.agent.sensors.SimpleLocationProbe;

@Probe.DisplayName("Simple Location Probe")
@Probe.RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION})
@Probe.RequiredFeatures("android.hardware.location")
@Schedule.DefaultSchedule(interval = 1800)
@Probe.RequiredProbes(LocationProbe.class)
public class SimpleLocationProbe extends Probe.Base implements Probe.PassiveProbe, ProbeKeys.LocationKeys
{

    @Configurable
    private BigDecimal maxWaitTime = BigDecimal.valueOf(20);

    @Configurable
    private BigDecimal maxAge = BigDecimal.valueOf(900);

    @Configurable
    private BigDecimal goodEnoughAccuracy = BigDecimal.valueOf(80);

    @Configurable
    private boolean useGps = true;

    @Configurable
    private boolean useNetwork = true;


    private LocationProbe locationProbe;

    private BigDecimal startTime;

    private IJsonObject bestLocation;

    private BigDecimal age;

    private BigDecimal bestLocationAge;

    private Runnable sendLocationRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            sendCurrentBestLocation();
        }
    };

    private DataListener listener = new DataListener()
    {

        @Override
        public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data)
        {
            Log.d(LogUtil.TAG, "SimpleLocationProbe received data: " + data.toString());
            if (startTime == null)
            {
                startTime = TimeUtil.getTimestamp();
                getHandler().postDelayed(sendLocationRunnable, TimeUtil.secondsToMillis(maxWaitTime));
            }
            if (isBetterThanCurrent(data))
            {
                Log.d(LogUtil.TAG, "SimpleLocationProbe evaluated better location.");
                bestLocation = data;
                bestLocationAge = startTime.subtract(bestLocation.get("mTime").getAsBigDecimal().divide(new
                        BigDecimal(1000)));
            }
            if (goodEnoughAccuracy != null && bestLocationAge.doubleValue() < maxAge.doubleValue() && bestLocation
                    .get(ACCURACY).getAsDouble() < goodEnoughAccuracy.doubleValue())
            {
                Log.d(LogUtil.TAG, "SimpleLocationProbe evaluated good enough location.");
                if (getState() == State.RUNNING)
                {
                    stop();
                }
                else if (getState() == State.ENABLED)
                { // Passive listening
                    // TODO: do we want to prematurely end this, or wait for the full duration
                    // Things to consider:
                    // - the device falling to sleep before we send
                    // - too much unrequested data if we send all values within accuracy limits
                    // (this will restart immediately if more passive data continues to come in)
                    getHandler().removeCallbacks(sendLocationRunnable);
                    sendCurrentBestLocation();
                }
            }

        }

        @Override
        public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint)
        {
        }
    };

    private void sendCurrentBestLocation()
    {
        Log.d(LogUtil.TAG, "SimpleLocationProbe sending current best location.");
        if (bestLocation != null)
        {
            JsonObject data = bestLocation.getAsJsonObject();
            data.remove(PROBE); // Remove probe so that it fills with our probe name
            sendData(data);
        }
        startTime = null;
        bestLocation = null;
        age = null;
        bestLocationAge = null;
    }

    private boolean isBetterThanCurrent(IJsonObject newLocation)
    {

        if (bestLocation == null)
        {
            return true;
        }

        age = startTime.subtract(newLocation.get("mTime").getAsBigDecimal().divide(new BigDecimal(1000)));
        bestLocationAge = startTime.subtract(bestLocation.get("mTime").getAsBigDecimal().divide(new BigDecimal(1000)));

        if (age.doubleValue() < maxAge.doubleValue() && bestLocationAge.doubleValue() < maxAge.doubleValue())
        {
            return bestLocation.get(ACCURACY).getAsDouble() > newLocation.get(ACCURACY).getAsDouble();
        }

        else if (age.doubleValue() < bestLocationAge.doubleValue())
        {
            return true;
        }

        return false;
    }

    @Override
    protected void onEnable()
    {
        super.onEnable();
        JsonObject config = new JsonObject();
        if (!useGps)
        {
            config.addProperty("useGps", false);
        }
        if (!useNetwork)
        {
            config.addProperty("useNetwork", false);
        }
        locationProbe = getGson().fromJson(config, LocationProbe.class);
        locationProbe.registerPassiveListener(listener);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.d(LogUtil.TAG, "SimpleLocationProbe starting, registering listener");
        startTime = TimeUtil.getTimestamp();
        locationProbe.registerListener(listener);
        getHandler().sendMessageDelayed(getHandler().obtainMessage(STOP_MESSAGE),
                TimeUtil.secondsToMillis(maxWaitTime));
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.d(LogUtil.TAG, "SimpleLocationProbe stopping");
        getHandler().removeMessages(STOP_MESSAGE);
        locationProbe.unregisterListener(listener);
        sendCurrentBestLocation();
    }

    @Override
    protected void onDisable()
    {
        super.onDisable();
        locationProbe.unregisterPassiveListener(listener);
    }


}
