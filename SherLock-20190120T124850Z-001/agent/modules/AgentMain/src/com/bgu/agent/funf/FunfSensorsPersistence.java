/**
 * Created by IntelliJ IDEA.
 * User: Aviram Dayan
 * Date: 24/06/13
 * Time: 13:41
 *
 *<a href=mailto:avdayan@cs.bgu.ac.il>avdayan@cs.bgu.ac.il</a>
 */
package com.bgu.agent.funf;

import FunfFix.NameValueDatabaseHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import com.bgu.agent.SensorData;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.datalayer.ISensorPersistence;
import com.bgu.agent.sensors.manager.SensorManager;
import com.bgu.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.SensorProbe;

import java.util.*;

public class FunfSensorsPersistence implements ISensorPersistence {
private static final String GET_FUNF_DATA_SQL_TIMESTAMP = "SELECT * FROM " + NameValueDatabaseHelper.DATA_TABLE.name + " WHERE " + NameValueDatabaseHelper.DB_INSERT_TIME + " > ? AND name = ?";

    private static FunfSensorsPersistence instance;
    private ServiceConnection FUNF_MANAGER_CONN;
    private FunfManager funfManager;
    private static final String pipelineName = "funfSensors";
    private HashMap<String, HashSet<Integer>> requiredSensorTypesMap;
    public static final int FUNF_SERVICE_CONNECTED = 901;

    public FunfSensorsPersistence(){
        requiredSensorTypesMap = new HashMap<String, HashSet<Integer>>();
    }

