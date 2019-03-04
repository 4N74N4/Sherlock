package com.bgu.agent.sensors;

/**
 * Created by Simon!
 */

import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredProbes;
import edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe;
import edu.mit.media.funf.probe.builtin.SensorProbe;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

//Pipe line path:  com.bgu.agent.sensors.OrientationRotationProbe

@Schedule.DefaultSchedule( duration = 3 * Constants.SECOND)
@RequiredFeatures("android.hardware.sensor.accelerometer")
@RequiredProbes(AccelerometerSensorProbe.class)
@DisplayName("OrientationRotationStatisticsProbe")
public class OrientationRotationProbe extends ContinuousDataProbe {

    private String[] sensors = {"edu.mit.media.funf.probe.builtin.RotationVectorSensorProbe",
            "edu.mit.media.funf.probe.builtin.OrientationSensorProbe"};

    @Configurable
    private String sensorDelay = "GAME";

    private SensorProbe[] sensorProbes;

    private OrientationRotationDataListener[] OrientationRotationDataListener;

    public OrientationRotationProbe() {
        super();
        maxWaitTime = BigDecimal.valueOf(1 * Constants.SECOND);
    }

    @Override
    protected void secureOnEnable() {
        super.secureOnEnable();
        long threadID = Thread.currentThread().getId();
        Log.d(OrientationRotationProbe.class.toString(), "OrientationRotation: enable thread id is: " + threadID);
//         Log.d(OrientationRotationProbe.class.toString(), "Enable: " + System.currentTimeMillis());

        initSensors();
        initDataListeners();


    }

    @Override
    protected void secureOnStart() {
        super.secureOnStart();
//         Log.d(OrientationRotationProbe.class.toString(), "Start: " + System.currentTimeMillis());
        registerDataListeners();
    }

    @Override
    protected void endOfTimeSendData() {
        unRegisterDataListeners();
        sendSensorsData();


    }

    @Override
    protected void secureOnStop() {
        super.secureOnStop();
//         Log.d(OrientationRotationProbe.class.toString(), "Stop: " + System.currentTimeMillis());
    }

    @Override
    protected void secureOnDisable() {
        super.secureOnDisable();
//         Log.d(OrientationRotationProbe.class.toString(), "Disable: " + System.currentTimeMillis());
    }

    private void sendSensorsData() {
        long idtry = Thread.currentThread().getId();
        int threadID = android.os.Process.getThreadPriority(android.os.Process.myTid());
        Log.d(OrientationRotationProbe.class.toString(), "OrientationRotation: sendData thread id is: " + idtry);
//         Log.d(OrientationRotationProbe.class.toString(), "Send Data: " + System.currentTimeMillis());
        JsonObject data = new JsonObject();
        for (int i = 0; i < sensors.length; i++) {
            ConcurrentLinkedQueue<IJsonObject> sensorDataArray = OrientationRotationDataListener[i].sensorData;
            HashMap<String, ArrayList<Number>> rawDataMap = new HashMap<>();
            for (IJsonObject sensorData : sensorDataArray) {
                Set<Map.Entry<String, JsonElement>> eSet = (Set<Map.Entry<String, JsonElement>>) sensorData.entrySet();

                for (Map.Entry entry : eSet) {
                    if (entry.getKey().equals("accuracy") || entry.getKey().equals("timestamp"))
                        continue;
                    if (!rawDataMap.containsKey(entry.getKey())) {
                        rawDataMap.put((String) entry.getKey(), new ArrayList<Number>());
                    }

                    ArrayList<Number> entryData = rawDataMap.get(entry.getKey());
                    entryData.add(((JsonPrimitive) entry.getValue()).getAsNumber());
                }
            }
//            Log.d(OrientationRotationProbe.class.toString(), "Sensor name" + sensors[i].toString() + "started at  " + System.currentTimeMillis());
            JsonObject temp = extractStat(rawDataMap);

//            Log.d(OrientationRotationProbe.class.toString(), "Sensor name" + sensors[i].toString() + "ended at  " + System.currentTimeMillis());
            Log.d(OrientationRotationProbe.class.toString(), "data is null?   :   " + (temp == null ? true : false));
            data.add(sensors[i], temp);
        }
        Logger.e(OrientationRotationProbe.class, data.toString());
        if (data != null) {
            sendData(data);
            Log.d(OrientationRotationProbe.class.toString(), "OrientationRotation probe after sendata: " + System.currentTimeMillis());
        } else {
            Log.d(OrientationRotationProbe.class.toString(), "OrientationRotation probe has null as data");
//            JsonObject temp  = new JsonObject();
//            temp.addProperty("Data Error", "Data is null");
//            sendData(temp);
            data.addProperty("No Data", "No Data");
            sendData(data);
        }

    }

