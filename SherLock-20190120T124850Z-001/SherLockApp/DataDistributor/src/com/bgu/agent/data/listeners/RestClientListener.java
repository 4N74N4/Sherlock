package com.bgu.agent.data.listeners;

import android.content.Context;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpVersion;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.client.utils.URIBuilder;
import ch.boye.httpclientandroidlib.entity.ByteArrayEntity;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.params.HttpProtocolParams;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.agent.data.exceptions.*;
import com.bgu.congeor.Constants;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/1/13
 * Time: 9:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestClientListener extends AbstractCommunicationListener {

    private final String timestampFilename = "timestamp.txt";
    WeakReference<Context> appContext;
    long lastTimeStamp = 0;
    ICommunicationService serviceDetails;
    PublicKey publicKey;

    public RestClientListener (Context appContext, ICommunicationService serviceDetails, PublicKey publicKey ){
        this.appContext = new WeakReference<Context>(appContext);
        this.serviceDetails = serviceDetails;
        this.publicKey = publicKey;
    }

    @Override
    public String getDataListenerName() {
        return "RestClient";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T> T processObject(String serviceMethod, Object payload, Map<String, String> params, Class<T> returnClassType, boolean encrypt, boolean GET) throws CongeorException {
        boolean reachable = Utils.isOnline(appContext.get(), true);
        if ( !reachable ){
            NoInternetException congeorException = new NoInternetException(new Exception ("no internet exception"));
            throw congeorException;
        }
        String service = Utils.getService(serviceMethod);
        String method = Utils.getMethod(serviceMethod);
        String hostName = serviceDetails.getServerIP(service);
        String baseUrl = serviceDetails.getBaseURL(service);
        int port = serviceDetails.getServerPort(service);
        HttpHost host = new HttpHost(hostName, port);
        Logger.d(getClass(), method + " " + ( payload != null ? payload.toString() : "" ));
        HttpUriRequest request;
        T returnVal = null;

        try {
            String encryptedSymKey = null;
            byte [] simKey = null;
            if ( encrypt ){
                simKey = generateKey();
                encryptedSymKey = new String ( Hex.encodeHex(Utils.assymetricEncryptWithRSA(simKey, publicKey)));
            }
            Logger.d(getClass(), method);
            Iterator<String> i = params.keySet().iterator();
            URIBuilder builder = new URIBuilder().setScheme(host.getSchemeName()).setHost(host.getHostName()).setPort(host.getPort()).setPath("/" + baseUrl + "/" + method);
            while ( i.hasNext()){
                String key = i.next();
                builder = builder.addParameter(key, params.get(key));
            }
            if ( encrypt )
                builder = builder.addParameter(Constants.ENC_SYM_KEY_REQ_ARG_NAME, encryptedSymKey);
            URI uri = builder.build();

            if ( GET )
                request = new HttpGet(uri);
            else
                request = new HttpPost(uri);
            if ( payload != null ){

                ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
                Kryo kryo = new Kryo();
                Output output = new Output(baos);
                kryo.writeObject(output, payload);
                output.flush();
                baos.close();
                byte [] byteArray = baos.toByteArray();
                ByteArrayOutputStream zippedOutputStream = new ByteArrayOutputStream(512);
                GZIPOutputStream outputStream = new GZIPOutputStream(zippedOutputStream);
                outputStream.write(byteArray);
                outputStream.close();
                zippedOutputStream.close();
                byte [] zippedByteArray = zippedOutputStream.toByteArray();
                byte [] toSend = zippedByteArray;
                if ( encrypt ){
                    toSend = Utils.encryptWithAESKey(zippedByteArray, simKey);
                }
                ByteArrayEntity entity = new ByteArrayEntity(toSend);
                ((HttpPost)request).setEntity(entity);

            }
            request.getRequestLine();
        }
        catch ( Throwable e ){ // Request construction error.
            CongeorException congeorException = new BuildRequestException(e);
            throw congeorException;
        }
        try {
            returnVal = sendRestRequest(request, returnClassType, host);
        }
        catch ( Throwable e ){
            CongeorException congeorException = new RestSendException(e);
            throw congeorException;
        }
        if ( method.equalsIgnoreCase(Constants.REQ_STORE_DATA) || method.equalsIgnoreCase(Constants.REQ_INSERT_ZIPPED_SER_SENSORS_DATA_ENC)){
            try {
                Map<String, Object> object = ( Map<String,Object> ) payload;
                setLastTimeStamp(Math.max(Long.parseLong(object.get(Constants.LAST_SYNC_TIME).toString()), lastTimeStamp));
            }
            catch ( Throwable e ){
                CongeorException congeorException = new UpdateTimestampException(e);
                throw congeorException;
            }
        }
        return returnVal;
        //throw new UnsupportedOperationException ( "Operation not implemented" );
    }

    @Override
    public JsonElement processJsonElement(String serviceMethod, Object payload, Map<String, String> params, boolean encrypt, boolean GET) throws CongeorException {
        boolean reachable = Utils.isOnline(appContext.get(), true);
        if ( !reachable ){
            NoInternetException congeorException = new NoInternetException(new Exception ("no internet exception"));
            throw congeorException;
        }
        String service = Utils.getService(serviceMethod);
        String method = Utils.getMethod(serviceMethod);
        String hostName = serviceDetails.getServerIP(service);
        String baseUrl = serviceDetails.getBaseURL(service);
        int port = serviceDetails.getServerPort(service);
        HttpHost host = new HttpHost(hostName, port);
        Logger.d(getClass(), method + " " + ( payload != null ? payload.toString() : "" ));
        HttpUriRequest request;
        JsonElement returnVal = null;

        try {
            String encryptedSymKey = null;
            byte [] simKey = null;
            if ( encrypt ){
                simKey = generateKey();
                encryptedSymKey = new String ( Hex.encodeHex(Utils.assymetricEncryptWithRSA(simKey, publicKey)));
            }
            Logger.d(getClass(), method);
            Iterator<String> i = params.keySet().iterator();
            URIBuilder builder = new URIBuilder().setScheme(host.getSchemeName()).setHost(host.getHostName()).setPort(host.getPort()).setPath("/" + baseUrl + "/" + method);
            while ( i.hasNext()){
                String key = i.next();
                builder = builder.addParameter(key, params.get(key));
            }
            if ( encrypt )
                builder = builder.addParameter(Constants.ENC_SYM_KEY_REQ_ARG_NAME, encryptedSymKey);
            URI uri = builder.build();
            if ( GET )
                request = new HttpGet(uri);
            else
                request = new HttpPost(uri);
            if ( payload != null ){
                ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
                Kryo kryo = new Kryo();
                Output output = new Output(baos);
                kryo.writeObject(output, payload);
                output.flush();
                baos.close();
                byte [] byteArray = baos.toByteArray();
                ByteArrayOutputStream zippedOutputStream = new ByteArrayOutputStream(512);
                GZIPOutputStream outputStream = new GZIPOutputStream(zippedOutputStream);
                outputStream.write(byteArray);
                outputStream.close();
                zippedOutputStream.close();
                byte [] zippedByteArray = zippedOutputStream.toByteArray();
                byte [] toSend = zippedByteArray;
                if ( encrypt ){
                    toSend = Utils.encryptWithAESKey(zippedByteArray, simKey);
                }
                ByteArrayEntity entity = new ByteArrayEntity(toSend);
                ((HttpPost)request).setEntity(entity);
            }
            request.getRequestLine();
        }
        catch ( Throwable e ){ // Request construction error.
            CongeorException congeorException = new BuildRequestException(e);
            throw congeorException;
        }
        try {
            returnVal = sendRestRequestJsonElement(request, host);
        }
        catch ( Throwable e ){
            CongeorException congeorException = new RestSendException(e);
            throw congeorException;
        }
        if ( method.equalsIgnoreCase(Constants.REQ_STORE_DATA) || method.equalsIgnoreCase(Constants.REQ_INSERT_ZIPPED_SER_SENSORS_DATA_ENC)){
            try {
                Map<String, Object> object = ( Map<String,Object> ) payload;
                setLastTimeStamp(Math.max(Long.parseLong(object.get(Constants.LAST_SYNC_TIME).toString()), lastTimeStamp));
            }
            catch ( Throwable e ){
                CongeorException congeorException = new UpdateTimestampException(e);
                throw congeorException;
            }
        }
        return returnVal;
        //throw new UnsupportedOperationException ( "Operation not implemented" );
    }

    private byte[] generateKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        SecretKey key = generator.generateKey();
        byte[] symmetricKey =key.getEncoded();
        return symmetricKey;
    }

    @Override
    public Long getLastTimeStamp() throws CongeorException {
        Context appContext = this.appContext.get();
        File timestampFile = appContext.getFileStreamPath(timestampFilename);
        String timeStampStr = "0";
        if ( timestampFile.exists()){
            try {
                InputStream in = appContext.openFileInput(timestampFilename);
                timeStampStr = com.bgu.utils.Utils.readStreamAsString(in);
                in.close();
            } catch (Exception e) {
                throw new GetTimestampException( e );
            }
        }
        lastTimeStamp = Long.parseLong(timeStampStr);
        return lastTimeStamp;
    }

    @Override
    public void setLastTimeStamp ( long timeStamp ) throws CongeorException{
        try {
            Context appContext = this.appContext.get();
            OutputStream out = appContext.openFileOutput(timestampFilename, Context.MODE_PRIVATE);
            PrintWriter printWriter = new PrintWriter(out);
            printWriter.print(timeStamp);
            printWriter.close();
            out.close();
        }
        catch ( Exception ex ){
            throw new UpdateTimestampException(ex);
        }
    }

    private JsonElement sendRestRequestJsonElement(HttpUriRequest request, HttpHost host ) throws Throwable {
        String req = request.getURI().toString();
        Logger.i(getClass(), "Sending rest request: " + req);
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpConnectionParams.setConnectionTimeout(params, Constants.CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, Constants.SOCKET_TIMEOUT);
        HttpClient client = new DefaultHttpClient(params);
        Logger.i(RestClientListener.class, "Data send time: " + new Date(System.currentTimeMillis()));
        HttpResponse response = client.execute(request);
        JsonElement returnVal = null;
        if (!(response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204))
            throw new Exception( response.getStatusLine().toString());
        String value = com.bgu.utils.Utils.readStreamAsString(response.getEntity().getContent());
        returnVal = new JsonParser().parse(value);
        return returnVal;
    }

    private <T> T sendRestRequest(HttpUriRequest request, Class<T> returnClassType, HttpHost host ) throws Throwable {
        String req = request.getURI().toString();
        Logger.i(getClass(), "Sending rest request: " + req);
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpConnectionParams.setConnectionTimeout(params, Constants.CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, Constants.SOCKET_TIMEOUT);
        HttpClient client = new DefaultHttpClient(params);
        Logger.i(RestClientListener.class, "Data send time: " + new Date(System.currentTimeMillis()));
        HttpResponse response = client.execute(request);
        T returnVal = null;
        if (!(response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204))
            throw new Exception( response.getStatusLine().toString());
        if ( returnClassType != Void.class){
            if ( returnClassType != String.class ){
                GZIPInputStream gZippedStream = new GZIPInputStream( response.getEntity().getContent());
                Kryo kryo = new Kryo();
                returnVal = kryo.readObject(new Input(gZippedStream), returnClassType);
            }
            else
                returnVal = (T) com.bgu.utils.Utils.readStreamAsString(response.getEntity().getContent());
        }
        return returnVal;
    }
}
