package com.bgu.agent.sensors;

import android.app.AlarmManager;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by simondzn on 23/11/2015.
 */

@Probe.DisplayName("MoriartyProbe")
@Schedule.DefaultSchedule(interval = Constants.SECOND*60*30)
public class MoriartyProbe extends SecureBase {
    @Override
    protected void secureOnStart() {
        super.secureOnStart();
        Log.e("tagg", "Moriarty start");
        File moriartyFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "Moriarty");
        File moriartyLog = new File(moriartyFolder, "MoriartyClues.json");
        File lastUp = new File(moriartyFolder, "lastUp.txt");
//        checking if moriarty has been used
        if (/*((System.currentTimeMillis() - moriartyLog.lastModified()) > 216000000 && moriartyLog.exists()) ||*/ (lastUp.exists() && (System.currentTimeMillis()-lastUp.lastModified()> AlarmManager.INTERVAL_DAY*2.5))) {
//        if((System.currentTimeMillis() - moriartyFolder.lastModified())>300 && moriartyFolder.exists()){
            Log.e(MoriartyProbe.class.toString(), " found interval error sending broadcast");
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction("com.bgu.congeor.sherlockapp.Moriarty");
            getContext().sendBroadcast(intent);
        }
        try {
            if (moriartyLog.exists()) {
                String content = FileUtils.readFileToString(moriartyLog);
//                Log.e("tagg", "moriarty: " +content);
                JsonObject data = new JsonObject();
                data.addProperty("MoriartyClues", "[" + content + "]");
                sendData(data);
                moriartyLog.delete();
                Log.e("tagg", "Moriarty finished");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            stop();
        }

    }
}
