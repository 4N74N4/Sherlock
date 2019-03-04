package com.bgu.congeor.sherlockapp.intentservices;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import au.com.bytecode.opencsv.CSVWriter;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.AcceVals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Simon on 16/07/2015.
 * Class for sensing sensors continuously
 */


@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class SensorService extends Service implements SensorEventListener {
    SensorManager sensorManager;
    private ArrayList sensorData;
    private ArrayList rotationData, gyroData, magnoData;
    private ArrayList linAccelData;
    private static final String TAG = SensorService.class.getSimpleName();
    private long timestamp;
    File zipFolder;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Sensor accel = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accel,
                SensorManager.SENSOR_DELAY_NORMAL);
        Sensor rotation = sensorManager
                .getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, rotation,
                SensorManager.SENSOR_DELAY_NORMAL);
//        Sensor linAccel = sensorManager
//                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        sensorManager.registerListener(this, linAccel,
//                SensorManager.SENSOR_DELAY_FASTEST);
        Sensor gyro = sensorManager
                .getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyro,
                SensorManager.SENSOR_DELAY_NORMAL);
//        Sensor magno = sensorManager
//                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        sensorManager.registerListener(this, magno,
//                SensorManager.SENSOR_DELAY_FASTEST);


        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorData = new ArrayList();
        linAccelData = new ArrayList();
        rotationData = new ArrayList();
        gyroData = new ArrayList();
        magnoData = new ArrayList();
        zipFolder= new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + Constants.APPLICATION_NAME + "/zip");
        zipFolder.mkdirs();
        timestamp = System.currentTimeMillis();

    }


    public void midWrite() throws IOException {
        Log.d(TAG,"start write!!");
//        Write the Accelerometer data
        String csv = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + Constants.APPLICATION_NAME + "/Data/Accel.csv";
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(csv,true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int j=0;j<sensorData.size(); j += 1) {
            writer.writeNext(new String[]{sensorData.get(j).toString()});
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sensorData.clear();

//      Write the rotation data
        String csv2 = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + Constants.APPLICATION_NAME + "/Data/Rotation.csv";
        CSVWriter writer_rotation = null;
        try {
            writer_rotation = new CSVWriter(new FileWriter(csv2,true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int j=0;j<rotationData.size(); j += 1) {
            writer_rotation.writeNext(new String[]{rotationData.get(j).toString()});
        }
        try {
            writer_rotation.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        rotationData.clear();

        // Write the Gyroscope
        String csv4 = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + Constants.APPLICATION_NAME + "/Data/Gyro.csv";
        CSVWriter writer_gyro = new CSVWriter(new FileWriter(csv4,true));
        for(int j=0;j<gyroData.size(); j += 1) {
            writer_gyro.writeNext(gyroData.get(j).toString());
        }
        writer_gyro.close();
        gyroData.clear();


        Log.d(TAG, "Writing has finished!");
        Log.d(TAG, "time diff: " + (System.currentTimeMillis()-timestamp) + "  timeConst: " + Constants.HOUR*1000);
        if ((System.currentTimeMillis()-timestamp)>Constants.HOUR*1000){
            String[] files = {csv, csv2, csv4};
            DateFormat dateFormat = new SimpleDateFormat("dd-MM HH:mm");
            Calendar cal = Calendar.getInstance();
            Log.d(TAG, "length: " + zipFolder.listFiles().length + " list: " +zipFolder.list());
            boolean zip =  Utils.multipaleFileZipping(files, zipFolder.getAbsolutePath()+"/" +dateFormat.format(cal.getTime()) + ".zip");
            Log.d(TAG, "zipping suc? " + zip);
            if (zip){
                new File(csv).delete();
                new File(csv2).delete();
                new File(csv4).delete();
                timestamp = System.currentTimeMillis();
            }
            if (zipFolder.listFiles().length>=24){
                List<String> filesPaths = new ArrayList<>();
                File[] zipFiles = zipFolder.listFiles();
                for (File file:zipFiles){
                    if (file.getName().endsWith(".zip"))
                    filesPaths.add(file.getAbsolutePath());
                }
                Object[] listObj = filesPaths.toArray();
                String[] strings = Arrays.copyOf(listObj, listObj.length, String[].class);
                Utils.multipaleFileZipping(strings, zipFolder + "/Continues.zip");
                String sherlokPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data/";
                if (Utils.fileZipping(zipFolder + "/Continues.zip", "Continues.zip", sherlokPath + "Continues" + System.currentTimeMillis() + ".zip")) {
                    new File(zipFolder + "/Continues.zip").delete();
                    for (File file:zipFiles){
                        if (file.getName().endsWith(".zip"))
                           file.delete();
                    }
                }
                Log.d(TAG, filesPaths.toString());
//                Utils.fileZipping()
            }
        }
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        long timestamp = event.timestamp;

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];

            AcceVals acceVals = new AcceVals(x, y, z, timestamp);
            sensorData.add(acceVals);
        }
        else if(event.sensor.getType()== Sensor.TYPE_ROTATION_VECTOR){
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
//            long timestamp = System.currentTimeMillis();
            AcceVals acceVals = new AcceVals(x, y, z, timestamp);
            rotationData.add(acceVals);
        }
      else if(event.sensor.getType()== Sensor.TYPE_GYROSCOPE){
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
//            long timestamp = System.currentTimeMillis();
            AcceVals acceVals = new AcceVals(x, y, z, timestamp);
            gyroData.add(acceVals);
        }
        if(gyroData.size()>2000||sensorData.size()>20000||rotationData.size()>20000)
            try {
                midWrite();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        Log.d(TAG, "onDestroy");
        try {
            midWrite();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
