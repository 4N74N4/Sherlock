package com.bgu.agent.data.exceptions;

import com.bgu.congeor.Constants;

/**
 * Created by clint on 12/24/13.
 */
public class UpdateTimestampException extends CongeorException {
    public UpdateTimestampException(Throwable e) {
        super(Constants.UPDATE_TIMESTAMP_EXCEPTION, e);
    }
}
