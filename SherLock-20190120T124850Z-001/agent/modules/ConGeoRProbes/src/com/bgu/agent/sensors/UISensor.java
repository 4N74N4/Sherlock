package com.bgu.agent.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.time.TimeUtil;

import java.util.LinkedHashMap;


/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/27/13
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */

//Pipe line path:  com.bgu.agent.sensors.UISensor

@Probe.DisplayName("UISensor")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class UISensor extends SecureBase implements Probe.ContinuousProbe
{

    private BroadcastReceiver uiReceiver;


    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        IntentFilter filter = new IntentFilter(Constants.UI_MESSAGE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        uiReceiver = new UIBroadcastReceiver();
        getContext().registerReceiver(uiReceiver, filter);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().unregisterReceiver(uiReceiver);
    }


    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }

    public class UIBroadcastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Bundle extras = intent.getExtras();
                LinkedHashMap<String, Object> uiValues = new LinkedHashMap<String, Object>();
                for (String key : extras.keySet())
                {
                    uiValues.put(key, extras.get(key));
                }
                JsonObject data = getGson().toJsonTree(uiValues).getAsJsonObject();
                data.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
                sendData(data);
            }
            catch (Throwable t)
            {
                sendErrorLog(t);
            }
        }
    }
}


