package com.bgu.agent.sensors;


        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import com.bgu.agent.commons.logging.Logger;
        import com.bgu.congeor.Constants;
        import com.google.gson.JsonArray;
        import com.google.gson.JsonElement;
        import com.google.gson.JsonObject;
        import edu.mit.media.funf.Schedule;
        import edu.mit.media.funf.config.Configurable;
        import edu.mit.media.funf.json.IJsonObject;
        import edu.mit.media.funf.probe.Probe;
        import edu.mit.media.funf.time.TimeUtil;

        import java.util.*;

@Probe.DisplayName("GenericEventBaseProbeT3")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class GenericEventBaseProbeT3 extends SecureBase implements Probe.ContinuousProbe
{

    Object lock = new Object();
    // got from configuration file
    @Configurable
    private String[] sensors;
    // got from configuration file
    @Configurable
    private String[] events;

    @Configurable
    private long maxWaitingTime = 4;

    private List<String> sensorsDataMapping;

    private JsonArray sensorsData;

    private HashMap<String, JsonArray> eventsDic;

    private DataListener genericDataListener = new GenericDataListener();

    private HashMap<String, Object> sensorsObjects = new HashMap<String, Object>();

    private BroadcastReceiver genericEventReceiver;

    private Boolean onEventProcessing = false;

    private long startTime = 0;

    private Runnable endOfTimeRunnable;
    private String sessionUUID = null;

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        IntentFilter filter = new IntentFilter();
        // events that triger the probe so filter is added
        if (events != null)
        {
            for (String event : events)
            {
                filter.addAction(event);
            }
        }
        // list of sensors in the "sensors" attribute in conf file
        if (sensors != null)
        {
            for (String sensor : sensors)
            {
                try
                {
                    //reflection? creating a class using the class name string
                    sensorsObjects.put(sensor, getGson().fromJson(DEFAULT_CONFIG, Class.forName(sensor)));
                }
                catch (ClassNotFoundException e)
                {
                    Logger.e(GenericEventBaseProbeT3.class, "Sensor " + sensor + " in generic event not found", e);
                }
            }
        }


        genericEventReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                synchronized (lock)
                {
                    if (!onEventProcessing)
                    {
                        Logger.i(GenericEventBaseProbeT3.class, "Get new event");

                        onEventProcessing = true;
                        startTime = System.currentTimeMillis();

                        eventsDic = new HashMap<String, JsonArray>();
                        JsonArray bundleList = new JsonArray();
                        eventsDic.put(intent.getAction(), bundleList);

                        if (intent.getExtras() != null)
                        {
                            if (intent.hasExtra(Constants.UUID))
                            {

                                sessionUUID =  sessionUUID = String.valueOf(new Date().getTime());
                                // sessionUUID = intent.getStringExtra(Constants.UUID);
                            }
                            bundleList.add(getGson().toJsonTree(intent.getExtras()));
                        }

                        sensorsData = new JsonArray();
                        sensorsDataMapping = new ArrayList<String>();

                        for (Object sensorObject : sensorsObjects.values())
                        {
                            ((Base) sensorObject).registerListener(genericDataListener);
                        }

                        endOfTimeRunnable = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    synchronized (lock)
                                    {
                                        Logger.i(GenericEventBaseProbeT3.class, "Events waiting time over");

                                        Set<String> sensorsKeys = new HashSet<String>(sensorsObjects.keySet());
                                        sensorsKeys.removeAll(sensorsDataMapping);
                                        for (String key : sensorsKeys)
                                        {
                                            ((Base) sensorsObjects.get(key)).unregisterListener(genericDataListener);
                                        }
                                        sensorsKeys = null;
                                        sendSensorsData();
                                    }
                                }
                                catch (Throwable t)
                                {
                                    sendErrorLog(t);
                                }
                                endOfTimeRunnable = null;
                            }
                        };
                        getHandler().postDelayed(endOfTimeRunnable, TimeUtil.secondsToMillis(maxWaitingTime));

                    }
                    else
                    {
                        Logger.i(GenericEventBaseProbeT3.class, "Get new event BUT in prosses");
                        if (eventsDic.containsKey(intent.getAction()))
                        {

                            if (intent.getExtras() != null)
                            {
                                eventsDic.get(intent.getAction()).add(getGson().toJsonTree(intent.getExtras()));
                            }
                        }
                        else
                        {
                            JsonArray bundleList = new JsonArray();
                            eventsDic.put(intent.getAction(), bundleList);

                            if (intent.getExtras() != null)
                            {
                                bundleList.add(getGson().toJsonTree(intent.getExtras()));
                            }
                        }
                    }
                }
            }
        };
        getContext().registerReceiver(genericEventReceiver, filter);
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().unregisterReceiver(genericEventReceiver);
        onEventProcessing = false;
    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }

    private void sendSensorsData()
    {
        try
        {
            if (endOfTimeRunnable != null)
            {
                getHandler().removeCallbacks(endOfTimeRunnable);
                endOfTimeRunnable = null;
            }
        }
        catch (Throwable t)
        {

        }

        JsonObject dataToSend = new JsonObject();
        JsonArray events = new JsonArray();
        if (eventsDic != null)
        {
            for (String key : eventsDic.keySet())
            {
                JsonObject e = new JsonObject();
                e.addProperty("Action", key);
                e.addProperty("NumberOfOccurrences", eventsDic.get(key).size());
                e.add("Extras", eventsDic.get(key));
                events.add(e);
            }

            if (sessionUUID != null)
            {
                dataToSend.addProperty(Constants.UUID, sessionUUID);
            }
            dataToSend.add("Events", events);
            dataToSend.add("value", sensorsData);

            Logger.i(GenericEventBaseProbeT3.class, "Send data");

            try
            {
                if(sensorsData != null)
                {
                    sendData(dataToSend);
                }
                else
                {
                    Logger.e(GeneralSampleProbeT3.class, " Generic T3 - Null Data has been detected");
                }

            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
        else
        {

            Logger.e(getClass(), "eventsDic == null");
        }
        eventsDic = null;
        startTime = 0;
        sessionUUID = null;
        sensorsDataMapping = null;
        onEventProcessing = false;
        sensorsData = null;
        eventsDic = null;
    }

    private class GenericDataListener implements DataListener
    {

        @Override
        public void onDataReceived(IJsonObject probeConfig, IJsonObject data)
        {
            try
            {
                String sensorName = probeConfig.getAsJsonObject().get("@type").getAsString();


                if (sensorsDataMapping != null && !sensorsDataMapping.contains(sensorName))
                {

                    sensorsDataMapping.add(sensorName);

                    JsonObject sd = new JsonObject();
                    sd.addProperty("subProbeName", sensorName);
                    sd.add("value", data);

                    if (sensorsData != null)
                    {
                        sensorsData.add(sd);
                    }

                    ((Base) sensorsObjects.get(sensorName)).unregisterListener(genericDataListener);

                    Logger.i(GenericEventBaseProbeT3.class, "Get new data from : " + sensorName);

                }
                if (sensorsData != null && (sensorsData.size() == sensors.length))
                {

                    synchronized (lock)
                    {
                        sendSensorsData();
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

