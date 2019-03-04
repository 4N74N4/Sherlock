package com.bgu.agent.data.exceptions;

import com.bgu.congeor.Constants;

/**
 * Created by clint on 12/24/13.
 */
public class RestSendException extends CongeorException {
    public RestSendException(Throwable e) {
        super(Constants.REST_SEND_EXCEPTION, e);          }
}
