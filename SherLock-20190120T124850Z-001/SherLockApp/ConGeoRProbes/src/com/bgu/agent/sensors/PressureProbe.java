package com.bgu.agent.sensors;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import com.bgu.congeor.Constants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.PressureSensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.probe.builtin.SensorProbe;
import edu.mit.media.funf.time.TimeUtil;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by shedan on 24/02/2015.
 */
@Schedule.DefaultSchedule(interval = 5, duration = 3 * Constants.SECOND)
@Probe.RequiredProbes(PressureSensorProbe.class)
@Probe.DisplayName("PressureProbe")
public class PressureProbe extends SensorProbe implements ProbeKeys.PressureSensorKeys {
    private SensorProbe pressure;
    public JsonObject data;
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorListener;
    private float previousData;

    public PressureProbe()
    {
        previousData = 0;
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    protected void onEnable() {

        this.sensorManager = (SensorManager) this.getContext().getSystemService("sensor");
        this.sensor = this.sensorManager.getDefaultSensor(this.getSensorType());
        final String[] valueNames = this.getValueNames();
        this.sensorListener = new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                JsonObject data = new JsonObject();
                data.addProperty("timestamp", TimeUtil.uptimeNanosToTimestamp(event.timestamp));
                data.addProperty("accuracy", Integer.valueOf(event.accuracy));
                int valuesLength = Math.min(event.values.length, valueNames.length);
                Log.e(PressureProbe.class.toString() , "pressure listener has been executed");
                for (int i = 0; i < valuesLength; ++i) {
                    String valueName = valueNames[i];
                    if(previousData==0) {
                        data.addProperty(valueName, Float.valueOf(previousData));

                    }
                    else
                        data.addProperty(valueName, Float.valueOf(event.values[i]-previousData ));
                    previousData = Float.valueOf(event.values[i]);
                }

                sendData(data);
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    protected void onStart() {

        this.getSensorManager().registerListener(this.sensorListener, this.sensor, this.getSensorDelay("FASTEST"));
    }
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    protected void onStop() {
        this.getSensorManager().unregisterListener(this.sensorListener);
    }
    @Override
    protected SensorManager getSensorManager() {
        if(this.sensorManager == null) {
            this.sensorManager = (SensorManager)this.getContext().getSystemService("sensor");
        }

        return this.sensorManager;
    }
    @Override
    protected int getSensorDelay(String specifiedSensorDelay) {
        int sensorDelay = -1;
        JsonElement el = this.getGson().toJsonTree(specifiedSensorDelay);
        if(!el.isJsonNull()) {
            try {
                int cce = el.getAsInt();
                if(cce == 0 || cce == 1 || cce == 2 || cce == 3) {
                    sensorDelay = cce;
                }
            } catch (NumberFormatException var6) {
                ;
            } catch (ClassCastException var7) {
                ;
            } catch (IllegalStateException var8) {
                ;
            }
        }

        if(sensorDelay < 0) {
            try {
                String cce1 = el.getAsString().toUpperCase().replace("SENSOR_DELAY_", "");
                if("FASTEST".equals(cce1)) {
                    sensorDelay = 0;
                } else if("GAME".equals(cce1)) {
                    sensorDelay = 1;
                } else if("UI".equals(cce1)) {
                    sensorDelay = 2;
                } else if("NORMAL".equals(cce1)) {
                    sensorDelay = 3;
                }
            } catch (ClassCastException var5) {
                Log.w("Funf", "Unknown sensor delay value: " + specifiedSensorDelay);
            }
        }

        if(sensorDelay < 0) {
            sensorDelay = 0;
        }

        return sensorDelay;
    }
    @Override
    public int getSensorType() {
        return 6;
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"pressure"};
    }
}