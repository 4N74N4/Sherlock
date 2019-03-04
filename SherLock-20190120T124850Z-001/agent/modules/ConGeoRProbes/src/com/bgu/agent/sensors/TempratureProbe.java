package com.bgu.agent.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.time.TimeUtil;

/**
 * Created by shedan on 04/03/2015.
 */
@Schedule.DefaultSchedule(interval = 5, duration = 4 * Constants.SECOND)
public class TempratureProbe extends ContinuousDataProbe implements SensorEventListener {

    @Configurable
    private String sensorDelay = "FASTEST";
    private SensorManager mSensorManager;
    private Sensor mTemperature;
    private SensorEventListener sensorListener;
    JsonObject data;

    @Override
    protected void secureOnStart(){
        super.secureOnStart();
        mSensorManager.registerListener(this , mTemperature , SensorManager.SENSOR_DELAY_FASTEST);
        data = new JsonObject();
    }

    protected void endOfTimeSendData()
    {

    }
    @Override
    protected void secureOnStop(){
        super.secureOnStop();
       }
    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        mSensorManager = (SensorManager) getContext().getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
//        this.sensorListener = new SensorEventListener() {
//            @Override
//            public void onSensorChanged(SensorEvent event) {
//                Log.e(TempratureProbe.class.toString(), "temp sensor changed");
//                data.addProperty("timestamp", TimeUtil.uptimeNanosToTimestamp(event.timestamp));
//                data.addProperty("accuracy", Integer.valueOf(event.accuracy));
//                float value = event.values[0];
//                data.addProperty("Temperature", value);
//
//                }
//
//            @Override
//            public void onAccuracyChanged(Sensor sensor, int accuracy) {
//
//            }
//        };


    }
    @Override
    protected void secureOnDisable(){
        mSensorManager.unregisterListener(sensorListener);
        sendData(data);
        Log.e(TempratureProbe.class.toString() , "temp data has been sent. looks like: "+data.toString());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.e(TempratureProbe.class.toString(), "temp sensor changed");
        data.addProperty("timestamp", TimeUtil.uptimeNanosToTimestamp(event.timestamp));
        data.addProperty("accuracy", Integer.valueOf(event.accuracy));
        float value = event.values[0];
        data.addProperty("Temperature", value);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //TYPE_AMBIENT_TEMPERATURE
}
