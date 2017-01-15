package com.example.yanhang.tangoimurecorder;

/**
 * Created by yanhang on 1/9/17.
 */

import com.google.atap.tangoservice.TangoPoseData;

import android.content.Intent;
import android.hardware.SensorEvent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;

public class PoseIMURecorder {
    private FileWriter pose_writer_;
    private FileWriter gyro_writer_;
    private FileWriter acce_writer_;
    private FileWriter grav_writer_;
    private FileWriter linacce_writer_;

    private static final float mulNanoToSec = 1000000000;

    private final static String LOG_TAG = PoseIMURecorder.class.getName();

    MainActivity parent_;

    public PoseIMURecorder(String path, MainActivity parent){
        parent_ = parent;

        Calendar file_timestamp = Calendar.getInstance();
        String header = "# Created at " + file_timestamp.getTime().toString() + "\n";

        try {
            pose_writer_ = createFile(path + "/pose.txt", header);
            gyro_writer_ = createFile(path + "/gyro.txt", header);
            acce_writer_ = createFile(path + "/acce.txt", header);
            grav_writer_ = createFile(path + "/gravity.txt", header);
            linacce_writer_ = createFile(path + "/linacce.txt", header);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void endFiles(){
        try {
            pose_writer_.close();
            gyro_writer_.close();
            acce_writer_.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public Boolean addPoseRecord(TangoPoseData new_pose){
        StringBuilder builder = new StringBuilder();
        float[] translation = new_pose.getTranslationAsFloats();
        float[] rotation = new_pose.getRotationAsFloats();
        try {
            pose_writer_.write(String.format(Locale.US,
                    "%d %.6f %.6f %.6f %.6f %.6f %.6f %.6f\n", (long)(new_pose.timestamp * mulNanoToSec),
                    translation[0], translation[1], translation[2],
                    rotation[0], rotation[1], rotation[2], rotation[3]));
            pose_writer_.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    public Boolean addAcclerometerRecord(SensorEvent event){
        float[] values = event.values;
        long timestamp = event.timestamp;
        try {
            acce_writer_.write(String.format(Locale.US,"%d %.6f %.6f %.6f\n", timestamp, values[0], values[1], values[2]));
            acce_writer_.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    public Boolean addGravityRecord(SensorEvent event){
        float[] values = event.values;
        long timestamp = event.timestamp;
        try {
            grav_writer_.write(String.format(Locale.US,"%d %.6f %.6f %.6f\n", timestamp, values[0], values[1], values[2]));
            grav_writer_.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    public Boolean addLinerAccelerationRecord(SensorEvent event){
        float[] values = event.values;
        long timestamp = event.timestamp;
        try {
            linacce_writer_.write(String.format(Locale.US,"%d %.6f %.6f %.6f\n", timestamp, values[0], values[1], values[2]));
            linacce_writer_.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }


    public Boolean addGyroscopeRecord(SensorEvent event){
        float[] values = event.values;
        long timestamp = event.timestamp;

        try {
            gyro_writer_.write(String.format(Locale.US,"%d %.6f %.6f %.6f\n", timestamp, values[0], values[1], values[2]));
            gyro_writer_.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    private FileWriter createFile(String path, String header) throws IOException{
        File file = new File(path);
        FileWriter writer = new FileWriter(file);
        Intent scan_intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scan_intent.setData(Uri.fromFile(file));
        parent_.sendBroadcast(scan_intent);
        if(header != null && header.length() != 0) {
            writer.append(header);
        }
        return writer;
    }
}
