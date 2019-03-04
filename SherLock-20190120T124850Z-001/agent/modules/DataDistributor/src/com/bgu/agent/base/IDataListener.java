package com.bgu.agent.base;

import com.bgu.agent.data.exceptions.CongeorException;
import com.google.gson.JsonElement;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/1/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IDataListener {

    String getDataListenerName ();
    public <T> T processObject(String method, Object payload, Map<String, String> params, Class<T> returnClassType, boolean encrypt, boolean GET) throws CongeorException;
    public JsonElement processJsonElement(String method, Object payload, Map<String, String> params, boolean encrypt, boolean GET) throws CongeorException;
}