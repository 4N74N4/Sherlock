package com.bgu.agent.data.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class GeneralException extends CongeorException {
    public GeneralException(Exception e) {
        super(999, e);
    }
}
