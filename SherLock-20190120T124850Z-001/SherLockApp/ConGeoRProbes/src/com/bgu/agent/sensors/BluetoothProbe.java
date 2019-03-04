package com.bgu.agent.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.*;
import edu.mit.media.funf.time.TimeUtil;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@DisplayName("Nearby Bluetooth Devices Probe")
@DefaultSchedule(
        interval = 300.0D
)
@RequiredFeatures({"android.hardware.bluetooth"})
@RequiredPermissions({"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"})
public class BluetoothProbe extends Base implements PassiveProbe {
    @Configurable
    private BigDecimal maxScanTime = BigDecimal.valueOf(30.0D);
    ConcurrentLinkedQueue<JsonObject> bluetoothData;
    private BluetoothAdapter adapter;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.device.action.FOUND")) {
                JsonObject bluetoothPoint = BluetoothProbe.this.getGson().toJsonTree(intent.getExtras()).getAsJsonObject();
                if (bluetoothPoint != null)
                    Logger.i(BluetoothProbe.class, "access point is added to list");
                bluetoothData.add(bluetoothPoint);
            } else if (action.equals("android.bluetooth.adapter.action.DISCOVERY_FINISHED")) {
                endOfScanSendData();
                BluetoothProbe.this.stop();
            }

        }
    };

    private void endOfScanSendData() {
        Logger.i(BluetoothProbe.class, "send data is called");
        if (bluetoothData.isEmpty() || bluetoothData == null) {
            Logger.i(BluetoothProbe.class, "No data");
            JsonObject temp = new JsonObject();
            temp.addProperty("data", "no data");
            BluetoothProbe.this.sendData(temp);
        } else {
            JsonArray jsonArray = new JsonArray();
            for (JsonObject temp : bluetoothData) {
                jsonArray.add(temp);
            }
            Logger.i(BluetoothProbe.class, "before send data");
            JsonObject data = new JsonObject();
            data.add("data", jsonArray);
            sendData(data);
        }
    }


    private BroadcastReceiver stateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int newState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 10);
            if (newState == 12) {
                try {
                    BluetoothProbe.this.startDiscovery();
                    BluetoothProbe.this.getContext().unregisterReceiver(this);
                } catch (IllegalArgumentException var5) {

                }
            }

        }
    };
    private boolean shouldDisableOnFinish = false;

    public BluetoothProbe() {
    }

    private void startDiscovery() {
        if (this.adapter.isEnabled()) {
            this.adapter.startDiscovery();
        } else {
            this.shouldDisableOnFinish = true;
            this.getContext().registerReceiver(this.stateChangedReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
            this.adapter.enable();
        }

    }

    protected void onEnable() {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        if (this.adapter.isEnabled()) {
            this.shouldDisableOnFinish = false;
            Log.i(BluetoothProbe.class.toString(), " HAHA bluetooth will not  be Disabled");
        } else {
            this.shouldDisableOnFinish = true;
            Log.i(BluetoothProbe.class.toString(), " HAHA bluetooth will be Disabled");
        }
        if (this.maxScanTime != null) {
            this.getHandler().sendMessageDelayed(this.getHandler().obtainMessage(3), TimeUtil.secondsToMillis(this.maxScanTime));
        }
        IntentFilter intentFilter = new IntentFilter("android.bluetooth.device.action.FOUND");
        intentFilter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        this.getContext().registerReceiver(this.receiver, intentFilter);
    }

    protected void onStart() {
        super.onStart();
        bluetoothData = new ConcurrentLinkedQueue<>();
        this.startDiscovery();


    }

    protected void onStop() {
        super.onStop();
        bluetoothData = null;
        this.getHandler().removeMessages(3);

        try {
            this.getContext().unregisterReceiver(this.stateChangedReceiver);
        } catch (IllegalArgumentException var2) {
            ;
        }

        if (this.adapter.isDiscovering()) {
            this.adapter.cancelDiscovery();
        }

        if (this.shouldDisableOnFinish == true) {
            this.adapter.disable();
            Log.i(BluetoothProbe.class.toString(), " HAHA bluetooth has been Disabled");
            this.shouldDisableOnFinish = true;
        }

    }

    protected void onDisable() {
        try {
            this.getContext().unregisterReceiver(this.receiver);
        } catch (IllegalArgumentException var2) {
            Log.w("Funf", this.getClass().getName() + "Broadcast receiver not registered.", var2);
        }

    }

    private boolean checkIfBluetoothPaired() {
        Set<BluetoothDevice> pairedDEvices = this.adapter.getBondedDevices();
        return (pairedDEvices.size() > 0) ? true : false;
    }
}
