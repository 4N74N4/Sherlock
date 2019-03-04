package com.bgu.congeor.sherlockapp.baseactivities;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.agent.sensors.manager.SensorManager;

/**
 * Created by clint on 12/23/13.
 */
public class BaseActivity extends Activity
{

    private static final String DOUBLE_LINE_SEP = "\n\n";
    private static final String SINGLE_LINE_SEP = "\n";
    private static final String EXTRA_CRASHED_FLAG = "CRUSH";


    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new GenericExceptionHandler());
        Thread.currentThread().setName(this.getClass().getCanonicalName());
    }

    public class GenericExceptionHandler implements Thread.UncaughtExceptionHandler
    {

        @Override
        public void uncaughtException(Thread thread, final Throwable ex)
        {
            String report = Utils.getReport(ex, getAddedInformation());
            /*Intent crashedIntent = new Intent(BaseActivity.this, CrushActivity.class);
            crashedIntent.putExtra(EXTRA_CRASHED_FLAG,  "Unexpected Error occurred.");
            crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            crashedIntent.putExtra("report", report);
            crashedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(crashedIntent);*/
            Logger.e(BaseActivity.this.getClass(), report);
            Utils.sendEmailAndExit(getApplicationContext(), report, Utils.getLogcatLog(getApplicationContext()), Utils.getEmail(getApplicationContext(), getApplication()));
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    protected String getAddedInformation()
    {
        StringBuilder str = new StringBuilder();
        try
        {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pInfo.versionName;
            String versionCode = "" + pInfo.versionCode;
            String sensorManagerVersion = SensorManager.getSensorsModuleVersion();
            str.append("Version Code: " + versionCode + "\n");
            str.append("Version Name: " + versionName + "\n");
            str.append("Sensor manager version : " + sensorManagerVersion + "\n");
        }
        catch (Throwable t)
        {

        }
        return str.toString();
    }
}