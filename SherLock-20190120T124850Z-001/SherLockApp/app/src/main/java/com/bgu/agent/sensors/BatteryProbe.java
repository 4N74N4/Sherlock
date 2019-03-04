package com.bgu.agent.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import org.json.JSONObject;

/**
 * Created by shedan on 25/03/2015.
 */
@Schedule.DefaultSchedule(
        interval = 300.0D
)
@Probe.RequiredPermissions({"android.permission.BATTERY_STATS"})
public class BatteryProbe extends SecureBase implements Probe.PassiveProbe {
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                JsonObject batData = new JsonObject();
                batData =  BatteryProbe.this.getGson().toJsonTree(intent.getExtras()).getAsJsonObject();
                if(batData!=null)
                    BatteryProbe.this.sendData(batData);
                BatteryProbe.this.stop();
            }

        }
    };

    public BatteryProbe() {
    }

    protected void secureOnStart() {

        this.getContext().registerReceiver(this.receiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
    }

    protected void secureOnStop() {

        this.getContext().unregisterReceiver(this.receiver);
    }
}