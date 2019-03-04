package com.bgu.utils;

import com.esotericsoftware.minlog.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by clint on 1/20/14.
 */
public class Utils {
    public static String readStreamAsString ( InputStream inputStream ) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer);
        return writer.toString();
    }

    public static String readStreamAsString ( InputStream inputStream , Charset cs) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, cs);
        return writer.toString();
    }

    public static String jsonToString ( Object o ){
        GsonBuilder gsonBuilder = new GsonBuilder ();
        Gson gson = gsonBuilder.disableHtmlEscaping().create();
        return gson.toJson(o);
    }

    public static String getClassName ( Class className ){
        return className.getCanonicalName().substring(className.getCanonicalName().indexOf(".") + 1);
    }

    public static boolean classExists ( String className ){
        try {
            Class.forName( className );
        } catch( ClassNotFoundException e ) {
            return false;
        }
        return true;
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }


    public static boolean checkFutureDate(String time)
    {
//        String currentTime = new SimpleDateFormat("HH:mm").format(new Date());
//        String[] current = currentTime.split(":");
//        String[] future = time.split(":");
//        if(Integer.parseInt(current[0])<Integer.parseInt(future[0])){return true;}
//        else {
//            if (Integer.parseInt(current[0]) == Integer.parseInt(future[0]))
//                if (Integer.parseInt(current[1]) < Integer.parseInt(future[1]))
//                    return true;
//        }
//        return false;
        String[] future = time.split(":");
        Calendar now = Calendar.getInstance();
        Calendar alarm = Calendar.getInstance();
        alarm.set(Calendar.HOUR_OF_DAY, Integer.parseInt(future[0]));
        alarm.set(Calendar.MINUTE, Integer.parseInt(future[1]));
        if (alarm.before(now))
            return false;
        return true;
    }

    public static boolean checkSendingTimeSlot()
    {
        Calendar current = Calendar.getInstance();

        boolean ans = current.HOUR_OF_DAY<18 && current.HOUR_OF_DAY>6;
        return !ans;
    }
}
