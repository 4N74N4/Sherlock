package com.bgu.agent;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.bgu.agent.base.IContinuousDataListener;
import com.bgu.agent.base.IDataListener;
import com.bgu.agent.base.operations.IDataOperation;
import com.bgu.agent.commons.IAppContextProvider;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.agent.data.listeners.FileListener;
import com.bgu.agent.data.listeners.ICommunicationService;
import com.bgu.agent.data.listeners.RestClientListener;
import com.bgu.agent.data.manager.DataManager;
import com.bgu.agent.data.operations.CheckForConfigurationUpdate;
import com.bgu.agent.data.operations.ServerSyncData;
import com.bgu.agent.executor.SerialExecutor;
import com.bgu.agent.funf.FunfSensorsPersistence;
import com.bgu.agent.sensors.manager.SensorManager;
import com.bgu.agent.sensors.provider.SensorsDataProvider;

import com.bgu.agentmain.R;
import com.bgu.congeor.Constants;
import com.google.gson.*;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.BasicPipeline;

import java.io.IOException;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
* Created by clint on 1/25/14.
*/
public abstract class AgentDataService extends Service implements IAppContextProvider {

    private static final String AGENT_START_SERVICE = "AGENT_START_SERVICE";
    private static final String AGENT_CONFIG_CHANGED = "AGENT_CONFIG_CHANGED";
    private static final String PUBLIC_CONFIG_CHANGED = "PUBLIC_CONFIG_CHANGED";
    private static final String PRIVATE_CONFIG_CHANGED = "PRIVATE_CONFIG_CHANGED";
    private static final String MANUAL_SEND_DATA_TO_SERVER = "MANUAL_SEND_DATA_TO_SERVER";
    private static final String PERFORM_ARCHIVE = "PERFORM_ARCHIVE";
    public static final String AGENT_DATA_SERVICE_STARTED = "com.bgu.agent.AgentDataService.started";
    SensorManager sensorManager;
    boolean sendToServerOn = false;
    String TimeToSend = "";
    boolean sendToServerActive = false;
    private long sendToServerInterval;
    boolean sendOn3G = true;
    String version =null;
    String userId = null;
    String phoneId;
    String experimentId;
    String [] sensorsToSend;
    boolean funfInitialized = false;
    protected boolean contextInitialized = false;
    FunfSensorsPersistence funfPersistence;
    SensorsDataProvider sensorsDataProvider;
    protected Configuration privateCommConfig;
    protected Configuration agentConfiguration;
    protected Configuration publicCommConfiguration;
    // Thread changes
    Executor executor = new SerialExecutor(Executors.newSingleThreadExecutor());

    IDataListener listener;
    boolean initializingContext = false;
    //Handler handler = new Handler();
    protected BroadcastReceiver receiver = null;
    //Runnable runnable = null;
    long totalTime = 0;
    long previousTime = 0;
    long currentTime = 0;
    long archiveInterval = 0;
    String versionName;
    String versionCode;
    Intent funfService;

    public abstract String getUserId();

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isFileListener (){
        return listener instanceof FileListener;
    }

    @Override
    public Context getAppContext() {
        return this.getBaseContext();
    }

