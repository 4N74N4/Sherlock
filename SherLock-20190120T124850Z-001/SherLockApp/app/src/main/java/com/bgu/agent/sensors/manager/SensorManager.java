package com.bgu.agent.sensors.manager;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.os.Build;
import com.bgu.agent.SensorData;
import com.bgu.agent.sensors.datalayer.ISensorPersistence;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/2/13
 * Time: 7:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class SensorManager
{

    private static SensorManager instance;

    private android.hardware.SensorManager sensorManager;

    ISensorPersistence sensorPersistence;

    HashSet<Integer> hardwareAvailableSensorTypes = new HashSet<Integer>();

    public SensorManager()
    {
    }

    public static String getSensorsModuleVersion()
    {
        return "1.0";
    }

    public void init(Context appContext)
    {
        sensorManager = (android.hardware.SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        instance.initAvailableSensorTypes();
    }

    public static SensorManager getInstance()
    {
        if (instance == null)
        {
            instance = new SensorManager();
        }
        return instance;
    }

    public static void setInstance(SensorManager _instance)
    {
        instance = _instance;
    }

    public void setSensorPersistence(ISensorPersistence sensorPersistence)
    {
        this.sensorPersistence = sensorPersistence;
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public void initAvailableSensorTypes()
    {
        // TODO: update phone inner configuration with the available sensors.
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor i : sensors)
        {
            hardwareAvailableSensorTypes.add(i.getType());
        }
    }

    public ArrayList<SensorData> getSensorData(HashSet<String> inputs, long timestamp)
    {
        ArrayList<SensorData> data = new ArrayList<SensorData>();
        ArrayList<SensorData> dataLastSensors;
        for (String sensor : inputs)
        {
            dataLastSensors = sensorPersistence.getData(sensor, timestamp);
            data.addAll(dataLastSensors);
        }
        Collections.sort(data, new Comparator<SensorData>()
        {
            @Override
            public int compare(SensorData lhs, SensorData rhs)
            {
                if (lhs.getDBInsertTime() < rhs.getDBInsertTime())
                {
                    return -1;
                }
                if (lhs.getDBInsertTime() > rhs.getDBInsertTime())
                {
                    return 1;
                }
                return 0;
            }
        });
        return data;
    }

    public boolean areSensorsAvailable(Collection<Integer> sensorTypes)
    {
        for (Integer sensorType : sensorTypes)
        {
            if (!hardwareAvailableSensorTypes.contains(sensorType))
            {
                return false;
            }
        }
        return true;
    }

    public void destroy()
    {
        sensorPersistence = null;
        if (hardwareAvailableSensorTypes != null)
        {
            hardwareAvailableSensorTypes.clear();
        }
        hardwareAvailableSensorTypes = null;
    }
}
