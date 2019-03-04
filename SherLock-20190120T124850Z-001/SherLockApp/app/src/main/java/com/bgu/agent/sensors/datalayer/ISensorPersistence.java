package com.bgu.agent.sensors.datalayer;

import com.bgu.agent.SensorData;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/4/13
 * Time: 11:26 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ISensorPersistence
{

    public final int SENSOR_PERSISTANCE_INITIALIZED = 900;

    ArrayList<SensorData> getData(String nameOfSensor, long timestamp);

    Object getSensorConfiguration(String nameOfSensor);

    Object getManager();

    void disableSensorProbing();

    void enableSensorProbing();
}
