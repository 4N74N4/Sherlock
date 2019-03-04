package com.bgu.agent.data.listeners;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import com.bgu.agent.commons.logging.Logger;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 10/29/13
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class MediaScannerClient implements MediaScannerConnection.MediaScannerConnectionClient {

    private MediaScannerConnection mMs;
    private File mFile;
    private Context context;
    private static MediaScannerClient instance;
    boolean isPendingScan = false;

    public static MediaScannerClient getInstance (){
        if ( instance == null )
            instance = new MediaScannerClient();
        return instance;
    }

    public void init ( Context context ){
        this.context = context;
    }

    public void scan (File f) {
        mFile = f;
        if ( !isPendingScan ){
            mMs = new MediaScannerConnection(context, this);
            if ( !mMs.isConnected())
                mMs.connect();
        }
    }

    @Override
    public void onMediaScannerConnected() {
        isPendingScan = true;
        if (mMs.isConnected()){
            Logger.i(MediaScannerClient.class, "Scan connected " + mFile.getAbsolutePath());
            mMs.scanFile(mFile.getAbsolutePath(), null);
            Logger.i(MediaScannerClient.class, "Running scan of " + mFile.getAbsolutePath());
        }
        else
            Logger.e(MediaScannerClient.class, "Media scanner not connected failure !");
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        Logger.i(MediaScannerClient.class, "Scan completed of " + path);
        mMs.disconnect();
        mFile.getParentFile().listFiles();
        mFile = null;
        isPendingScan = false;
    }
}