    protected void keepAliveServices (){
        funfService = new Intent(getBaseContext(), FunfManager.class);
        getBaseContext().startService(funfService);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ( contextInitialized )
            keepAliveServices();
        if ( intent != null && intent.hasExtra("FLAG") && intent.getStringExtra("FLAG").equals("WAKE_UP")){

        }
        else
            if ( intent != null && intent.hasExtra("FLAG") && intent.getStringExtra("FLAG").equals("SendToServer") ){
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendToServer();
                    }
                });
            }
            else {
                if ( !contextInitialized ){
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            keepAliveServices();
                        }
                    }, 500 );
                    new Handler().postDelayed( new Runnable() {
                        @Override
                        public void run() {
                            PackageInfo pInfo = null;
                            try {
                                pInfo = getBaseContext().getPackageManager().getPackageInfo(getBaseContext().getPackageName(), 0);
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            versionName = pInfo.versionName;
                            versionCode = "" + pInfo.versionCode;
                            Logger.i(AgentDataService.class, "Service start command");
                            receiver = new MessageReceiver ();
                            IntentFilter filter = new IntentFilter();
                            filter.addAction(AGENT_START_SERVICE);
                            filter.addAction(AGENT_CONFIG_CHANGED);
                            filter.addAction(PUBLIC_CONFIG_CHANGED);
                            filter.addAction(MANUAL_SEND_DATA_TO_SERVER);
                            filter.addCategory("com.bgu.agent");
                            registerReceiver(receiver, filter);
                            init(new IRunnableWithParameters() {
                                @Override
                                public void run(Object... params) {
                                    if (((Boolean) params [ 0 ]).booleanValue())
                                        onInitComplete();
                                }
                            });
                        }
                    }, 5000);
                }
            }
        return START_STICKY;
    }

    protected void startForeground (){
        startForeground(4, getNotification());
    }

    /**
     * function for congurdataservice, alarm for update will be initiated there.
     *
     */
    protected void onInitComplete(){
        startPeriodicSendToServer();
    }

    protected void stopForeground (){
        super.stopForeground(true);
        NotificationManager mNotificationManager = (NotificationManager)
        getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(4);
    }

    protected JsonElement removeExcludedSensors ( JsonElement pipelineContents ){
        try {
            String currentModel = "CURRENT_MODEL=" + android.os.Build.MODEL;
            String currentSDK = "SDK=" + Build.VERSION.SDK;
            String currentBrand = "BRAND=" + Build.BRAND;
            String currentDevice = "CURRENT_DEVICE=" + Build.DEVICE;
            JsonObject p = pipelineContents.getAsJsonObject();
            if ( !agentConfiguration.hasKey(Constants.EXCLUDED_SENSORS)){
                return pipelineContents;
            }
            else {
                JsonObject excludedSensors = agentConfiguration.getKey(Constants.EXCLUDED_SENSORS).getAsJsonObject();
                Iterator<Map.Entry<String, JsonElement>> it = excludedSensors.entrySet().iterator();
                while ( it.hasNext()){
                    Map.Entry<String, JsonElement> entry = it.next();
                    String key = entry.getKey();
                    if ( currentDevice.equalsIgnoreCase(key) ||
                         currentSDK.equalsIgnoreCase(key) ||
                         currentBrand.equalsIgnoreCase(key) ||
                         currentModel.equalsIgnoreCase(key)) {
                        JsonArray sensors = entry.getValue().getAsJsonArray();
                        return removeFromPipeline ( p, sensors );
                    }
                }
            }
        }
        catch ( Throwable t ){

        }
        return pipelineContents;
    }
