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
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;

public class PoseIMURecorder {
    private FileWriter pose_writer_;
    private FileWriter gyro_writer_;
    private FileWriter acce_writer_;

    private final static String LOG_TAG = PoseIMURecorder.class.getName();

    MainActivity parent_;

    public PoseIMURecorder(String path, MainActivity parent){
        parent_ = parent;

        Calendar file_timestamp = Calendar.getInstance();
        // pose file
        File pose_file = new File(path + "/pose.txt");
        Intent pose_scan_intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        pose_scan_intent.setData(Uri.fromFile(pose_file));
        parent_.sendBroadcast(pose_scan_intent);

        try{
            pose_writer_ = new FileWriter(pose_file);
            pose_writer_.append("#Created at " + file_timestamp.getTime().toString() + '\n');
            pose_writer_.flush();
        }catch (Exception e){
            e.printStackTrace();
        }

        File gyro_file = new File(path + "/gyro.txt");
        Intent gyro_scan_intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        gyro_scan_intent.setData(Uri.fromFile(gyro_file));
        parent_.sendBroadcast(gyro_scan_intent);
        try{
            gyro_writer_ = new FileWriter(gyro_file);
            gyro_writer_.append("#Created at " + file_timestamp.getTime().toString() + '\n');
        }catch (Exception e){
            e.printStackTrace();
        }

        File acce_file = new File(path + "/acce.txt");
        Intent acce_scan_intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        acce_scan_intent.setData(Uri.fromFile(acce_file));
        parent_.sendBroadcast(acce_scan_intent);
        try{
            acce_writer_ = new FileWriter(acce_file);
            acce_writer_.append("#Created at " + file_timestamp.getTime().toString() + '\n');
        }catch (Exception e){
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
        builder.append(new_pose.timestamp + " " + rotation[0] + " " + rotation[1] + " " + rotation[2] + " ");
        builder.append(translation[0] + " " + translation[1] + " " + translation[2] + "\n");
        try {
            pose_writer_.write(builder.toString());
            pose_writer_.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    public Boolean addIMURecord(SensorEvent event){
        return true;
    }

}
