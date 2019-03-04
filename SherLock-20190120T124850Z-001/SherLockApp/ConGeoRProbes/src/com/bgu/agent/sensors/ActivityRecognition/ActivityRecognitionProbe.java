package com.bgu.agent.sensors.ActivityRecognition;

/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/22/13
 * Time: 11:48 AM
 * To change this template use File | Settings | File Templates.
 */


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.ContinuousDataProbe;
import com.bgu.congeor.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;

import java.math.BigDecimal;

@Schedule.DefaultSchedule(interval = 2 * Constants.MINUTE, duration = 5 * Constants.SECOND)
@Probe.DisplayName("ActivityRecognitionProbe")

//Pipe line path:  com.bgu.agent.sensors.ActivityRecognition.ActivityRecognitionProbe


public class ActivityRecognitionProbe extends ContinuousDataProbe implements Probe.ContinuousProbe,
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener
{

    // receive from google activity recognition service
    public BroadcastReceiver activityRecognitionDataReceiver;

    @Configurable
    private BigDecimal sensorInterval = BigDecimal.valueOf(1);

    private ActivityRecognitionClient mActivityRecognitionClient;

    private PendingIntent callbackIntent;

    private Intent activityRecognitionIntent;

    private double[] activityCounter;

    public ActivityRecognitionProbe()
    {
        super();
        maxWaitTime = BigDecimal.valueOf(6 * Constants.SECOND);
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();

        activityCounter = new double[6];

        if (mActivityRecognitionClient != null && callbackIntent != null && mActivityRecognitionClient.isConnected()
                && mActivityRecognitionClient.isConnectionCallbacksRegistered(this))
        {
            mActivityRecognitionClient.removeActivityUpdates(callbackIntent);
            callbackIntent = null;
            activityRecognitionIntent = null;
        }
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getContext()) == ConnectionResult.SUCCESS)
        {
            // initialize of the google activity recognition client
            mActivityRecognitionClient = new ActivityRecognitionClient(this.getContext(), this, this);

            // register the activityRecognitionDataReceiver for action from the activity recognition service
            IntentFilter filter = new IntentFilter("com.bgu.congeor.sensors.ActivityRecognition.Data");
            filter.addCategory(Intent.CATEGORY_DEFAULT);

            activityRecognitionDataReceiver = new BroadcastReceiver()
            {
                //@Override
                public void onReceive(Context context, Intent intent)
                {
                    try
                    {
                        if (getState() != State.RUNNING)
                        {
                            Logger.i(ActivityRecognitionProbe.class, "Get event while not running");
                            try
                            {
                                getContext().unregisterReceiver(this);
                                if (activityRecognitionDataReceiver == this)
                                {
                                    activityRecognitionDataReceiver = null;
                                }

                            }
                            catch (Throwable t)
                            {
                                Logger.e(ActivityRecognitionProbe.class, "State is not RUNNING, We getting events but there is no receiver");
                            }
                            return;
                        }

                        if (ActivityRecognitionResult.hasResult(intent))
                        {

                            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

                            if (result != null && activityCounter != null)
                            {


                                activityCounter[0] += (result.getActivityConfidence(0) / 100.0);
                                activityCounter[1] += (result.getActivityConfidence(1) / 100.0);
                                activityCounter[2] += (result.getActivityConfidence(2) / 100.0);
                                activityCounter[3] += (result.getActivityConfidence(3) / 100.0);
                                activityCounter[4] += (result.getActivityConfidence(4) / 100.0);
                                activityCounter[5] += (result.getActivityConfidence(5) / 100.0);


                                Logger.d(ActivityRecognitionProbe.class, "Detect activity:\n" +
                                        ActivityType.getType(0) + ":" + result.getActivityConfidence(0)+ "\n" +
                                        ActivityType.getType(1) + ":" + result.getActivityConfidence(1)+ "\n" +
                                        ActivityType.getType(2) + ":" + result.getActivityConfidence(2)+ "\n" +
                                        ActivityType.getType(3) + ":" + result.getActivityConfidence(3)+ "\n" +
                                        ActivityType.getType(4) + ":" + result.getActivityConfidence(5)+ "\n" +
                                        ActivityType.getType(5) + ":" + result.getActivityConfidence(4)+ "\n"
                                );
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        sendErrorLog(t);
                    }
                }

            };

            if (activityRecognitionDataReceiver != null && filter != null)
            {
                getContext().registerReceiver(activityRecognitionDataReceiver, filter);
            }

            Logger.d(getClass(), "Activity Recognition Probe enabled");
        }
        else
        {
            Logger.e(getClass(), "Activity Recognition Probe Cant enabled, cant locate Google Play services on device");
            disable();
        }

    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        if (mActivityRecognitionClient != null)
        {
            mActivityRecognitionClient.connect();
            Logger.d(getClass(), "Activity Recognition Probe connect to Google Play services");
        }

    }

    @Override
    protected void secureOnStop()
    {
        super.secureOnStop();
        if (mActivityRecognitionClient != null && callbackIntent != null && mActivityRecognitionClient.isConnected()
                && mActivityRecognitionClient.isConnectionCallbacksRegistered(this))
        {
            mActivityRecognitionClient.removeActivityUpdates(callbackIntent);
        }
        activityCounter = null;
        callbackIntent = null;
        activityRecognitionIntent = null;

        Logger.d(getClass(), "Activity Recognition Probe remove updates from Google Play services");
    }

    @Override
    protected void endOfTimeSendData()
    {
        JsonObject data = new JsonObject();

        double[] activityExpectation = calculateActivityExpectations();
        data.addProperty(Constants.ACTIVITY, ActivityType.getType(getTheMostCommonActivity(activityExpectation)).toString
                ());
        for (int i = 0; i < activityExpectation.length ; i++)
        {
            data.addProperty(ActivityType.getType(i).toString(),activityExpectation[i]);
        }

        Logger.d(ActivityRecognitionProbe.class, "Sending Data:\n" +
                ActivityType.getType(0) + ":" + activityExpectation[0]+ "\n" +
                ActivityType.getType(1) + ":" + activityExpectation[1]+ "\n" +
                ActivityType.getType(2) + ":" + activityExpectation[2]+ "\n" +
                ActivityType.getType(3) + ":" + activityExpectation[3]+ "\n" +
                ActivityType.getType(4) + ":" + activityExpectation[4]+ "\n" +
                ActivityType.getType(5) + ":" + activityExpectation[5]+ "\n"
        );
        sendData(data);
        Logger.d(getClass(), data.toString());
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        if (mActivityRecognitionClient != null)
        {
            mActivityRecognitionClient.disconnect();

            mActivityRecognitionClient = null;
            if (activityRecognitionDataReceiver != null)
            {
                try
                {
                    getContext().unregisterReceiver(activityRecognitionDataReceiver);
                }
                catch (Throwable t)
                {
                    Logger.e(getClass(), t.getMessage());
                }
            }
            activityRecognitionDataReceiver = null;
            Logger.d(getClass(), "Activity Recognition Probe disconnect from Google Play services");
        }
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        activityRecognitionIntent = new Intent(this.getContext(), ActivityRecognitionService.class);
        callbackIntent = PendingIntent.getService(this.getContext(), 0, activityRecognitionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (mActivityRecognitionClient != null)
        {
            mActivityRecognitionClient.requestActivityUpdates(sensorInterval.longValue() * 500, callbackIntent);
        }
        else
        {
            onConnectionFailed();
        }

        Logger.d(getClass(), "Activity Recognition Probe request updates from Google Play services");
    }

    @Override
    public void onDisconnected()
    {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        onConnectionFailed();
    }

    public void onConnectionFailed()
    {
        if (getState() == State.RUNNING)
        {
            stop();
        }
        else if (getState() == State.ENABLED)
        {
            disable();
        }
    }

    private int getTheMostCommonActivity(double[] ac)
    {
        double max = 0;
        int maxID = 4;

        if (ac != null)
        {
            max = ac[0];
        }
        else
        {
            return ActivityType.UNKNOWN.value;
        }

        for (int i = 0; i < ac.length; i++)
        {
            if (max < ac[i])
            {
                max = ac[i];
                maxID = i;

            }
        }

        return maxID;
    }

    private double[] calculateActivityExpectations()
    {
        double[] activityExpectation = new double[6];
        double sum = 0;
        for(int i = 0; i < activityCounter.length ; i++)
        {
            sum += activityCounter[i];
        }

        for(int i = 0; i < activityCounter.length ; i++)
        {
            activityExpectation[i] = activityCounter[i]/sum;
        }

        return activityExpectation;
    }

    private enum ActivityType
    {
        IN_VEHICLE(0), ON_BICYCLE(1), ON_FOOT(2), STILL(3), UNKNOWN(4), TILTING(5);

        private int value;

        ActivityType(int value)
        {
            this.value = value;
        }

        static ActivityType getType(int type)
        {
            switch (type)
            {
                case 0:
                    return ActivityType.IN_VEHICLE;
                case 1:
                    return ActivityType.ON_BICYCLE;
                case 2:
                    return ActivityType.ON_FOOT;
                case 3:
                    return ActivityType.STILL;
                case 4:
                    return ActivityType.UNKNOWN;
                case 5:
                    return ActivityType.TILTING;
            }
            return ActivityType.UNKNOWN;
        }
    }


}




