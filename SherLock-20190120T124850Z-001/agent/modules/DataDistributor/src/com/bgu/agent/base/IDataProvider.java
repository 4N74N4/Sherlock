package com.bgu.agent.base;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/1/13
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IDataProvider<T> {
    T getData ( long timestamp );
    long getLastTimeStampInData ();
    void setProvidedSensors(String[] sensorsToSend);
}
