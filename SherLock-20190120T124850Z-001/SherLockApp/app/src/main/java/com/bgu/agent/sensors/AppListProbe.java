package com.bgu.agent.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.DisplayName;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;


// com.bgu.agent.sensors.AppListProbe

@DisplayName("AppListProbe")
@Schedule.DefaultSchedule(interval = Constants.WEEK)
public class AppListProbe extends SecureBase
{

    public static final long MEGA = 1024 * 1024;

    @Override
    protected void secureOnStart() {
        super.secureOnStart();
        SharedPreferences sp = getContext().getSharedPreferences("PREFERENCE", Context.MODE_PRIVATE);
        boolean isRun = sp.getBoolean("AppList", false);
        if (!isRun) {
            PackageManager packageManager = getContext().getPackageManager();//manager of all apps
            JsonArray appsData = new JsonArray(); // final jason

            List<ApplicationInfo> applicationsInfoList = packageManager.getInstalledApplications(PackageManager
                    .GET_META_DATA); //list of all apps

            for (ApplicationInfo app : applicationsInfoList) {
                JsonObject appData = new JsonObject(); //app jason
                try {
                    PackageInfo permInfo = packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS);
                    String[] appPerms = permInfo.requestedPermissions;

                    JsonArray permsData = new JsonArray();

                    if (appPerms != null) {
                        permsData = ((new Gson()).toJsonTree(appPerms)).getAsJsonArray();
                    }
                    PackageInfo appInfo = packageManager.getPackageInfo(app.packageName, packageManager.GET_SIGNATURES);
                    appData.addProperty(Constants.APP_NAME, app.loadLabel(packageManager).toString());
                    appData.addProperty(Constants.APP_PACKAGE, app.packageName);
                    appData.addProperty(Constants.APP_UID, app.uid);
                    appData.addProperty(Constants.INSTALL_SOURCE, packageManager.getInstallerPackageName(app.packageName));
                    appData.addProperty(Constants.VERSION_NAME, appInfo.versionName);
                    appData.addProperty(Constants.VERSION_CODE, appInfo.versionCode);
                    appData.addProperty(Constants.INSTALL_TIME, appInfo.firstInstallTime);
                    appData.addProperty(Constants.PACKAGE_HASH, getHashOfAPK(app.sourceDir));
                    appData.add(Constants.PERMISSIONS, permsData);


                    appsData.add(appData);
                } catch (Throwable t) {
                    Logger.e(getClass(), t.getMessage());
                }
            }
            JsonObject dataToSend = new JsonObject();
            dataToSend.add(Constants.VALUE, appsData);
            sendData(dataToSend);
            stop();
            sp.edit().putBoolean("AppList", true).commit();
        }
    }

    public static String getHashOfAPK(String apkPath)
    {
        String hash = Constants.NOT_AVAILABLE;
        try
        {
            File apkFile = new File(apkPath);
            InputStream in = new FileInputStream(apkFile);
            hash = Utils.hashWithSHA(in);
            in.close();
            apkFile = null;
            in = null;
        }
        catch (Throwable t)
        {
            android.util.Log.d("GetHash", t.getMessage());
        }
        return hash;
    }
}
