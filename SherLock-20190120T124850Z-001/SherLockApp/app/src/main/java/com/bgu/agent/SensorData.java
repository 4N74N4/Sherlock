package com.bgu.agent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/1/13
 * Time: 11:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class SensorData implements KryoSerializable {
    private String sensorName;
    transient private Long ts = null;
    transient private Long dbInsertTime = null;
    private String value;

    public SensorData (){

    }

    public SensorData(String sensorName, long ts, long dbInsertTime, String value) {
        this.sensorName = sensorName;
        this.ts = ts;
        this.value = value;
        this.dbInsertTime = dbInsertTime;
    }

    public String getSensorName() {
        return sensorName;
    }

    public long getTs() {
        return ts;
    }

    public long getDBInsertTime (){
        return dbInsertTime;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        output.writeString(sensorName);
        output.writeString(value);
    }

    @Override
    public void read(Kryo kryo, Input input) {
        sensorName = input.readString();
        value = input.readString();
    }

    @Override
    public String toString(){
        return this.getSensorName() + ": " + this.getValue();
    }
}
