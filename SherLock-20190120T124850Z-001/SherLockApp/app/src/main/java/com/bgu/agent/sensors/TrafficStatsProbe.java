package com.bgu.agent.sensors;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;
import com.bgu.agent.sensors.datalayer.BandwidthData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.probe.Probe;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by Simon!
 */

//Pipe line path:  com.bgu.agent.sensors.TrafficStatsProbe

//@Schedule.DefaultSchedule(interval = 10 * Constants.MINUTE)
@Probe.DisplayName("TrafficStatsProbe")
public class TrafficStatsProbe extends SecureBase {
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
    JsonObject overallMemory;
    JsonObject totalCpu = new JsonObject();

    public Long lastTime = new Long(0);

    @Override
    protected void secureOnEnable() {
        super.secureOnEnable();
        String TAG = "Restart tag traffic";
        Long nowTime;
        nowTime = System.currentTimeMillis();
        SharedPreferences sp = getContext().getSharedPreferences("INTERVAL", Context.MODE_PRIVATE);
        lastTime = sp.getLong("motion_interval", 0);
        if (lastTime == null)
            lastTime = 0L;
        long diff = nowTime - lastTime;
        Log.d(TAG, "Motion interval: Now: " + nowTime + " last: " + lastTime + " Diff" + diff);
        if (diff > 60000 && lastTime != 0) {
            Log.d(TAG, "Interval exceeded detected!" + Math.abs(lastTime - nowTime));
            Intent intent = new Intent();
            intent.putExtra("probe", "Motion");
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction("com.bgu.congeor.sherlockapp.RestartFunfReciver");
            getContext().sendBroadcast(intent);
            Log.e(TAG, "Restart broadcast sent!");
        }
        lastTime = nowTime;
        sp.edit().putLong("traffic_interval", nowTime).commit();
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
        JsonObject storageData = new JsonObject();
        cpuObject = new JsonObject();
        arrThreads = new ArrayList<>();
        procDataArray = new JsonArray();
        overallMemory = new JsonObject();
        JsonObject interrupts = getInterrupts();
        data.add("interrupts", interrupts);
        storageData = getStorageSpace();
        data.add("storageData", storageData);
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
//        Wifi stats:
        WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        data.addProperty("connectedWifi_SSID", wifiManager.getConnectionInfo().getSSID());
        data.addProperty("connectedWifi_Level", wifiManager.getConnectionInfo().getRssi());


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
        }
        overallMemory = getUsedMemorySize();
        data.add("TotalMemory", overallMemory);
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
            data.add("other_cpu_info", totalCpu);

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
                    PackageInfo packageInfo = pm.getPackageInfo(p.pkgList[0], 0);
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
                        File procStatFile = new File("/proc/" + p.pid + "/stat");
                        File uptimeFile = new File("/proc/uptime");

