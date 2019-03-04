package com.bgu.agent.sensors.AppsData;

import android.content.Context;
import com.bgu.agent.sensors.datalayer.BandwidthData;

/**
 * Created by shedan on 17/09/2014.
 */
public interface IAppsDataManager
{

    public void InitializeData(Context context);
    public BandwidthData getData(String packageName);
    public void DeleteApp(String packageName);
    public void AddApp(String packageName, BandwidthData band);
    public void UpdateApps(Context context);
}
