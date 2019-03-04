package com.bgu.agent.data.exceptions;

import com.bgu.congeor.Constants;

/**
 * Created by clint on 12/24/13.
 */
public class BuildRequestException extends CongeorException {
    public BuildRequestException(Throwable e) {
        super(Constants.ERROR_BUILD_REQUEST, e);
    }
}
