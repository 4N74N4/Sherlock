package com.bgu.congeor.sherlockapp.intentservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by clint on 1/25/14.
 */
public class AgentBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.hasExtra("FLAG")) {
            Intent i = new Intent(context, CongeorDataService.class);
            i.putExtra("FLAG", "WAKE_UP");
            context.startService(i);
            SharedPreferences sp = context.getSharedPreferences("INTERVAL", Context.MODE_PRIVATE);
            sp.edit().putInt("numCall", 0).commit();
        } else {
            Intent i = new Intent(context, CongeorDataService.class);
            i.putExtra("FLAG", intent.getStringExtra("FLAG"));
            context.startService(i);
        }
    }
}
