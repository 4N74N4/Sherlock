package com.bgu.agent.sensors.EventBaseProbes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import com.kristijandraca.backgroundmaillibrary.R;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by simondzn on 20/10/2015.
 */

@Probe.DisplayName("AllBroadcasts")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class AllBroadcastProbe extends SecureBase implements Probe.ContinuousProbe {
    private BroadcastReceiver allBroadcast;
    String TAG = AllBroadcastProbe.class.getSimpleName();


    @Override
    protected void secureOnEnable() {
        super.secureOnEnable();
        allBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    JsonObject data = new JsonObject();
                        data.addProperty("Extras",getExtrasString(intent));
                    data.addProperty(Constants.ACTION, intent.getAction());
                    Log.d(AllBroadcastProbe.class.toString(), "Sending data...(han)");
                    sendData(data);
                } catch (Throwable t) {
                    sendErrorLog(t);
                }
            }
        };
        registerAnyBroadcastReceiver();
    }

    @Override
    protected void secureOnDisable() {
        super.secureOnDisable();
        getContext().unregisterReceiver(allBroadcast);
    }


    private void registerAnyBroadcastReceiver() {
        try {
            registerBroadcastReceiverForActions();
            registerBroadcastReceiverForActionsWithDataType();
            registerBroadcastReceiverForActionsWithSchemes();
            Log.d(TAG, "Registered receivers.");

        } catch (Exception e) {
            Log.d(TAG, "Exception while registering: " + e.getMessage());
        }
    }

    private void registerBroadcastReceiverForActions() {
        IntentFilter intentFilter = new IntentFilter();
        addAllKnownActions(intentFilter);
        getContext().registerReceiver(allBroadcast, intentFilter);
    }

    /**
     * @throws IntentFilter.MalformedMimeTypeException
     */
    private void registerBroadcastReceiverForActionsWithDataType()
            throws IntentFilter.MalformedMimeTypeException {
        IntentFilter intentFilter = new IntentFilter();

        // This needed for broadcasts like new picture, which is data type: "image/*"
        intentFilter.addDataType("*/*");

        addAllKnownActions(intentFilter);
        getContext().registerReceiver(allBroadcast, intentFilter);
    }

    private void registerBroadcastReceiverForActionsWithSchemes() throws IntentFilter.MalformedMimeTypeException {
        IntentFilter intentFilter = new IntentFilter();

        // needed for uninstalls
        intentFilter.addDataScheme("package");

        // needed for file system mounts
        intentFilter.addDataScheme("file");

        // other schemes
        intentFilter.addDataScheme("geo");
        intentFilter.addDataScheme("market");
        intentFilter.addDataScheme("http");
        intentFilter.addDataScheme("tel");
        intentFilter.addDataScheme("mailto");
        intentFilter.addDataScheme("about");
        intentFilter.addDataScheme("https");
        intentFilter.addDataScheme("ftps");
        intentFilter.addDataScheme("ftp");
        intentFilter.addDataScheme("javascript");

        addAllKnownActions(intentFilter);
        getContext().registerReceiver(allBroadcast, intentFilter);
    }

    /**
     * Since we don't want to filter which actions have data and which don't we register two
     * different receivers with full list of actions.
     *
     * @param pIntentFilter
     */
    private void addAllKnownActions(IntentFilter pIntentFilter) {
        // System Broadcast
        List<String> sysBroadcasts = Arrays.asList(getContext().getResources().getStringArray(R.array.system_broadcast));
        for (String sysBroadcast : sysBroadcasts) {
            pIntentFilter.addAction(sysBroadcast);
        }

        //Custom Broadcast.
        List<String> customBroadcasts = Arrays.asList(getContext().getResources().getStringArray(R.array.system_broadcast));
        for (String customBroadcast : customBroadcasts) {
            pIntentFilter.addAction(customBroadcast);
        }
    }
    private String getExtrasString(Intent pIntent) {
        String extrasString = "";
        Bundle extras = pIntent.getExtras();
        try {
            if (extras != null) {
                Set<String> keySet = extras.keySet();
                for (String key : keySet) {
                    try {
                        String extraValue = pIntent.getExtras().get(key).toString();
                        extrasString += key + ": " + extraValue + "\n";
                    } catch (Exception e) {
                        Log.d(TAG, "Exception 2 in getExtrasString(): " + e.toString());
                        extrasString += key + ":Null" + "\n";
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception in getExtrasString(): " + e.toString());
            extrasString += "Exception:" + e.getMessage() + "\n";
        }
        Log.d(TAG, "extras=" + extrasString);
        return extrasString;
    }
}
