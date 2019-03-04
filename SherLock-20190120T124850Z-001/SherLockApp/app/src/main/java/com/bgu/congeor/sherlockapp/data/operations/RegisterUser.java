package com.bgu.congeor.sherlockapp.data.operations;

import com.bgu.agent.base.IDataListener;
import com.bgu.agent.base.operations.AbstractDataOperation;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.data.exceptions.CongeorException;
import com.bgu.agent.data.exceptions.OperationCreationException;
import com.bgu.congeor.Constants;
import com.bgu.congeor.UserDetails;

import java.lang.ref.WeakReference;
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
public class RegisterUser extends AbstractDataOperation<Map<String, Object>> {

    String experimentId;
    WeakReference<UserDetails> userDetails;


    public RegisterUser(String experimentId,  UserDetails userDetails) {
        super();
        this.experimentId = experimentId;
        this.userDetails = new WeakReference<UserDetails>(userDetails);
    }

    @Override
    public void performOperation(IDataListener listener, IRunnableWithParameters runnable, Executor... executors){
        new RegisterUserAsync(listener, runnable).execute();
    }

    private class RegisterUserAsync extends AsyncOperation {

        public RegisterUserAsync(IDataListener listener, IRunnableWithParameters runnable) {
            super ( listener, runnable );
        }

        @Override
        protected Map<String, Object> doInBackground(Object... params){
            setThreadName();
            return performRegisterUser();
        }

        public Map<String, Object> performRegisterUser(){
            Map<String, Object> returnVal = null;
            IDataListener listener = this.dataListener.get();
            Map<String, String> params = new LinkedHashMap<String, String>();
            try {
                params.put(Constants.EXPERIMENT_ID, experimentId);
//                params.put(Constants.PHONE_ID, phoneId);
            }
            catch (Throwable e) {
                onFail(new OperationCreationException( e ));
                return null;
            }
            try {
                returnVal = listener.processObject("users_" + Constants.REQ_REGISTER_USER_ENC, userDetails.get(), params, HashMap.class, true, false);
            }
            catch ( CongeorException e ){
                onFail(e);
                return null;
            }
            onSuccess();
            return returnVal;
        }
    }
}
