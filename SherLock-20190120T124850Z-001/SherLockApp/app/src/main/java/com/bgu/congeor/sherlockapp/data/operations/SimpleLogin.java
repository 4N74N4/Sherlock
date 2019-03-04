package com.bgu.congeor.sherlockapp.data.operations;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.R;

import javax.net.ssl.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

/**
 * Created by shedan on 10/12/2014.
 */
public class SimpleLogin extends AsyncTask<Object, Void, String> {

    IRunnableWithParameters runnableWithParameters;
    String hashedMail;
    Context context;

    public SimpleLogin(IRunnableWithParameters runnable, String hash, Context context) {
        runnableWithParameters = runnable;
        hashedMail = hash;
        this.context = context;
    }

    @Override
    protected String doInBackground(Object[] params) {
        String serverResult = null;
        if (!Utils.isOnline(context, false)) {
            return Constants.CONNECTION_ERROR;
        }
        try {
            Configuration privateCommConfig = Configuration.loadLocalConfiguration(context, Constants.CONF_PRIVATE_COMM_FILENAME, true);
            String server_address = privateCommConfig.getKeyAsString("data_server_ip");
            String port = privateCommConfig.getKeyAsString("data_server_port");
            String server_service = privateCommConfig.getKeyAsString("ID_services_base_url");

            Log.e("tag", "userid start");
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv =
                            HttpsURLConnection.getDefaultHostnameVerifier();
                    return true;
                }
            };

            URL url = null;
            url = new URL(server_address + ":80/" + server_service);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setHostnameVerifier(hostnameVerifier);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/txt");
            urlConnection.setChunkedStreamingMode(0);
            KeyStore trusted = KeyStore.getInstance("BKS");
            InputStream in = context.getApplicationContext().getResources().openRawResource(R.raw.certificate);
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
            SSLSocketFactory sslSocketFactory = context.getSocketFactory();
//
//                    //Set our custom SSLSocketFactory to be used by our HttpsURLConnection instance
            urlConnection.setSSLSocketFactory(sslSocketFactory);

            DataOutputStream request = new DataOutputStream(
                    urlConnection.getOutputStream());

            request.writeBytes(hashedMail);
            request.flush();
            request.close();

                if (urlConnection.getResponseCode() == 200) {
                    Log.e("tag", "userid success" );
                    serverResult = Constants.VALID_HASH;
                } else {
                    serverResult = Constants.SERVER_ERROR;

                }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return serverResult;
        }
    }


    @Override
    public void onPostExecute(String result) {
        runnableWithParameters.run(result, hashedMail);
    }
}
