package com.bgu.agent.sensors;

import android.app.ActivityManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by Alex on 29/05/2014.
 */
@Probe.DisplayName("ProcessStatisticsData")
//@Schedule.DefaultSchedule(interval = 30)
public class ProcessStatisticsData extends SecureBase{
    private JsonArray jarr;
    private Gson gson;
    private JsonObject data;
    private ActivityManager am;
    private ArrayList<Thread> arrThreads;

    @Override
    protected void secureOnStart() {
        super.secureOnStart();

        Date startDate=new Date();

        this.jarr=new JsonArray();
        this.gson = getGson();
        this.data = new JsonObject();
        this.am = (ActivityManager)getContext().getApplicationContext().getSystemService(getContext().ACTIVITY_SERVICE);
        this.arrThreads=new ArrayList<>(100);

        Thread tRunningAppProc=new Thread(new Runnable() {
            @Override
            public void run() {
                getRunningAppProcessesInfo();
            }
        });

        //retrieve device cpu data from /proc/cpuinfo file
        ((ActivityManager) this.getContext().getSystemService(this.getContext().ACTIVITY_SERVICE)).getProcessesInErrorState();
        Thread tCpuLoad=new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle=getCpuLoad();
                synchronized (data){
                    data.add("CPU_LOAD", gson.toJsonTree(bundle));
                }
            }
        });

        //retrieve device memory data from /proc/meminfo file
        Thread tMemInfo=new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle=getMemInfo();
                synchronized (data){
                    data.add("MEM_INFO", gson.toJsonTree(bundle));
                }
            }
        });

        //retrieve device total network data via android api, TrafficStats object
        Thread tNetDev=new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle=getNetDev();
                synchronized (data){
                    data.add("NET_DEV", gson.toJsonTree(bundle));
                }
            }
        });

        //start parallel data retrieval
        //tCpuLoad.start();
        //tMemInfo.start();
        //tNetDev.start();
        tRunningAppProc.start();

        //wait for the completion of data retrieval
        try {
            //tCpuLoad.join();
           // tMemInfo.join();
           // tNetDev.join();
            tRunningAppProc.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        data.add("RUNNING_PROCESS_INFO", this.jarr);
        data.add("ERRORED_PROCESS_INFO", gson.toJsonTree(am.getProcessesInErrorState()));

        Date endDate=new Date();
        Log.v(this.getClass().toString(),(endDate.getTime()-startDate.getTime())+"\n\n");

        //send the collected data to funf
        sendData(data);
        stop();
        System.gc();
    }

    /**
     * gets devices memory data from /proc/meminfo file
     * @return bundle object containing the retrieved data
     */
    public Bundle getMemInfo(){
        Bundle result = new Bundle();
        StringTokenizer linest;
        String key, value;

        try
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( "/proc/meminfo" ) ), 2048 );

            char[] buffer = new char[2024];
            reader.read(buffer, 0, 2000);

            StringTokenizer st = new StringTokenizer(new String(buffer), "\n", false);

            for (int i = 0; i < st.countTokens(); i++)
            {

                linest = new StringTokenizer(st.nextToken());
                key = linest.nextToken();
                value = linest.nextToken();
                result.putLong(key, Long.valueOf(value));
            }

            reader.close();
        }
        catch (Exception e)
        {
            Log.e("", "Exception parsing the file", e);
        }

        return result;
    }

    /**
     * gets devices cpu data from /proc/cpuinfo file
     * @return bundle object containing the retrieved data
     */
    public Bundle getCpuLoad(){
        Bundle result = new Bundle();

        float totalUsage, userUsage, niceUsage, systemUsage;
        Double cpuFreq = 0.0;
        long sTotal = 0;

        String line;
        String[] toks;
        String[] words;

        try{
            BufferedReader reader = new BufferedReader( new
                    InputStreamReader( new
                    FileInputStream( "/proc/cpuinfo" ) ), 2048 );

            while ( (line = reader.readLine()) != null ){
                toks = line.split(" ");
                words = toks[0].split("\t");

                if (words[0].equals("BogoMIPS")){
                    cpuFreq = Double.parseDouble(toks[1]);
                }
            }

            reader.close();
        }
        catch (IOException ioe){
            //Log.e(LogUtil.TAG, "Exception parsing /proc/cpuinfo", ioe);
        }

        try{
            BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( "/proc/stat" ) ), 2048 );

            while ( (line = reader.readLine()) != null ){
                toks = line.split(" ");

                if (toks[0].equals("cpu")){

                    Bundle cpuObject = new Bundle();

                    cpuObject.putFloat("user",Long.parseLong(toks[2]));
                    cpuObject.putFloat("nice",Long.parseLong(toks[3]));
                    cpuObject.putFloat("system",Long.parseLong(toks[4]));
                    cpuObject.putFloat("idle",Long.parseLong(toks[5]));
                    cpuObject.putFloat("iowait",Long.parseLong(toks[6]));
                    cpuObject.putFloat("irq",Long.parseLong(toks[7]));
                    cpuObject.putFloat("softirq",Long.parseLong(toks[8]));
                    cpuObject.putFloat("steal",Long.parseLong(toks[9]));
                    cpuObject.putFloat("guest",Long.parseLong(toks[10]));
                    cpuObject.putFloat("guest_nice", Long.parseLong(toks[11]));

                    result.putBundle("cpu", cpuObject);
                }
                else if (toks[0].equals("ctxt")){
                    String ctxt = toks[1];
                    result.putLong("ContextSwitch", Long.valueOf(ctxt));
                }
                else if (toks[0].equals("btime")){
                    String btime = toks[1];
                    result.putLong("BootTime", Long.valueOf(btime));
                }
                else if (toks[0].equals("processes")){
                    String procs = toks[1];
                    result.putLong("Processes", Long.valueOf(procs));
                }
            }
            result.putLong("CpuTotalTime", sTotal);
            reader.close();
        }
        catch( IOException ex ){
        }

        return result;
    }

    /**
     * gets devices network usage data via android api, TrafficStats object
     * @return bundle object containing the retrieved data
     */
    public Bundle getNetDev(){

        Bundle result = new Bundle();

        result.putLong("getMobileRxBytes",TrafficStats.getMobileRxBytes());
        result.putLong("getMobileRxPackets", TrafficStats.getMobileRxPackets());
        result.putLong("getMobileTxBytes", TrafficStats.getMobileTxBytes());
        result.putLong("getMobileTxPackets", TrafficStats.getMobileTxPackets());
        result.putLong("getTotalRxBytes", TrafficStats.getTotalRxBytes());
        result.putLong("getTotalTxBytes", TrafficStats.getTotalTxBytes());
        result.putLong("getTotalRxPackets", TrafficStats.getTotalRxPackets());
        result.putLong("getTotalTxPackets", TrafficStats.getTotalTxPackets());

        return result;
    }

    /**
     *
     */
    private void getRunningAppProcessesInfo() {
        List<ActivityManager.RunningAppProcessInfo> processes=am.getRunningAppProcesses();
        for(ActivityManager.RunningAppProcessInfo pInfo:processes){

            //adds process data to the process json array
            Thread t=new Thread(new OneShotTask(pInfo));

            this.arrThreads.add(t);
            t.start();
        }

        Thread.yield();

        for(Thread t:this.arrThreads){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.arrThreads.clear();
    }

    /**
     * gets process data (cpu,memory traffic) via /proc/[pid]/stat file and android api (memory & traffic)
     * @param pInfo process info for which to retrieve data
     * @return bundle object containing the retrieved data
     */
    private JsonObject getProcessDataById(ActivityManager.RunningAppProcessInfo pInfo){
        ActivityManager manager = (ActivityManager) this.getContext().getSystemService(this.getContext().ACTIVITY_SERVICE);

        //create a json object for this specific process
        JsonObject pData = new JsonObject();

        //fill process data to json object
        pData.addProperty("processName", pInfo.processName);
        pData.addProperty("pid", pInfo.pid);
        pData.addProperty("uid", pInfo.uid);
        pData.addProperty("importance",pInfo.importance);
        pData.addProperty("importanceReasonCode",pInfo.importanceReasonCode);
        pData.addProperty("importanceReasonPid",pInfo.importanceReasonPid);
        pData.addProperty("lru",pInfo.lru);
        pData.addProperty("pkgList", pInfo.pkgList[0]);

        //fill process network data to json object
        pData.addProperty("getUidRxBytes", TrafficStats.getUidRxBytes(pInfo.uid));
        pData.addProperty("getUidRxPackets", TrafficStats.getUidRxPackets(pInfo.uid));
        pData.addProperty("getUidTcpRxBytes", TrafficStats.getUidTcpRxBytes(pInfo.uid));
        pData.addProperty("getUidTcpRxSegments", TrafficStats.getUidTcpRxSegments(pInfo.uid));
        pData.addProperty("getUidTcpTxBytes", TrafficStats.getUidTcpTxBytes(pInfo.uid));
        pData.addProperty("getUidTcpTxSegments", TrafficStats.getUidTcpTxSegments(pInfo.uid));
        pData.addProperty("getUidTxBytes", TrafficStats.getUidTxBytes(pInfo.uid));
        pData.addProperty("getUidTxPackets", TrafficStats.getUidTxPackets(pInfo.uid));
        pData.addProperty("getUidUdpRxBytes", TrafficStats.getUidUdpRxBytes(pInfo.uid));
        pData.addProperty("getUidUdpRxPackets", TrafficStats.getUidUdpRxPackets(pInfo.uid));
        pData.addProperty("getUidUdpTxBytes", TrafficStats.getUidUdpTxBytes(pInfo.uid));
        pData.addProperty("getUidUdpTxPackets", TrafficStats.getUidUdpTxPackets(pInfo.uid));

        //fill process memory data to json object
        Debug.MemoryInfo memInfo=manager.getProcessMemoryInfo(new int[]{pInfo.pid})[0];
        pData.addProperty("dalvikPrivateDirty", memInfo.dalvikPrivateDirty);
        pData.addProperty("dalvikPss", memInfo.dalvikPss);
        pData.addProperty("dalvikSharedDirty", memInfo.dalvikSharedDirty);
        pData.addProperty("nativePrivateDirty", memInfo.nativePrivateDirty);
        pData.addProperty("nativePss", memInfo.nativePss);
        pData.addProperty("nativeSharedDirty", memInfo.nativeSharedDirty);
        pData.addProperty("otherPrivateDirty", memInfo.otherPrivateDirty);
        pData.addProperty("otherPss", memInfo.otherPss);
        pData.addProperty("otherSharedDirty", memInfo.otherSharedDirty);


        //fill process cpu data from proc/[pid]/stat file to json object
        String line;
        String[] toks=new String[41];
        String[] times = new String[3];
        double Hertz = 100;

        try {
            //read the stat file
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/" + pInfo.pid + "/stat")), 4096);
            while ((line = reader.readLine()) != null) {
                toks = line.split(" ");
            }
            reader.close();

            reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/uptime")), 4096);
            while ((line = reader.readLine()) != null) {
                times = line.split(" ");
            }
            reader.close();
            //placing the retrieved data from file to local variables
            long pid = Long.parseLong(toks[0]);
//            String tcomm = toks[1];
//            pData.addProperty("tcomm", tcomm);
            String state = toks[2];
            pData.addProperty("state", state);
//            long ppid = Long.parseLong(toks[3]);
//            pData.addProperty("ppid", ppid);
//            long pgid = Long.parseLong(toks[4]);
//            pData.addProperty("pgid", pgid);
//            long sid = Long.parseLong(toks[5]);
//            pData.addProperty("sid", sid);
//            long tty_nr = Long.parseLong(toks[6]);
//            pData.addProperty("tty_nr", tty_nr);
//            long tty_pgrp = Long.parseLong(toks[7]);
//            pData.addProperty("tty_pgrp", tty_pgrp);
//            long flags = Long.parseLong(toks[8]);
//            pData.addProperty("flags", flags);
//            long min_flt = Long.parseLong(toks[9]);
//            pData.addProperty("min_flt", min_flt);
//            long cmin_flt = Long.parseLong(toks[10]);
//            pData.addProperty("cmin_flt", cmin_flt);
//            long maj_flt = Long.parseLong(toks[11]);
//            pData.addProperty("maj_flt", maj_flt);
//            long cmaj_flt = Long.parseLong(toks[12]);
//            pData.addProperty("cmaj_flt", cmaj_flt);
            double utime = Double.parseDouble(toks[13]);
            pData.addProperty("utime", utime);
            double stime = Double.parseDouble(toks[14]);
            pData.addProperty("stime", stime);
            double total_time = utime + stime;
            double cutime = Double.parseDouble(toks[15]);
            pData.addProperty("cutime", cutime);
            double cstime = Double.parseDouble(toks[16]);
            pData.addProperty("cstime", cstime);
            total_time = total_time + cutime + cstime;
            double cpu_usage;
//            long priority = Long.parseLong(toks[17]);
//            pData.addProperty("priority", priority);
//            long nice = Long.parseLong(toks[18]);
//            pData.addProperty("nice", nice);
            long num_threads = Long.parseLong(toks[19]);

//            double it_real_value = Double.parseDouble(toks[20]);
//            pData.addProperty("it_real_value", it_real_value);
            String starttime = toks[21];
            pData.addProperty("start_time", starttime);
            double seconds = Double.parseDouble(times[0]) - (Double.parseDouble(starttime) / Hertz);
            cpu_usage = 100 * ((total_time / Hertz) / seconds);
            pData.addProperty("CPU_USAGE" , cpu_usage);
            pData.addProperty("num_threads", num_threads);
            long vsize = Long.parseLong(toks[22]);
            pData.addProperty("vsize", vsize);
            long rss = Long.parseLong(toks[23]);
            pData.addProperty("rss", rss);
//            long rsslim = Long.parseLong(toks[24]);
//            pData.addProperty("rsslim", rsslim);
//            long start_code = Long.parseLong(toks[25]);
//            pData.addProperty("start_code", start_code);
//            long end_code = Long.parseLong(toks[26]);
//            pData.addProperty("end_code", end_code);
//            long start_stack = Long.parseLong(toks[27]);
//            pData.addProperty("start_stack", start_stack);
//            long esp = Long.parseLong(toks[28]);
//            pData.addProperty("esp", esp);
//            long eip = Long.parseLong(toks[29]);
//            pData.addProperty("eip", eip);
//            long pending = Long.parseLong(toks[30]);
//            pData.addProperty("pending", pending);
//            long blocked = Long.parseLong(toks[31]);
//            pData.addProperty("blocked", blocked);
//            long sigign = Long.parseLong(toks[32]);
//            pData.addProperty("sigign", sigign);
//            long sigcatch = Long.parseLong(toks[33]);
//            pData.addProperty("sigcatch", sigcatch);
//            long wchan = Long.parseLong(toks[34]);
//            pData.addProperty("wchan", wchan);
//            long zero1 = Long.parseLong(toks[35]);
//            pData.addProperty("zero1", zero1);
//            long zero2 = Long.parseLong(toks[36]);
//            pData.addProperty("zero2", zero2);
//            long exit_signal = Long.parseLong(toks[37]);
//            pData.addProperty("exit_signal", exit_signal);
//            long cpu = Long.parseLong(toks[38]);
//            pData.addProperty("cpu", cpu);
//            long rt_priority = Long.parseLong(toks[39]);
//            pData.addProperty("rt_priority", rt_priority);
//            long policy = Long.parseLong(toks[40]);
//            pData.addProperty("policy", policy);
        }
        catch( IOException ex)
        {
            Log.e(this.getClass().toString(), "Could not read "+"/proc/" + pInfo.pid + "/stat"+" file", ex);
        }

        return pData;
    }

    /**
     * this class receives a ActivityManager.RunningAppProcessInfo object and runs a thread that retrieves the
     * process information and puts it in the jarr array
     */
    class OneShotTask implements Runnable {
        ActivityManager.RunningAppProcessInfo pInfo;

        OneShotTask(ActivityManager.RunningAppProcessInfo pInfo) {
            this.pInfo = pInfo;
        }

        /**
         *
         */
        public void run() {
            try {
                JsonObject jobj=getProcessDataById(this.pInfo);

                synchronized (ProcessStatisticsData.this.jarr){
                    ProcessStatisticsData.this.jarr.add(jobj);
                }
            } catch (Exception ex) {
                Log.e(OneShotTask.class.toString(),ex.getMessage());
            }
        }
    }
}
