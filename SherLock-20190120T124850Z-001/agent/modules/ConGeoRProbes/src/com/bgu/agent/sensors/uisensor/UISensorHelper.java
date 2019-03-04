//package com.bgu.agent.sensors.uisensor;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.Bundle;
//import com.bgu.congeor.Constants;
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//
//import java.lang.ref.WeakReference;
//import java.util.LinkedHashMap;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//
///**
// * Created by clint on 12/22/13.
// */
//public class UISensorHelper {
//
//
//
//    public UISensorHelper (Context context){
//        messages = new LinkedBlockingQueue<JsonObject>();_
//        this.context = new WeakReference<Context>(context);
//        IntentFilter filter = new IntentFilter(Constants.UI_MESSAGE);
//        filter.addCategory(Intent.CATEGORY_DEFAULT);
//        receiver = new UIBroadcastReceiver();
//        context.registerReceiver(receiver, filter);
//    }
//
//    @Override
//    protected void finalize() throws Throwable {
//        context.get().unregisterReceiver(receiver);
//        context = null;
//        receiver = null;
//    }
//
//    public JsonObject remove (){
//        return messages.remove();
//    }
//
//    public boolean isEmpty (){
//        return messages.isEmpty();
//    }
//
//
//}
