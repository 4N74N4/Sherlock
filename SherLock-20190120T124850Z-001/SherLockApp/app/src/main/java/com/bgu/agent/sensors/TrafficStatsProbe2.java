package com.bgu.agent.sensors;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Debug;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.datalayer.BandwidthData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.probe.Probe;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by Danny Shefer
 */

//Pipe line path:  com.bgu.agent.sensors.TrafficStatsProbe

//@Schedule.DefaultSchedule(interval = 10 * Constants.MINUTE)
@Probe.DisplayName("TrafficStatsProbe")
public class TrafficStatsProbe2 extends SecureBase {
    long MobileTxBytes = 0;
    long MobileRxBytes = 0;
    long MobileRxPackets = 0;
    long MobileTxPackets = 0;
    long WifiTxBytes = 0;
    long WifiRxBytes = 0;
    long WifiRxPackets = 0;
    long WifiTxPackets = 0;
    long TotalRxBytes = 0;
    long TotalRxPackets = 0;
    long TotalTxBytes = 0;
    long TotalTxPackets = 0;
    static JsonArray procDataArray;
    private ArrayList<Thread> arrThreads;
    HashMap<String, BandwidthData> appData = new HashMap<String, BandwidthData>();
    PackageManager pm;
    ActivityManager am;
    int fileErrorCounter=0;
    Double CPUfreq;
    ExecutorService exService;
   // ThreadPoolExecutor threadPoolExecutor;
    final int PROCESSES_BATCH_SIZE = 10;
    @Override
    protected void secureOnEnable() {
        super.secureOnEnable();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    @Override
    protected void secureOnStart() {
        super.secureOnStart();
        //BlockingQueue<Runnable> poolQueue = new LinkedBlockingQueue<>();
        double numOfCores =  Runtime.getRuntime().availableProcessors()*0.5;
        exService = Executors.newFixedThreadPool(((int)numOfCores));
        //threadPoolExecutor = new ThreadPoolExecutor(numOfCores,numOfCores,30, TimeUnit.MILLISECONDS,poolQueue);

//        long sTime = System.currentTimeMillis();
//        long idtry = Thread.currentThread().getId();
//        int i;
//        Log.e(TrafficStatsProbe2.class.toString(),"TrafficStatsProbe threadID: "  + idtry);
//        Log.e(TrafficStatsProbe2.class.toString() , "processDAtaProbe started: " + sTime);
        try
        {
            BufferedReader reader = new BufferedReader( new
                    InputStreamReader( new
                    FileInputStream( "/proc/cpuinfo" ) ), 2048 );

            String line;
            String[] toks;
            String[] words;
            while ( (line = reader.readLine()) != null )
            {
                toks = line.split(" ");
                words = toks[0].split("\t");

                if (words[0].equals("BogoMIPS"))
                {
                    CPUfreq = Double.parseDouble(toks[1]);
                    break;
                }
            }
            reader.close();
        }
        catch (IOException ioe)
        {
            Log.e(TrafficStatsProbe2.class.toString(), "Exception parsing /proc/cpuinfo");
        }
        JsonObject data = new JsonObject();

        arrThreads = new ArrayList<>();
        procDataArray = new JsonArray();
        pm = getContext().getPackageManager();
        am = (ActivityManager) getContext().getSystemService(getContext().ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        Logger.e(TrafficStatsProbe2.class, "number of running proccesses "+ processes.size());
        ArrayList<Future> futures = new ArrayList<>();
        int count=0;
        for (Iterator<ActivityManager.RunningAppProcessInfo> iterator = processes.iterator(); iterator.hasNext();)
        {
            //threadPoolExecutor.execute(new OneShotTask(iterator.next()));
            count++;

            futures.add(exService.submit(new OneShotTask(iterator.next())));

        }
        Logger.e(TrafficStatsProbe2.class, "Threads that has been started : " + count);
        count = 0;
        for (Future future : futures) {
            try {
                count++;
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.e(TrafficStatsProbe2.class, "this thread has stopped for some reason "+ e.getMessage());
                Logger.e(TrafficStatsProbe2.class, " Error : Threads that has ended : " + count);
            } catch (ExecutionException e) {
                Logger.e(TrafficStatsProbe2.class, "something didnt work here.... " + e.getCause());
                Logger.e(TrafficStatsProbe2.class, " Error : Threads that has ended : " + count);
                e.printStackTrace();
            }
        }
        Logger.e(TrafficStatsProbe2.class, "Threads that has ended : " + count);

//            List<ActivityManager.RunningAppProcessInfo> batch = new ArrayList<>();
//            int limit = PROCESSES_BATCH_SIZE<processes.size()? PROCESSES_BATCH_SIZE : processes.size();
//            for(i=0;i<limit;i++)
//            {
//                if(iterator.hasNext()){
//                batch.add(iterator.next());
//                iterator.remove();}
//
//            }
//            if(!batch.isEmpty())
//                divideThreadWork(batch);
//
//        }
//        while(!processes.isEmpty())
//        {
//            List<ActivityManager.RunningAppProcessInfo> temp = processes;
//            List<ActivityManager.RunningAppProcessInfo> batch = temp.subList(0, PROCESSES_BATCH_SIZE> temp.size() ? temp.size() : PROCESSES_BATCH_SIZE);
//            divideThreadWork(batch);
//
//            processes=temp;
//
//        }
//        for (ActivityManager.RunningAppProcessInfo p : processes) {
//
//            Thread t = new Thread(new OneShotTask(p));
//
//            this.arrThreads.add(t);
//            t.start();
//        }
//
//        Thread.yield();
//
//        for (Thread t : this.arrThreads) {
//            try {
//                t.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

//        arrThreads.clear();

        data.addProperty("MobileTxBytes", TrafficStats.getMobileTxBytes() - MobileTxBytes);
        data.addProperty("MobileTxPackets", TrafficStats.getMobileTxPackets() - MobileTxPackets);
        data.addProperty("MobileRxBytes", TrafficStats.getMobileRxBytes() - MobileRxBytes);
        data.addProperty("MobileRxPackets", TrafficStats.getMobileRxPackets() - MobileRxPackets);
        data.addProperty("TotalWifiTxBytes", TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes() - WifiTxBytes);
        data.addProperty("TotalWifiTxPackets", TrafficStats.getTotalTxPackets() - TrafficStats.getMobileTxPackets() - WifiTxPackets);
        data.addProperty("TotalWifiRxBytes", TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes() - WifiRxBytes);
        data.addProperty("TotalWifiRxPackets", TrafficStats.getTotalRxPackets() - TrafficStats.getMobileRxPackets() - WifiRxPackets);
        data.addProperty("TotalRxBytes", TrafficStats.getTotalRxBytes() - TotalRxBytes);
        data.addProperty("TotalRxPackets", TrafficStats.getTotalRxPackets() - TotalRxPackets);
        data.addProperty("TotalTxBytes", TrafficStats.getTotalTxBytes() - TotalTxBytes);
        data.addProperty("TotalTxPackets", TrafficStats.getTotalTxPackets() - TotalTxPackets);
        MobileTxBytes = TrafficStats.getMobileTxBytes();
        MobileTxPackets = TrafficStats.getMobileTxPackets();
        MobileRxBytes = TrafficStats.getMobileRxBytes();
        MobileRxPackets = TrafficStats.getMobileRxPackets();
        TotalRxBytes = TrafficStats.getTotalRxBytes();
        TotalRxPackets = TrafficStats.getTotalRxPackets();
        TotalTxBytes = TrafficStats.getTotalTxBytes();
        TotalTxPackets = TrafficStats.getTotalTxPackets();
        WifiTxBytes = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes();
        WifiRxBytes = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes();
        WifiRxPackets = TrafficStats.getTotalRxPackets() - TrafficStats.getMobileRxPackets();
        WifiTxPackets = TrafficStats.getTotalTxPackets() - TrafficStats.getMobileTxPackets();

        //addding the array of process's data
        data.add("ProcTraffic", procDataArray);
        sendData(data);
        Log.e(TrafficStatsProbe2.class.toString(), "probes ended, it took: "+ System.currentTimeMillis());
        stop();

    }

    private boolean divideThreadWork (List<ActivityManager.RunningAppProcessInfo> processes)
    {
        for (ActivityManager.RunningAppProcessInfo p : processes) {

            Thread t = new Thread(new OneShotTask(p));
            this.arrThreads.add(t);
            t.start();
        }

        Thread.yield();

        for (Thread t : this.arrThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    @Override
    protected void secureOnDisable() {
        super.secureOnDisable();
    }

    class OneShotTask implements Runnable {

        ActivityManager.RunningAppProcessInfo p;
        BandwidthData bd = null;

        OneShotTask(ActivityManager.RunningAppProcessInfo pInfo) {
            this.p = pInfo;
        }
        public void run() {
            try {
                if (p.pkgList.length == 1) {

                    JsonObject procData = new JsonObject();
                    //get the application info according the package the process stating in pkglist
                    ApplicationInfo ai = pm.getApplicationInfo(p.pkgList[0], pm.GET_META_DATA);
                    procData.addProperty("ApplicationName", ai.loadLabel(pm).toString());
                    procData.addProperty("PackageName", ai.packageName);
                    procData.addProperty("PackageUID", ai.uid);
                    //procData.addProperty("processName", p.processName);
                    procData.addProperty("pid", p.pid);
                    //procData.addProperty("uid", p.uid);
                    procData.addProperty("importance", p.importance);
                    procData.addProperty("importanceReasonCode", p.importanceReasonCode);
                    procData.addProperty("importanceReasonPid", p.importanceReasonPid);
                    procData.addProperty("lru", p.lru);
                    //bandwidth data
                    long currentRxBytes = TrafficStats.getUidRxBytes(ai.uid);
                    long currentRxPack = TrafficStats.getUidRxPackets(ai.uid);
                    long currentTxBytes = TrafficStats.getUidTxBytes(ai.uid);
                    long currentTxPack = TrafficStats.getUidTxPackets(ai.uid);
                    bd = new BandwidthData(new Long[]{currentTxBytes, currentTxPack, currentRxBytes, currentRxPack});
                    if (appData.containsKey(p.pkgList[0])) {
                        procData.addProperty("UidRxBytes", currentRxBytes - appData.get(ai.packageName).RxBytes);
                        procData.addProperty("UidRxPackets", currentRxPack - appData.get(ai.packageName).RxPackets);
                        procData.addProperty("UidTxBytes", currentTxBytes - appData.get(ai.packageName).TxBytes);
                        procData.addProperty("UidTxPackets", currentTxPack - appData.get(ai.packageName).TxPackets);
                    } else {
                        procData.addProperty("UidRxBytes", -1);
                        procData.addProperty("UidRxPackets", -1);
                        procData.addProperty("UidTxBytes", -1);
                        procData.addProperty("UidTxPackets", -1);

                    }
                    appData.put(p.pkgList[0], bd);

                    //memory data
                    Debug.MemoryInfo memInfo = am.getProcessMemoryInfo(new int[]{p.pid})[0];
                    procData.addProperty("dalvikPrivateDirty", memInfo.dalvikPrivateDirty);
                    procData.addProperty("dalvikPss", memInfo.dalvikPss);
                    procData.addProperty("dalvikSharedDirty", memInfo.dalvikSharedDirty);
                    procData.addProperty("nativePrivateDirty", memInfo.nativePrivateDirty);
                    procData.addProperty("nativePss", memInfo.nativePss);
                    procData.addProperty("nativeSharedDirty", memInfo.nativeSharedDirty);
                    procData.addProperty("otherPrivateDirty", memInfo.otherPrivateDirty);
                    procData.addProperty("otherPss", memInfo.otherPss);
                    procData.addProperty("otherSharedDirty", memInfo.otherSharedDirty);

                    //fill process cpu data from proc/[pid]/stat file to json object
                    String line;
                    String[] toks = new String[41];
                    String[] times = new String[3];
                    double Hertz = 100;

                    try {
                        //read the stat file
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/" + p.pid + "/stat")), 4096);
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
                        String tcomm = toks[1];
                        procData.addProperty("tcomm", tcomm);
                        String state = toks[2];
                        procData.addProperty("state", state);
                        long ppid = Long.parseLong(toks[3]);
                        procData.addProperty("ppid", ppid);
                        long pgid = Long.parseLong(toks[4]);
                        procData.addProperty("pgid", pgid);
                        long sid = Long.parseLong(toks[5]);
                        procData.addProperty("sid", sid);
                        long cmaj_flt = Long.parseLong(toks[12]);
                        procData.addProperty("cmaj_flt", cmaj_flt);
                        double utime = Double.parseDouble(toks[13]);
                        procData.addProperty("utime", utime);
                        double stime = Double.parseDouble(toks[14]);
                        procData.addProperty("stime", stime);
                        double total_time = utime + stime;
                        double cutime = Double.parseDouble(toks[15]);
                        procData.addProperty("cutime", cutime);
                        double cstime = Double.parseDouble(toks[16]);
                        procData.addProperty("cstime", cstime);
                        total_time = total_time + cutime + cstime;
                        double cpu_usage;
                        long priority = Long.parseLong(toks[17]);
                        procData.addProperty("priority", priority);
                        long num_threads = Long.parseLong(toks[19]);
                        String starttime = toks[21];
                        procData.addProperty("start_time", starttime);
                        double seconds = Double.parseDouble(times[0]) - (Double.parseDouble(starttime) / Hertz);
                        cpu_usage = 100 * ((total_time / Hertz) / seconds);
                        DecimalFormat df = new DecimalFormat("#.##");
                        cpu_usage = Double.valueOf(df.format(cpu_usage));
                        procData.addProperty("CPU_USAGE", cpu_usage);
                        procData.addProperty("num_threads", num_threads);
                        long vsize = Long.parseLong(toks[22]);
                        procData.addProperty("vsize", vsize);
                        long rss = Long.parseLong(toks[23]);
                        procData.addProperty("rss", rss);
                        long rsslim = Long.parseLong(toks[24]);
                        procData.addProperty("rsslim", rsslim);
                        long guest_time = Long.parseLong(toks[42]);
                        procData.addProperty("guest_time", guest_time);
                        long cguest_time = Long.parseLong(toks[43]);
                        procData.addProperty("cguest_time", cguest_time);
                    }
                    catch (IOException ex) {
                        fileErrorCounter++;
                        Log.e(TrafficStatsProbe2.class.toString(), "Could not read " + "/proc/" + p.pid + "/stat" + " file", ex);
                        Log.e(TrafficStatsProbe2.class.toString(), "number of failed Files to read : "+ fileErrorCounter);
                    }

                    synchronized (TrafficStatsProbe2.procDataArray) {
                        TrafficStatsProbe2.procDataArray.add(procData);
                    }

                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TrafficStats.class.toString(), e.getMessage());
            }
        }
    }
//    class ThreadExecutor extends ThreadPoolExecutor
//    {
//
//        public ThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
//            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
//        }
//        @Override
//        protected void beforeExecute(Thread t, Runnable r) {
//            super.beforeExecute(t, r);
//            Logger.e(TrafficStatsProbe2.class,"Perform beforeExecute() logic");
//        }
//
//        @Override
//        protected void afterExecute(Runnable r, Throwable t) {
//            super.afterExecute(r, t);
//            if (t != null) {
//               Logger.e(TrafficStatsProbe2.class, "Perform exception handler logic");
//            }
//            Logger.e(TrafficStatsProbe2.class, "Perform afterExecute() logic");
//        }
//    }
}








