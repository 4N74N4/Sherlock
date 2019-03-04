package com.bgu.agent.sensors.EventBaseProbes;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import com.bgu.agent.sensors.SecureBase;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredProbes;
import edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/25/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */

//Pipe line path:  com.bgu.agent.sensors.EventBaseProbes.SmsLeakageProbe


@RequiredFeatures("android.hardware.sensor.accelerometer")
@Probe.RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredProbes(AccelerometerSensorProbe.class)
@DisplayName("SmsLeakageProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class SmsLeakageProbe extends SecureBase implements ContinuousProbe
{

    ContentResolver contentResolver;

    OutSMSObserver outSMSObserver;

    long smsTime = 0;

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        contentResolver = getContext().getContentResolver();
        outSMSObserver = new OutSMSObserver(new android.os.Handler());
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, outSMSObserver);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        contentResolver.unregisterContentObserver(outSMSObserver);
    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }


    private class OutSMSObserver extends ContentObserver
    {

        private List<String> smsIdList;

        public OutSMSObserver(android.os.Handler handler)
        {
            super(handler);
            this.smsIdList = new ArrayList<String>();
        }

        @Override
        public void onChange(boolean selfChange)
        {
            try
            {
                super.onChange(selfChange);
                Uri uriSMSURI = ProbeKeys.AndroidInternal.Sms.CONTENT_URI;
                Cursor cur = getContext().getContentResolver().query(uriSMSURI, null, null, null, null);
                cur.moveToNext();

                int typeColumnIndex = cur.getColumnIndex("type");

                int idColumnIndex = cur.getColumnIndex("_id");

                if (typeColumnIndex != -1 && idColumnIndex != -1)
                {
                    String content = cur.getString(typeColumnIndex);
                    String id = cur.getString(idColumnIndex);

                    if ((content.compareTo("2") == 0) && (!smsIdList.contains(id)))
                    {
                        smsIdList.add(id);
                        smsTime = System.currentTimeMillis();
                        sendSMS(smsTime);
                    }
                }
            }
            catch (Throwable t)
            {
                sendErrorLog(t);
            }

        }

        private void sendSMS(long smsTime)
        {
            Intent smsIntent = new Intent("com.bgu.congeor.SmsSent");
            smsIntent.putExtra("smsTime", smsTime);
            smsIntent.putExtra("smsType", "Real");

            getContext().sendBroadcast(smsIntent);
        }

    }
}

