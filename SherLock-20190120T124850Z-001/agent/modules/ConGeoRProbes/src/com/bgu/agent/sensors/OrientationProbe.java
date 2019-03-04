package com.bgu.agent.sensors;

import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.OrientationSensorProbe;
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

/**
 * Created by simondzn on 21/09/2015.
 */

@Probe.RequiredFeatures("android.hardware.sensor.accelerometer")
@Probe.RequiredProbes(OrientationSensorProbe.class)
@Probe.DisplayName("OrientationProbe")
public class OrientationProbe extends ContinuousDataProbe {

    @Configurable
    private double statInterval = 2 * Constants.SECOND;

    //private long startTime;
    private ActivityCounter activityCounter = new ActivityCounter();

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        if (activityCounter == null)
        {
            activityCounter = new ActivityCounter();
        }

        getOrientationProbe().registerPassiveListener(activityCounter);
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        if (activityCounter != null)
        {
            getOrientationProbe().registerListener(activityCounter);
        }
    }

    @Override
    protected void secureOnStop()
    {
        super.secureOnStop();
        if (activityCounter != null)
        {
            try
            {
                getOrientationProbe().unregisterListener(activityCounter);
            }
            catch (Throwable t)
            {

            }
        }
        Log.d(OrientationProbe.class.toString(), "Orientation Probe has stopped");
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        if (activityCounter != null)
        {
            try
            {
                getOrientationProbe().unregisterPassiveListener(activityCounter);
            }
            catch (Throwable t)
            {

            }
        }
    }

    private OrientationSensorProbe getOrientationProbe()
    {
        return getGson().fromJson(DEFAULT_CONFIG, OrientationSensorProbe.class);
    }


    private class ActivityCounter implements DataListener
    {
        int count = 0;
        @Override
        public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data)
        {
            count++;
            if(count >= 5) {
                JsonObject sensorData = new JsonObject();
                float x = data.get(OrientationSensorProbe.PITCH).getAsFloat();
                float y = data.get(OrientationSensorProbe.ROLL).getAsFloat();
                float z = data.get(OrientationSensorProbe.AZIMUTH).getAsFloat();
                sensorData.addProperty("PITCH", x);
                sensorData.addProperty("ROLL", y);
                sensorData.addProperty("AZIMUTH", z);
                sendData(sensorData);
                Log.d(OrientationProbe.class.toString(), "sent orientation: " + "pitch: " + x + " ROLL: " + y + " AZIMUTH: " + z);
                count = 0;
                secureOnStop();
            }
        }

        @Override
        public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint)
        {
            // Do nothing
        }
    }
}
