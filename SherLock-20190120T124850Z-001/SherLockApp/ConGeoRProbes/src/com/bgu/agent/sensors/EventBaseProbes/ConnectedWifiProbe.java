package com.bgu.agent.sensors.EventBaseProbes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

import java.util.List;
import java.util.UUID;

//Pipe line path:  com.bgu.agent.sensors.EventBaseProbes.ConnectedWifiProbe

@Probe.DisplayName("ConnectedWifiProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class ConnectedWifiProbe extends SecureBase implements Probe.ContinuousProbe
{

    private BroadcastReceiver wifiReceiver;

    private String lastSSID;
    private String lastAction;


    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();

        lastSSID = null;

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);


        wifiReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                try
                {
                    final String action = intent.getAction();
                    if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
                    {
                        String status = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE).toString();
                        if (((status.equals("COMPLETED")) || (status.equals("DISCONNECTED"))) && (lastAction == null || !status.equals(lastAction)))
                        {
                            lastAction = status;
                            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                            JsonObject data = new JsonObject();
                            String uuid;

                            if (status.equals("COMPLETED") && (lastSSID == null || (lastSSID.compareTo(wifiInfo.getSSID()) != 0)))
                            {
                                lastSSID = wifiInfo.getSSID();
                                uuid = UUID.randomUUID().toString();
                                Intent wifiInternalIntent = new Intent(Constants.INTENT_ACTION_WIFI);
                                wifiInternalIntent.putExtra(Constants.UUID, uuid);

                                getContext().sendBroadcast(wifiInternalIntent);

                                List<ScanResult> scanResults = wifiManager.getScanResults();
                                if (scanResults != null)
                                {
                                    for (ScanResult network : scanResults)
                                    {
                                        if (network != null && wifiInfo != null && network.BSSID.compareTo(wifiInfo
                                                .getBSSID()) == 0)
                                        {
                                            data.addProperty(Constants.NETWORK_SEC, network.capabilities);
                                            break;
                                        }

                                    }
                                }
                                data.addProperty(Constants.UUID, uuid);
                            }

                            data.addProperty(Constants.ACTION, status);
                            data.addProperty(Constants.SSID, wifiInfo.getSSID().replaceAll("\"", ""));
                            data.addProperty(Constants.BSSID, wifiInfo.getBSSID());
                            data.addProperty(Constants.IP_ADDRESS, wifiInfo.getIpAddress());
                            data.addProperty(Constants.MAC_ADDRESS, wifiInfo.getMacAddress());
                            data.addProperty(Constants.NETWORK_ID, wifiInfo.getNetworkId());
                            sendData(data);
                        }
                    }
                }
                catch (Throwable t)
                {
                    Logger.e(ConnectedWifiProbe.class, t.getMessage());
                }
            }
        };
        getContext().registerReceiver(wifiReceiver, filter);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        lastSSID = null;
        getContext().unregisterReceiver(wifiReceiver);

    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }


}

