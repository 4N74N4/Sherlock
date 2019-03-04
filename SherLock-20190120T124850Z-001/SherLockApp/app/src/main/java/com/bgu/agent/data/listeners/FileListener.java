package com.bgu.agent.data.listeners;

import android.content.Context;
import android.os.Environment;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.agent.data.exceptions.CongeorException;
import com.bgu.agent.data.exceptions.GetTimestampException;
import com.bgu.agent.data.exceptions.SendDataException;
import com.bgu.agent.data.exceptions.UpdateTimestampException;
import com.bgu.congeor.Constants;
import com.google.gson.JsonElement;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: clint
* Date: 12/1/13
* Time: 1:31 PM
* To change this template use File | Settings | File Templates.
*/
public class FileListener extends AbstractCommunicationListener {

    String dataFileName;
    String timestampFileName;
    WeakReference<Context> context;
    long lastTimeStamp = 0;

    public FileListener ( Context context, String timestampFileName, String dataFileName ){
        this.context = new WeakReference<Context>(context);
        this.timestampFileName = timestampFileName;
        this.dataFileName = dataFileName;
        MediaScannerClient.getInstance().init(context);
    }

    @Override
    public String getDataListenerName() {
        return "FileListener";
    }

//    @Override
//    public JsonObject processObject(String reqType, Object data, Map<String, String> params) throws CongeorException {
//        JsonObject returnVal = new JsonObject();
//        if ( reqType.equalsIgnoreCase(Constants.REQ_STORE_DATA)){
//            FileOutputStream p = null;
//            try {
//                p = new FileOutputStream(dataFile + "_binary");
//                /*
//                com.esotericsoftware.kryo.Kryo kyro = new com.esotericsoftware.kryo.Kryo ();
//                ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
//                com.esotericsoftware.kryo.io.Output output = new Output(baos);
//                kyro.writeObject(output, data);
//                output.close();
//                baos.close();
//                p.write(baos.toByteArray());
//                */
//                ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
//                Kryo kryo = new Kryo();
//                Output output = new Output(baos);
//                kryo.writeObject(output, data);
//                output.flush();
//                byte [] byteArray = baos.toByteArray();
//                p.write(baos.toByteArray());
//                baos.close();
//                p.close();
//                p = new FileOutputStream(dataFile + "_binary_compressed");
//                ByteArrayOutputStream zippedByteData = new ByteArrayOutputStream(512);
//                GZIPOutputStream outputStream = new GZIPOutputStream(zippedByteData);
//                outputStream.write(byteArray);
//                outputStream.close();
//                zippedByteData.close();
//                p.write(zippedByteData.toByteArray());
//                p.close();
//                kryo = new Kryo();
//                Map<String, Object> input;
//                input = kryo.readObject(new Input(byteArray), LinkedHashMap.class);
//                //processObject1(reqType, data);
//                Map<String, Object> object = ( Map<String,Object> ) data;
//                setLastTimeStamp(Long.parseLong(object.get(Constants.LAST_SYNC_TIME).toString()));
//                context.get().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(dataFile.getParentFile())));
//                //MediaScannerClient.getInstance().scan(dataFile);
//                returnVal.addProperty("Status", 0);
//            } catch (FileNotFoundException e) {
//                returnVal.addProperty("Status", 999);
//                Logger.e(getClass(), e.getMessage());
//            } catch (IOException e) {
//                returnVal.addProperty("Status", 999);
//                Logger.e(getClass(), e.getMessage());
//            }
//        }
//        return returnVal;
//    }

