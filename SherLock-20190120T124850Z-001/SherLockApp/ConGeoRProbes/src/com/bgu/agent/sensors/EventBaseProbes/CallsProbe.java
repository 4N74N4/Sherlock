package com.bgu.agent.sensors.EventBaseProbes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

import java.net.Inet4Address;
import java.util.List;
import java.util.UUID;

//Pipe line path:  com.bgu.agent.sensors.EventBaseProbes.CallsProbe

@Probe.DisplayName("CallsProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class CallsProbe extends SecureBase implements Probe.ContinuousProbe
{
    BroadcastReceiver callsBroadcastReceiver;
    String lastEvent = null;
    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();



        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        callsBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                try
                {
                    if (intent.getAction() == TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                    {
                        String status = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                        Intent callIntent = null;

                        if(status.equals(TelephonyManager.EXTRA_STATE_IDLE))
                        {
                            callIntent = new Intent(Constants.INTENT_ACTION_END_OF_CALL);
                            callIntent.putExtra(Constants.CALL_TYPE,Constants.CALL_TYPE_ENDED);
                            Logger.d(CallsProbe.class ,"END_OF_CALL");
                        }
                        else if(status.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) && ( (lastEvent == null) || (!lastEvent.equals(TelephonyManager.EXTRA_STATE_RINGING) )))
                        {
                            callIntent = new Intent(Constants.INTENT_ACTION_OUTGOING_CALL);
                            callIntent.putExtra(Constants.CALL_TYPE,Constants.CALL_TYPE_OUTGOING);
                            Logger.d(CallsProbe.class ,"OUTGOING_CALL");
                        }

                        else if(status.equals(TelephonyManager.EXTRA_STATE_OFFHOOK ) && lastEvent.equals(TelephonyManager.EXTRA_STATE_RINGING))
                        {
                            callIntent = new Intent(Constants.INTENT_ACTION_INCOMING_CALL);
                            callIntent.putExtra(Constants.CALL_TYPE,Constants.CALL_TYPE_INCOMING);
                            Logger.d(CallsProbe.class ,"INCOMING_CALL");
                        }

                        else if(status.equals(TelephonyManager.EXTRA_STATE_RINGING))
                        {
                            callIntent = new Intent(Constants.INTENT_ACTION_RINGING);
                            callIntent.putExtra(Constants.CALL_TYPE,Constants.CALL_TYPE_RINGING);
                            Logger.d(CallsProbe.class, "RINGING");
                        }

                        lastEvent = status;
                        getContext().sendBroadcast(callIntent);
                    }
                }
                catch (Throwable t)
                {
                    Logger.e(ConnectedWifiProbe.class, t.getMessage());
                }
            }
        };
        getContext().registerReceiver(callsBroadcastReceiver, filter);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().unregisterReceiver(callsBroadcastReceiver);

    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }


}

