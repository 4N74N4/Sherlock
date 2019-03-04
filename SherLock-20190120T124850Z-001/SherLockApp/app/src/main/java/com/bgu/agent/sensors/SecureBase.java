package com.bgu.agent.sensors;

import android.content.Intent;
import com.bgu.agent.commons.logging.Logger;
import com.google.gson.JsonObject;
import edu.mit.media.funf.probe.Probe;

/**
 * Created by clint on 2/3/14.
 */
public abstract class SecureBase extends Probe.Base
{


    @Override
    protected void onEnable()
    {
        try
        {
            Logger.v(this.getClass(), "Enabling Sensor");
            super.onEnable();
            secureOnEnable();
        }
        catch (Throwable t)
        {
            sendErrorLog(t);
        }
    }

    @Override
    protected void onStart()
    {
        try
        {
            Logger.v(this.getClass(), "Starting Sensor");
            super.onStart();
            secureOnStart();
        }
        catch (Throwable t)
        {
            sendErrorLog(t);
        }
    }

    @Override
    protected void onStop()
    {
        try
        {
            Logger.v(this.getClass(), "Stopping Sensor");
            super.onStop();
            secureOnStop();
        }
        catch (Throwable t)
        {
            sendErrorLog(t);
        }

    }

    @Override
    protected void onDisable()
    {
        try
        {
            Logger.v(this.getClass(), "Disable Sensor");
            super.onDisable();
            secureOnDisable();
        }
        catch (Throwable t)
        {
            sendErrorLog(t);
        }
    }

    @Override
    protected void sendData(final JsonObject data)
    {
        try
        {
           super.sendData(data);

        }
        catch (Throwable t)
        {
            sendErrorLog(t);
        }
    }

    protected void secureOnEnable()
    {

    }

    protected void secureOnStart() {

    }

    protected void secureOnStop()
    {

    }

    protected void secureOnDisable()
    {

    }


    public void sendErrorLog(Throwable t)
    {
        Logger.e(this.getClass(), t.getLocalizedMessage(), t);
        Intent intent = ErrorSensor.generateErrorIntent(this.getClass(), t);
        getContext().sendBroadcast(intent);
    }


}


//        List<String> keys = new Vector<String>();
//        Iterator<Map.Entry<String, JsonElement>> it = data.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<String, JsonElement> k = it.next();
//            JsonElement value = k.getValue();
//            if (!value.isJsonNull() && !value.isJsonArray()) {
//                String stringValue = null;
//                try {
//                    stringValue = value.getAsString();
//                } catch (Throwable t) {
//
//                }
//                if (stringValue == null) {
//                    try {
//                        stringValue = value.toString();
//                    } catch (Throwable t) {
//
//                    }
//                }
//                if (stringValue != null && stringValue.equalsIgnoreCase("NAN")) {
//                    Logger.e(SecureBase.this.getClass(), "Value NAN received in probe on field " + k.getKey());
//                    keys.add(k.getKey());
//                }
//            }
//        }
