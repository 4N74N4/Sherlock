package com.bgu.agent.data.operations;

import android.content.Context;
import android.content.Intent;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.ErrorSensor;
import com.bgu.congeor.Constants;

import java.util.Map;

/**
 * Created by clint on 1/28/14.
 */
public class CheckForConfigurationUpdate implements IRunnableWithParameters {

    private final IRunnableWithParameters runnable;
    Context context;

    public CheckForConfigurationUpdate(Context context, IRunnableWithParameters runnable) {
        this.runnable = runnable;
        this.context = context;
    }

    @Override
    public void run(Object... params) {
        boolean agentConfigChanged = false;
        boolean publicConfigChanged = false;
        Logger.d(this.getClass(), "Status = " + ((Integer) params[1]).intValue());
        Constants.ENUM_SEND_DATA_STATUS sendDataStatus = Constants.ENUM_SEND_DATA_STATUS.CONFIG_NOT_CHANGED;
        if ( params.length > 2 ){
            if ( ((Integer) params [ 1 ]).intValue() == 0 ){
                if ( params [ 2 ] != null ){
                    Map<String, Object> returnVal = ( Map<String, Object> ) params [ 2 ];
                    Logger.d(this.getClass(), ((Constants.ENUM_SEND_DATA_STATUS) returnVal.get(Constants.SEND_DATA_STATUS)).name());
                    if ( returnVal.containsKey(Constants.CONF_AGENT)){
                        try {
                            Configuration agentConfiguration = Configuration.loadConfiguration(false, Constants.CONF_AGENT_FILENAME,
                                    returnVal.get(Constants.CONF_AGENT).toString());
                            agentConfiguration.updateConfiguration(context);
                            Logger.d(this.getClass(), "SendData - Received an agent configuration change from server: " + agentConfiguration.toString());
                            agentConfigChanged = true;
                        } catch (Throwable e) {
                            Intent error = ErrorSensor.generateErrorIntent(this.getClass(), e);
                            context.sendBroadcast(error);
                            Logger.e(CheckForConfigurationUpdate.class, e.getLocalizedMessage(), e);
                        }
                    }
                    else
                        Logger.d(this.getClass(), "SendData - There was no change in agent configuration.");
                    if ( returnVal.containsKey(Constants.CONF_PUBLIC_COMM)){
                        try {
                            Configuration publicCommConfiguration = Configuration.loadConfiguration(false, Constants.CONF_PUBLIC_COMM_FILENAME,
                                    returnVal.get(Constants.CONF_PUBLIC_COMM).toString());
                            publicCommConfiguration.updateConfiguration(context);
                            Logger.d(CheckForConfigurationUpdate.class, "SendData - Received a public configuration change from server " + publicCommConfiguration.toString());
                            publicConfigChanged = true;
                        } catch (Throwable e) {
                            Intent error = ErrorSensor.generateErrorIntent(this.getClass(), e);
                            context.sendBroadcast(error);
                            Logger.e(CheckForConfigurationUpdate.class, e.getLocalizedMessage(), e);
                        }
                    }
                    else
                        Logger.d(this.getClass(), "SendData - There was no change in public configuration.");
                    if ( returnVal.containsKey(Constants.SEND_DATA_STATUS)){
                        sendDataStatus = (Constants.ENUM_SEND_DATA_STATUS) returnVal.get(Constants.SEND_DATA_STATUS);
                    }
                }
            }
        }
        runnable.run(((Integer) params[1]).intValue(), agentConfigChanged, publicConfigChanged, sendDataStatus);
    }
}
