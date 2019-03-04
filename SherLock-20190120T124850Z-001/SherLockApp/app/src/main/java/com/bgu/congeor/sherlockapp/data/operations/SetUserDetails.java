package com.bgu.congeor.sherlockapp.data.operations;

import com.bgu.agent.base.IDataListener;
import com.bgu.agent.base.operations.AbstractDataOperation;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.data.exceptions.CongeorException;
import com.bgu.agent.data.listeners.FileListener;
import com.bgu.congeor.Constants;
import com.bgu.congeor.UserDetails;

import java.lang.ref.WeakReference;
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
public class SetUserDetails extends AbstractDataOperation<Void> {

    WeakReference<UserDetails> userDetails;

    public SetUserDetails(UserDetails userDetails) {
        super();
        this.userDetails = new WeakReference<UserDetails>(userDetails);
    }

    @Override
    public void performOperation(IDataListener listener, IRunnableWithParameters runnable, Executor... executors){
        new setUserDetailsAsync(listener, runnable).execute();
    }

    private class setUserDetailsAsync extends AsyncOperation {

        public setUserDetailsAsync(IDataListener listener, IRunnableWithParameters runnable) {
            super(listener, runnable);
        }

        @Override
        protected Void doInBackground(Object... params){
            setThreadName();
            return setUserDetails();
        }

        public Void setUserDetails(){
            Void returnVal = null;
            IDataListener listener = this.dataListener.get();
            if (!( listener instanceof FileListener )){
                Map<String, String> params = new LinkedHashMap<String, String>();
                try {
                    returnVal = listener.processObject("users_" + Constants.ENC_REQ_UPDATE_USER_DETAILS, userDetails.get(), params, Void.class, true, false);
                }
                catch ( CongeorException ex ){
                    onFail(ex);
                    return null;
                }
            }
            onSuccess();
            return returnVal;
        }
    }
}