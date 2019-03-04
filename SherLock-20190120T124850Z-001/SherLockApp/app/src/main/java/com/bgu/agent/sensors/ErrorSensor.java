package com.bgu.agent.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import com.kristijandraca.backgroundmaillibrary.BackgroundMail;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.time.TimeUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;


/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/27/13
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */

//Pipe line path:  com.bgu.agent.sensors.ErrorSensor

@Probe.DisplayName("ErrorSensor")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class ErrorSensor extends SecureBase implements Probe.ContinuousProbe
{

    public static final String ERROR_IN_SENSOR = "errorInSensor";

    public static final String THROWABLE_MESSAGE = "errorMessage";

    private BroadcastReceiver errorReceiver;

    public static Intent generateErrorIntent(Class probeClass, Throwable t)
    {
        String message = "";
        if (t != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            pw.close();
            message = sw.toString();
        }
        Intent errorIntent = new Intent();
        errorIntent.addCategory(Intent.CATEGORY_DEFAULT);
        errorIntent.setAction(Constants.ERROR_MESSAGE);
        errorIntent.putExtra(THROWABLE_MESSAGE, message);
        errorIntent.putExtra(ERROR_IN_SENSOR, probeClass.getCanonicalName());
        return errorIntent;
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        IntentFilter filter = new IntentFilter(Constants.ERROR_MESSAGE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        errorReceiver = new ErrorBroadcastReceiver();
        getContext().registerReceiver(errorReceiver, filter);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().unregisterReceiver(errorReceiver);
    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }

    public class ErrorBroadcastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Bundle extras = intent.getExtras();
                LinkedHashMap<String, String> errorValues = new LinkedHashMap<String, String>();
                for (String key : extras.keySet())
                {
                    errorValues.put(key, extras.getString(key));
                }
                JsonObject data = getGson().toJsonTree(errorValues).getAsJsonObject();
                Logger.d(this.getClass(), "Adding Error Message " + data.toString());


                Configuration privateCommConfiguration = null;
                Configuration hashedUserData = null;
                try {
                    hashedUserData = Configuration.loadLocalConfiguration(getContext(), Constants.CONF_USER_DATA_HASHED, false);
                    privateCommConfiguration = Configuration.loadLocalConfiguration(getContext(), Constants.CONF_PRIVATE_COMM_FILENAME, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String experimentId = hashedUserData.getKeyAsString(Constants.HASHED_MAIL);
                String version = privateCommConfiguration.getKeyAsString(Constants.VERSION);
                BackgroundMail bm = new BackgroundMail(getContext());
                final BackgroundMail sendA;
                bm.setFormSubject("Error from User " + experimentId.substring(0, 9));
                bm.setFormBody("User " + experimentId +"\n" +"Installed version: " + version + "\n Error: " + data.toString());
                bm.setProcessVisibility(false);
                sendA = bm;
                Runnable runnable = new Runnable() {

                    public void run() {
                        sendA.secureSend(sendA);

                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
                Log.d(ErrorSensor.class.toString(), "Error Mail has been sent!");

                data.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
                sendData(data);
            }
            catch (Throwable t)
            {
                Logger.e(getClass(), t.getMessage());
            }
        }
    }
}


