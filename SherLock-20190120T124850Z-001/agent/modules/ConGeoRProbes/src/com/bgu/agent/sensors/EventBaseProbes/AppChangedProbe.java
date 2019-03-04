package com.bgu.agent.sensors.EventBaseProbes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.AppListProbe;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;


/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/27/13
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */

//Pipe line path:  com.bgu.agent.sensors.EventBaseProbes.AppChangedProbe

@Probe.DisplayName("AppChangedProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class AppChangedProbe extends SecureBase implements Probe.ContinuousProbe
{

    private BroadcastReceiver appReceiver;


    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();

        IntentFilter filter = new IntentFilter(Constants.INTENT_ACTION_APPINFO);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        appReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                try
                {
                    JsonObject data = new JsonObject();
String packageName = intent.getStringExtra(Constants.APP_NAME);
                    data.addProperty(Constants.APP_PACKAGE, packageName);
                    data.addProperty(Constants.ACTION, intent.getStringExtra(Constants.ACTION));
                    if (!intent.getStringExtra(Constants.ACTION).equals("Removed")){
                        try {
                            PackageManager packageManager = getContext().getPackageManager();
                            ApplicationInfo app = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                            PackageInfo permInfo = packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS);
                            String[] appPerms = permInfo.requestedPermissions;

                            JsonArray permsData = new JsonArray();

                            if (appPerms != null) {
                                permsData = ((new Gson()).toJsonTree(appPerms)).getAsJsonArray();
                            }
                            PackageInfo appInfo = packageManager.getPackageInfo(app.packageName, packageManager.GET_SIGNATURES);
                            data.addProperty(Constants.APP_NAME, app.loadLabel(packageManager).toString());
                            data.addProperty(Constants.APP_PACKAGE, app.packageName);
                            data.addProperty(Constants.APP_UID, app.uid);
                            data.addProperty(Constants.INSTALL_SOURCE, packageManager.getInstallerPackageName(app.packageName));
                            data.addProperty(Constants.VERSION_NAME, appInfo.versionName);
                            data.addProperty(Constants.VERSION_CODE, appInfo.versionCode);
                            data.addProperty(Constants.INSTALL_TIME, appInfo.firstInstallTime);
                            data.addProperty(Constants.PACKAGE_HASH, AppListProbe.getHashOfAPK(app.sourceDir));
                            data.add(Constants.PERMISSIONS, permsData);

                        } catch (Throwable t) {
                            Logger.e(getClass(), t.getMessage());
                        }
                    }
                    sendData(data);
                }
                catch (Throwable t)
                {
                    sendErrorLog(t);
                }
            }
        };
        getContext().registerReceiver(appReceiver, filter);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().unregisterReceiver(appReceiver);
    }


    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }


}


