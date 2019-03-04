package com.bgu.agent.sensors;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

@RequiredPermissions({"android.permission.ACCESS_WIFI_STATE", "android.permission.CHANGE_WIFI_STATE"})
@RequiredFeatures({"android.hardware.wifi"})
@DisplayName("Nearby Wifi Devices Probe")
public class WifiProbe extends Base {
    public static final String TSF = "tsf";
    private static final String LOCK_KEY = WifiProbe.class.getName();
    private WifiManager wifiManager;
    private int numberOfAttempts;
    private int previousWifiState;
    private WifiLock wifiLock;
    private boolean hotSpotOn;
    private BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction())) {
                List results = WifiProbe.this.wifiManager.getScanResults();
                if (results != null) {
                    Gson gson = WifiProbe.this.getGson();
                    JsonArray wifiArr = new JsonArray();
                    JsonObject data = new JsonObject();
                    JsonObject temp;
                    for (Iterator i$ = results.iterator(); i$.hasNext(); ) {
                        temp = new JsonObject();
                        ScanResult result = (ScanResult) i$.next();
                        temp.addProperty(Constants.SSID, result.SSID);
                        temp.addProperty(Constants.BSSID , result.BSSID);
                        temp.addProperty("capabilities", result.capabilities);
                        temp.addProperty("freq", result.frequency);
                        temp.addProperty("lvl", result.level);
                        wifiArr.add(temp);
//                        data = gson.toJsonTree(result).getAsJsonObject();
//                        if(data.has("timestamp")) {
//                            JsonElement el = data.remove("timestamp");
//                            data.add("tsf", el);
//                        }
                    }
                    data.add("data", wifiArr);
                    WifiProbe.this.sendData(data);
                }

                if (WifiProbe.this.getState() == State.RUNNING) {
                    WifiProbe.this.stop();
                }
            }

        }
    };
    private BroadcastReceiver waitingToStartScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent i) {
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(i.getAction())) {
                try {
                    WifiProbe.this.getContext().unregisterReceiver(this);
                    WifiProbe.this.saveWifiStateAndRunScan();
                } catch (IllegalArgumentException var4) {
                    Log.e("Funf", "Unregistered WIFIE_STATE_CHANGED receiver more than once.");
                }
            }

        }
    };
    private BroadcastReceiver retryScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent i) {
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(i.getAction())) {
                try {
                    WifiProbe.this.getContext().unregisterReceiver(this);
                    WifiProbe.this.runScan();
                } catch (IllegalArgumentException var4) {

                }
            }

        }
    };

    public WifiProbe() {
    }

    private boolean isSharingWiFi(final WifiManager manager) {
        try {
            final Method method = manager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true); //in the case of visibility change in future APIs
            boolean temp = (Boolean) method.invoke(manager);
            Logger.i(WifiProbe.class , "isSharingWifi : "+ temp);
            return temp;
        } catch (final Throwable ignored) {
        }

        return false;
    }

    protected void onEnable() {
        super.onEnable();
        this.wifiManager = (WifiManager) this.getContext().getSystemService("wifi");
        hotSpotOn = isSharingWiFi(wifiManager);
        if (hotSpotOn){
            hotSpotSendData();
            Logger.i(WifiProbe.class, "HotSpot is on so no sampling");
            stop();

        } else {

            this.numberOfAttempts = 0;
            this.getContext().registerReceiver(this.scanResultsReceiver, new IntentFilter("android.net.wifi.SCAN_RESULTS"));
        }
    }

    private void hotSpotSendData() {
        JsonObject data = new JsonObject();
        data.addProperty("HotSpot", "ON");
        this.sendData(data);
    }

    protected void onStart() {
        super.onStart();
        this.acquireWifiLock();
        this.saveWifiStateAndRunScan();
    }

    protected void onStop() {
        super.onStop();
        this.releaseWifiLock();
        this.loadPreviousWifiState();
    }

    protected void onDisable() {
        super.onDisable();
        if(!hotSpotOn)
            this.getContext().unregisterReceiver(this.scanResultsReceiver);
    }

    private void loadPreviousWifiState() {
        this.wifiManager.setWifiEnabled(this.previousWifiState == 3);
    }

    private void saveWifiStateAndRunScan() {
        int state = this.wifiManager.getWifiState();
        if (state != 0 && state != 2) {
            this.previousWifiState = state;
            this.runScan();
        } else {
            this.getContext().registerReceiver(this.waitingToStartScanReceiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
        }

    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void acquireWifiLock() {
        this.wifiLock = this.wifiManager.createWifiLock(2, LOCK_KEY);
        this.wifiLock.setReferenceCounted(false);
        this.wifiLock.acquire();
    }

    private void releaseWifiLock() {
        if (this.wifiLock != null) {
            if (this.wifiLock.isHeld()) {
                this.wifiLock.release();
            }
            this.wifiLock = null;
        }
    }

    private void runScan() {
        ++this.numberOfAttempts;
        int state = this.wifiManager.getWifiState();
        if (state == 3) {
            boolean successfulStart = this.wifiManager.startScan();
            if (successfulStart) {
                Log.i("Funf", "WIFI scan started succesfully");
            } else {
                Log.e("Funf", "WIFI scan failed.");
            }

            this.numberOfAttempts = 0;
        } else if (this.numberOfAttempts <= 3) {
            this.getContext().registerReceiver(this.retryScanReceiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
            this.wifiManager.setWifiEnabled(true);
        } else {
            this.stop();
        }

    }
}
