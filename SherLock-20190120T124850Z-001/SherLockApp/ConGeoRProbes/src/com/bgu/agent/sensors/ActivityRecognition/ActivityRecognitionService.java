package com.bgu.agent.sensors.ActivityRecognition;

import android.app.IntentService;
import android.content.Intent;


/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/22/13
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActivityRecognitionService extends IntentService
{


    public ActivityRecognitionService()
    {
        super("ActivityRecognitionService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {

        // broadcast the activity recognition data gets from google client
        Intent i = new Intent("com.bgu.congeor.sensors.ActivityRecognition.Data");
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.putExtras(intent.getExtras());
        sendBroadcast(i);
    }

}