                        if (procStatFile.exists() && uptimeFile.exists()) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(procStatFile)), 4096);
                            while ((line = reader.readLine()) != null) {
                                toks = line.split(" ");
                            }
                            reader.close();

                            reader = new BufferedReader(new InputStreamReader(new FileInputStream(uptimeFile)), 4096);
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
                            String rsslim = toks[24];
                            procData.addProperty("rsslim", rsslim);
                            long guest_time = Long.parseLong(toks[42]);
                            procData.addProperty("guest_time", guest_time);
                            long cguest_time = Long.parseLong(toks[43]);
                            procData.addProperty("cguest_time", cguest_time);

                            String tpgid = toks[7];
                            procData.addProperty("tgpid", tpgid);
                            String flags = toks[8];
                            procData.addProperty("flags", flags);
                            String wchan = toks[34];
                            procData.addProperty("wchan", wchan);
                            String exitSignal = toks[37];
                            procData.addProperty("exit_signal", exitSignal);
                            String delayacct = toks[41];
                            procData.addProperty("delayacct_blkio_ticks", delayacct);
                            String minflt = toks[9];
                            procData.addProperty("minflt", minflt);
                            String cminflt = toks[10];
                            procData.addProperty("cminflt", cminflt);
                            String majflt = toks[11];
                            procData.addProperty("majflt", majflt);
                            String startcode = toks[25];
                            procData.addProperty("startcode", startcode);
                            String endcode = toks[26];
                            procData.addProperty("endcode", endcode);
                            String startstack = toks[27];
                            procData.addProperty("startstack", startstack);
                            String kstkesp = toks[28];
                            procData.addProperty("kstkesp", kstkesp);
                            String kstkeip = toks[29];
                            procData.addProperty("kstkeip", kstkeip);
                            String nswap = toks[35];
                            procData.addProperty("nswap", nswap);
                            String cnswap = toks[36];
                            procData.addProperty("cnswap", cnswap);
                            String nice = toks[18];
                            procData.addProperty("nice", nice);
                            String itrealvalue = toks[20];
                            procData.addProperty("itrealvalue", itrealvalue);
                            String processor = toks[38];
                            procData.addProperty("processor", processor);
                            String rt_priority = toks[39];
                            procData.addProperty("rt_priority", rt_priority);

                        }
                    } catch (IOException ex) {
                        fileErrorCounter++;
//                        sendErrorLog(ex);
                        Log.e(TrafficStatsProbe.class.toString(), "Could not read " + "/proc/" + p.pid + "/stat" + " file", ex);
                        Log.e(TrafficStatsProbe.class.toString(), "number of failed Files to read : " + fileErrorCounter);
                    } catch (NumberFormatException e) {
                        Log.e(TrafficStatsProbe.class.toString(), "parseLong" + toks[24]);
                    }

                    synchronized (TrafficStatsProbe.procDataArray) {
                        TrafficStatsProbe.procDataArray.add(procData);
//                        Log.e(TrafficStatsProbe.class.toString(), "procData: " + procData.toString());
                    }

                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TrafficStats.class.toString(), e.getMessage());
            }
        }
    }

    public static int readIntegerFile(String filePath) {

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


    public static JsonObject getUsedMemorySize() {
        JsonObject memObject = new JsonObject();
        long freeSize = 0L;
        long totalSize = 0L;
        long usedSize = -1L;
        long maxSize = 0L;
        String load;
        String[] toks;
        try {
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
            memObject.addProperty("free_size", freeSize);
            totalSize = info.totalMemory();
            memObject.addProperty("total_size", totalSize);
            usedSize = totalSize - freeSize;
            memObject.addProperty("used_size", usedSize);
            maxSize = info.maxMemory();
            memObject.addProperty("max_size", maxSize);

//            new data!!
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
            while ((load = reader.readLine()) != null) {
                load = load.replace(":", "");
                load = load.trim().replaceAll(" +", " ");
                toks = load.split(" ");
                memObject.addProperty(toks[0], toks[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return memObject;

    }

    public void readCpu() {
        Cpu cpu = new Cpu();
        cpu.execute();
    }

    private class Cpu extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            Float[] array = new Float[5];
            array = readCore(4);
            for (int i = 0; i < 4; i++) {
                if (java.lang.Float.isNaN(array[i]))
                    array[i] = (float) 0;
            }
            array[4] = (array[1] + array[0] + array[2] + array[3]) / 4;
            cpuObject.addProperty("CPU_0", array[0]);
            cpuObject.addProperty("CPU_1", array[1]);
            cpuObject.addProperty("CPU_2", array[2]);
            cpuObject.addProperty("CPU_3", array[3]);
            cpuObject.addProperty("Total CPU", array[4]);

            return "Cpu0: " + array[0] + " Cpu1: " + array[1] + " Cpu2: " + array[2] + " Cpu3: " + array[3] + " Total: " + array[4];
        }

        @Override
        protected void onPostExecute(String s) {

//            cpuObject.addProperty("CPU_USAGE", s);
            Log.d(TrafficStats.class.toString(), "added cpu: " + s);

        }
    }


//        }

//
//    private Float refresh(int i) {
//        Float a = readCore(i) * 100;
//        return a;
//    }

    //for multi core value
    private Float[] readCore(int i) {
            /*
             * how to calculate multicore
             * this function reads the bytes from a logging file in the android system (/proc/stat for cpu values)
             * then puts the line into a string
             * then spilts up each individual part into an array
             * then(since he know which part represents what) we are able to determine each cpu total and work
             * then combine it together to get a single float for overall cpu usage
             */

        String[] toks;
        String load;
        Long[] work1 = new Long[4];
        Long[] total1 = new Long[4];
        Long[] work2 = new Long[4];
        Long[] total2 = new Long[4];
        Float[] result = new Float[5];
        Arrays.fill(result, 0f);
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            //skip to the line we need
//            TODO add nice,user.... in here
            load = reader.readLine();
            if (load.contains("cpu ")) {
                toks = load.split(" ");
                totalCpu.addProperty("tot_user", toks[2]);
                totalCpu.addProperty("tot_nice", toks[3]);
                totalCpu.addProperty("tot_system", toks[4]);
                totalCpu.addProperty("tot_idle", toks[5]);
                totalCpu.addProperty("tot_iowait", toks[6]);
                totalCpu.addProperty("tot_irq", toks[7]);
                totalCpu.addProperty("tot_softirq", toks[8]);

            }


            for (int ii = 0; ii < i; ++ii) {
                load = reader.readLine();

                //cores will eventually go offline, and if it does, then it is at 0% because it is not being
                //used. so we need to do check if the line we got contains cpu, if not, then this core = 0
                if (load.contains("cpu")) {
                    toks = load.split(" ");

                    //we are recording the work being used by the user and system(work) and the total info
                    //of cpu stuff (total)
                    //http://stackoverflow.com/questions/3017162/how-to-get-total-cpu-usage-in-linux-c/3017438#3017438

                    work1[ii] = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                    total1[ii] = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                            Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                }
            }
            try {
                //short sleep time = less accurate. But android devices typically don't have more than
                //4 cores, and I'n my app, I run this all in a second. So, I need it a bit shorter
                Thread.sleep(200);
            } catch (Exception e) {
            }

            reader.seek(0);
            //skip to the line we need
            reader.readLine();
            for (int ii = 0; ii < i + 6; ++ii) {

                load = reader.readLine();
//                Log.d("some", "tok: " + load);
                //cores will eventually go offline, and if it does, then it is at 0% because it is not being
                //used. so we need to do check if the line we got contains cpu, if not, then this core = 0%
                if (load.contains("cpu")) {
                    toks = load.split(" ");

                    work2[ii] = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                    total2[ii] = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                            Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                    result[ii] = (float) (work2[ii] - work1[ii]) / (total2[ii] - total1[ii]) * 100;
                }
                if (load.contains("ctxt")) {
                    toks = load.split(" ");
                    totalCpu.addProperty("ctxt", toks[1]);
                } else if (load.contains("btime")) {
                    toks = load.split(" ");
                    totalCpu.addProperty("btime", toks[1]);
                } else if (load.contains("processes")) {
                    toks = load.split(" ");
                    totalCpu.addProperty("processes", toks[1]);
                } else if (load.contains("procs_running")) {
                    toks = load.split(" ");
                    totalCpu.addProperty("procs_running", toks[1]);
                } else if (load.contains("procs_blocked")) {
                    toks = load.split(" ");
                    totalCpu.addProperty("procs_blocked", toks[1]);
                }

            }
            reader.close();
            return result;

            //here we find the change in user work and total info, and divide by one another to get our total
            //seems to be accurate need to test on quad core
            //http://stackoverflow.com/questions/3017162/how-to-get-total-cpu-usage-in-linux-c/3017438#3017438
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private JsonObject getInterrupts() {
        JsonObject inter = new JsonObject();
        String otherCpu = "";
        String[] toks = new String[41];
        String line = "";
        File procStatFile = new File("/proc/interrupts");
        byte byteFile[] = convertFileToByteArray(procStatFile);
        String fileToParse = new String(byteFile);
        String lines[] = fileToParse.split("\n");
        String[] interrupts = new String[]{"msmgpio", "wcd9xxx", "pn547", "cypress_touchkey", "synaptics_rmi4_i2c", "sec_headset_detect", "flip_cover", "home_key", "volume_down", "volume_up", "companion", "SLIMBUS", "call"};
        line = lines[0];
        line = line.trim().replaceAll(" +", " ");
        toks = line.split(" ");

//        Log.e("tagg", "tok: " + line);
        if (toks.length > 1) {
            if (line.contains("CPU1") && line.contains("CPU2") && line.contains("CPU3")) {
                otherCpu = "111";
            } else if (line.contains("CPU1") && line.contains("CPU2")) {
                otherCpu = "110";
            } else if (line.contains("CPU1") && line.contains("CPU3")) {
                otherCpu = "101";
            } else if (line.contains("CPU1")) {
                otherCpu = "100";
            } else if (line.contains("CPU2") && line.contains("CPU3")) {
                otherCpu = "011";
            } else if (line.contains("CPU2")) {
                otherCpu = "010";
            } else if (line.contains("CPU3")) {
                otherCpu = "001";
            }
        } else otherCpu = "000";

//            Log.e("tagg", "toks: " + otherCpu);
        inter.addProperty("cpu123_intr_prs", otherCpu);
        int coreNum = toks.length;
//        Log.e("tagg", "toks: core" + coreNum);

        for (int i =1; i<lines.length; i++) {
            line = lines[i].trim().replaceAll(" +", " ");
            toks = line.split(" ");
//            Log.i("tag", "tok: " + lines[i]);
            if (!toks[0].contains("IPI3")) {
                if (toks.length > (coreNum + 2)) {
                    if (Arrays.asList(interrupts).contains(toks[coreNum + 2])) {
                        Collection array = new ArrayList(Arrays.asList(interrupts));
                        array.remove(toks[coreNum + 2]);
                        interrupts = (String[]) array.toArray(new String[array.size()]);
                        Long num1, sum = 0L;
                        num1 = Long.parseLong(toks[1]);
                        if (coreNum > 1) {
                            for (int ii = 2; ii < (coreNum + 1); ii++) {
                                sum += Long.parseLong(toks[ii]);
//                            sum = num1 + num2 + num3 + num4;
                            }
                        } else sum = null;
//                            if (toks.length > 6) {
//                                if (toks[6].equals("call")) {
//                                    inter.addProperty("function_call_interrupts_cpu0", num1);
//                                    inter.addProperty("function_call_interrupts_sum_cpu123", sum);
//                                } else {
//                                    inter.addProperty(toks[coreNum + 2] + "_cpu0", num1);
//                                    inter.addProperty(toks[coreNum + 2] + "_sum_cpu123", sum);
//                                }
//                            } else {

                            inter.addProperty(toks[coreNum + 2] + "_cpu0", num1);
                            inter.addProperty(toks[coreNum + 2] + "_sum_cpu123", sum);

                    }
                }
            }else {
                if (toks[6].equals("call")) {
                    Collection array = new ArrayList(Arrays.asList(interrupts));
                    array.remove("call");
                    interrupts = (String[]) array.toArray(new String[array.size()]);
                    Long num1, sum = 0L;
                    num1 = Long.parseLong(toks[1]);
                        for (int ii = 2; ii < 5; ii++) {
                            sum += Long.parseLong(toks[ii]);
                        }
                    inter.addProperty("function_call_interrupts_cpu0", num1);
                    inter.addProperty("function_call_interrupts_sum_cpu123", sum);
                }
            }
        }
//            add null to fields that don't exists in the file
        for (String str : interrupts) {
            inter.addProperty(str + "_cpu0", "null");
            inter.addProperty(str + "_sum_cpu123", "null");
        }
        return inter;
    }
    public static byte[] convertFileToByteArray(File f)
    {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead =0;

            while ((bytesRead = inputStream.read(b)) != -1)
            {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return byteArray;
    }

    private JsonObject getStorageSpace(){
        JsonObject storage = new JsonObject();
        StatFs internalStatFs = new StatFs( Environment.getRootDirectory().getAbsolutePath() );
        storage.addProperty("Internal_AvailableBlocks", internalStatFs.getAvailableBlocksLong());
        storage.addProperty("Internal_BlockCount", internalStatFs.getBlockCountLong());
        storage.addProperty("Internal_FreeBlocks", internalStatFs.getFreeBlocksLong());
        storage.addProperty("Internal_BlockSize", internalStatFs.getBlockSizeLong());
        storage.addProperty("Internal_AvailableBytes", internalStatFs.getAvailableBytes());
        storage.addProperty("Internal_FreeBytes", internalStatFs.getFreeBytes());
        storage.addProperty("Internal_TotalBytes", internalStatFs.getTotalBytes());

        StatFs externalStatFs = new StatFs( Environment.getExternalStorageDirectory().getAbsolutePath() );
        storage.addProperty("External_AvailableBlocks", externalStatFs.getAvailableBlocksLong());
        storage.addProperty("External_BlockCount", externalStatFs.getBlockCountLong());
        storage.addProperty("External_FreeBlocks", externalStatFs.getFreeBlocksLong());
        storage.addProperty("External_BlockSize", externalStatFs.getBlockSizeLong());
        storage.addProperty("External_AvailableBytes", externalStatFs.getAvailableBytes());
        storage.addProperty("External_FreeBytes", externalStatFs.getFreeBytes());
        storage.addProperty("External_TotalBytes", externalStatFs.getTotalBytes());

        return storage;
    }


}








