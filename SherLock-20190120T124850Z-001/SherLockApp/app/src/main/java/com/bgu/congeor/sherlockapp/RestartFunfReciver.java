package com.bgu.congeor.sherlockapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.intentservices.CongeorDataService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by simondzn on 11/11/2015.
 */
public class RestartFunfReciver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences("INTERVAL", Context.MODE_PRIVATE);
        int numCall = sp.getInt("numCall", 0);
        numCall += 1;
        sp.edit().putInt("numCall", numCall).commit();
        if (numCall > 6) {
            String TAG = "Restart Reciver";
            Log.d(TAG, "Received restart broadcast!");
            Intent i = new Intent(context, CongeorDataService.class);
            context.stopService(i);
            context.startService(i);
            String probe = intent.getStringExtra("probe");
            Log.d(TAG, "Restarted the service");
            sp.edit().putInt("numCall", 0);
            try {
                FileWriter log = new FileWriter(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data/restart_log.txt"), true);
                log.write("Restarted at " + System.currentTimeMillis() + " Failed probe: " + probe + "\n");
                log.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
