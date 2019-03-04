package com.bgu.congeor.sherlockapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.bgu.agent.sensors.MoriartyProbe;

/**
 * Created by simondzn on 14/06/2016.
 */
public class GpsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(MoriartyProbe.class.toString(), "recived intent");
        Intent i = new Intent(context, GpsDialog.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