    private JsonObject extractStat(HashMap<String, ArrayList<Number>> rawDataMap) {
        JsonObject sensorData = new JsonObject();
        for (String key : rawDataMap.keySet()) {

            ArrayList<Number> axisData = rawDataMap.get(key);
            double[] axisDataAsDoubleArray = getAsDoubleArray(axisData);
            HashMap<String, Number> axisStat = extractStatFromAxis(axisDataAsDoubleArray);

            for (String statKey : axisStat.keySet()) {
                sensorData.addProperty(key + "_" + statKey, axisStat.get(statKey));
            }
        }


        return sensorData;
    }

    private double[] getAsDoubleArray(ArrayList<Number> axisData) {
        double[] ans = new double[axisData.size()];
        int i = 0;
        for (Number number : axisData) {
            ans[i++] = number.doubleValue();
        }
        return ans;
    }

    private HashMap<String, Number> extractStatFromAxis(double[] axis) {

        Mean mean = new Mean();
        Median median = new Median();


        HashMap<String, Number> data = new HashMap();

        data.put("MIDDLE_SAMPLE", axis[axis.length/2]);
        data.put("MEAN", mean.evaluate(axis));
        data.put("MEDIAN", median.evaluate(axis));

        return data;
    }

    public void initSensors() {
        sensorProbes = new SensorProbe[sensors.length];

        for (int i = 0; i < sensorProbes.length; i++) {
            try {
                JsonObject conf = new JsonObject();
                conf.addProperty("sensorDelay", sensorDelay);
                sensorProbes[i] = (SensorProbe) getGson().fromJson(conf, Class.forName(sensors[i]));
            } catch (ClassNotFoundException e) {
                Logger.e(OrientationRotationProbe.class, "Sensor " + sensors[i] + " sensor not found", e);
            }
        }
    }

    public void initDataListeners() {
        OrientationRotationDataListener = new OrientationRotationDataListener[sensorProbes.length];
        for (int i = 0; i < sensors.length; i++) {
            OrientationRotationDataListener[i] = new OrientationRotationDataListener();
        }
    }

    public void registerDataListeners() {
        Log.d(OrientationRotationProbe.class.toString(), "Register Listeners: " + System.currentTimeMillis());
        for (int i = 0; i < sensorProbes.length; i++) {
            sensorProbes[i].registerListener(OrientationRotationDataListener[i]);
        }
    }

    public void unRegisterDataListeners() {
         Log.d(OrientationRotationProbe.class.toString(), "Unregister Listeners: " + System.currentTimeMillis());
        for (int i = 0; i < sensorProbes.length; i++) {
            sensorProbes[i].unregisterListener(OrientationRotationDataListener[i]);
        }
    }

    private class OrientationRotationDataListener implements DataListener {
        public ConcurrentLinkedQueue<IJsonObject> sensorData;

        private OrientationRotationDataListener() {
            this.sensorData = new ConcurrentLinkedQueue<IJsonObject>();
        }

        @Override
        public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
            sensorData.add(data);
//            Log.i(OrientationRotationDataListener.class.toString(),"data Recieved");
        }

        @Override
        public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {

        }
    }
}