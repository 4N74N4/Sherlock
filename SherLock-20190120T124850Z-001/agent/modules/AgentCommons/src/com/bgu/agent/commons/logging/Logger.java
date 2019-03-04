package com.bgu.agent.commons.logging;

import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/2/13
 * Time: 7:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class Logger {
    public static String TAG = "Default";

    public static void d ( Class tClass, String message ){
        d(tClass, message, null);
    }

    public static void d ( Class tClass, String message, Throwable t ){
        if ( message == null )
            message = "Empty message";
        Log.d(TAG + "/" + tClass.getCanonicalName(), message, t);
    }

    public static void i ( Class tClass, String message ){
        if ( message == null )
            message = "Empty message";
        i(tClass, message, null);
    }

    public static void i ( Class tClass, String message, Throwable t ){
        if ( message == null )
            message = "Empty message";
        Log.i(TAG + "/" + tClass.getCanonicalName(), message, t);
    }

    public static void e ( Class tClass, String message ){
        if ( message == null )
            message = "Empty message";
        e(tClass, message, null);
    }

    public static void e ( Class tClass, String message, Throwable t ){
        if ( message == null )
            message = "Empty message";
        Log.e(TAG + "/" + tClass.getCanonicalName(), message, t);        
    }

    public static void w ( Class tClass, String message ){
        if ( message == null )
            message = "Empty message";
        w(tClass, message, null);
    }

    public static void w ( Class tClass, String message, Throwable t ){
        if ( message == null )
            message = "Empty message";
        Log.w(TAG + "/" + tClass.getCanonicalName(), message, t);
    }

    public static void v ( Class tClass, String message ){
        if ( message == null )
            message = "Empty message";
        v(tClass, message, null);
    }

    public static void v ( Class tClass, String message, Throwable t ){
        if ( message == null )
            message = "Empty message";
        Log.v(TAG + "/" + tClass.getCanonicalName(), message, t);
    }
}
