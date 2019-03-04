package com.bgu.agent.sensors;

import FunfFix.NameValueDatabaseHelper;
import android.content.*;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;
import android.util.TimeUtils;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.FileUtil;
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.funf.util.StringUtil;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by clint on 2/9/14.
 */
public class HierarchicalPipeline extends BasicPipeline {

    protected Handler handler;
    Queue<JsonObject> messages;
    MessageReceiver receiver;
    protected static final int SEND_ALL_MESSAGES = 4;

    private Handler.Callback callback = new Handler.Callback() {

        @Override
        public boolean handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case ARCHIVE: {
                    Logger.d(HierarchicalPipeline.class, "HierarchicalPipeline - Archiving database after sending data.");
                    HierarchicalPipeline.super.runArchive();
                    break;
                }
                case SEND_ALL_MESSAGES: {
                    while (!messages.isEmpty()) {
                        if (messages.size() > 0) {
                            JsonObject message = messages.remove();
                            Logger.d(HierarchicalPipeline.class, "HierarchicalPipeline - Sending data to FUNF\n" + message);
                            getHandler().obtainMessage(DATA, message).sendToTarget();
                        }
                    }
                    break;
                }
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    protected void runArchive() {
        Logger.d(HierarchicalPipeline.class, "HierarchicalPipeline - Running archive on database - This deletes all the contents of the database - Not done FUNF ran this command.");
        // super.runArchive();
    }

    @Override
    public void onCreate(FunfManager manager) {
        super.onCreate(manager);
        archive = new FileArchive() {
            @Override
            public boolean add(File file) {
                return true;
            }

            @Override
            public boolean remove(File file) {
                return true;
            }

            @Override
            public boolean contains(File file) {
                return true;
            }

            @Override
            public File[] getAll() {
                return new File[0];
            }
        };
        handler = new Handler(getHandler().getLooper(), callback);
        messages = new ConcurrentLinkedQueue<JsonObject>();
        receiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("PERFORM_ARCHIVE");
        filter.addCategory("com.bgu.agent");
        getFunfManager().getBaseContext().registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (receiver != null) {
                getFunfManager().getBaseContext().unregisterReceiver(receiver);
            }
        } catch (Throwable t) {

        }
    }

    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        try {
            Logger.d(HierarchicalPipeline.class, "HierarchicalPipeline - Processing data\n" + data);
            //String uuid = data.has(Constants.UUID) ? data.get(Constants.UUID).getAsString() : null;
            /**************************************************************************************************************/
            String uuid = String.valueOf(new Date().getTime());

            Iterator<Map.Entry<String, JsonElement>> iterator = data.entrySet().iterator();
            JsonObject newData = new JsonObject();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonElement> current = iterator.next();
                boolean handled = false;
                if (current.getValue().isJsonObject()) {
                    JsonObject object = current.getValue().getAsJsonObject();
                    if (object.has("subProbeName")) {
                        if (uuid == null) {
                            uuid = UUID.randomUUID().toString();
                        }
                        sendSubProbe(current.getValue().getAsJsonObject(), uuid);
                        handled = true;
                    }
                } else {

                    if (current.getValue().isJsonArray()) {
                        JsonArray array = current.getValue().getAsJsonArray();
                        if (array.size() > 0 && array.get(0).isJsonObject() && array.get(0).getAsJsonObject().has("subProbeName")) {
                            if (uuid == null) {
                                uuid = UUID.randomUUID().toString();
                            }
                            for (int i = 0; i < array.size(); i++) {
                                sendSubProbe(array.get(i).getAsJsonObject(), uuid);
                            }
                            handled = true;
                        }
                    }
                }
                if (!handled) {
                    newData.add(current.getKey(), current.getValue());
                }
            }
            if (uuid != null) {
                newData.addProperty(Constants.UUID, uuid);
            }
            putData(probeConfig.get(RuntimeTypeAdapterFactory.TYPE), new IJsonObject(newData));
            handler.sendEmptyMessage(SEND_ALL_MESSAGES);
        } catch (Throwable t) {
            Logger.e(getClass(), t.getMessage());
        }

    }

    protected void sendSubProbe(JsonObject subObject, String uuid) {
        Logger.d(HierarchicalPipeline.class, "Subitem");
        JsonObject value = subObject.get("value").getAsJsonObject();
        value.addProperty(Constants.UUID, uuid);
        subObject.add("value", value);
        String sensorName = subObject.get("subProbeName").getAsString();
        putData(new JsonPrimitive(sensorName), new IJsonObject(subObject.get("value").getAsJsonObject()));
    }

    protected void putData(JsonElement name, IJsonObject data) {
        JsonObject record = new JsonObject();
        record.add("name", name);
        record.add("value", data);
        messages.add(record);
    }

    public HierarchicalPipeline() {
        super();
        version = 8;
    }

    class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase("PERFORM_ARCHIVE")) {
                handler.obtainMessage(ARCHIVE).sendToTarget();
            }
        }
    }

    @Override
    protected void reloadDbHelper(Context ctx) {
        setDatabaseHelper(new NameValueDatabaseHelper(ctx, StringUtil.simpleFilesafe(name), version));
        Logger.i(HierarchicalPipeline.class, "reloadDbHelper has been called");
    }

    @Override
    protected void writeData(String name, IJsonObject data) {

            SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
            final double timestamp = data.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsDouble();
            final String value = data.toString();
            if (timestamp == 0L || name == null || value == null) {
                Log.e(LogUtil.TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + name + " - " + value);
                throw new SQLException("Not all required fields specified.");
            }
            Logger.i(HierarchicalPipeline.class , data.toString());
            ContentValues cv = new ContentValues();
            cv.put(NameValueDatabaseHelper.COLUMN_NAME, name);
            cv.put(NameValueDatabaseHelper.COLUMN_VALUE, value);
            cv.put(NameValueDatabaseHelper.COLUMN_TIMESTAMP, timestamp);
            long millis = System.currentTimeMillis();
            cv.put(NameValueDatabaseHelper.DB_INSERT_TIME, millis);
            db.insertOrThrow(NameValueDatabaseHelper.DATA_TABLE.name, "", cv);



    }
}