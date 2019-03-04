package com.bgu.agent.sensors.provider;

import com.bgu.agent.SensorData;
import com.bgu.agent.base.IDataProvider;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.manager.SensorManager;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/1/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class SensorsDataProvider implements IDataProvider<SensorData []> {

    long lastTimestamp;
    SensorManager sensorManager;
    HashSet<String> providedSensors;

    public SensorsDataProvider(SensorManager sensorManager){
        this.sensorManager = sensorManager;
        this.providedSensors = new HashSet();
    }

    public void setProvidedSensors ( String [] providedSensors ){
        for ( String i:providedSensors )
            this.providedSensors.add(i);
    }

    public String [] getProvidedSensors (){
        return providedSensors.toArray(new String [ 0 ]);
    }


    @Override
    public SensorData [] getData(long timestamp) {
        ArrayList<SensorData> data = sensorManager.getSensorData(providedSensors, timestamp);
        //String currentTime = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        lastTimestamp = data.size() == 0 ? 0 : data.get(data.size()-1).getDBInsertTime();
        SensorData[] sds = new SensorData[data.size()];
        Logger.i(SensorsDataProvider.class, "Retrieved from funf db " + data.size() + " records" );
        Logger.i(SensorsDataProvider.class, "Sensors values when retrieved from funf: " + data.toString() );
        return data.toArray(sds);
    }

    @Override
    public long getLastTimeStampInData() {
        return lastTimestamp;
    }
}
