package com.bgu.agent.base;

import com.bgu.agent.data.exceptions.CongeorException;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IContinuousDataListener extends IDataListener {
    Long getLastTimeStamp () throws CongeorException;
    public void setLastTimeStamp(long timestamp) throws CongeorException;
}
