package com.bgu.agent.sensors.EventBaseProbes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;

/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/30/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */


public class AppChangedReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            Intent i = new Intent(Constants.INTENT_ACTION_APPINFO);
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED))
            {
                i.putExtra(Constants.ACTION, Constants.APP_REMOVED);
            }

            else if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED))
            {
                i.putExtra(Constants.ACTION, Constants.APP_CHANGED);
            }

            else if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED))
            {
                i.putExtra(Constants.ACTION, Constants.APP_ADDED);
            }
            else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED))
            {
                i.putExtra(Constants.ACTION, Constants.APP_UPDATED);
            }

            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.putExtra(Constants.APP_NAME, intent.getDataString().substring(8));
            Log.i("TAG", "change:"  +intent.getDataString().substring(8));
            context.sendBroadcast(i);
        }
        catch (Throwable t)
        {
            Logger.e(getClass(), t.getMessage());
        }
    }
}
