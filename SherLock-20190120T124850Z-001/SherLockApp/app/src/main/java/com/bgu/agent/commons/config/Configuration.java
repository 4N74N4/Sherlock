package com.bgu.agent.commons.config;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/2/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class Configuration {
    JsonObject configuration;
    String configurationType;
    String configStr;
    int configurationHash;
    long lastModified;
    boolean isDefaultConfig = false;

    protected Configuration (){
    }

    public static Configuration loadLocalConfiguration ( Context appContext, String configurationType ) throws IOException {
        return loadLocalConfiguration( appContext, configurationType, false );
    }

    public static Configuration loadLocalConfiguration ( Context appContext, String configurationType, boolean useDefault ) throws IOException {
        try {
            InputStream in;
            boolean isDefault = false;
            long lastModified = 0;
            if ( useDefault ){
                in = getDefaultConfigurationFileName(appContext, configurationType);
                isDefault = true;
            }
            else {
                File configuration = getConfigurationLocation(appContext, configurationType);
                if ( configuration.exists()){
                    in = getInnerConfigurationFileName(appContext, configurationType);
                    lastModified = configuration.lastModified();
                }
                else
                    return null;
            }
            String config = com.bgu.utils.Utils.readStreamAsString(in);
            Configuration configObj = loadConfiguration(isDefault, configurationType, config);
            configObj.lastModified = lastModified;
            in.close();
            return configObj;
            }
        catch ( Throwable e ){

        }
        return null;
    }

    public void updateConfiguration ( Context appContext ) throws IOException {
        //OutputStream out = appContext.openFileOutput(configurationType, Context.MODE_PRIVATE);
        //out.write(configStr.getBytes());
        //out.close();
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/" + Utils.getConfigurationFolder(appContext));
        dir.mkdirs();
        File file = new File(dir, configurationType);
        OutputStream out = new FileOutputStream(file);
        out.write(configStr.getBytes());
        out.close();
        Log.e(Configuration.class.toString(),"conf has been updated");
    }

    public static InputStream getDefaultConfigurationFileName ( Context appContext, String configurationType ) throws IOException {
        return getConfigurationFromAsset( appContext, "default" + configurationType);
    }

    public static InputStream getConfigurationFromAsset ( Context appContext, String fileName ) throws IOException {
        return appContext.getAssets().open(fileName);
    }

    public static File getConfigurationLocation ( Context appContext, String fileName ){
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/" + Utils.getConfigurationFolder(appContext));
        File file = new File(dir, fileName);
        return file;
        // return appContext.getFileStreamPath(fileName);
    }


    public static InputStream getInnerConfigurationFileName ( Context appContext, String fileName ) throws IOException {
        //return appContext.openFileInput(fileName);
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/" + Utils.getConfigurationFolder(appContext));
        File file = new File(dir, fileName);
        return new FileInputStream( file );
    }

    public static Configuration loadConfiguration ( boolean isDefault, String configurationType, String config ) throws IOException {
        Configuration configObj = new Configuration();
        Gson gson = new Gson();
        configObj.configuration = gson.fromJson(config, JsonElement.class).getAsJsonObject();
        configObj.configurationHash = config.hashCode();
        configObj.configurationType = configurationType;
        configObj.isDefaultConfig = isDefault;
        configObj.configStr = config;
        return configObj;
    }

    public boolean isDefault() {
        return this.isDefaultConfig;
    }

    public JsonElement getKey(String key){
        JsonElement returnVal = null;
        if ( !configuration.has(key))
            Logger.w(getClass(), "Key " + key + " does not exist in configuration");
        else
            returnVal = configuration.get(key);
        return returnVal;
    }

    public boolean hasKey (String key){
        return configuration.has(key);
    }

    public String getKeyAsString(String key) {
        String returnVal = null;
        if ( !configuration.has(key))
            Logger.w(getClass(), "Key " + key + " does not exist in configuration");
        else {
            Object o = configuration.get(key);
            System.out.println ( o );
            returnVal = configuration.get(key).getAsString();
        }
        return returnVal;
    }

    public int getKeyAsInt ( String key ) {
        return Integer.parseInt(getKeyAsString(key));
    }

    public Double getKeyAsDouble ( String key ){
        return Double.parseDouble(getKeyAsString(key));
    }

    public boolean getKeyAsBoolean(String key) {
        return getKeyAsString(key).equalsIgnoreCase("TRUE");
    }

    public int getConfigurationHash (){
        return configurationHash;
    }

    @Override
    public String toString (){
        StringBuffer str = new StringBuffer();
        str.append ( "configurationType=" );
        str.append( configurationType );
        str.append ( "\n" );
        str.append ( "configurationHash=" );
        str.append ( configurationHash );
        str.append ( "\n" );
        str.append ( "isDefault=");
        str.append ( isDefaultConfig );
        str.append ( "\n" );
        str.append ( configStr );
        return str.toString();
    }
    public String getConfContent() {return configStr;}
}