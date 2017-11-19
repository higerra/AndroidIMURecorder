package com.example.yanhang.androidimurecorder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;

import android.hardware.SensorEventListener;
import android.hardware.display.DisplayManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int REQUEST_CODE_WRITE_EXTERNAL = 1001;

    private final Handler mUIHandler = new Handler(Looper.getMainLooper());

    private PoseIMURecorder mRecorder;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mGravity;
    private Sensor mLinearAcce;
    private Sensor mOrientation;
    private Sensor mMagnetometer;
    private Sensor mStepCounter;

    private float mInitialStepCount = -1;

    // Gyroscope
    private TextView mLabelRx;
    private TextView mLabelRy;
    private TextView mLabelRz;
    // Accelerometer
    private TextView mLabelAx;
    private TextView mLabelAy;
    private TextView mLabelAz;
    // Linear acceleration
    private TextView mLabelLx;
    private TextView mLabelLy;
    private TextView mLabelLz;
    // Gravity
    private TextView mLabelGx;
    private TextView mLabelGy;
    private TextView mLabelGz;

    // Magnetometer
    private TextView mLabelMx;
    private TextView mLabelMy;
    private TextView mLabelMz;


    private Button mStartStopButton;
    private ToggleButton mToggleFileButton;
//    private GLSurfaceView mVideoSurfaceView;
//    private HelloVideoRenderer mVideoRenderer;

    private int mCameraToDisplayRotation = 0;

    private Boolean mIsRecordingPose = true;
    private Boolean mIsWriteFile = true;
    private Boolean mCanWriteToExternal = false;

    private AtomicBoolean mIsConnected = new AtomicBoolean(false);
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);


//    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
//    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mVideoSurfaceView = (GLSurfaceView) findViewById(R.id.video_surface_view);
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if(displayManager != null){
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {

                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    synchronized (this){
                        setAndroidOrientation();
                    }
                }

                @Override
                public void onDisplayChanged(int displayId) {

                }
            }, null);
        }

        // initialize IMU sensor
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAcce = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        mLabelRx = (TextView)findViewById(R.id.label_rx);
        mLabelRy = (TextView)findViewById(R.id.label_ry);
        mLabelRz = (TextView)findViewById(R.id.label_rz);
        mLabelAx = (TextView)findViewById(R.id.label_ax);
        mLabelAy = (TextView)findViewById(R.id.label_ay);
        mLabelAz = (TextView)findViewById(R.id.label_az);
        mLabelLx = (TextView)findViewById(R.id.label_lx);
        mLabelLy = (TextView)findViewById(R.id.label_ly);
        mLabelLz = (TextView)findViewById(R.id.label_lz);
        mLabelGx = (TextView)findViewById(R.id.label_gx);
        mLabelGy = (TextView)findViewById(R.id.label_gy);
        mLabelGz = (TextView)findViewById(R.id.label_gz);
        mStartStopButton = (Button)findViewById(R.id.button_start_stop);
        mToggleFileButton = (ToggleButton)findViewById(R.id.toggle_file);


    }

    @Override
    protected void onPause(){
        super.onPause();
        stopRecording();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mGyroscope);
        mSensorManager.unregisterListener(this, mGravity);
        mSensorManager.unregisterListener(this, mLinearAcce);
        mSensorManager.unregisterListener(this, mOrientation);
        mSensorManager.unregisterListener(this, mMagnetometer);
        mSensorManager.unregisterListener(this, mStepCounter);
    }

    @Override
    protected void onResume(){
        super.onResume();
        mStartStopButton.setText(R.string.start_title);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLinearAcce, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST);

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL);
        }else{
            mCanWriteToExternal = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode){
            case REQUEST_CODE_WRITE_EXTERNAL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCanWriteToExternal = true;
                }
        }
    }
    private void startNewRecording(){
        if(!mCanWriteToExternal){
            // request permission
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission not granted")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopRecording();
                        }
                    }).show();
            return;
        }
        //initialize recorder
        if(mIsWriteFile) {
            try {
                String output_dir = setupOutputFolder();
                mRecorder = new PoseIMURecorder(output_dir, this);
                // prevent screen lock
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } catch (FileNotFoundException e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.alert_title)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopRecording();
                            }
                        }).show();
                e.printStackTrace();
            }
        }

        mToggleFileButton.setEnabled(false);
        mInitialStepCount = -1.0f;
        mIsRecording.set(true);
    }

    private void stopRecording(){
        mIsRecording.set(false);
        if(mRecorder != null) {
            mRecorder.endFiles();
        }
        mToggleFileButton.setEnabled(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void startStopRecording(View view){
        if(!mIsRecording.get()){
            startNewRecording();
            mStartStopButton.setText(R.string.stop_title);
        }else{
            stopRecording();
            mStartStopButton.setText(R.string.start_title);
        }
    }

    public void toogleFileWriting(View view) {
        mIsWriteFile = mToggleFileButton.isChecked();
    }

    private String setupOutputFolder() throws FileNotFoundException{
        Calendar current_time = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss", Locale.US);
        File output_dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                formatter.format(current_time.getTime()));
        Log.i(LOG_TAG, output_dir.getAbsolutePath());
        if(!output_dir.exists()) {
            if (!output_dir.mkdirs()) {
                Log.e(LOG_TAG, "Can not create output directory");
                throw new FileNotFoundException();
            }
        }
        Log.i(LOG_TAG, "Output directory: " + output_dir.getAbsolutePath());
        return output_dir.getAbsolutePath();
    }

    private void setAndroidOrientation(){
        Display display = getWindowManager().getDefaultDisplay();
        Camera.CameraInfo depthCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(1, depthCameraInfo);

        int depthCameraRotation = Surface.ROTATION_0;
        switch(depthCameraInfo.orientation){
            case 90:
                depthCameraRotation = Surface.ROTATION_90;
                break;
            case 180:
                depthCameraRotation = Surface.ROTATION_180;
                break;
            case 270:
                depthCameraRotation = Surface.ROTATION_270;
                break;
        }

        mCameraToDisplayRotation = display.getRotation() - depthCameraRotation;
        if(mCameraToDisplayRotation < 0){
            mCameraToDisplayRotation += 4;
        }
    }


    // receive IMU data
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void onSensorChanged(final SensorEvent event){
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLabelAx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelAy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelAz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                mRecorder.addIMURecord(event, PoseIMURecorder.ACCELEROMETER);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLabelRx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelRy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelRz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                mRecorder.addIMURecord(event, PoseIMURecorder.GYROSCOPE);
            }
        }
        else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLabelLx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelLy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelLz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                mRecorder.addIMURecord(event, PoseIMURecorder.LINEAR_ACCELERATION);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLabelGx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelGy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelGz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                mRecorder.addIMURecord(event, PoseIMURecorder.GRAVITY);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            if(mIsRecording.get() && mIsWriteFile){
                mRecorder.addIMURecord(event, PoseIMURecorder.MAGNETOMETER);
            }
        }
        else if(event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR){
            if(mIsRecording.get() && mIsWriteFile){
                mRecorder.addIMURecord(event, PoseIMURecorder.ROTATION_VECTOR);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (mIsRecording.get()) {
                if (mInitialStepCount < 0) {
                    mInitialStepCount = event.values[0] - 1;
                }
                if (mIsRecording.get() && mIsWriteFile) {
                    mRecorder.addStepRecord(event.timestamp,
                            (int) (event.values[0] - mInitialStepCount));
                }
            }
        }
    }
}
