package com.bgu.congeor.sherlockapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by simondzn on 26/06/2016.
 */
public class DataDialogReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("data notification", "recived broadcast");
        Intent i = new Intent(context.getApplicationContext(), DataDialog.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
