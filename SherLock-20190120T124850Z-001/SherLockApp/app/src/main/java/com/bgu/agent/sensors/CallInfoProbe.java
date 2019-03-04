package com.bgu.agent.sensors;

/**
 * Created by BittonRon on 2/19/14.
 */


import android.database.Cursor;
import android.provider.CallLog;
import android.provider.ContactsContract;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DisplayName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


// com.bgu.agent.sensors.CallInfoProbe

@DisplayName("CallInfoProbe")
@Schedule.DefaultSchedule(interval = 30 * Constants.MINUTE, duration = 30 * Constants.SECOND)

public class CallInfoProbe extends ContinuousDataProbe
{

    @Configurable
    long aggInterval = 59 * Constants.MINUTE;

    private long startTime;

    private int inCall = 0;

    private int missedCall = 0;

    private int outCall = 0;

    private int notInContacts = 0;

    private long totalDuration = 0;

    private List<String> contactList;

    private JsonArray callsDataArray;

    private DataListener callsDataListener;

    public CallInfoProbe()
    {
        super();
        maxWaitTime = BigDecimal.valueOf(20 * Constants.SECOND);
    }

    @Override
    protected void endOfTimeSendData()
    {
        JsonObject data = new JsonObject();
        data.addProperty("InCalls", inCall);
        data.addProperty("OutCalls", outCall);
        data.addProperty("MissedCalls", missedCall);
        data.addProperty("NotInContacts", notInContacts);
        data.addProperty("TotalDuration", totalDuration);
        if(callsDataArray!=null)
            data.add("CallsData", callsDataArray);
        sendData(data);
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        callsDataListener = new CallDataListener();
        contactList = getContactList();
        callsDataArray = new JsonArray();
        inCall = 0;
        outCall = 0;
        missedCall = 0;
        notInContacts = 0;
        totalDuration = 0;
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        startTime = System.currentTimeMillis();
        getCallProbe().registerListener(callsDataListener);
    }

    @Override
    protected void secureOnStop()
    {
        super.secureOnStop();
        getCallProbe().unregisterListener(callsDataListener);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        callsDataListener = null;
        contactList = null;
        callsDataArray = null;

        inCall = 0;
        missedCall = 0;
        outCall = 0;
        notInContacts = 0;
        totalDuration = 0;

    }


    private CallLogNoHashProbe getCallProbe()
    {
        return getGson().fromJson(DEFAULT_CONFIG, CallLogNoHashProbe.class);
    }

    private List<String> getContactList()
    {

        ArrayList<String> contactList = new ArrayList<String>();
        Cursor cursor = null;

        try
        {
            cursor = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
            int phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            cursor.moveToFirst();

            do
            {
                try
                {
                    contactList.add(Utils.hashWithSHA(cursor.getString(phoneNumberIdx).replace("-",
                            "").replace("+972", "0").replace(" ", "")));
                }
                catch (Throwable t)
                {
                    Logger.e(getClass(), t.getMessage());
                }

            } while (cursor.moveToNext());

        }
        catch (Exception e)
        {
            Logger.e(getClass(), e.getMessage());
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return contactList;
    }

    private class CallDataListener implements DataListener
    {

        @Override
        public void onDataReceived(IJsonObject probeConfig, IJsonObject data)
        {
            try
            {

                int type = data.get(CallLog.Calls.TYPE).getAsInt();
                long date = data.get(CallLog.Calls.DATE).getAsLong();
                long duration = data.get(CallLog.Calls.DURATION).getAsLong();

                ArrayList urls;
                String address = Utils.hashWithSHA(data.get(CallLog.Calls.NUMBER).getAsString().replace("-",
                        "").replace("+972", "0").replace(" ", ""));

                if ((startTime - date) / 1000 < aggInterval)
                {

                    JsonObject callData = new JsonObject();
                    boolean fromContacts = contactList.contains(address);
                    callData.addProperty("Date", date);
                    callData.addProperty("Address", address);
                    callData.addProperty("Type", type);
                    callData.addProperty("Duration", duration);
                    callData.addProperty("FromContacts", fromContacts);

                    callsDataArray.add(callData);
                    totalDuration += duration;
                    notInContacts += fromContacts ? 0 : 1;
                    outCall += type == CallLog.Calls.OUTGOING_TYPE ? 1 : 0;
                    inCall += type == CallLog.Calls.INCOMING_TYPE ? 1 : 0;
                    missedCall += type == CallLog.Calls.MISSED_TYPE ? 1 : 0;
                }
                else
                {
                    if (getState() == State.RUNNING)
                    {
                        getCallProbe().unregisterListener(callsDataListener);
                        endOfTimeSendData();
                        stop();
                    }

                }

            }
            catch (Throwable t)
            {
                sendErrorLog(t);
            }
        }

        @Override
        public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint)
        {
        }
    }
}



