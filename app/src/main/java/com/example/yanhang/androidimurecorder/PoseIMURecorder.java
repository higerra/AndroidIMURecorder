package com.example.yanhang.androidimurecorder;

/**
 * Created by yanhang on 1/9/17.
 */

/**
 * NOTICE: The order of quaternion is modified to be agree with event.values[] array
 */

import android.content.Intent;
import android.hardware.SensorEvent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.RunnableFuture;

import static android.content.ContentValues.TAG;

public class PoseIMURecorder {
    private static final float mulNanoToSec = 1000000000;

    private final static String LOG_TAG = PoseIMURecorder.class.getName();

    private MainActivity parent_;

    public static final int SENSOR_COUNT = 8;
    public static final int GYROSCOPE = 0;
    public static final int ACCELEROMETER = 1;
    public static final int MAGNETOMETER = 2;
    public static final int LINEAR_ACCELERATION = 3;
    public static final int GRAVITY = 4;
    public static final int ROTATION_VECTOR = 5;
    public static final int TANGO_POSE = 6;
    public static final int STEP_COUNTER = 7;

    private BufferedWriter[] file_writers_ = new BufferedWriter[SENSOR_COUNT];
    // private Vector<Vector<String>> data_buffers_ = new Vector<Vector<String> >();
    private String[] default_file_names_ = {"gyro.txt", "acce.txt", "magnet.txt", "linacce.txt",
            "gravity.txt", "orientation.txt", "pose.txt", "step.txt"};

    public PoseIMURecorder(String path, MainActivity parent){
        parent_ = parent;
        Calendar file_timestamp = Calendar.getInstance();
        String header = "# Created at " + file_timestamp.getTime().toString() + "\n";
        try {
            for(int i=0; i<SENSOR_COUNT; ++i) {
                file_writers_[i] = createFile(path + "/" + default_file_names_[i], header);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void writeBufferToFile(FileWriter writer, Vector<String> buffer) throws IOException{
        for (String line : buffer) {
            writer.write(line);
        }
        writer.close();
    }

    public void endFiles(){
        try {
            for(int i=0; i<SENSOR_COUNT; ++i){
                //writeBufferToFile(file_writers_[i], data_buffers_.get(i));
                file_writers_[i].flush();
                file_writers_[i].close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public Boolean addRecord(long timestamp, float[] record, int kBins, int type){
        if(type < 0 && type > SENSOR_COUNT){
            return false;
        }
        try{
            String line = String.format(Locale.US, "%d", timestamp);
            for(int i=0; i<kBins; ++i){
                line += String.format(Locale.US, " %.6f", record[i]);
            }
            line += "\n";
            file_writers_[type].write(line);
            return true;
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
    }
    public Boolean addIMURecord(SensorEvent event, int type){
        if(type < 0 && type >= SENSOR_COUNT){
            return false;
        }
        float[] values = event.values;
        long timestamp = event.timestamp;
        try {
            if (type == ROTATION_VECTOR ) {
                //data_buffers_.get(type).add(String.format(Locale.US,"%d %.6f %.6f %.6f\n", timestamp, values[0], values[1], values[2]));
                file_writers_[type].write(String.format(Locale.US,"%d %.6f %.6f %.6f %.6f\n",
                        timestamp, values[0], values[1], values[2], values[3]));
            }else{
                //data_buffers_.get(type).add(String.format(Locale.US,"%d %.6f %.6f %.6f %.6f\n", timestamp, values[3], values[0], values[1], values[2]));
                file_writers_[type].write(String.format(Locale.US, "%d %.6f %.6f %.6f\n", timestamp, values[0], values[1], values[2]));
            }
            return true;
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
    }

    public Boolean addStepRecord(long timestamp, int value){
        try{
            file_writers_[STEP_COUNTER].write(String.format(Locale.US, "%d %d\n", timestamp, value));
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private BufferedWriter createFile(String path, String header) throws IOException{
        File file = new File(path);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        Intent scan_intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scan_intent.setData(Uri.fromFile(file));
        parent_.sendBroadcast(scan_intent);
        if(header != null && header.length() != 0) {
            writer.append(header);
        }
        return writer;
    }
}
