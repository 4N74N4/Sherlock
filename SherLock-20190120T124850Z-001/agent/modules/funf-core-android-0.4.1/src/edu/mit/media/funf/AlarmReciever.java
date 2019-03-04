package edu.mit.media.funf;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/**
 * Created by simondzn on 28/12/2015.
 */
public class AlarmReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        String type = intent.getExtras().getString("type");
        Uri componentAndAction = (Uri) intent.getExtras().get("uricomp");
//        Log.e(FunfManager.class.toString(), " In broadcast " + componentAndAction + "type: "+ type);
        long interval =  intent.getLongExtra("interval", 0);
        Intent i = FunfManager.getFunfIntent(context, type, componentAndAction);
        context.startService(i);
        int k = intent.getIntExtra("pi",0);
        PendingIntent pending = PendingIntent.getService(context, k,i,PendingIntent.FLAG_UPDATE_CURRENT);
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),pending);
        Intent recieverIntent = new Intent(context, AlarmReciever.class);
        recieverIntent.putExtra("type", type);
        recieverIntent.putExtra("uricomp", componentAndAction);
        recieverIntent.putExtra("interval", interval);
        recieverIntent.putExtra("pi", k);
//        PendingIntent pi = PendingIntent.getBroadcast(context, k, recieverIntent, PendingIntent.FLAG_ONE_SHOT);
        long intervalMilis = System.currentTimeMillis() + interval;
        SetAlarm(context, recieverIntent, intervalMilis, k);
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, intervalMilis, pi);
//        Log.i(AlarmReciever.class.toString(), "k = " + k + " in broadcast: " + componentAndAction.getPath());

    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void SetAlarm(Context context, Intent intent, long interval , int k)
    {
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, k, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(interval, pi);
            am.setAlarmClock(alarmClockInfo, pi);
        }else {
            am.setExact(AlarmManager.RTC_WAKEUP, interval, pi);
        }

    }
}
