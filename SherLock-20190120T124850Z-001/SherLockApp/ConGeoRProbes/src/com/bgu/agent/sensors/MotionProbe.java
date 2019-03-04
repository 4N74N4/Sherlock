package com.bgu.agent.sensors;

/**
 * Created by shedan on 08/01/2015.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

//Pipe line path:  com.bgu.agent.sensors.MotionProbe
@Schedule.DefaultSchedule(duration = 9 * Constants.SECOND)
@RequiredFeatures("android.hardware.sensor.accelerometer")
@RequiredProbes(AccelerometerSensorProbe.class)
@DisplayName("MotionStatisticsProbe")
public class MotionProbe extends ContinuousDataProbe {
    //    @Configurable
    private String[] sensors = {"edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe",
            "edu.mit.media.funf.probe.builtin.GyroscopeSensorProbe",
            "edu.mit.media.funf.probe.builtin.MagneticFieldSensorProbe",
            "edu.mit.media.funf.probe.builtin.PressureSensorProbe",
            "edu.mit.media.funf.probe.builtin.LinearAccelerationSensorProbe"};

    //    @Configurable
    private String[] sensors1;

    @Configurable
    private String sensorDelay = "GAME";
    public Long lastTime = new Long(0);
    private SensorProbe[] sensorProbes;

    private MotionDataListener[] motionDataListener;

    public MotionProbe() {
        super();
        maxWaitTime = BigDecimal.valueOf(4 * Constants.SECOND);
    }

    @Override
    protected void secureOnEnable() {
        super.secureOnEnable();
        long threadID = Thread.currentThread().getId();
        Log.d(MotionProbe.class.toString(), "Motion: enable thread id is: " + threadID);
        // Log.e(MotionProbe.class.toString(), "Enable: " + System.currentTimeMillis());


//        restart service!
        String TAG = "Restart tag motion";
        Long nowTime;
        nowTime = System.currentTimeMillis();
        SharedPreferences sp = getContext().getSharedPreferences("INTERVAL", Context.MODE_PRIVATE);
        lastTime = sp.getLong("traffic_interval", 0);
        if (lastTime == null)
            lastTime = 0L;
        long diff = nowTime - lastTime;
        Log.d(TAG, "Traffic interval: Now: " + nowTime + " last: " + lastTime + " Diff" + diff);
        if (diff > 60000 && lastTime!=0) {
            Log.e(TAG, "Interval exceeded detected!" + Math.abs(lastTime - nowTime));
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.putExtra("probe", "Traffic");
            intent.setAction("com.bgu.congeor.sherlockapp.RestartFunfReciver");
            getContext().sendBroadcast(intent);
            Log.e(TAG, "Restart broadcast sent!");
        }
        lastTime = nowTime;
        sp.edit().putLong("motion_interval", nowTime).commit();
        initSensors();
        initDataListeners();


    }

    @Override
    protected void secureOnStart() {
        super.secureOnStart();
        // Log.e(MotionProbe.class.toString(), "Start: " + System.currentTimeMillis());
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
        // Log.e(MotionProbe.class.toString(), "Stop: " + System.currentTimeMillis());
    }

    @Override
    protected void secureOnDisable() {
        super.secureOnDisable();
        // Log.e(MotionProbe.class.toString(), "Disable: " + System.currentTimeMillis());
    }

    private void sendSensorsData() {
        long idtry = Thread.currentThread().getId();
        int threadID = android.os.Process.getThreadPriority(android.os.Process.myTid());
        Log.e(MotionProbe.class.toString(), "Motion: sendData thread id is: " + idtry);
        // Log.e(MotionProbe.class.toString(), "Send Data: " + System.currentTimeMillis());
        JsonObject data = new JsonObject();
        for (int i = 0; i < sensors.length; i++) {
            ConcurrentLinkedQueue<IJsonObject> sensorDataArray = motionDataListener[i].sensorData;
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
            //Log.e(MotionProbe.class.toString(), "Sensor name" + sensors[i].toString() + "started at  " + System.currentTimeMillis());
            JsonObject temp = extractStat(rawDataMap);

            //Log.e(MotionProbe.class.toString(), "Sensor name" + sensors[i].toString() + "ended at  " + System.currentTimeMillis());
            Log.e(MotionProbe.class.toString(), "data is null?   :   " + (temp == null ? true : false));
            data.add(sensors[i], temp);
        }
        //Logger.e(MotionProbe.class, data.toString());
        if (data != null) {
            sendData(data);
            Log.e(MotionProbe.class.toString(), "Motion probe after sendata: " + System.currentTimeMillis());
        } else {
            Log.e(MotionProbe.class.toString(), "Motion probe has null as data");
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

        String[] keys = rawDataMap.keySet().toArray(new String[rawDataMap.keySet().size()]);

        for (int i = 0; i < keys.length; i++) {
            ArrayList<Number> axis1 = rawDataMap.get(keys[i]);

            for (int j = i + 1; j < keys.length; j++) {

                ArrayList<Number> axis2 = rawDataMap.get(keys[j]);

                Covariance cov = new Covariance();
                if (axis1.size()>2 && axis2.size()>2)
                sensorData.addProperty("COV(" + keys[i] + "," + keys[j] + ")", cov.covariance(getAsDoubleArray(axis1), getAsDoubleArray(axis2)));
                else sensorData.addProperty("COV(" + keys[i] + "," + keys[j] + ")","Null");
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

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Variance variance = new Variance();
        Mean mean = new Mean();
        Median median = new Median();

        double[] axisAfterPad = new double[512];

        for (int i = 0; i < axisAfterPad.length && i < axis.length; i++) {
            axisAfterPad[i] = axis[i];
        }
        Complex[] axisAfterFFT = fft.transform(axisAfterPad, TransformType.FORWARD);
        double axisDC = axisAfterFFT[0].abs();
        double[] absOfAxisAfterFFT = new double[axisAfterFFT.length / 2 + 1];
        for (int i = 0; i < absOfAxisAfterFFT.length; i++) {
            absOfAxisAfterFFT[i] = axisAfterFFT[i + 1].abs();
        }
        double[] sortedAxis_FFT = absOfAxisAfterFFT.clone();
        Arrays.sort(sortedAxis_FFT);
        int[] axisIndexesOfSortedValue = getIndices(absOfAxisAfterFFT, sortedAxis_FFT);

        HashMap<String, Number> data = new HashMap();

        data.put("MIDDLE_SAMPLE", axis[axis.length / 2]);
        data.put("VAR", variance.evaluate(axis));
        data.put("MEAN", mean.evaluate(axis));
        data.put("MEDIAN", median.evaluate(axis));
        data.put("VAR_FFT", variance.evaluate(absOfAxisAfterFFT));
        data.put("MEAN_FFT", mean.evaluate(absOfAxisAfterFFT));
        data.put("MEDIAN_FFT", median.evaluate(absOfAxisAfterFFT));
        data.put("DC_FFT", axisDC);
        data.put("FIRST_VAL_FFT", sortedAxis_FFT[0]);
        data.put("SECOND_VAL_FFT", sortedAxis_FFT[1]);
        data.put("THIRD_VAL_FFT", sortedAxis_FFT[2]);
        data.put("FOURTH_VAL_FFT", sortedAxis_FFT[3]);
        data.put("FIRST_IDX_FFT", axisIndexesOfSortedValue[0]);
        data.put("SECOND_IDX_FFT", axisIndexesOfSortedValue[1]);
        data.put("THIRD_IDX_FFT", axisIndexesOfSortedValue[2]);
        data.put("FOURTH_IDX_FFT", axisIndexesOfSortedValue[3]);

        return data;
    }

    private int[] getIndices(double[] originalArray, double[] sortedArray) {
        int[] indices = new int[originalArray.length];

        for (int index = 0; index < originalArray.length; index++)
            indices[index] = Arrays.binarySearch(sortedArray, originalArray[index]);
        return indices;
    }

    public void initSensors() {
        sensorProbes = new SensorProbe[sensors.length];

        for (int i = 0; i < sensorProbes.length; i++) {
            try {
                JsonObject conf = new JsonObject();
                conf.addProperty("sensorDelay", sensorDelay);
                sensorProbes[i] = (SensorProbe) getGson().fromJson(conf, Class.forName(sensors[i]));
            } catch (ClassNotFoundException e) {
                Logger.e(MotionProbe.class, "Sensor " + sensors[i] + " sensor not found", e);
            }
        }
    }

    public void initDataListeners() {
        motionDataListener = new MotionDataListener[sensorProbes.length];
        for (int i = 0; i < sensors.length; i++) {
            motionDataListener[i] = new MotionDataListener();
        }
    }

    public void registerDataListeners() {
        //Log.e(MotionProbe.class.toString(), "Register Listeners: " + System.currentTimeMillis());
        for (int i = 0; i < sensorProbes.length; i++) {
            sensorProbes[i].registerListener(motionDataListener[i]);
        }
    }

    public void unRegisterDataListeners() {
        // Log.e(MotionProbe.class.toString(), "Unregister Listeners: " + System.currentTimeMillis());
        for (int i = 0; i < sensorProbes.length; i++) {
            sensorProbes[i].unregisterListener(motionDataListener[i]);
        }
    }

    private class MotionDataListener implements DataListener {
        public ConcurrentLinkedQueue<IJsonObject> sensorData;

        private MotionDataListener() {
            this.sensorData = new ConcurrentLinkedQueue<IJsonObject>();
        }

        @Override
        public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
            sensorData.add(data);
            //Log.i(MotionDataListener.class.toString(),"data Recieved");
        }

        @Override
        public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {

        }
    }
}