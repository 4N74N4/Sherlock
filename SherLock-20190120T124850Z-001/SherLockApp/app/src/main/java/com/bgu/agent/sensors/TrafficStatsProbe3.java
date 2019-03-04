package com.bgu.agent.sensors;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.util.Log;
import com.bgu.agent.sensors.datalayer.BandwidthData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.probe.Probe;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Simon!
 */

//Pipe line path:  com.bgu.agent.sensors.TrafficStatsProbe

//@Schedule.DefaultSchedule(interval = 10 * Constants.MINUTE)
@Probe.DisplayName("TrafficStatsProbe")
public class TrafficStatsProbe3 extends SecureBase {
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
    int fileErrorCounter = 0;
    Double CPUfreq;
    final int PROCESSES_BATCH_SIZE = 10;
    JsonObject cpuObject;

    @Override
    protected void secureOnEnable() {
        super.secureOnEnable();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    @Override
    protected void secureOnStart() {
        super.secureOnStart();
        int i;
        long sTime = System.currentTimeMillis();
        long idtry = Thread.currentThread().getId();


        Log.e(TrafficStatsProbe.class.toString(), "TrafficStatsProbe threadID: " + idtry);
        Log.e(TrafficStatsProbe.class.toString(), "processDAtaProbe started: " + sTime);
        try {
            BufferedReader reader = new BufferedReader(new
                    InputStreamReader(new
                    FileInputStream("/proc/cpuinfo")), 2048);

            String line;
            String[] toks;
            String[] words;
            while ((line = reader.readLine()) != null) {
                toks = line.split(" ");
                words = toks[0].split("\t");

                if (words[0].equals("BogoMIPS")) {
                    CPUfreq = Double.parseDouble(toks[1]);
                    break;
                }
            }
            reader.close();
        } catch (IOException ioe) {
            Log.e(TrafficStatsProbe.class.toString(), "Exception parsing /proc/cpuinfo");
        }
        JsonObject data = new JsonObject();

        cpuObject = new JsonObject();
        arrThreads = new ArrayList<>();
        procDataArray = new JsonArray();
        pm = getContext().getPackageManager();
        am = (ActivityManager) getContext().getSystemService(getContext().ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        for (Iterator<ActivityManager.RunningAppProcessInfo> iterator = processes.iterator(); iterator.hasNext(); ) {

            List<ActivityManager.RunningAppProcessInfo> batch = new ArrayList<>();
            int limit = PROCESSES_BATCH_SIZE < processes.size() ? PROCESSES_BATCH_SIZE : processes.size();
            for (i = 0; i < limit; i++) {
                if (iterator.hasNext()) {
                    batch.add(iterator.next());
                    iterator.remove();
                }
            }
            if (!batch.isEmpty())
                divideThreadWork(batch);

        }

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


        int hertz = readIntegerFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
        if (hertz != 0) {
            data.addProperty("CpuHertz", formatFreq(hertz));
            Log.d(TrafficStatsProbe.class.toString(), "CpuHertz = " + hertz);
        }
//        float usage = readUsage();
//        data.addProperty("CPU_USAGE", usage);
//        Log.d(TrafficStatsProbe.class.toString(), "CPU_USAGE " + usage);
        long memoryUsage = getUsedMemorySize();
        Log.d(TrafficStatsProbe.class.toString(), "memory!! " + memoryUsage);
        readCpu();
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //addding the array of process's data
        data.add("ProcTraffic", procDataArray);
        synchronized (cpuObject) {
            data.add("CpuStats", cpuObject);

        }
        sendData(data);
        Log.e(TrafficStatsProbe.class.toString(), "probes ended, it took: " + System.currentTimeMillis());
        stop();

    }

    private boolean divideThreadWork(List<ActivityManager.RunningAppProcessInfo> processes) {
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
                    PackageInfo packageInfo = pm.getPackageInfo(p.pkgList[0],0);
                    procData.addProperty("ApplicationName", ai.loadLabel(pm).toString());
                    procData.addProperty("Version_Name", packageInfo.versionName);
                    procData.addProperty("Version_Code", packageInfo.versionCode);
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
                    } catch (IOException ex) {
                        fileErrorCounter++;
                        Log.e(TrafficStatsProbe.class.toString(), "Could not read " + "/proc/" + p.pid + "/stat" + " file", ex);
                        Log.e(TrafficStatsProbe.class.toString(), "number of failed Files to read : " + fileErrorCounter);
                    }

                    synchronized (TrafficStatsProbe3.procDataArray) {
                        TrafficStatsProbe3.procDataArray.add(procData);
                    }

                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TrafficStats.class.toString(), e.getMessage());
            }
        }
    }

    private static int readIntegerFile(String filePath) {

        try {
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath)), 1000);
            final String line = reader.readLine();
            reader.close();

            return Integer.parseInt(line);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatFreq(int clockHz) {

        if (clockHz < 1000 * 1000) {
            return (clockHz / 1000) + " MHz";
        }

        // a.b GHz
        final int a = (clockHz / 1000 / 1000);      // a.b GHz ? a ?
        final int b = (clockHz / 1000 / 100) % 10;  // a.b GHz ? b ?
        return a + "." + b + " GHz";
    }


    public static long getUsedMemorySize() {

        long freeSize = 0L;
        long totalSize = 0L;
        long usedSize = -1L;
        try {
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
            totalSize = info.totalMemory();
            usedSize = totalSize - freeSize;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return freeSize;

    }

    public void readCpu() {
        Cpu cpu = new Cpu();
        cpu.execute();
    }

    private class Cpu extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            Float[] array = new Float[5];
            array[0] = refresh(0);
            array[1] = refresh(0);
            array[2] = refresh(0);
            array[3] = refresh(0);
            array[4] = (array[1] + array[0] + array[2] + array[3]) / 4;
            return "Cpu0: " + array[0] + "Cpu1: " + array[1] + "Cpu2: " + array[2] + "Cpu3: " + array[3] + " Total: " + array[4];
        }

        @Override
        protected void onPostExecute(String s) {

            cpuObject.addProperty("CPU_USAGE", s);
            Log.d(TrafficStats.class.toString(), "added cpu: " + s);

        }
    }