//sensors put to pipeline but not to write - removed from sensors to sample.
    protected JsonElement removeFromPipeline(JsonObject pipeline, JsonArray sensors) {
        HashSet<String> sensorsToRemove = new HashSet<String>();
        for ( int i = 0; i < sensors.size(); i++ )
            sensorsToRemove.add(sensors.get(i).getAsString());
        JsonArray data = pipeline.getAsJsonArray("data");
        JsonArray newData = new JsonArray();
        for ( int i = 0; i < data.size(); i++ ){
            JsonObject current = data.get(i).getAsJsonObject();
            String sensorType = current.getAsJsonPrimitive("@type").getAsString();
            if ( !sensorsToRemove.contains(sensorType)){
                current = removeInnerSensorDefs ( sensorType, current, sensorsToRemove );
                newData.add(current);
            }
            else {
                Logger.v(AgentDataService.class, "Removed " + sensorType + " from pipeline.");
            }
        }
        pipeline.remove("data");
        pipeline.add("data", newData);
        return pipeline;
    }

    private JsonObject removeInnerSensorDefs( String sensorType, JsonObject current, HashSet<String> sensorsToRemove) {
        JsonObject newCurrent = new JsonObject();
        Iterator<Map.Entry<String, JsonElement>> it = current.entrySet().iterator();
        while ( it.hasNext()){
            Map.Entry<String, JsonElement> currentEntry = it.next();
            String key = currentEntry.getKey();
            JsonElement value = currentEntry.getValue();
            if ( key.equals("sensors") && value.isJsonArray()){
                JsonArray sensorsArray = value.getAsJsonArray();
                JsonArray newSensorsArray = new JsonArray();
                for ( int i = 0; i < sensorsArray.size(); i++ ){
                    String sensorName = sensorsArray.get(i).getAsString();
                    if ( !sensorsToRemove.contains(sensorName))
                        newSensorsArray.add(new JsonPrimitive(sensorName));
                    else
                        Logger.v(AgentDataService.class, "Removed " + sensorName + " from " + sensorType);
                }
                newCurrent.add(key, newSensorsArray);
            }
            else
                newCurrent.add(key, value);
        }
        return newCurrent;
    }

    public abstract Notification getNotification ();

    public void updateFunfSensors(Configuration agentConfiguration) {
        JsonElement pipelineContents = agentConfiguration.getKey(Constants.CONF_SEC_FUNF_SENSORS);
        pipelineContents = removeExcludedSensors ( pipelineContents );
        BasicPipeline pipeline = ( BasicPipeline ) funfPersistence.loadSensorsFromPipeline(pipelineContents, SensorManager.getInstance());
        if ( funfPersistence.getFunfManager().getRegisteredPipeline(pipeline.getName()) == null )
            funfPersistence.loadPipeline(pipeline);
        else
            funfPersistence.reloadPipeline(pipeline);
    }

    public Integer getPublicCommConfigHash() {
        if ( publicCommConfiguration == null )
            return null;
        return publicCommConfiguration.getConfigurationHash();
    }

    public Integer getAgentConfigHash() {
        if ( agentConfiguration == null )
            return null;
        return agentConfiguration.getConfigurationHash();
    }
    public Integer getPrivateCommConfigHash() {
        if ( privateCommConfig == null )
            return null;
        return privateCommConfig.getConfigurationHash();
    }
    public class LocalBinder extends Binder {
        public AgentDataService getManager() {
            return AgentDataService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    void init (final IRunnableWithParameters onComplete){
        try {
            initializingContext = true;
            if ( userId == null )
                userId = getUserId();
            privateCommConfig = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_PRIVATE_COMM_FILENAME, true);
            setPrivateCommConfigParams();
            agentConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_AGENT_FILENAME, false);
            publicCommConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_PUBLIC_COMM_FILENAME, false);
            if ( userId == null || privateCommConfig == null || agentConfiguration == null || publicCommConfiguration == null){
                initializingContext = false;
                onComplete.run(false);
                return;
            }
            if ( !contextInitialized ){
                if ( phoneId == null )
                    phoneId = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                if ( sensorManager == null ){
                    sensorManager = new SensorManager();
                    SensorManager.setInstance(sensorManager);
                    SensorManager.getInstance().init(getBaseContext());
                    sensorsDataProvider = new SensorsDataProvider(SensorManager.getInstance());
                }
                if ( funfPersistence == null ){
                    funfPersistence = new FunfSensorsPersistence();
                    SensorManager.getInstance().setSensorPersistence(funfPersistence);
                    funfPersistence.connect( new Runnable() {
                        @Override
                        public void run() {
                            funfInitialized = true;
                            Logger.i(AgentDataService.class, "Funf persistence connected.");
                            contextInitialized = true;
                            initializingContext = false;
                            if ( onComplete != null )
                                onComplete.run(true);
                        }
                    });
                    funfPersistence.bindService(getBaseContext());
                }
            }
        }
        catch ( Throwable e ){
            Logger.e(AgentDataService.class, e.getLocalizedMessage(), e);
            initializingContext = false;
            onComplete.run(false);
        }
    }

    public boolean checkUserMailExist() throws IOException {
        boolean result;
        Configuration userData  = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_USER_DATA_HASHED, true);
        String hashedMail = userData.getKeyAsString(Constants.HASHED_MAIL);
        if(hashedMail.equals("")) return false;
        else return true;
    }
    boolean applyConfig (){
        userId = getUserId();
        boolean success = setPrivateCommConfigParams();
        success = success && setPublicConfigParams();
        success = success && setAgentConfigParams();
        return success;
    }

    boolean setPublicConfigParams (){
        try {
            Logger.d(AgentDataService.class, "Configuration Event - Updating public configuration to " + publicCommConfiguration.toString());
            sendToServerInterval = publicCommConfiguration.getKeyAsInt(Constants.COMM_SEND_TO_SERVER_INTERVAL);

            archiveInterval = publicCommConfiguration.hasKey(Constants.COMM_ARCHIVE_INTERVAL) ? publicCommConfiguration.getKeyAsInt(Constants.COMM_ARCHIVE_INTERVAL) * 1000 : 259200;
        }
        catch ( Throwable t ){
            return false;
        }
        return true;
    }

    boolean setAgentConfigParams (){
        try {
            Logger.d(AgentDataService.class, "Configuration Event - Updating FUNF with the configuration: " + agentConfiguration.toString());
            sensorsToSend = getSensorsToSend(agentConfiguration);
            sensorsDataProvider.setProvidedSensors(sensorsToSend);
            updateFunfSensors(agentConfiguration);
        }
        catch ( Throwable t ){
            Logger.e(AgentDataService.class, t.getLocalizedMessage(), t);
            return false;
        }
        return true;
    }

    boolean setPrivateCommConfigParams (){
        try {
            Logger.d(AgentDataService.class, "Configuration Event - Updating private configuration to " + privateCommConfig.toString());
            sendOn3G = privateCommConfig.getKeyAsString(Constants.SEND_ON_3G).equalsIgnoreCase("true");
            experimentId = privateCommConfig.getKeyAsString(Constants.EXPERIMENT_ID);
            version = privateCommConfig.getKeyAsString(Constants.VERSION);
            listener = getDefaultListener(this, privateCommConfig);
            DataManager.getInstance(getBaseContext()).unRegisterAllListeners();
            DataManager.getInstance(getBaseContext()).registerListener(listener);
        }
        catch ( Throwable t ){
            return false;
        }
        return true;
    }

    public void agentConfigurationChanged (){
        if ( !contextInitialized )
            return;
        Logger.i(AgentDataService.class, "Configuration Event - Service received agent configuration changed.");
        try {
            Configuration previousConfiguration = agentConfiguration;
            agentConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_AGENT_FILENAME, false);
            Logger.d(AgentDataService.class, "Configuration Event - Agent configuration was read from file.");
            boolean success = setAgentConfigParams();
            if ( !success ){
                Logger.e(AgentDataService.class, "Configuration Event - Error updating agent configuration.");
                agentConfiguration = previousConfiguration;
                setAgentConfigParams();
            }
            else {
                Logger.d(AgentDataService.class, "Configuration Event - The agent configuration was updated successfully.");
            }
        } catch (IOException e) {
            Logger.e(AgentDataService.class, "Configuration Event - Error updating agent configuration.");
        }
    }

    public void publicConfigurationChanged (){
        if ( !contextInitialized )
            return;
        Logger.i(AgentDataService.class, "Configuration Event - Service received public configuration changed.");
        try {
            Configuration previousConfiguration = publicCommConfiguration;
            publicCommConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_PUBLIC_COMM_FILENAME, false);
            Logger.d(AgentDataService.class, "Configuration Event - Public configuration was read from file.");
            boolean success = setPublicConfigParams();
            if ( !success ){
                Logger.e(AgentDataService.class, "Configuration Event - Error updating public configuration.");
                publicCommConfiguration = previousConfiguration;
                setPublicConfigParams();
            }
            else
                Logger.d(AgentDataService.class, "Configuration Event - The public configuration was updated successfully.");
        } catch (IOException e) {
            Logger.e(AgentDataService.class, "Configuration Event - Error updating public configuration.");
        }
    }

    public void startPeriodicSendToServer (){
        if ( sendToServerOn )
            return;
        if ( sendToServerActive ){
            new Thread (new Runnable() {
                @Override
                public void run() {
                    initStartPeriodicSendToServer();
                }
            });
        }
        else
            initStartPeriodicSendToServer ();
    }


    void initStartPeriodicSendToServer (){
        if ( initializingContext )
            return;
        if ( contextInitialized ){
            sendToServerOn = true;
            if (!applyConfig()){
                sendToServerActive = false;
                sendToServerOn = false;
                return;
            }
            sendToServer();
        }
        else {
            init(new IRunnableWithParameters() {
                @Override
                public void run(Object... params) {
                    if (((Boolean) params[0]).booleanValue())
                        onInitComplete();
                }
            });
        }
    }

   // protected abstract Class getBroadcastReceiverClass ();

    void scheduleNextAlarm (){
        Class thisClass = ((Object) this).getClass();
        Intent myIntent = new Intent(this, thisClass);
        myIntent.putExtra("FLAG", "SendToServer");
        PendingIntent pendingIntent = PendingIntent.getService(this, 14252, myIntent, 0);
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        long nextTime = System.currentTimeMillis() + sendToServerInterval * 1000;
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent);
    }

    protected void onAfterSend (){

    }

    void sendToServer (){
        Logger.i(AgentDataService.class, "Send to server started");
        if ( sendToServerActive ){
            sendToServerActive = true;
            onServerCommunication();
            ServerSyncData syncData = new ServerSyncData(sensorsDataProvider, AgentDataService.this, getAgentConfigHash(),
                    getPublicCommConfigHash(), sendOn3G, userId, phoneId, experimentId, versionCode, versionName,version, SensorManager.getSensorsModuleVersion(), publicCommConfiguration.getKeyAsInt(Constants.COMM_SEND_TO_SERVER_INTERVAL));
            performOperation(syncData, new CheckForConfigurationUpdate(AgentDataService.this, new IRunnableWithParameters() {
                @Override
                public void run(Object... params) {
                    onAfterSend();
                    boolean stopService = false;
                    currentTime = System.currentTimeMillis();
                    totalTime += currentTime - previousTime;
                    if ( totalTime >= archiveInterval ){
                        performArchive ();
                        totalTime = 0;
                    }
                    previousTime = currentTime;
                    if (((Boolean) params[1]).booleanValue()) {
                        agentConfigurationChanged();
                    }
                    if (((Boolean) params[2]).booleanValue()) {
                        publicConfigurationChanged();
                    }
                    if (Constants.ENUM_SEND_DATA_STATUS.VERSION_ERROR.equals(params[3])) {
                        sendToServerOn = false;
                        updateRequired ();
                        stopService = true;
                    }
                    if ( sendToServerOn )
                        scheduleNextAlarm();
                    else {
                        sendToServerActive = false;
                        stopForeground();
                    }
                    if ( stopService ){
                        stopService(funfService);
                        stopKeepAliveServices();
                        stopSelf();
                    }
                }

            }), executor);
        }
        else {
            if ( sendToServerOn ){
                previousTime = System.currentTimeMillis();
                startForeground();
                scheduleNextAlarm();
                sendToServerActive = true;
            }
            else
                sendToServerActive = false;
        }
    }

    protected void stopKeepAliveServices() {

    }

    protected int getAppIcon (){
        return R.drawable.ic_launcher;
    }

    abstract protected Uri getAppURL();

    protected void updateRequired() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getBaseContext())
                        .setSmallIcon(getAppIcon())
                        .setContentTitle("ConGeoR upgrade available")
                        .setContentText("Please upgrade the application")
                        .setLights(Color.GREEN, 300, 1000)
                        .setAutoCancel(true);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(getAppURL());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(5, mBuilder.build());
    }

    private void performArchive() {
        Intent intent = new Intent();
        intent.setAction(PERFORM_ARCHIVE);
        intent.addCategory("com.bgu.agent");
        getAppContext().sendBroadcast(intent);
    }

    private void sendDataToServer (){
        if ( contextInitialized ){
            onServerCommunication();
            ServerSyncData syncData = new ServerSyncData(sensorsDataProvider, AgentDataService.this, getAgentConfigHash(),
                    getPublicCommConfigHash(), sendOn3G, userId, phoneId, experimentId, versionCode, versionName, version ,SensorManager.getSensorsModuleVersion(), publicCommConfiguration.getKeyAsInt(Constants.COMM_SEND_TO_SERVER_INTERVAL));
            performOperation(syncData, new CheckForConfigurationUpdate(AgentDataService.this, new IRunnableWithParameters() {
                @Override
                public void run(Object... params) {
                    if (((Boolean) params[0]).booleanValue()) {
                        agentConfigurationChanged();
                    }
                    if (((Boolean) params[1]).booleanValue()) {
                        publicConfigurationChanged();
                    }
                }

            }));
        }
    }

    @Override
    public void onDestroy (){
        Logger.d(AgentDataService.class, "Service destroyed");
        try {
            if ( receiver != null )
                unregisterReceiver(receiver);
        }
        catch ( Throwable t ){}
        if ( funfPersistence != null ){
            funfPersistence.destroy(getBaseContext());
            funfPersistence = null;
            SensorManager.getInstance().destroy();
        }
        DataManager.getInstance(getBaseContext()).Dispose(getBaseContext());
        super.onDestroy();
    }

    public boolean getSendToServer (){
        return this.sendToServerOn;
    }

    private static class AgentGeneralConfiguration {
        String [] sendToServer;
    }

    private String [] getSensorsToSend (Configuration agentConfiguration){
        JsonElement element =  agentConfiguration.getKey("agentGeneralConfig");
        Gson gson = new Gson();
        AgentGeneralConfiguration config = gson.fromJson(element, AgentGeneralConfiguration.class);
        return config.sendToServer;
    }

    public static IContinuousDataListener getDefaultListener ( Context context, final Configuration privateCommConfig){
        String listenerType = privateCommConfig.getKeyAsString(Constants.COMM_OUTPUT);
        if ( listenerType.equalsIgnoreCase(FileListener.class.getCanonicalName())){
            return new FileListener(context, "timestamp.csv", "data.csv");
        }
        if ( listenerType.equalsIgnoreCase(RestClientListener.class.getCanonicalName())){
            ICommunicationService communicationService = new ICommunicationService() {
                @Override
                public String getServerIP(String service) {
                    return privateCommConfig.getKeyAsString(service + "_" + Constants.COMM_SERVER_IP);
                }

                @Override
                public int getServerPort(String service) {
                    return privateCommConfig.getKeyAsInt(service + "_" + Constants.COMM_SERVER_PORT);
                }

                @Override
                public String getBaseURL(String service) {
                    return privateCommConfig.getKeyAsString(service + "_" + Constants.COMM_BASE_URL);
                }
            };
            PublicKey publicKey = null;
            try {
                publicKey = Utils.readPublicKeyFromFile(context, "serverPublicKey.key");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new RestClientListener ( context, communicationService, publicKey);
        }
        return null;
    }

    public void performOperation ( IDataOperation operation, IRunnableWithParameters callback, Executor...executors ){
        DataManager.getInstance(getBaseContext()).performDataOperation ( operation, callback,executors );
    }

    public static Intent generateStartServiceIntent (){
        Intent intent = new Intent();
        intent.setAction(AGENT_START_SERVICE);
        intent.addCategory("com.bgu.agent");
        return intent;
    }

    public static Intent generateAgentConfigChanged (){
        Intent intent = new Intent();
        intent.setAction(AGENT_CONFIG_CHANGED);
        intent.addCategory("com.bgu.agent");
        return intent;
    }

    public static Intent generatePublicConfigChanged (){
        Intent intent = new Intent();
        intent.setAction(PUBLIC_CONFIG_CHANGED);
        intent.addCategory("com.bgu.agent");
        return intent;
    }


    public static Intent manualSendDataToServer (){
        Intent intent = new Intent();
        intent.setAction(MANUAL_SEND_DATA_TO_SERVER);
        intent.addCategory("com.bgu.agent");
        return intent;
    }

    protected void receivedBroadcast (Context context, Intent intent){

    }

    class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ( intent.getAction().equals(AgentDataService.AGENT_START_SERVICE)){
                startPeriodicSendToServer();
            }
            if ( intent.getAction().equals(AgentDataService.AGENT_CONFIG_CHANGED)){
                Log.e(AgentDataService.class.toString(), "Intent regarding conf changed has been captured");
                agentConfigurationChanged();
            }
            if ( intent.getAction().equals(AgentDataService.PUBLIC_CONFIG_CHANGED)){
                publicConfigurationChanged();
            }

            if ( intent.getAction().equals(AgentDataService.MANUAL_SEND_DATA_TO_SERVER)){
                sendDataToServer();
            }
            receivedBroadcast(context, intent);
        }
    }

    protected void onServerCommunication (){

    }
}
