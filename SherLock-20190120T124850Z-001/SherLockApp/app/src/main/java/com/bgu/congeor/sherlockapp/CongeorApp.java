package com.bgu.congeor.sherlockapp;

import android.app.Application;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.sherlockapp.manager.CongeorManager;

/**
 * Created by clint on 12/23/13.
 */
public class CongeorApp extends Application
{
    private static CongeorApp instance;
    private CongeorManager agentManager;

    public static CongeorApp getInstance()
    {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Logger.TAG = getResources().getText(getResources().getIdentifier("app_name", "string", getPackageName())).toString();
        Logger.d(this.getClass(), "Application create method start");
        instance = this;
        agentManager = new CongeorManager();
        CongeorManager.setInstance(agentManager);
        agentManager.setApplicationContext(this.getApplicationContext());
        agentManager.init();
        registerActivityLifecycleCallbacks(agentManager);
    }
}