//        }


    private Float refresh(int i) {
        Float a = readCore(i) * 100;
        return a;
    }

    //for multi core value
    private float readCore(int i) {
            /*
             * how to calculate multicore
             * this function reads the bytes from a logging file in the android system (/proc/stat for cpu values)
             * then puts the line into a string
             * then spilts up each individual part into an array
             * then(since he know which part represents what) we are able to determine each cpu total and work
             * then combine it together to get a single float for overall cpu usage
             */
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            //skip to the line we need
            for (int ii = 0; ii < i + 1; ++ii) {
                reader.readLine();
            }
            String load = reader.readLine();

            //cores will eventually go offline, and if it does, then it is at 0% because it is not being
            //used. so we need to do check if the line we got contains cpu, if not, then this core = 0
            if (load.contains("cpu")) {
                String[] toks = load.split(" ");

                //we are recording the work being used by the user and system(work) and the total info
                //of cpu stuff (total)
                //http://stackoverflow.com/questions/3017162/how-to-get-total-cpu-usage-in-linux-c/3017438#3017438

                long work1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                long total1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                        Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                        + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

                try {
                    //short sleep time = less accurate. But android devices typically don't have more than
                    //4 cores, and I'n my app, I run this all in a second. So, I need it a bit shorter
                    Thread.sleep(160);
                } catch (Exception e) {
                }

                reader.seek(0);
                //skip to the line we need
                for (int ii = 0; ii < i + 1; ++ii) {
                    reader.readLine();
                }
                load = reader.readLine();
                //cores will eventually go offline, and if it does, then it is at 0% because it is not being
                //used. so we need to do check if the line we got contains cpu, if not, then this core = 0%
                if (load.contains("cpu")) {
                    reader.close();
                    toks = load.split(" ");

                    long work2 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                    long total2 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                            Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);


                    //here we find the change in user work and total info, and divide by one another to get our total
                    //seems to be accurate need to test on quad core
                    //http://stackoverflow.com/questions/3017162/how-to-get-total-cpu-usage-in-linux-c/3017438#3017438

                    return (float) (work2 - work1) / ((total2 - total1));
                } else {
                    reader.close();
                    return 0;
                }

            } else {
                reader.close();
                return 0;
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }


}








