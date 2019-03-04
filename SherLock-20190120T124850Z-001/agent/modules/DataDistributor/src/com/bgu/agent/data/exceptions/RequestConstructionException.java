package com.bgu.agent.data.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 9:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestConstructionException extends CongeorException {
    public RequestConstructionException(Exception e) {
        super(992, e);
    }
}
