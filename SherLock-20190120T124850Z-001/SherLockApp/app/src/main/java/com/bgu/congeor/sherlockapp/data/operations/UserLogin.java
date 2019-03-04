package com.bgu.congeor.sherlockapp.data.operations;

import com.bgu.agent.base.IDataListener;
import com.bgu.agent.base.operations.AbstractDataOperation;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.data.exceptions.CongeorException;
import com.bgu.agent.data.exceptions.OperationCreationException;
import com.bgu.congeor.Constants;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
* Created with IntelliJ IDEA.
* User: clint
* Date: 12/3/13
* Time: 4:51 PM
* To change this template use File | Settings | File Templates.
*/
public class UserLogin extends AbstractDataOperation<Map<String, Object>> {

    String phoneId;
    String experimentId;
    String publicAgentConfigHash = null;
    String publicCommConfigHash = null;
    String versionCode;
    String versionName;
    String sensorsModuleVersion;

    public UserLogin( String phoneId, String experimentId, Integer publicAgentConfigHash, Integer publicCommConfigHash, String versionCode, String versionName,
                      String sensorsModuleVersion) {
        super();
        this.phoneId = phoneId;
        this.experimentId = experimentId;
        this.publicAgentConfigHash = publicAgentConfigHash != null ? "" + publicAgentConfigHash : "";
        this.publicCommConfigHash = publicCommConfigHash != null ? "" + publicCommConfigHash : "";
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.sensorsModuleVersion = sensorsModuleVersion;
    }

    @Override
    public void performOperation(IDataListener listener, IRunnableWithParameters runnable, Executor... executors){
        new UserLoginAsync(listener, runnable).execute();
    }

    private class UserLoginAsync extends AsyncOperation {

        public UserLoginAsync(IDataListener listener, IRunnableWithParameters runnable) {
            super(listener, runnable);
        }

        @Override
        protected Map<String, Object> doInBackground(Object... params){
            setThreadName();
            return performLogin();
        }

        public Map<String, Object> performLogin(){
            Map<String, Object> returnVal = null;
            Map<String, String> params = new LinkedHashMap<String, String>();
            IDataListener listener = this.dataListener.get();
            try {
                params.put(Constants.EXPERIMENT_ID, experimentId);
                params.put(Constants.PHONE_ID, phoneId);
                String publicAgentConfigHashStr = "" + ( publicAgentConfigHash != null ? publicAgentConfigHash : "" );
                String publicCommConfigHashStr = "" + ( publicCommConfigHash != null ? publicCommConfigHash : "" );
                params.put(Constants.CONF_COMM_HASH, publicCommConfigHashStr);
                params.put(Constants.CONF_AGENT_HASH, publicAgentConfigHashStr);
                params.put(Constants.VERSION_CODE, versionCode);
                params.put(Constants.VERSION_NAME, versionName);
                params.put(Constants.SENSORS_MODULE_VERSION, sensorsModuleVersion);

            } catch (Throwable e) {
                onFail(new OperationCreationException ( e ));
                return null;
            }
            try {
                returnVal = listener.processObject("users_" + Constants.REQ_LOGIN, null, params, HashMap.class, false, true);
            }
            catch ( CongeorException ex ){
                onFail(ex);
                return null;
            }
            onSuccess();
            return returnVal;
        }
    }
}
