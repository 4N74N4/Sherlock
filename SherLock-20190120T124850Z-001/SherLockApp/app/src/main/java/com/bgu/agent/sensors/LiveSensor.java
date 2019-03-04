package com.bgu.agent.sensors;

import android.content.Intent;
import android.content.pm.PackageInfo;
import com.bgu.agent.commons.logging.Logger;
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

//Pipe line path:  com.bgu.agent.sensors.LiveSensor

@Probe.DisplayName("LiveSensor")
@Schedule.DefaultSchedule(interval = Constants.HOUR)
public class LiveSensor extends SecureBase
{

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        try
        {
            Logger.d(LiveSensor.class, "Sending broadacast");
            Intent intent = new Intent(Constants.FUNF_KEEP_ALIVE_ACTION);
            intent.addCategory(Constants.FUNF_AGENT_PACKAGE);
            getContext().sendBroadcast(intent);
            LinkedHashMap<String, String> sensorValues = new LinkedHashMap<String, String>();
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            sensorValues.put("appVersion", pInfo.versionName);
            JsonObject data = getGson().toJsonTree(sensorValues).getAsJsonObject();
            Logger.i(this.getClass(), "Adding UI Message " + data.toString());
            data.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
            sendData(data);
            stop();
        }
        catch (Throwable t)
        {
            sendErrorLog(t);
        }
    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }
}


