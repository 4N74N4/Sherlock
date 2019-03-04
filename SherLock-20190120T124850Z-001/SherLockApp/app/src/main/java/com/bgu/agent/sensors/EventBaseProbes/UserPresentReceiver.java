package com.bgu.agent.sensors.EventBaseProbes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;

/**
 * Created by shedan on 27/01/2015.
 */
public class UserPresentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try
        {
            Log.e(this.getClass().toString(), "user present event has been caught");
            Intent i = new Intent(Constants.INTENT_ACTION_USER_PRESENT);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.putExtra(Constants.TIMESTAMP, System.currentTimeMillis());
            context.sendBroadcast(i);
            Log.e(this.getClass().toString(), "user present event has been fired to probe");
        }
        catch (Throwable t)
        {
            Logger.e(getClass(), t.getMessage());
        }
    }
}

