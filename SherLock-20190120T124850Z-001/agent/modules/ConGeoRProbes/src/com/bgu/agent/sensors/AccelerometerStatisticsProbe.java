package com.bgu.agent.sensors;

import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.*;
import edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe;

//Pipe line path:  com.bgu.agent.sensors.AccelerometerStatisticsProbe

@Schedule.DefaultSchedule(interval = 5, duration = 5 * Constants.SECOND)
@RequiredFeatures("android.hardware.sensor.accelerometer")
@RequiredProbes(AccelerometerSensorProbe.class)
@DisplayName("AccelerometerStatisticsProbe")
public class AccelerometerStatisticsProbe extends SecureBase implements ContinuousProbe, PassiveProbe
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
        getAccelerometerProbe().registerPassiveListener(activityCounter);
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        if (activityCounter != null)
        {
            getAccelerometerProbe().registerListener(activityCounter);
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
                getAccelerometerProbe().unregisterListener(activityCounter);
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
                getAccelerometerProbe().unregisterPassiveListener(activityCounter);
            }
            catch (Throwable t)
            {

            }

        }
    }

    private AccelerometerSensorProbe getAccelerometerProbe()
    {
        return getGson().fromJson(DEFAULT_CONFIG, AccelerometerSensorProbe.class);
    }


    private class ActivityCounter implements DataListener
    {

        private double intervalStartTime;

        private double sTime;

        private double eTime;


        // Sx = sum of x
        private double Sx;

        private double Sy;

        private double Sz;

        // Sxx = sum of (x * x)
        private double Sxx;

        private double Syy;

        private double Szz;

        // Sxy = sum of (x * y)
        private double Sxy;

        private double Sxz;

        private double Syz;

        // S_ABSx = sum of |x|
        private double S_ABSx;

        private double S_ABSy;

        private double S_ABSz;

        // S_ABSxy = sum of (|x| * |y|)
        private double S_ABSxy;

        private double S_ABSxz;

        private double S_ABSyz;


        private int count;

        private void reset()
        {
            // If more than an statInterval away, start a new scan
            count = 0;
            Sx = Sy = Sz = 0;
            S_ABSx = S_ABSy = S_ABSz = 0;
            Sxx = Syy = Szz = 0;
            Sxy = Sxz = Syz = 0;
            S_ABSxy = S_ABSxz = S_ABSyz = 0;

            Logger.d(getClass(), "Reset Statistics");
        }

        private void intervalReset()
        {
            if (count != 0 && count != 1)
            {

                double AVGx = Sx / count;
                double AVGy = Sy / count;
                double AVGz = Sz / count;

                double SSx = Sxx - (AVGx * AVGx * count);
                double SSy = Syy - (AVGy * AVGy * count);
                double SSz = Szz - (AVGz * AVGz * count);

                double SSxy = Sxy - (AVGx * AVGy * count);
                double SSxz = Sxz - (AVGx * AVGz * count);
                double SSyz = Syz - (AVGy * AVGz * count);

                double AVG_ABSx = S_ABSx / count;
                double AVG_ABSy = S_ABSy / count;
                double AVG_ABSz = S_ABSz / count;

                double SS_ABSx = Sxx - (AVG_ABSx * AVG_ABSx * count);
                double SS_ABSy = Syy - (AVG_ABSy * AVG_ABSy * count);
                double SS_ABSz = Szz - (AVG_ABSz * AVG_ABSz * count);

                double SS_ABSxy = S_ABSxy - (AVG_ABSx * AVG_ABSy * count);
                double SS_ABSxz = S_ABSxz - (AVG_ABSx * AVG_ABSz * count);
                double SS_ABSyz = S_ABSyz - (AVG_ABSy * AVG_ABSz * count);


                JsonObject data = new JsonObject();

                data.addProperty(Constants.SUM_X, Sx);
                data.addProperty(Constants.SUM_Y, Sy);
                data.addProperty(Constants.SUM_Z, Sz);

                data.addProperty(Constants.SUM_XY, Sxy);
                data.addProperty(Constants.SUM_XZ, Sxz);
                data.addProperty(Constants.SUM_YZ, Syz);

                data.addProperty(Constants.AVG_X, AVGx);
                data.addProperty(Constants.AVG_Y, AVGy);
                data.addProperty(Constants.AVG_Z, AVGz);

                data.addProperty(Constants.VAR_X, SSx / (count - 1));
                data.addProperty(Constants.VAR_Y, SSy / (count - 1));
                data.addProperty(Constants.VAR_Z, SSz / (count - 1));

                data.addProperty(Constants.COV_XY, SSxy / (count - 1));
                data.addProperty(Constants.COV_XZ, SSxz / (count - 1));
                data.addProperty(Constants.COV_YZ, SSyz / (count - 1));

                if ((SSx * SSy) <= 0 || (SSx * SSz) <= 0 || (SSy * SSz) <= 0 || (SS_ABSx * SS_ABSy) <= 0 || (SS_ABSx
                        * SS_ABSz) <= 0 || (SS_ABSy * SS_ABSz) <= 0)
                {
                    data.addProperty(Constants.R_XY, Constants.NOT_AVAILABLE);
                    data.addProperty(Constants.R_XZ, Constants.NOT_AVAILABLE);
                    data.addProperty(Constants.R_YZ, Constants.NOT_AVAILABLE);
                    data.addProperty(Constants.R_ABS_XY, Constants.NOT_AVAILABLE);
                    data.addProperty(Constants.R_ABS_XZ, Constants.NOT_AVAILABLE);
                    data.addProperty(Constants.R_ABS_YZ, Constants.NOT_AVAILABLE);
                }
                else
                {
                    data.addProperty(Constants.R_XY, SSxy / Math.sqrt(SSx * SSy));
                    data.addProperty(Constants.R_XZ, SSxz / Math.sqrt(SSx * SSz));
                    data.addProperty(Constants.R_YZ, SSyz / Math.sqrt(SSy * SSz));
                    data.addProperty(Constants.R_ABS_XY, SS_ABSxy / Math.sqrt(SS_ABSx * SS_ABSy));
                    data.addProperty(Constants.R_ABS_XZ, SS_ABSxz / Math.sqrt(SS_ABSx * SS_ABSz));
                    data.addProperty(Constants.R_ABS_YZ, SS_ABSyz / Math.sqrt(SS_ABSy * SS_ABSz));
                }

                data.addProperty(Constants.SUM_ABS_X, S_ABSx);
                data.addProperty(Constants.SUM_ABS_Y, S_ABSy);
                data.addProperty(Constants.SUM_ABS_Z, S_ABSz);

                data.addProperty(Constants.SUM_ABS_XY, S_ABSxy);
                data.addProperty(Constants.SUM_ABS_XZ, S_ABSxz);
                data.addProperty(Constants.SUM_ABS_YZ, S_ABSyz);


                data.addProperty(Constants.AVG_ABS_X, AVG_ABSx);
                data.addProperty(Constants.AVG_ABS_Y, AVG_ABSy);
                data.addProperty(Constants.AVG_ABS_Z, AVG_ABSz);

                data.addProperty(Constants.VAR_ABS_X, SS_ABSx / (count - 1));
                data.addProperty(Constants.VAR_ABS_Y, SS_ABSy / (count - 1));
                data.addProperty(Constants.VAR_ABS_Z, SS_ABSz / (count - 1));

                data.addProperty(Constants.COV_ABS_XY, SS_ABSxy / (count - 1));
                data.addProperty(Constants.COV_ABS_XZ, SS_ABSxz / (count - 1));
                data.addProperty(Constants.COV_ABS_YZ, SS_ABSyz / (count - 1));

                data.addProperty(Constants.START_SAMPLE_TIME, sTime);
                data.addProperty(Constants.END_SAMPLE_TIME, eTime);

                Logger.i(getClass(), "Interval Reset Statistics");

                sendData(data);

                count = 0;
                Sx = Sy = Sz = 0;
                S_ABSx = S_ABSy = S_ABSz = 0;
                Sxx = Syy = Szz = 0;
                Sxy = Sxz = Syz = 0;
                S_ABSxy = S_ABSxz = S_ABSyz = 0;

            }
            else
            {
                Logger.e(getClass(), "Number of Counts = 0 or 1");
            }
        }

        private void update(float x, float y, float z)
        {
            count++;
            Sx += x;
            Sy += y;
            Sz += z;
            Sxx += x * x;
            Syy += y * y;
            Szz += z * z;
            Sxy = x * y;
            Sxz = x * z;
            Syz = y * z;
            S_ABSx += Math.abs(x);
            S_ABSy += Math.abs(y);
            S_ABSz += Math.abs(z);
            S_ABSxy = Math.abs(x) * Math.abs(y);
            S_ABSxz = Math.abs(x) * Math.abs(z);
            S_ABSyz = Math.abs(y) * Math.abs(z);
        }


        @Override
        public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data)
        {
            try
            {
                if (getState() != State.RUNNING)
                {
                    Logger.i(AccelerometerStatisticsProbe.class, "Get event while not running");
                    try
                    {
                        getAccelerometerProbe().unregisterListener(this);
                        getAccelerometerProbe().unregisterPassiveListener(this);
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
                    eTime = timestamp- sTime;
                    intervalReset();
                    intervalStartTime = timestamp;
                }
                float x = data.get(AccelerometerSensorProbe.X).getAsFloat();
                float y = data.get(AccelerometerSensorProbe.Y).getAsFloat();
                float z = data.get(AccelerometerSensorProbe.Z).getAsFloat();
                update(x, y, z);
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
