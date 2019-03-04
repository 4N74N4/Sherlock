package com.bgu.congeor.sherlockapp.intentservices;

import android.app.AlarmManager;
import android.app.IntentService;
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
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.R;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

/**
 * Created by Danny on 20/10/2014.
 */
public class SendToServerService2 extends BroadcastReceiver {

    private final int BUFFER = 2048;
    private final String timestampFileName = "lastServerSend.csv";
    private final String alarmFileName = "lastAlarmTry.csv";
    static String server_address = "";
    static String port = "";
    static String server_service = "";
    static String privetCom, agentConf;
    static String userHashedMail;
    static String userFullHashedMail;
    static long nextalarm;
    static int succCounter;
    static String sendTimeNormal, agentConfigUpdate;
    public static String version;

    @Override
    public void onReceive(final Context context, Intent intent) {
        nextalarm = 0L;
        //
        Log.e(SendToServerService2.class.toString(), "alarm has been activated ");
        succCounter = 0;
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


            File[] files = getZippedFiles();

            loadComConf(context);
            if (files.length > 0) {
                try {
                    int counter = 0;
                    for (File f : files) {
//                        TODO encrypt the files here!!!
//                        if (!f.getName().contains("encrypted")) {
//                            f = Encrypter(f);
//                        }
                        Intent i = new Intent(context, SendRunnable.class);
                        i.putExtra("path", f.getAbsolutePath());
                        i.putExtra("wifi", wifiTries);
                        i.putExtra("id", counter);
                        Log.i(SendToServerService2.class.toString(), "intent is about to start id: " + counter);
                        counter++;
                        context.startService(i);
                    }
                    setLastTimeStamp(System.currentTimeMillis());
                    setAlarm(tempCon, "", wifiTries);
                } catch (Exception ex) {
                    setAlarm(tempCon, "Retry", wifiTries);
                }
            } else {
                setAlarm(tempCon, "Retry", wifiTries);
            }
        }
    }

    private File[] getZippedFiles() {

        File directory = new File(getDataFolderPath());
        FileFilter zipFilter = new RegexFileFilter(".*\\.zip");
        File[] files = directory.listFiles(zipFilter);
        return files;
    }

    public void setAlarm(Context context, String status, int tries) {
        Intent itnt = new Intent(context, SendToServerService2.class);

        itnt.putExtra("wifiTries", tries);
        itnt.putExtra("send_time", sendTimeNormal);
        itnt.putExtra("Initiation", false);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 1, itnt, PendingIntent.FLAG_UPDATE_CURRENT);

        if (status.equals("Error")) {

            alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() +
                            1000 * 60 * 5, alarmIntent);
            nextalarm = (System.currentTimeMillis() + (1000 * 60 * 5));
            Log.i(SendToServerService2.class.toString(), " error alarm has been set");
        } else {
//           Here we can change the alarm interval
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() +
                            1000 * 60 * 60, alarmIntent);
            nextalarm = (System.currentTimeMillis() + (1000 * 60 * 60));
        }
        setLastAlarmTry(nextalarm);
        Log.i(SendToServerService2.class.toString(), "alarm set finished, next alarm occurs at: " + nextalarm);
    }

    public static File Encrypter(File input) {
        long timestamp = System.currentTimeMillis();

        String path = input.getAbsolutePath().replace(input.getName(), "");
        File outfile = new File(path + "encrypted_" + input.getName());
        try {
            FileInputStream fis = new FileInputStream(input);
            String type;
            Log.i(SendToServerService2.class.toString(), "Starting Encryption: " + input.getName());

            int read;
            FileOutputStream fos = new FileOutputStream(outfile);
            Cipher encipher = Cipher.getInstance("AES");
            KeyGenerator kgen = KeyGenerator.getInstance("AES");

            SecretKey skey = new SecretKeySpec(userFullHashedMail.substring(0,16).getBytes(), "AES");
            encipher.init(Cipher.ENCRYPT_MODE, skey);
            CipherInputStream cis = new CipherInputStream(fis, encipher);
            while ((read = cis.read()) != -1) {
                fos.write((char) read);
                fos.flush();
            }
            fos.close();
            input.delete();
            Log.i(SendToServerService2.class.toString(), "Finished Encryption");
        } catch (Exception e) {
            Log.e(SendToServerService2.class.toString(), "Error encrypt: " + e.toString());
        }
        return outfile;
    }

    private void loadComConf(Context context) {
        try {
            Configuration agentConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_AGENT_FILENAME, false);
            Configuration publicCommConfiguration = Configuration.loadLocalConfiguration(context, Constants.CONF_PUBLIC_COMM_FILENAME, false);
            Configuration privateCommConfig = Configuration.loadLocalConfiguration(context, Constants.CONF_PRIVATE_COMM_FILENAME, false);
            Configuration hashedUserData = Configuration.loadLocalConfiguration(context, Constants.CONF_USER_DATA_HASHED, false);
            if (privateCommConfig != null) {

                userHashedMail = hashedUserData.getKeyAsString(Constants.HASHED_MAIL);
                userFullHashedMail = userHashedMail;
                Log.e(SendToServerService2.class.toString(), "userID is: " + userHashedMail);
                if (userHashedMail != null && userHashedMail.length() >= 10) {
                    userHashedMail = userHashedMail.substring(0, 10);
                    //Log.e(SendToServerService2.class.toString(), "userID after substring is: " + userHashedMail);
                } else {
                    //Log.e(SendToServerService2.class.toString(), "no substring has been called!");
                    userHashedMail = "";
                }
                server_address = privateCommConfig.getKeyAsString("data_server_ip");
                port = privateCommConfig.getKeyAsString("data_server_port");
                server_service = privateCommConfig.getKeyAsString("data_services_base_url");
                version = privateCommConfig.getKeyAsString("version");
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

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getLastTimeStamp() {
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

    public static boolean checkSendingTimeSlot() {
        Calendar current = Calendar.getInstance();
        boolean ans = ((current.get(Calendar.HOUR_OF_DAY) < 18) && (current.get(Calendar.HOUR_OF_DAY) > 6));

        return !ans;
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

    //test function
    private void setLastAlarmTry(long timestamp) {
        try {
            File albumStorageDir = new File(getDataFolderPath());
            File timestampFile = new File(albumStorageDir, alarmFileName);
            if (timestampFile.exists())
                timestampFile.delete();
            PrintWriter p = new PrintWriter(new FileWriter(timestampFile, true));
            p.println(timestamp);
            p.close();
        } catch (Exception e) {
            Logger.e(getClass(), e.getMessage());
        }
    }

    private boolean checkLastSendingTimeInterval() {
        long limit = 1000 * 60 * 60 * 12;
        long last = getLastTimeStamp();
        Log.e(SendToServerService2.class.toString(), "last send to server was : " + last);
        Log.e(SendToServerService2.class.toString(), "Diff is  " + (System.currentTimeMillis() - last));
        if (last == 0 || (System.currentTimeMillis() - last >= limit))
            return true;
        else return false;
    }


    private String getDataFolderPath() {
        File sdCard = Environment.getExternalStorageDirectory();
        String path = sdCard.getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data";
        return path;
    }

    public static class SendRunnable extends IntentService {
        public SendRunnable() {
            super("sendRunnable");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            ConnectivityManager connManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnected()) {
                Log.i(SendToServerService2.class.toString(), "onHandleIntent started id: " + intent.getIntExtra("id", -1));
                HttpParams params;
                params = new BasicHttpParams();
                params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
                params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
                params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, "utf8");
                HttpClient.setContext(getApplicationContext());
                try {
                    File zipToSend = new File(intent.getStringExtra("path"));
                    FileBody fileBody = new FileBody(zipToSend);
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.addPart("data", fileBody);
                    String fileName = "";
                    if (zipToSend.getName().contains("Continues")) {
                        fileName = userHashedMail + "_Continues_" + System.currentTimeMillis() + ".zip";
                    } else {
                        fileName = userHashedMail + "_" + System.currentTimeMillis() + "_" + version;
                    }
                    if (zipToSend.getName().contains("encrypted")){
                        fileName = userHashedMail + "_" + System.currentTimeMillis() + "_" + version + "_" + "encrypted";
                    }
                    builder.addTextBody("file_name", fileName);
                    builder.addTextBody(Constants.CONF_AGENT_HASH, agentConf.toString());
                    builder.addTextBody(Constants.CONF_COMM_HASH, privetCom.toString());
                    builder.addTextBody(Constants.APP_VERSION, version);
                    HttpEntity entity = builder.build();

//                New send code due to problems with httpClient

//
                HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        HostnameVerifier hv =
                                HttpsURLConnection.getDefaultHostnameVerifier();
                        return true;
                    }
                };
                    URL url = null;
                    url = new URL("server_address + : + 80 + / + server_service");
                    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setHostnameVerifier(hostnameVerifier);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "multipart/form-data");
                    urlConnection.setChunkedStreamingMode(0);
                    KeyStore trusted = KeyStore.getInstance("BKS");
                    InputStream in = getApplicationContext().getResources().openRawResource(R.raw.certificate);
                    try {
                        // Initialize the keystore with the provided trusted certificates
                        // Provide the password of the keystore
                        trusted.load(in, "cert_key".toCharArray());
                    } finally {
                        in.close();
                    }

//                    Initialise a TrustManagerFactCluesory with the CA keyStore
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                    tmf.init(trusted);
                    //Create new SSLContext using our new TrustManagerFactory
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, tmf.getTrustManagers(), null);
//                    //Get a SSLSocketFactory from our SSLContext
//
                    SSLSocketFactory sslSocketFactory = context.getSocketFactory();
//
//
//                    //Set our custom SSLSocketFactory to be used by our HttpsURLConnection instance
                    urlConnection.setSSLSocketFactory(sslSocketFactory);

                    DataOutputStream request = new DataOutputStream(
                            urlConnection.getOutputStream());

                    entity.writeTo(request);
                    request.flush();
                    request.close();


                    if (urlConnection.getResponseCode() == 200) {
                        Log.e(SendToServerService2.class.toString(), "SentoServer completed" + urlConnection.getResponseCode());
                        zipToSend.delete();
                        succCounter++;
                        return;
                    } else {
                        Log.e(SendToServerService2.class.toString(), "error: " + urlConnection.getResponseMessage());
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Intent itnt = new Intent(getApplicationContext(), SendToServerService2.class);

                    itnt.putExtra("wifiTries", 4);
                    itnt.putExtra("send_time", sendTimeNormal);
                    itnt.putExtra("Initiation", false);
                    AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(getApplicationContext().ALARM_SERVICE);
                    PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, itnt, PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() +
                                    1000 * 60 * 10, alarmIntent);
                    nextalarm = (System.currentTimeMillis() + (1000 * 60 * 5));
                    return;
                }
            } else return;
        }
    }
}

