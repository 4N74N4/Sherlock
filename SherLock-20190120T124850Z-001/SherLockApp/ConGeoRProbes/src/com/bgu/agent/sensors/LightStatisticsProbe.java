package com.bgu.agent.sensors;

import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe;
import edu.mit.media.funf.probe.builtin.LightSensorProbe;

//Pipe line path:  com.bgu.agent.sensors.LightStatisticsProbe

@Schedule.DefaultSchedule(interval = 5, duration = 5 * Constants.SECOND)
@Probe.RequiredProbes(AccelerometerSensorProbe.class)
@Probe.DisplayName("LightStatisticsProbe")
public class LightStatisticsProbe extends SecureBase implements Probe.ContinuousProbe, Probe.PassiveProbe
{

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

        getLightSensorProbe().registerPassiveListener(activityCounter);
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        if (activityCounter != null)
        {
            getLightSensorProbe().registerListener(activityCounter);
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
                getLightSensorProbe().unregisterListener(activityCounter);
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
                getLightSensorProbe().unregisterPassiveListener(activityCounter);
            }
            catch (Throwable t)
            {

            }
        }
    }

    private LightSensorProbe getLightSensorProbe()
    {
        return getGson().fromJson(DEFAULT_CONFIG, LightSensorProbe.class);
    }


    private class ActivityCounter implements DataListener
    {

        private double intervalStartTime;

        private double sTime;

        private double eTime;

        // Sx = sum of x
        private double Sx;

        // Sxx = sum of (x * x)
        private double Sxx;

        private int count;

        private void reset()
        {
            // If more than an statInterval away, start a new scan
            count = 0;
            Sx = 0;
            Sxx = 0;

            Logger.d(getClass(), "Reset Statistics");
        }

        private void intervalReset()
        {
            if (count != 0 && count != 1)
            {

                double AVGx = Sx / count;
                double SSx = Sxx - (AVGx * AVGx * count);

                JsonObject data = new JsonObject();

                data.addProperty(Constants.SUM_LUX, Sx);
                data.addProperty(Constants.AVG_LUX, AVGx);
                data.addProperty(Constants.VAR_LUX, SSx / (count - 1));

                data.addProperty(Constants.START_SAMPLE_TIME, sTime);
                data.addProperty(Constants.END_SAMPLE_TIME, eTime);

                Logger.i(getClass(), "Interval Reset Statistics");

                sendData(data);

                count = 0;
                Sx = 0;
                Sxx = 0;

            }
            else
            {
                Logger.e(getClass(), "Number of Counts = 0 or 1");
            }
        }

        private void update(float lux)
        {
            count++;
            Sx += lux;
            Sxx += lux * lux;
        }


        @Override
        public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data)
        {
            try
            {
                if (getState() != State.RUNNING)
                {
                    Logger.i(LightStatisticsProbe.class, "Get event while not running");
                    try
                    {
                        getLightSensorProbe().unregisterListener(this);
                        getLightSensorProbe().unregisterPassiveListener(this);
                    }
                    catch (Throwable t)
                    {

                    }

                    return;
                }
                double timestamp = data.get(TIMESTAMP).getAsDouble();
                if (intervalStartTime == 0.0 || (timestamp >= intervalStartTime + 2 * statInterval))
                {
                    reset();
                    intervalStartTime = timestamp;
                    sTime = timestamp ;
                }
                else if (timestamp >= intervalStartTime + statInterval)
                {
                    eTime = sTime -timestamp ;
                    intervalReset();
                    intervalStartTime = timestamp;
                }
                float lux = data.get(LightSensorProbe.LUX).getAsFloat();
                update(lux);
            }
            catch (Throwable t)
            {
                sendErrorLog(t);
            }
        }

        @Override
        public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint)
        {
            // Do nothing
        }
    }
}

