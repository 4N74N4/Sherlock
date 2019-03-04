package com.bgu.agent.manager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import com.bgu.agent.AgentDataService;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.data.listeners.FileListener;
import com.bgu.congeor.Constants;

import java.io.File;
import java.io.IOException;

/**
 * Created by clint on 2/4/14.
 */
public class AgentManager {
    public static void startSampling(Context context) {
        Intent intent = AgentDataService.generateStartServiceIntent();
        context.sendBroadcast(intent);
    }

    public static void agentConfigurationChanged (Context context){
        Intent intent = AgentDataService.generateAgentConfigChanged();
        context.sendBroadcast(intent);
        Log.e(AgentManager.class.toString(),"Intent regarding cong change has been fired");
    }

    public static void publicCommConfigurationChanged (Context context){
        Intent intent = AgentDataService.generatePublicConfigChanged();
        context.sendBroadcast(intent);
    }

    public static Configuration getPrivateConfiguration (Context context) throws IOException{
        Configuration privateCommConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_PRIVATE_COMM_FILENAME, true);
        return privateCommConfiguration;
    }

    public static boolean initInitialConfiguration (Context context) throws IOException{
        boolean fileListener;
        boolean deleted = false;

        File sdCard = Environment.getExternalStorageDirectory();        
        File dir = new File (sdCard.getAbsolutePath() + "/" + com.bgu.agent.commons.utils.Utils.getConfigurationFolder(context));
        Configuration privateCommConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_PRIVATE_COMM_FILENAME, true);
        Configuration privateInnerCommConfiguration = null;
        try {
            privateInnerCommConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_PRIVATE_COMM_FILENAME, false);
        }
        catch ( Throwable t ){

        }
        if ( privateInnerCommConfiguration != null ){
            if ( privateCommConfiguration.getConfigurationHash() != privateInnerCommConfiguration.getConfigurationHash()){

                deleted = true;
            }
        }
        if ( new File ( dir, Constants.CONF_PUBLIC_COMM_FILENAME).exists() || new File ( dir, Constants.CONF_AGENT_FILENAME).exists()) {
            Configuration pub = null;
            Configuration agent = null;
            try {
                pub = Configuration.loadLocalConfiguration(context, Constants.CONF_PUBLIC_COMM_FILENAME, false);
                agent = Configuration.loadLocalConfiguration(context, Constants.CONF_AGENT_FILENAME, true);
            } catch (Throwable t) {
            } finally {
                if ( pub == null || agent == null ) {
                    if (new File(dir, Constants.CONF_AGENT_FILENAME).exists())
                        new File(dir, Constants.CONF_AGENT_FILENAME).delete();
                    if (new File(dir, Constants.CONF_PUBLIC_COMM_FILENAME).exists())
                        new File(dir, Constants.CONF_PUBLIC_COMM_FILENAME).delete();
                    try {
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_UNMOUNTED, Uri.fromFile(new File(dir, Constants.CONF_AGENT_FILENAME))));
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_UNMOUNTED, Uri.fromFile(new File(dir, Constants.CONF_PUBLIC_COMM_FILENAME))));
                    } catch (Throwable ta) {

                    }
                }
            }
        }
        if ( deleted || privateInnerCommConfiguration == null ) {
            storeConfiguration(context, privateCommConfiguration);
        }
        fileListener = privateCommConfiguration.getKeyAsString(Constants.COMM_OUTPUT).equalsIgnoreCase(FileListener.class.getCanonicalName());
        if ( fileListener ){
            Configuration publicCommConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_PUBLIC_COMM_FILENAME, true);
            storeConfiguration(context, publicCommConfiguration);
            Configuration agentConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_AGENT_FILENAME, true);
            storeConfiguration(context, agentConfiguration);
        }
        if ( deleted || privateCommConfiguration == null ) {
            try {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(dir)));
            } catch (Throwable t) {

            }
        }
        return fileListener;
    }

    //region Configuration management
    public static void storeConfiguration ( Context context, Configuration configuration ) throws IOException {
        configuration.updateConfiguration(context);
    }
    //endregion
}
