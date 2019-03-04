package com.bgu.agent.data.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class CongeorException extends Throwable {
    int statusCode;

    public CongeorException ( int code, Throwable e ){
        super ( e );
        this.statusCode = code;
    }

    public int getStatusCode(){
        return statusCode;
    }
}
