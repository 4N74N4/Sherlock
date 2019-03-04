package com.bgu.agent.data.manager;

import android.content.Context;
import com.bgu.agent.base.IDataListener;
import com.bgu.agent.base.operations.IDataOperation;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.data.listeners.AbstractCommunicationListener;
import com.bgu.agent.data.listeners.RestClientListener;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/1/13
 * Time: 12:21 PM
 * modified 7/10/14 : registerListener -> before adding the listner checking its instance and changing the new "networkNeeded"
 * boolean field.
 * To change this template use File | Settings | File Templates.
 */
public class DataManager {
    private static Map<Context, DataManager> dataManager = new HashMap<Context, DataManager>();
    private Set<IDataListener> dataListeners = Collections.synchronizedSet(new HashSet<IDataListener>());

    public static DataManager getInstance (Context context){
        DataManager dataManagerInstance = dataManager.get(context);
        if ( dataManagerInstance == null ){
            Logger.d(DataManager.class, "Creating instance");
            dataManagerInstance = new DataManager();
            dataManager.put(context, dataManagerInstance);
        }
        return dataManagerInstance;
    }

    public void registerListener ( IDataListener ... listeners ){
        if ( listeners != null ){
            for (IDataListener listener : listeners) {
                if(listener instanceof RestClientListener) {  ((AbstractCommunicationListener) listener).NetworkNeeded=true;}
                dataListeners.add(listener);
            }
        }
    }

    public Set<IDataListener> getRegisteredListeners (){
        return dataListeners;
    }

    public void unRegisterListener(IDataListener... listeners) {
        if ( listeners != null ){
            for (IDataListener listener : listeners) {
                dataListeners.remove(listener);
            }
        }
    }

    public void performDataOperation ( IDataOperation operation, IRunnableWithParameters runnable,Executor... executors ) {
        for ( IDataListener listener:dataListeners ){
            operation.performOperation(listener, runnable, executors);
        }
    }

    public void Dispose(Context context) {
        unRegisterAllListeners();
        dataManager.remove(context);
    }

    public void unRegisterAllListeners() {
        IDataListener[] listeners = null;
        synchronized (dataListeners) {
            listeners = new IDataListener[dataListeners.size()];
            listeners = dataListeners.toArray(listeners);
        }
        unRegisterListener(listeners);
    }
}
