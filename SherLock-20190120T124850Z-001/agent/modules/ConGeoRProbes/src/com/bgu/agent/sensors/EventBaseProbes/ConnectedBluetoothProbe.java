package com.bgu.agent.sensors.EventBaseProbes;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 1/24/14
 * Time: 12:21 AM
 * To change this template use File | Settings | File Templates.
 */

// com.bgu.agent.sensors.EventBaseProbes.ConnectedBluetoothProbe

@Probe.DisplayName("ConnectedBluetoothProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)

public class ConnectedBluetoothProbe extends SecureBase implements Probe.ContinuousProbe
{

    private BroadcastReceiver bluetoothReceiver;


    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        bluetoothReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                try
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    final String action = intent.getAction();

                    JsonObject data = new JsonObject();

                    if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
                    {
                        data.addProperty(Constants.DEVICE_ACTION, Constants.DEVICE_CONNECTED);
                    }

                    else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
                    {
                        data.addProperty(Constants.DEVICE_ACTION, Constants.DEVICE_DISCONNECTED);
                    }

                    String uuid = UUID.randomUUID().toString();
                    data.addProperty(Constants.UUID, uuid);
                    Intent bluetoothIntent = new Intent(Constants.INTENT_ACTION_BLUETOOTH);
                    bluetoothIntent.putExtra(Constants.UUID, uuid);
                    getContext().sendBroadcast(bluetoothIntent);

                    data.addProperty(Constants.DEVICE_NAME, device.getName());
                    data.addProperty(Constants.DEVICE_ADDRESS, device.getAddress());
                    sendData(data);
                }
                catch (Throwable t)
                {
                    sendErrorLog(t);
                }
            }
        };
        getContext().registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().unregisterReceiver(bluetoothReceiver);
    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }

}

