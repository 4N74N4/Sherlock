package com.bgu.agent.sensors;

/**
 * Created by BittonRon on 2/12/14.
 */

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.provider.ContactsContract;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AndroidInternal.Sms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// com.bgu.agent.sensors.SmsInfoProbe
@DisplayName("SmsInfoProbe")
@Schedule.DefaultSchedule(interval = 30 * Constants.MINUTE, duration = 30 * Constants.SECOND)

public class SmsInfoProbe extends ContinuousDataProbe
{

    @Configurable
    long aggInterval = 59 * Constants.MINUTE;

    private long startTime;

    private int inSMS = 0;

    private int outSMS = 0;

    private int notInContacts = 0;

    private List<String> contactList;

    private List<String> urlList;

    private JsonArray smsDataArray;

    private DataListener smsDataListener;

    public SmsInfoProbe()
    {
        super();
        maxWaitTime = BigDecimal.valueOf(20 * Constants.SECOND);
    }

    @Override
    protected void endOfTimeSendData()
    {
        JsonObject data = new JsonObject();
        JsonArray urls = ((new Gson()).toJsonTree(urlList)).getAsJsonArray();
        data.addProperty("InSMS", inSMS);
        data.addProperty("OutSMS", outSMS);
        data.addProperty("NotInContacts", notInContacts);
        if(urls!=null)
        data.add("urlList", urls);
        data.add("SmsData", smsDataArray);
        Logger.d(getClass(), data.toString());
        sendData(data);
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        smsDataListener = new SMSDataListener();
        contactList = getContactList();
        urlList = new ArrayList<String>();
        smsDataArray = new JsonArray();
        inSMS = 0;
        outSMS = 0;
        notInContacts = 0;
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        startTime = System.currentTimeMillis();
        getSmsProbe().registerListener(smsDataListener);
        final LocationManager manager = (LocationManager) getContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            Log.e("GPS", "GPS is off");
          Intent i = new Intent();
            i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            i.setAction("com.bgu.congeor.sherlockapp.gps");
            getContext().sendBroadcast(i);
        }

    }

    @Override
    protected void secureOnStop()
    {
        super.secureOnStop();
        getSmsProbe().unregisterListener(smsDataListener);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        smsDataListener = null;
        contactList = null;
        urlList = null;
        smsDataArray = null;

        inSMS = 0;
        outSMS = 0;
        notInContacts = 0;

    }


    private SmsLogNoHashProbe getSmsProbe()
    {
        return getGson().fromJson(DEFAULT_CONFIG, SmsLogNoHashProbe.class);
    }


    private ArrayList pullLinks(String text)
    {
        ArrayList links = new ArrayList();

        String regex = "\\(?\\b(https://|http://|www[.]|@|WWW[.]|Www[.])[-A-Za-z0-9+&@#/%?=~_()|!:," +
                "" + ".;]*[-A-Za-z0-9+&@#/%=~_()|]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        while (m.find())
        {
            String urlStr = m.group();
            if (urlStr.startsWith("(") && urlStr.endsWith(")"))
            {
                urlStr = urlStr.substring(1, urlStr.length() - 1);
            }
            links.add(urlStr);
        }
        return links;
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
            e.printStackTrace();
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

    private class SMSDataListener implements DataListener
    {

        @Override
        public void onDataReceived(IJsonObject probeConfig, IJsonObject data)
        {
            try
            {
                int type = data.get(Sms.TYPE).getAsInt();

                if (type == Sms.MESSAGE_TYPE_INBOX || type == Sms.MESSAGE_TYPE_SENT)
                {
                    String address = Utils.hashWithSHA(data.get(Sms.ADDRESS).getAsString().replace("-",
                            "").replace("+972", "0").replace(" ", ""));
                    String body = data.get(Sms.BODY).getAsString();
                    long date = data.get(Sms.DATE).getAsLong();
                    ArrayList urls;

                    if ((startTime - date) / 1000 < aggInterval)
                    {

                        JsonObject smsData = new JsonObject();
                        urls = pullLinks(body);
                        boolean fromContacts = contactList.contains(address);

                        smsData.addProperty("Date", date);
                        smsData.addProperty("Address", address);
                        smsData.addProperty("Type", type);
                        smsData.addProperty("ContainsURL", !urls.isEmpty());
                        smsData.addProperty("FromContacts", fromContacts);

                        smsDataArray.add(smsData);

                        urlList.addAll(urls);
                        notInContacts += fromContacts ? 0 : 1;
                        outSMS += type == Sms.MESSAGE_TYPE_SENT ? 1 : 0;
                        inSMS += type == Sms.MESSAGE_TYPE_INBOX ? 1 : 0; //need to test this in other device
                    }
                    else
                    {
                        if (getState() == State.RUNNING)
                        {
                            getSmsProbe().unregisterListener(smsDataListener);
                            endOfTimeSendData();
                            stop();
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                Logger.e(SmsInfoProbe.class, t.getMessage());

            }
        }

        @Override
        public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint)
        {
        }
    }

}


