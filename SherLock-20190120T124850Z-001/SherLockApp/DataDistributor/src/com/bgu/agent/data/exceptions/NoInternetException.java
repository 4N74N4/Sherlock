package com.bgu.agent.data.exceptions;

import com.bgu.congeor.Constants;

/**
 * Created by clint on 12/24/13.
 */
public class NoInternetException extends CongeorException {
    public NoInternetException(Throwable e) {
        super(Constants.NO_INTERNET_EXCEPTION, e);
    }
}
