package com.bgu.agent.data.exceptions;

import com.bgu.congeor.Constants;

/**
 * Created by clint on 12/24/13.
 */
public class GetTimestampException extends CongeorException {
    public GetTimestampException(Throwable e) {
        super(Constants.GET_TIMESTAMP_EXCEPTION, e);
    }
}
