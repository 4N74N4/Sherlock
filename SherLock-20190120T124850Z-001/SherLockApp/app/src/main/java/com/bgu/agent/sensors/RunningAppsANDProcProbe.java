package com.bgu.agent.sensors;

/**
 * Created by BittonRon on 2/21/14.
 */


import android.app.ActivityManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.DisplayName;

import java.util.List;


// com.bgu.agent.sensors.RunningAppsANDProcProbe

@DisplayName("RunningAppsANDProcProbe")
@Schedule.DefaultSchedule(interval = 30 * Constants.MINUTE)
public class RunningAppsANDProcProbe extends SecureBase
{
    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        try
        {

            JsonObject data = new JsonObject();
            JsonArray processesData = new JsonArray();
            JsonArray runningAppData = new JsonArray();
            ActivityManager manager = (ActivityManager) getContext().getSystemService(getContext().ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
            List<ActivityManager.RunningTaskInfo> rTasks = manager.getRunningTasks(100);
            PackageManager packageManager = getContext().getPackageManager();

            for (ActivityManager.RunningAppProcessInfo pInfo : processes)
            {
                JsonObject pData = new JsonObject();
                pData.addProperty(Constants.PROC_NAME, pInfo.processName);
                pData.addProperty(Constants.PROC_ID, pInfo.pid);
                pData.addProperty(Constants.PROC_UID, pInfo.uid);


                processesData.add(pData);
            }
            for (ActivityManager.RunningTaskInfo rTask : rTasks)
            {
                PackageInfo packageInfo = null;

                packageInfo = packageManager.getPackageInfo(rTask.baseActivity.getPackageName(),
                        PackageManager.GET_META_DATA);

                JsonObject tData = new JsonObject();
                tData.addProperty(Constants.APP_NAME, packageInfo.applicationInfo.loadLabel(packageManager).toString());
                tData.addProperty(Constants.APP_PACKAGE, rTask.baseActivity.getPackageName());
                tData.addProperty(Constants.APP_UID, packageInfo.applicationInfo.uid);


                runningAppData.add(tData);
            }

            data.add(Constants.RUNNING_PROC, processesData);
            data.add(Constants.RUNNING_APPS, runningAppData);
            sendData(data);

        }
        catch (PackageManager.NameNotFoundException e)
        {
            Logger.e(getClass(), e.getMessage());
        }

        stop();

    }
}
