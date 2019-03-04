package com.bgu.congeor.sherlockapp.intentservices;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.bgu.agent.AgentDataService;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.bgu.congeor.UserDetails;
import com.bgu.congeor.sherlockapp.MainActivity;
import com.bgu.congeor.sherlockapp.R;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by clint on 1/30/14.
 * modified by Danny 9/2/14
 */
public class CongeorDataService extends AgentDataService {

    SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);
    String lastFunfMessage = "";
    String lastServerCommunicationMessage = "";
    Intent sensorService;

    @Override
    protected void keepAliveServices() {
        super.keepAliveServices();
//        sensorService = new Intent(getBaseContext(), SensorService.class);
//        getBaseContext().startService(sensorService);
    }

    @Override
    public String getUserId() {
        try {
            File userDetailsFile = getUserDetailsFile();
            if (!userDetailsFile.exists()) {
                return null;
            }
            InputStream r = new FileInputStream(userDetailsFile);
            Kryo kryo = new Kryo();
            Input input = new Input(r);
            UserDetails userDetails = kryo.readObject(input, UserDetails.class);
            input.close();
            r.close();
            return userDetails.getUserId();
        } catch (FileNotFoundException e) {
            Logger.e(CongeorDataService.class, e.getLocalizedMessage(), e);
            return null;
        } catch (IOException e) {
            Logger.e(CongeorDataService.class, e.getLocalizedMessage(), e);
            return null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //check if service is not already running, meaning this is first initialization - initialize Update alarm service
        //int threadID = android.os.Process.getThreadPriority(android.os.Process.myTid());
        //Log.e(CongeorDataService.class.toString(), "ConDataService threadID: " + threadID);
        Thread.UncaughtExceptionHandler handlerUnCaughtException = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Intent intent = new Intent(getApplicationContext(), AgentBroadcastReceiver.class);
                PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 5, intent, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+3000,pIntent);
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(handlerUnCaughtException);
        dataFilesSizeNotification();
        //
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getAppContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.WIFI_TOGGLE, true);
        editor.apply();
        //
        int returnVal = super.onStartCommand(intent, flags, startId);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.FUNF_KEEP_ALIVE_ACTION);
        intentFilter.addCategory(Constants.FUNF_AGENT_PACKAGE);
        registerReceiver(receiver, intentFilter);
        return returnVal;
    }


    @Override
    public Notification getNotification() {
        return getNotification(lastFunfMessage, lastServerCommunicationMessage);
    }

    public Notification getNotification(String... lines) {
        String title = "SherLock Agent Active";
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getBaseContext())
                        .setSmallIcon(R.drawable.sherlock1_wb)
                        .setContentTitle("SherLock Application")
                        .setContentText(title)
                        .setOngoing(true)
                        .setWhen(System.currentTimeMillis());
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);
        if (lines != null && lines.length > 0) {
            for (String i : lines) {
                inboxStyle.addLine(i);
            }
        }
        Intent mainActivityActivator = new Intent(this, MainActivity.class);
        mainActivityActivator.setAction(Intent.ACTION_MAIN);
        mainActivityActivator.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, mainActivityActivator, Notification.FLAG_ONGOING_EVENT | PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder = mBuilder.setStyle(inboxStyle).setContentIntent(contentIntent);
        Notification not = mBuilder.build();
        not.flags = Notification.FLAG_ONGOING_EVENT;

//        not.setLatestEventInfo(this, "SherLock Application", title, contentIntent);
        return not;
    }

    protected void stopKeepAliveServices() {
//        stopService(sensorService);
    }

    public void updateNotification(Notification not) {
        startForeground(4, not);
    }

    private File getUserDetailsFile() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/" + "/" + Utils.getConfigurationFolder(getBaseContext()));
        return new File(dir, "userDetails");
    }

    String[] getMessages(String... messages) {
        if (messages == null) {
            return new String[0];
        } else {
            List<String> returnVal = new Vector<String>();
            for (int i = 0; i < messages.length; i++) {
                if (!messages[i].isEmpty()) {
                    returnVal.add(messages[i]);
                }
            }
            return returnVal.toArray(new String[0]);
        }
    }

    @Override
    protected void onServerCommunication() {
        lastServerCommunicationMessage = "Server communication at " + String.format(df.format(System.currentTimeMillis()));
        Notification not = getNotification(getMessages(lastFunfMessage, lastServerCommunicationMessage));
        updateNotification(not);
    }

    protected void receivedBroadcast(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Constants.FUNF_KEEP_ALIVE_ACTION)) {
            lastFunfMessage = "FUNF live " + String.format(df.format(System.currentTimeMillis()));
        }
        Notification not = getNotification(getMessages(lastFunfMessage, lastServerCommunicationMessage));
        updateNotification(not);
    }

    @Override
    protected Uri getAppURL() {
        return Uri.parse("market://details?id=com.bgu.congeor.congeorapp");
    }


    @Override
    protected int getAppIcon() {
        return R.drawable.notification_icon;
    }

    @Override
    protected void onInitComplete() {

        super.onInitComplete();

        //check if this alarm has already been set
        Intent itnt = new Intent(getApplicationContext(), SendToServerService2.class);
        String timeToSend = super.publicCommConfiguration.getKeyAsString("send_data_time");
        itnt.putExtra("send_time", timeToSend);
        itnt.putExtra("wifiTries", 4);
        itnt.putExtra("Initiation",true);
        if (PendingIntent.getBroadcast(this, 1, itnt, PendingIntent.FLAG_NO_CREATE) == null) {
            try {
                AlarmManager alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
                PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 1, itnt, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() +
                                1000 * 60 * 10, alarmIntent);
                Log.i(CongeorDataService.class.toString(), " Alarm Set: from initcomplete in CongeourDataService");
            } catch (Exception ex) {
                Log.i(CongeorDataService.class.toString(), "alarm setting error  " + ex.getMessage());
            }
        }
        else
            Log.i(CongeorDataService.class.toString(), "Alarm Set : Pending intent already exists");
    }

    private void dataFileSizeNotification() {
        File sdCard = Environment.getExternalStorageDirectory();
        String path = sdCard.getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data/data.csv_text";
        File dataFile = new File(path);
        if(dataFile.exists()) {
            double size = dataFile.length()/1048576;
            if(size>2500)
            {
                createNotification();
            }
        }
    }
    private void dataFilesSizeNotification(){
        File sdCard = Environment.getExternalStorageDirectory();
        FileFilter zipFilter = new RegexFileFilter("[0-9]+.zip");
        File[] files = sdCard.listFiles(zipFilter);
        long totalSize=0;
        for(File f : files)
        {
            totalSize+=f.length();
        }
        if(totalSize>2500)
        {
            createNotification();
        }
    }

    private void createNotification() {
        Intent itnt = new Intent(this,MainActivity.class);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Sherlock Data Limit Exceeded")
                .setContentText("Please connect to WIFI and upload the data through the application button")
                .setPriority(1);
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Sherlock Data Limit Exceeded");
        inboxStyle.addLine("Please connect to WIFI");
        inboxStyle.addLine("and upload the data through the application button");
        mBuilder = mBuilder.setStyle(inboxStyle);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(itnt);
        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0,PendingIntent.FLAG_ONE_SHOT);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0,mBuilder.build());
    }


}

