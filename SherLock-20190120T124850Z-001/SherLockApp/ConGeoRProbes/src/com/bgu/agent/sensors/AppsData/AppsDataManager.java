package com.bgu.agent.sensors.AppsData;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.datalayer.BandwidthData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shedan on 17/09/2014.
 */
public class AppsDataManager extends BroadcastReceiver implements IAppsDataManager{

    SQLiteHelper sqLiteHelper;

    public AppsDataManager(Context context)
    {
        sqLiteHelper = new SQLiteHelper(context);
    }
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Logger.i((( Object ) this ).getClass(),"Apps Data base has been updated");
        UpdateApps(context);
    }

    @Override
    public void InitializeData(Context context) {
        final Long[] init = {Long.valueOf(-1),Long.valueOf(-1),Long.valueOf(-1),Long.valueOf(-1)};
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo p : packages)
        {
            sqLiteHelper.addApp(p.packageName ,init);
        }
    }

    @Override
    public BandwidthData getData(String packageName) {
        Long[] res = sqLiteHelper.getApp(packageName);
        if(res!=null){
            BandwidthData ans = new BandwidthData(res);
            return ans;
        }
        else{

            return null;
        }

    }

    @Override
    public void DeleteApp(String packageName) {
        sqLiteHelper.RemoveApp(packageName);
    }

    @Override
    public void AddApp(String packageName, BandwidthData band) {
        sqLiteHelper.addApp(packageName, band.toArray());
    }

    @Override
    public void UpdateApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> previous = sqLiteHelper.getAllApps();
        List<String> current = new ArrayList<String>();
        for (ApplicationInfo p : packages)
        {
            current.add(p.packageName);
        }
        current.removeAll(previous);
        final Long[] init = {Long.valueOf(-1),Long.valueOf(-1),Long.valueOf(-1),Long.valueOf(-1)};
        for(String p : current)
        {
            sqLiteHelper.addApp(p , init);
        }
    }
    public void UpdateApp(String pname , BandwidthData bd)
    {
        sqLiteHelper.updateApp(pname , bd.toArray());
    }

}
