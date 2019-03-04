package com.bgu.agent.sensors;

import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe;

import java.math.BigDecimal;

/**
 * Created by BittonRon on 3/17/14.
 */


@Schedule.DefaultSchedule(interval = 1 * Constants.MINUTE, duration = 15 * Constants.SECOND)
@Probe.RequiredProbes(AccelerometerSensorProbe.class)
@Probe.DisplayName("TestProbe")
public class TestProbe extends ContinuousDataProbe
{

    long startTime;

    long stopTime;

    long sendDataTime = 0;


    public TestProbe()
    {
        super();
        maxWaitTime = BigDecimal.valueOf(14);
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();
        Logger.i(getClass(), "Enable");
    }

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        startTime = System.currentTimeMillis();
        Logger.i(getClass(), "Start");
    }

    @Override
    protected void secureOnStop()
    {
        super.secureOnStop();
        stopTime = System.currentTimeMillis();
        Logger.i(getClass(), "Stop");
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        Logger.i(getClass(), "Disable");
        Logger.i(getClass(), "Time Until send data is : " + ((sendDataTime - startTime) / 1000));
        Logger.i(getClass(), "Duration is : " + ((stopTime - startTime) / 1000));
    }

    @Override
    protected void endOfTimeSendData()
    {
        sendDataTime = System.currentTimeMillis();
        JsonObject data = new JsonObject();
        data.addProperty("Clint", "Ron");
        Logger.i(getClass(), "Sending Data");

    }

}
