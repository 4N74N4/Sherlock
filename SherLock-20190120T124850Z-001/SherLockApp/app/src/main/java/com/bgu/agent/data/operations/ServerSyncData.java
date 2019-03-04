package com.bgu.agent.data.operations;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.bgu.agent.base.IContinuousDataListener;
import com.bgu.agent.base.IDataListener;
import com.bgu.agent.base.IDataProvider;
import com.bgu.agent.base.operations.AbstractDataOperation;
import com.bgu.agent.commons.IAppContextProvider;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.data.exceptions.CongeorException;
import com.bgu.agent.data.exceptions.OperationCreationException;
import com.bgu.agent.data.listeners.AbstractCommunicationListener;
import com.bgu.congeor.Constants;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerSyncData extends AbstractDataOperation<Map<String, Object>> {

    private final WeakReference<IDataProvider> dataProvider;

    String publicAgentConfigHash;
    String publicCommConfigHash;
    String androidId;
    String userId;
    String experimentId;
    boolean sendOn3G;
    WeakReference<IAppContextProvider> appContext;
    String versionCode;
    String versionName;
    String ConfigVersion;
    String sensorsModuleVersion;
    long intervalFromConf;

    public ServerSyncData(IDataProvider dataProvider, IAppContextProvider appContextProvider, Integer publicAgentConfigHash, Integer publicCommConfigHash, boolean sendOn3G, String userName,
                          String androidId, String experimentId, String versionCode, String versionName,String version, String sensorsModuleVersion, long intervalFromConf) {
        super();
        if ( dataProvider == null )
            Logger.e(this.getClass(), "dataProvider is null in constructor");
        this.dataProvider          = new WeakReference<IDataProvider>( dataProvider );
        this.publicAgentConfigHash = publicAgentConfigHash != null ? "" + publicAgentConfigHash : "";
        this.publicCommConfigHash  = publicCommConfigHash != null ? "" + publicCommConfigHash : "";
        this.sendOn3G              = sendOn3G;
        this.androidId             = androidId;
        this.userId                = userName;
        this.experimentId          = experimentId;
        this.appContext            = new WeakReference<IAppContextProvider>(appContextProvider);
        this.versionCode           = versionCode;
        this.versionName           = versionName;
        this.ConfigVersion         = version;
        this.sensorsModuleVersion  = sensorsModuleVersion;
        this.intervalFromConf      = intervalFromConf;
    }

    @Override
    public void performOperation(IDataListener listener, IRunnableWithParameters runnable, Executor ... executors){
        boolean netNeed = ((AbstractCommunicationListener) listener).NetworkNeeded;
        if( !netNeed && (this.sendOn3G || isConnectedToWifi()))
            if(executors.length==0)
                new SyncDataAsync(listener, runnable).execute();
            else
                new SyncDataAsync(listener, runnable).executeOnExecutor(executors[0]);
        else {
            status = Constants.THREEG_NO_SYNC;
            onPostRun(runnable, listener, null);
        }
    }

    public boolean isConnectedToWifi(){
        ConnectivityManager connManager = (ConnectivityManager) appContext.get().getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    protected class SyncDataAsync extends AsyncOperation {

        public SyncDataAsync(IDataListener listener, IRunnableWithParameters runnable) {
            super(listener, runnable);
        }

        @Override
        protected Map<String, Object> doInBackground(Object... params){
            setThreadName();
            Map<String, Object> returnVal = syncData();
            return returnVal;
        }

        public Map<String, Object> syncData(){
            SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);
            Map<String, Object> returnVal = null;
            IDataListener listener = this.dataListener.get();
            if ( listener instanceof IContinuousDataListener ){
                long timestamp = 0L;
                IContinuousDataListener continuousDataListener = ( IContinuousDataListener ) listener;
                Map<String, Object> dataRequest = new HashMap<String, Object>();
                Map<String, String> params = new LinkedHashMap<String, String>();
                Long lastSyncTime = null;
                boolean isEmpty = true;
                try {
                    Date currentTime = Calendar.getInstance().getTime();
                    if ( continuousDataListener.getLastTimeStamp() == null || continuousDataListener.getLastTimeStamp() == -5){
                        timestamp = currentTime.getTime() - intervalFromConf;
                    }
                    else {
                        timestamp = continuousDataListener.getLastTimeStamp();

                    }
                        //timestamp = Math.max( , currentTime.getTime() - Constants.MAX_SYNC_TIME );

                    Logger.d(getClass(), "Got the time " + timestamp + " from " + continuousDataListener.getDataListenerName());
                    if ( dataProvider == null || dataProvider.get() == null ){
                        Logger.e(this.getClass(), "provider is null in SyncDataAsync");
                    }
                    Object data = dataProvider.get().getData(timestamp);
                    isEmpty = false;
                    Logger.d(getClass(), "Last timestamp in data provider is " + dataProvider.get().getLastTimeStampInData());
                    if ( dataProvider.get().getLastTimeStampInData() == 0 )
                        lastSyncTime = timestamp;
                    else
                        lastSyncTime = dataProvider.get().getLastTimeStampInData();
                    dataRequest.put(Constants.LAST_SYNC_TIME, lastSyncTime);
                    dataRequest.put(Constants.EXPERIMENT_ID, experimentId);
                    dataRequest.put(Constants.USER_ID, userId);
                    dataRequest.put(Constants.VERSION, ConfigVersion);
                    String currentTimeStr = df.format(currentTime);
                    dataRequest.put(Constants.SEND_TO_SERVER_TIME, currentTimeStr);
                    dataRequest.put(Constants.SENSORS_DATA, data);
                    params.put(Constants.EXPERIMENT_ID, "" + experimentId);
                    params.put(Constants.USER_ID, userId);
                    String publicAgentConfigHashStr = "" + ( publicAgentConfigHash != null ? publicAgentConfigHash : "" );
                    String publicCommConfigHashStr = "" + ( publicCommConfigHash != null ? publicCommConfigHash : "" );
                    params.put(Constants.CONF_COMM_HASH, publicCommConfigHashStr);
                    params.put(Constants.CONF_AGENT_HASH, publicAgentConfigHashStr);


                    Logger.i(ServerSyncData.class, "Sensors data request to server: " + dataRequest + ". Data timestamp: " + new Date(timestamp));
                }
                catch ( Throwable ex ){
                    onFail(new OperationCreationException(ex));
                    return null;
                }
                try {
                    if ( !isEmpty )
                        returnVal = continuousDataListener.processObject("data_" + Constants.REQ_INSERT_ZIPPED_SER_SENSORS_DATA_ENC, dataRequest, params, HashMap.class, true, false);
                }
                catch ( CongeorException ex ){
                    onFail(ex);
                    return null;
                }
                try {
                    continuousDataListener.setLastTimeStamp(lastSyncTime);
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