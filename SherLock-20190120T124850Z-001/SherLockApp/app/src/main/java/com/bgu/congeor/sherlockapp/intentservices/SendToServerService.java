package com.bgu.congeor.sherlockapp.intentservices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.agent.manager.AgentManager;
import com.bgu.agent.sensors.ErrorSensor;
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.manager.CongeorManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Danny on 20/10/2014.
 */
public class SendToServerService extends BroadcastReceiver {

    private final int BUFFER = 2048;
    private final String timestampFileName = "lastServerSend.csv";
    String server_address = "";
    String port = "";
    String server_service = "";
    String privetCom, agentConf;
    String userHashedMail;
    String sendTimeNormal, agentConfigUpdate;

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.e("SendToServer", "alarm has been activated ");
        sendTimeNormal = intent.getStringExtra("send_time");
        final int wifiTries = intent.getIntExtra("wifiTries", 4);
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final Context tempCon = context;

        if (!mWifi.isConnected()) {

            if (wifiTries >= 0) {
                setAlarm(context, "noWifi", wifiTries - 1);
            } else {
                setAlarm(context, "", 0);//normal time
            }
        } else {
            //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            //boolean WifiToggle = preferences.getBoolean(Constants.WIFI_TOGGLE, true);
            boolean WifiToggle = true;

            //initiation after turn on or crash with file size enough for send
        boolean case1 = (intent.hasExtra("Initiation") )&&( intent.getBooleanExtra("Initiation",false)==true ) &&((checkFileSize(getDataFolderPath() + "/data.csv_text",500) || checkFileSize(getDataFolderPath()+"/compressedData.zip",0))) ;
            //force upload
        boolean case2 = (intent.hasExtra("force_upload") && WifiToggle);
            //normal use case.
        boolean case3 = (WifiToggle && com.bgu.utils.Utils.checkSendingTimeSlot() && checkLastSendingTimeInterval());
            if (case1 || case2 || case3) {
                //if (( WifiToggle ) || (intent.hasExtra("force_upload"))) {
                loadComConf(context);
                Thread sendThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String result = "";
                        HttpParams params;
                        long startTime;
                        boolean success = false;
                        boolean zippedSuccess = false;
                        params = new BasicHttpParams();
                        params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
                        params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
                        params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);

                        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                        HttpProtocolParams.setContentCharset(params, "utf8");

                        DefaultHttpClient client = new HttpClient(params);
                        HttpClient.setContext(tempCon.getApplicationContext());

                        HttpResponse getResponse = null;
                        try {
                            HttpPost postRequest = new HttpPost(server_address + ":" + port + "/" + server_service);
                            File toSend = new File(getDataFolderPath() + "/compressedData.zip");
                            if (!toSend.exists()) {
                                File dataToSend = new File(getDataFolderPath() + "/data.csv_text");
                                File compressedData = new File(getDataFolderPath() + "/data.csv_ToCompress");
                                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                if(!compressedData.exists()) {
                                    success = dataToSend.renameTo(compressedData);
                                    Log.e(SendToServerService.class.toString(), "rename success? , boolean value: " + success);


                                    if (!success) {
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        dataToSend.renameTo(compressedData);

                                    }
                                }
                                zippedSuccess = Utils.fileZipping(getDataFolderPath() + "/data.csv_ToCompress", "data.csv_ToCompress", getDataFolderPath() + "/compressedData.zip");
                                Log.e(SendToServerService.class.toString(), "zip success? , boolean value: " + zippedSuccess);
                                Thread.sleep(3000);
                                if (zippedSuccess) {
                                    compressedData.delete();
                                }
                            }
                            //clean Up in case of error leaving compressed
                            File compressedData = new File(getDataFolderPath() + "/data.csv_ToCompress");
                            if (compressedData.exists()) {
                                compressedData.delete();
                            }

                            FileBody fileBody = new FileBody(toSend);
                            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                            builder.addPart("data", fileBody);
                            String fileName = userHashedMail + "_" + System.currentTimeMillis();
                            builder.addPart("file_name", new StringBody(fileName));
                            Log.e(SendToServerService.class.toString(), "sending file with name:  " + fileName);
                            builder.addTextBody(Constants.CONF_AGENT_HASH, agentConf.toString());
                            builder.addTextBody(Constants.CONF_COMM_HASH, privetCom.toString());

                            postRequest.setEntity(builder.build());
                            Log.e(SendToServerService.class.toString(), "sending a file with size : " + String.valueOf(toSend.length()));
                            getResponse = client.execute(postRequest);
                            HttpEntity responseEntity = null;
                            if (getResponse.getFirstHeader(Constants.CONF_AGENT_HASH).getValue().equals("False"))
                                responseEntity = getResponse.getEntity();
                            String response_str = null;
                            if (responseEntity != null)
                                response_str = EntityUtils.toString(responseEntity, HTTP.UTF_8);
                            String[] configsStr = new String[2];
                            if (getResponse.getStatusLine().getStatusCode() == 200) {
                                setLastTimeStamp(System.currentTimeMillis());
                                Log.e(SendToServerService.class.toString(), "response content looks like this " + "\t" + response_str);
                                toSend.delete();
                                if (toSend.exists()) {
                                    toSend.delete();
                                }
                                setAlarm(tempCon, "", wifiTries);
                                Log.e("SendToServer", "send success!!! noraml alarm has been set again");
                            } else {
                                setAlarm(tempCon, "Error", wifiTries);
                                Log.e("SendToServer", getResponse.getStatusLine().toString());
                            }


                        } catch (Exception ex) {
                            setAlarm(tempCon, "Error", wifiTries);
                            Log.e("SendToServer", "the error while sending is: " + ex.getMessage());
                        }
                    }
                });
                sendThread.setPriority(Thread.MAX_PRIORITY);
                sendThread.start();
            } else {
                setAlarm(tempCon, "Retry", wifiTries);
            }
        }
    }

    public void setAlarm(Context context, String status, int tries) {
        Intent itnt = new Intent(context, SendToServerService.class);
        itnt.putExtra("wifiTries", tries);
        itnt.putExtra("send_time", sendTimeNormal);
        itnt.putExtra("Initiation",false);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 1, itnt, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        if (status.equals("Error")) {
            //------------- production -----------
            //String timeToSend = super.privateCommConfig.getKeyAsString("send_data_time");
            //calendar.set(Calendar.HOUR_OF_DAY, Integer.getInteger(timeToSend.split(":")[0]));
            //calendar.set(Calendar.MINUTE, Integer.getInteger(timeToSend.split(":")[1]));
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() +
                            1000 * 60 * 5, alarmIntent);
            Log.e("SendToServer", " error alarm has been set");
        } else {
            if (status.equals("noWifi")) { //try every 15 mins
                //TODO add counter to limit the tries
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() +
                                1000 * 60 * 15, alarmIntent);
                Log.e("SendToServer", " noWifi alarm has been set ");
            } else {
                if (status.equals("Retry"))
                    alarmManager.set(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() +
                                    1000 * 60 * 60, alarmIntent);
                else {
                    Calendar current = Calendar.getInstance();
                    Calendar sendTIme = Calendar.getInstance();
                    sendTIme.set(Calendar.HOUR_OF_DAY,Integer.parseInt(sendTimeNormal.split(":")[0]));
                    sendTIme.set(Calendar.MINUTE,Integer.parseInt(sendTimeNormal.split(":")[1]));
                    if(current.before(sendTIme) )
                        if((current.HOUR_OF_DAY+12> sendTIme.HOUR_OF_DAY))
                                alarmManager.set(AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis() +
                                    1000 * 60 * 60 * 12, alarmIntent);
                        else{
                            alarmManager.set(AlarmManager.RTC_WAKEUP,sendTIme.getTimeInMillis(), alarmIntent);
                        }
                    else {
                        sendTIme.add(Calendar.DAY_OF_WEEK,1);
                        alarmManager.set(AlarmManager.RTC_WAKEUP,sendTIme.getTimeInMillis() , alarmIntent);
                    }
                }
                Log.e("SendToServer", " Next planned scheduled alarm has been set to " + calendar.getTime().getTime());
            }
        }
    }

    private boolean fileZipping(String prevPath, String prevName, String destPath) {
        byte[] buffer = new byte[1024];
        try {

            FileOutputStream fos = new FileOutputStream(destPath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry ze = new ZipEntry(prevName);
            zos.putNextEntry(ze);
            FileInputStream in = new FileInputStream(prevPath);

            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            in.close();
            zos.closeEntry();
            zos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("readying data to send ", "Error zipping the file : " + e.getMessage());
            return false;

        }
    }

    private void loadComConf(Context context) {
        try {
            Configuration agentConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_AGENT_FILENAME, false);
            Configuration publicCommConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_PUBLIC_COMM_FILENAME, false);
            Configuration privateCommConfig = Configuration.loadLocalConfiguration(context, Constants.CONF_PRIVATE_COMM_FILENAME, false);
            Configuration hashedUserData = Configuration.loadLocalConfiguration(context, Constants.CONF_USER_DATA_HASHED, false);
            if (privateCommConfig != null) {

                userHashedMail = hashedUserData.getKeyAsString(Constants.HASHED_MAIL);

                Log.e(SendToServerService.class.toString(), "userID is: " + userHashedMail);
                if (userHashedMail != null && userHashedMail.length() >= 10) {
                    userHashedMail = userHashedMail.substring(0, 10);
                    Log.e(SendToServerService.class.toString(), "userID after substring is: " + userHashedMail);
                } else {
                    Log.e(SendToServerService.class.toString(), "no substring has been called!");
                    userHashedMail = "";
                }
                server_address = privateCommConfig.getKeyAsString("data_server_ip");
                port = privateCommConfig.getKeyAsString("data_server_port");
                server_service = privateCommConfig.getKeyAsString("data_services_base_url");
                byte[] ComData = privateCommConfig.getConfContent().getBytes();

                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                md.update(ComData);
                byte[] hash = md.digest();
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < hash.length; i++) {
                    String hex = Integer.toHexString(0xff & hash[i]);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                privetCom = hexString.toString();
                Log.e(SendToServerService.class.toString(), "privateCom hash is: " + privetCom);
            }


            if (agentConfiguration != null) {


                byte[] data = agentConfiguration.getConfContent().getBytes();

                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                md.update(data);
                byte[] hash = md.digest();
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < hash.length; i++) {
                    String hex = Integer.toHexString(0xff & hash[i]);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                agentConf = hexString.toString();
                Log.e(SendToServerService.class.toString(), "agent hash is: " + agentConf);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void confChanged(Context context, String confType, String jsonContent) {
        try {
            Log.e(SendToServerService.class.toString(), "starting to change conf");

            Configuration configuration = Configuration.loadConfiguration(false, confType, jsonContent);
            AgentManager.storeConfiguration(context, configuration);
            Logger.d(SendToServerService.class, "Login - Received new Agent configuration from server: " + configuration.toString());
            AgentManager.agentConfigurationChanged(context);
        } catch (Throwable e) {
            Intent error = ErrorSensor.generateErrorIntent(CongeorManager.class, e);
            context.sendBroadcast(error);
            Logger.e(SendToServerService.class, e.getLocalizedMessage(), e);

        }
    }

    private long getLastTimeStamp() {
        File albumStorageDir = new File(getDataFolderPath());
        File timestampFile = new File(albumStorageDir, timestampFileName);
        String timestampStr = "0";
        if (timestampFile.exists()) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(timestampFile));
                timestampStr = r.readLine();
                r.close();
            } catch (Exception ex) {

            }
        }
        long lastTimeStamp = Long.parseLong(timestampStr);
        return lastTimeStamp;
    }

    private void setLastTimeStamp(long timestamp) {
        try {
            File albumStorageDir = new File(getDataFolderPath());
            File timestampFile = new File(albumStorageDir, timestampFileName);
            if (timestampFile.exists())
                timestampFile.delete();
            PrintWriter p = new PrintWriter(timestampFile);
            p.println(timestamp);
            p.close();
        } catch (Exception e) {
            Logger.e(getClass(), e.getMessage());


        }
    }

    private boolean checkLastSendingTimeInterval() {
        long limit = 1000 * 60 * 60 * 12;
        long last = getLastTimeStamp();
        Log.e(SendToServerService.class.toString(), "last send to server was : " + last);
        Log.e(SendToServerService.class.toString(), "Diff is  " + (System.currentTimeMillis() - last));
        if (last == 0 || (System.currentTimeMillis() - last >= limit))
            return true;
        else return false;
    }
    private boolean checkFileSize(String path,int size){
        File file = new File(path);
        if(file.exists() && ((file.length()/1024)/1024)>=size){
            return true;
        }
        else return false;
    }

    private String getDataFolderPath() {
        File sdCard = Environment.getExternalStorageDirectory();
        String path = sdCard.getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data";
        return path;
    }
}
