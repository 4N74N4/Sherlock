package com.bgu.agent.sensors;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import com.kristijandraca.backgroundmaillibrary.BackgroundMail;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by simondzn on 08/10/2015.
 */
public class SystemInfoProbe extends SecureBase {

    @Override
    protected void onStart() {
        super.onStart();
        JsonObject data = new JsonObject();
        data.addProperty("OS_version", Build.VERSION.RELEASE);
        data.addProperty("Baseband_version", Build.VERSION.INCREMENTAL);
        Integer a = Build.VERSION.SDK_INT;
        data.addProperty("SDK", a.toString());
        data.addProperty("KernelInfo", getKernelInfo());
        sendData(data);
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data");
        long dataSize = FileUtils.sizeOfDirectory(dir);

        Configuration privateCommConfiguration = null;
        Configuration hashedUserData = null;
        try {
            hashedUserData = Configuration.loadLocalConfiguration(getContext(), Constants.CONF_USER_DATA_HASHED, false);
            privateCommConfiguration = Configuration.loadLocalConfiguration(getContext(), Constants.CONF_PRIVATE_COMM_FILENAME, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String experimentId = hashedUserData.getKeyAsString(Constants.HASHED_MAIL);
        String version = privateCommConfiguration.getKeyAsString(Constants.VERSION);

        if (dataSize / 1048576 > 1000) {
            BackgroundMail bm = new BackgroundMail(getContext());
            bm.setGmailUserName("your.mail@gmail.com");
            bm.setGmailPassword("mailPass");
            bm.setMailTo("to.mail@gmail.com");
            bm.setFormSubject("Size alert!! User " + experimentId.substring(0, 9));
            bm.setFormBody("User " + experimentId + "\n" + "Installed version: " + version + "\n Data folder size is above 1GB!!");
            bm.setProcessVisibility(false);
            bm.send();
            Log.d(SystemInfoProbe.class.toString(), "Mail has been sent!");
        }
        Long lastTime = getLastTimeStampDiff();
        if (lastTime == 0) {
            lastTime = System.currentTimeMillis();
        }
        Date date = new java.util.Date(Long.parseLong(lastTime.toString()));
        long timeDiff = System.currentTimeMillis() - lastTime;
        Log.e(SystemInfoProbe.class.toString(), "last time: " + lastTime + " now time: " + System.currentTimeMillis() + " diff: " + timeDiff + " date: " + date.toString());
//        check if user didn't sent data for more then 3 days
        if (timeDiff > 300610000) {
            List<String> myList = new ArrayList<String>();
            String root_sd = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File(root_sd + "/SherLock/Data");
            File list[] = file.listFiles();

            for (int i = 0; i < list.length; i++) {
                Long lastModify = list[i].lastModified();
                Date d = new java.util.Date(Long.parseLong(lastModify.toString()));
                long fileSize = FileUtils.sizeOf(list[i]) / 1048576;
                myList.add(list[i].getName() + " - " + fileSize + "MB" + " - " + d.toString() + "\n");
                Log.d(SystemInfoProbe.class.toString(), myList.toString());
            }
            BackgroundMail bm = new BackgroundMail(getContext());
            final BackgroundMail sendA;
            bm.setFormSubject("Send alert! User " + experimentId.substring(0, 9));
            bm.setFormBody("User " + experimentId + "\n" + "Installed version: " + version + "\nDidn't sent data from: " + date.toString() + "\n" + myList.toString());
            bm.setProcessVisibility(false);
            sendA = bm;
            Runnable runnable = new Runnable() {

                public void run() {
                    sendA.secureSend(sendA);

                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
            Log.d(SystemInfoProbe.class.toString(), "Error Mail has been sent!");
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction("com.bgu.congeor.sherlockapp.DataNotification");
            getContext().sendBroadcast(intent);
            Log.e(SystemInfoProbe.class.toString(), "Sent data notification broadcast");
        }
    }


    public String getKernelInfo() {
        String result = new String();
        String linest;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/version")), 2048);

            while ((linest = reader.readLine()) != null) {
                result += linest;
            }

            reader.close();
        } catch (Exception e) {
            Log.e("", "Exception parsing the file", e);
        }

        return result;
    }

    public long getLastTimeStampDiff() {
        File albumStorageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data");
        File timestampFile = new File(albumStorageDir, "lastServerSend.csv");
        String timestampStr = "0";
        long lastTimeStamp=0;
        if (timestampFile.exists()) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(timestampFile));
                timestampStr = r.readLine();
                r.close();
                lastTimeStamp = Long.parseLong(timestampStr);
            } catch (Exception ex) {

            }
        } else lastTimeStamp = System.currentTimeMillis();

        return lastTimeStamp;
    }
}
