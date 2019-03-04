package com.bgu.congeor.sherlockapp.intentservices;

/**
 * Created by shedan on 20/10/2014.
 */

import android.content.Context;
import com.bgu.congeor.sherlockapp.R;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import java.io.InputStream;
import java.security.KeyStore;

public class HttpClient extends DefaultHttpClient {

    private static Context context;

    public static void setContext(Context context) {
        HttpClient.context = context;
    }

    public HttpClient(HttpParams params) {
        super(params);
    }

    public HttpClient(ClientConnectionManager httpConnectionManager, HttpParams params) {
        super(httpConnectionManager, params);
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 8080));
        // Register for port 443 our SSLSocketFactory with our keystore
        // to the ConnectionManager
        registry.register(new Scheme("https", newSslSocketFactory(), 8443));
        return new ThreadSafeClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory newSslSocketFactory() {

        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)

            //InputStream in = HttpClient.context.getResources().openRawResource(res.raw.Key);


//            InputStream in = HttpClient.context.getResources().openRawResource(
//                    HttpClient.context.getResources().getIdentifier("raw/"+Constants.Key,
//                            "raw", context.getPackageName()));


            InputStream in = context.getResources().openRawResource(R.raw.keyandroid);
            try {
                // Initialize the keystore with the provided trusted certificates
                // Provide the password of the keystore
                trusted.load(in,"123456".toCharArray());
            } finally {
                in.close();
            }
            // Pass the keystore to the SSLSocketFactory. The factory is responsible
            // for the verification of the server certificate.
            SSLSocketFactory sf = new SSLSocketFactory(trusted);
            // Hostname verification from certificate
            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
            sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER); // This can be changed to less stricter verifiers, according to need
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}