package com.bgu.agent.sensors;

import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.RotationVectorSensorProbe;

/**
 * Created by simondzn on 24/09/2015.
 */

@Probe.DisplayName("RotationVector")
public class RotationProbe extends SecureBase implements Probe.ContinuousProbe, Probe.PassiveProbe {

    @Configurable
    private double statInterval = 1 * Constants.SECOND;

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

        getRotationVectorProbe().registerPassiveListener(activityCounter);
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        if (activityCounter != null)
        {
            getRotationVectorProbe().registerListener(activityCounter);
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
                getRotationVectorProbe().unregisterListener(activityCounter);
            }
            catch (Throwable t)
            {

            }
        }
    }

    @Override
    protected void secureOnDisable()
    {

        super.secureOnDisable();
        if (activityCounter != null)
        {
            try
            {
                getRotationVectorProbe().unregisterPassiveListener(activityCounter);
            }
            catch (Throwable t)
            {

            }
        }
    }

    private RotationVectorSensorProbe getRotationVectorProbe()
    {
        return getGson().fromJson(DEFAULT_CONFIG, RotationVectorSensorProbe.class);
    }


    private class ActivityCounter implements Probe.DataListener
    {
        int count = 0;

        @Override
        public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data)
        {
            count++;
            if(count >= 5) {
                JsonObject sensorData = new JsonObject();
                float x = data.get(RotationVectorSensorProbe.X_SIN_THETA_OVER_2).getAsFloat();
                float y = data.get(RotationVectorSensorProbe.Y_SIN_THETA_OVER_2).getAsFloat();
                float z = data.get(RotationVectorSensorProbe.Z_SIN_THETA_OVER_2).getAsFloat();
                sensorData.addProperty("PITCH", x);
                sensorData.addProperty("ROLL", y);
                sensorData.addProperty("AZIMUTH", z);
                sendData(sensorData);
                Log.d(RotationProbe.class.toString(), "sent Rotation: " + "pitch: " + x + " ROLL: " + y + " AZIMUTH: " + z);
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