    @Override
    public <T> T processObject(String method, Object payload, Map<String, String> params, Class<T> returnClassType, boolean encrypt, boolean GET) throws CongeorException{
        T returnVal = null;
        if ( method.equalsIgnoreCase("data_" + Constants.REQ_STORE_DATA) || method.equalsIgnoreCase("data_" + Constants.REQ_INSERT_ZIPPED_SER_SENSORS_DATA_ENC)){
            Logger.e(getClass(), "file listener started");
            File albumStorageDir = getDataStorageDir();
//            File dataFile = new File(albumStorageDir, dataFileName);
            File dataFile = checkSizeForZip(albumStorageDir,dataFileName);
            PrintWriter p = null;
            try {
                Logger.e(getClass(),"starting to write");
                p = new PrintWriter(new FileOutputStream(dataFile, true), true);
                p.println(com.bgu.utils.Utils.jsonToString(payload));
               
                //context.get().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(dataFile.getParentFile())));
                //MediaScannerClient.getInstance().scan(dataFile);
            } catch (FileNotFoundException e) {
                Logger.e(getClass(),"writing failed");
                Logger.e(getClass(), e.getMessage());
                CongeorException exception = new SendDataException(e);
                throw exception;
            }
            finally{
                Logger.e(getClass(),"write succ");
                p.close();
            }
        }
        return returnVal;
    }

    private File checkSizeForZip(File folder, String dataFileName) {
        File dataFile = new File(folder,dataFileName+"_text");
        Logger.e(getClass(),"");
        if(dataFile.exists()){
            Logger.e(getClass(),"data file exists");
            //set file Size
            Logger.e(getClass(),"file size  "+dataFile.length());
            if((dataFile.length()/1048576)>500){
                Logger.e(getClass(),"File bigger than 500 mb");
                String zipName = System.currentTimeMillis()+".zip";
                File sdCard = Environment.getExternalStorageDirectory();
                String path = sdCard.getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data/";
               //Logger.e(getClass(),"details:  filePath: "+path+dataFile.getName()+ " file name: "+ dataFile.getName() + " zip file name will be: "+ zipName);
                boolean zipSuc = Utils.fileZipping(path+dataFile.getName(),dataFile.getName(),path+zipName);
                Logger.e(getClass(),"zip succssesful?  "+zipSuc);
                if(zipSuc) {
                    boolean t = dataFile.delete();
                    Logger.e(getClass(),"deletion succssesful?  "+t);
                    File current = new File(folder,dataFileName+"_text");
                    return current;
                }
            }
            return dataFile;
        }
        return dataFile;
    }

    @Override
    public JsonElement processJsonElement(String method, Object payload, Map<String, String> params, boolean encrypt, boolean GET) throws CongeorException {
        return null;
    }

    @Override
    public Long getLastTimeStamp() throws CongeorException {
        File albumStorageDir = getDataStorageDir();
        File timestampFile = new File(albumStorageDir, timestampFileName);
        String timestampStr = "0";
        if ( timestampFile.exists()){
            try {
                BufferedReader r = new BufferedReader( new FileReader(timestampFile));
                timestampStr = r.readLine();
                r.close();
            }
            catch ( Exception ex ){
                throw new GetTimestampException( ex );
            }
            finally{

            }
        }
        if (timestampStr != null) {
            lastTimeStamp = Long.parseLong(timestampStr);
        }else lastTimeStamp = -5;
        return lastTimeStamp;
    }

    public void setLastTimeStamp(long timestamp) throws CongeorException {
        try {
            File albumStorageDir = getDataStorageDir();
            File timestampFile = new File(albumStorageDir, timestampFileName);
            if ( timestampFile.exists())
                timestampFile.delete();
            PrintWriter p = new PrintWriter(timestampFile);
            p.println(timestamp);
            p.close();
        } catch (Exception e) {
            Logger.e(getClass(), e.getMessage());
            CongeorException exception = new UpdateTimestampException(e);
            throw exception;
        }
    }

    public static File getDataStorageDir() {
        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File(sdCard.getAbsolutePath() + "/" + Constants.APPLICATION_NAME+ "/Data");
        if (!file.exists() || !file.isDirectory()) {
            Logger.i(FileListener.class, "Directory created");
            file.mkdirs();
        }
        return file;
    }
}
