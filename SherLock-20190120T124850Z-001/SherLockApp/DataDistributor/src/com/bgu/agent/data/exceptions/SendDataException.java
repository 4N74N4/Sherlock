package com.bgu.agent.data.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 4:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class SendDataException extends CongeorException {
    public SendDataException(Exception e){
        super(991, e);
    }
}
