package com.bgu.agent.data.converters;


/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/1/13
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonConverter implements IDataConverter {

    @Override
    public String convert(Object data) {
        return com.bgu.utils.Utils.jsonToString(data);  //To change body of implemented methods use File | Settings | File Templates.
    }
}
