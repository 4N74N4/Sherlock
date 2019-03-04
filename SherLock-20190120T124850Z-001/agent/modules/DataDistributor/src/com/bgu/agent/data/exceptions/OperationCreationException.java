package com.bgu.agent.data.exceptions;

import com.bgu.congeor.Constants;

/**
 * Created by clint on 12/24/13.
 */
public class OperationCreationException extends CongeorException {
    public OperationCreationException(Throwable e) {
        super(Constants.OPERATION_CREATION_EXCEPTION, e);
    }
}