    public void connect (final Runnable r){
        FUNF_MANAGER_CONN = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                funfManager = ((FunfManager.LocalBinder) service).getManager();
                if ( r != null )
                    r.run();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                funfManager = null;
            }
        };
    }

    @Override
    public ArrayList<SensorData> getData(String nameOfSensor, long timestamp) {
        BasicPipeline pipeline = (BasicPipeline) getPipeline(pipelineName);
        SQLiteDatabase db = pipeline.getDb();
        Cursor cursor;
        long currentTime = System.currentTimeMillis();
        String[] args = new String[]{ String.valueOf(timestamp), nameOfSensor };
        cursor = db.rawQuery(GET_FUNF_DATA_SQL_TIMESTAMP, args);
        ArrayList<SensorData> sensorsData = new ArrayList<SensorData>();
        long ts;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(1);
            Double dts = cursor.getDouble(2);
            String value = cursor.getString(3);
            long dbInsertTime = (long) cursor.getDouble(4);
            ts = ( long )( dts.doubleValue() * 1000 );
            sensorsData.add(new SensorData(name, ts, dbInsertTime, value));
            cursor.moveToNext();
        }

        return sensorsData;
    }

    @Override
    public Object getSensorConfiguration(String nameOfSensor) {
        return funfManager;
    }

    @Override
    public Object getManager() {
        return funfManager;
    }

    @Override
    public void disableSensorProbing() {
        funfManager.disablePipeline(pipelineName);
    }

    @Override
    public void enableSensorProbing() {
        funfManager.enablePipeline(pipelineName);
    }

    public ServiceConnection getFunfManagerConn (){
        return FUNF_MANAGER_CONN;
    }

    public FunfManager getFunfManager (){
        return funfManager;
    }

    public Pipeline getPipeline ( String pipelineName ){
        return funfManager.getRegisteredPipeline(pipelineName);
    }

    public JsonArray getSensorsInPipeline ( String pipelineName ){
        return funfManager.getPipelineConfig(pipelineName).get("data").getAsJsonArray();
    }

    /*public <T> T getRegisteredProbe(String pipelineName, Class<T> probeClass)
    {
        Gson gson = funfManager.getGson();
        if ( funfManager.getRegisteredPipeline(pipelineName) != null ){
            BasicPipeline a;
            a.
            JsonArray dataInPipeline = getSensorsInPipeline(pipelineName);
            for ( int i = 0; i < dataInPipeline.size(); i++ ){
                JsonObject obj = ( JsonObject ) dataInPipeline.get(i);
                String type = obj.get("@type").getAsString();
                if ( type.contains(probeClass.getCanonicalName())){
                    Logger.i(getClass(), "Loading configuration of probe class " + probeClass.getCanonicalName() + " from pipeline.");
                    return gson.fromJson(obj, probeClass);
                }
            }
        }
        Logger.w(getClass(), "The probe class " + probeClass.getCanonicalName() + " was not configured in pipeline. Loading default.");
        return gson.fromJson(new JsonObject(), probeClass);
    } */

    public <T> T getProbe(String pipelineName, Class<T> probeClass)
    {
        Gson gson = funfManager.getGson();
        if ( funfManager.getRegisteredPipeline(pipelineName) != null ){
            JsonArray dataInPipeline = getSensorsInPipeline(pipelineName);
            for ( int i = 0; i < dataInPipeline.size(); i++ ){
                JsonObject obj = ( JsonObject ) dataInPipeline.get(i);
                String type = obj.get("@type").getAsString();
                if ( type.contains(probeClass.getCanonicalName())){
                    Logger.i(getClass(), "Loading configuration of probe class " + probeClass.getCanonicalName() + " from pipeline.");
                    return gson.fromJson(obj, probeClass);
                }
            }
        }
        Logger.w(getClass(), "The probe class " + probeClass.getCanonicalName() + " was not configured in pipeline. Loading default.");
        return gson.fromJson(new JsonObject(), probeClass);
    }

    public Pipeline loadSensorsFromPipeline ( JsonElement pipelineContents, SensorManager manager ){
        Gson gson = funfManager.getGson();
        BasicPipeline p = gson.fromJson(pipelineContents, BasicPipeline.class);
        List<JsonElement> sensorsToSample = p.getDataRequests();
        List<JsonElement> elements = new ArrayList<JsonElement>();
        Map<String, Probe.Base> mappedSensors = new HashMap<String, Probe.Base>();
        for ( JsonElement i:sensorsToSample ){
            String fullSensorName = i.getAsJsonObject().get("@type").getAsString();
            if ( Utils.classExists(fullSensorName)){
                HashSet<Integer> requiredSensorTypes = getRequiredSensorTypes(mappedSensors, fullSensorName);
                if ( manager.areSensorsAvailable (requiredSensorTypes)){
                    elements.add(i);
                }
            }
            else {
                Logger.e(FunfSensorsPersistence.class, "The sensor of class " + fullSensorName + " does not exist and therefore will not be sampled.");
            }
            p.setDataRequests(elements);
        }
        /*funfManager.registerPipeline(p.getName(), p);
        funfManager.unregisterPipeline(p.getName());
        p.getDb().close();
        new File ( p.getDb().getPath()).delete(); */
        return p;
    }

    public void loadPipeline ( BasicPipeline p ){
        Gson gson = funfManager.getGson();
        funfManager.registerPipeline(p.getName(), p);
        funfManager.save(this.pipelineName, gson.toJsonTree(p).getAsJsonObject());
    }

    public void reloadPipeline ( BasicPipeline p ){
        Gson gson = funfManager.getGson();
        funfManager.saveAndReload(this.pipelineName, gson.toJsonTree(p).getAsJsonObject());
    }

    public void requiredSensorTypes ( Map<String, Probe.Base> mappedSensors, String fullSensorName, HashSet<Integer> requiredTypes ){
        if ( !mappedSensors.containsKey(fullSensorName))
            try {
                mappedSensors.put(fullSensorName, (Probe.Base) getProbe(this.pipelineName, Class.forName(fullSensorName)));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        Probe.Base sensor = mappedSensors.get(fullSensorName);
        if ( sensor instanceof SensorProbe ){
            int type = (( SensorProbe ) sensor ).getSensorType();
            requiredTypes.add(type);
        }
        try {
            Probe.RequiredProbes requiredProbes = Class.forName(fullSensorName).getAnnotation(Probe.RequiredProbes.class);
            if ( requiredProbes != null ){
                Class [] probesRequired = requiredProbes.value();
                for ( Class i:probesRequired ){
                    requiredSensorTypes(mappedSensors, i.getCanonicalName(), requiredTypes);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public HashSet<Integer> getRequiredSensorTypes ( Map<String, Probe.Base> mappedSensors, String fullSensorName ){
        HashSet<Integer> requiredTypes = requiredSensorTypesMap.get(fullSensorName);
        if ( requiredTypes == null ){
            requiredTypes = new HashSet<Integer>();
            requiredSensorTypesMap.put(fullSensorName, requiredTypes);
            requiredSensorTypes(mappedSensors, fullSensorName, requiredTypes);
        }
        return requiredTypes;
    }

    public void bindService (Context context){
        context.bindService(new Intent(context, FunfManager.class), FUNF_MANAGER_CONN, Context.BIND_AUTO_CREATE);
    }

    public void unBindService (Context context){
        context.unbindService(FUNF_MANAGER_CONN);
    }

    public void destroy (Context context){
        BasicPipeline pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(pipelineName);
        funfManager.unregisterPipeline(pipelineName);
        if ( pipeline != null )
            pipeline.getDb().close();
        unBindService(context);
    }

}