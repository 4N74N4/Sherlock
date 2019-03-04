package com.bgu.agent.sensors.EventBaseProbes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * Created by shedan on 27/01/2015.
 */
@Probe.DisplayName("UserPresentProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class UserPresentProbe extends SecureBase implements Probe.ContinuousProbe {
private BroadcastReceiver unlockScreen;
JsonArray unlockEvents;

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();

        IntentFilter filter = new IntentFilter(Constants.INTENT_ACTION_USER_PRESENT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        Log.e(this.getClass().toString(), "userPrestProbe has been enabled");
        unlockScreen = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                try
                {
                    Log.e(this.getClass().toString(), "userPrestProbe caught intent");
                    JsonObject data = new JsonObject();
                    long s = intent.getLongExtra(Constants.TIMESTAMP,0);

                    data.addProperty(Constants.TIMESTAMP, s);
                    //data.addProperty(Constants.ACTION, "Screen unlocked");
                    sendData(data);
                }
                catch (Throwable t)
                {
                    sendErrorLog(t);
                }
            }
        };
        getContext().registerReceiver(unlockScreen, filter);
        Log.e(this.getClass().toString(), "userPrestProbe registered receiver");
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().unregisterReceiver(unlockScreen);
        Log.e(this.getClass().toString(), "userPrestProbe Unregistered receiver");
    }


    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }


}
