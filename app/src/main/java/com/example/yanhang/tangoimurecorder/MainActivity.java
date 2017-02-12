package com.example.yanhang.tangoimurecorder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;

import android.hardware.SensorEventListener;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.UxExceptionEvent;
import com.google.atap.tango.ux.UxExceptionEventListener;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoXyzIjData;

import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUx.StartParams;

import com.projecttango.tangosupport.TangoSupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.rajawali3d.surface.RajawaliSurfaceView;
import org.rajawali3d.scene.ASceneFrameCallback;


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String LOG_TAG = MainActivity.class.getName();
//    private static final int INVALID_TEXTURE_ID = 0;
//    private static final int COLOR_CAMERA_ID = 0;
//    private static final int FISHEYE_CAMERA_ID = 2;

    private final Handler mUIHandler = new Handler(Looper.getMainLooper());

    private Tango mTango;
    private TangoConfig mTangoConfig;
    private TangoUx mTangoUx;
    private UxExceptionEventListener mUxExceptionEventListener = new UxExceptionEventListener() {
        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(LOG_TAG, "Device lying on surface ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(LOG_TAG, "Very few depth points in mPoint cloud ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(LOG_TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_INCOMPATIBLE_VM) {
                Log.i(LOG_TAG, "Device not running on ART");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(LOG_TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(LOG_TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_OVER_EXPOSED) {
                Log.i(LOG_TAG, "Fisheye Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_UNDER_EXPOSED) {
                Log.i(LOG_TAG, "Fisheye Camera Under Exposed ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_TANGO_SERVICE_NOT_RESPONDING) {
                Log.i(LOG_TAG, "TangoService is not responding ");
            }

        }
    };

    private PoseIMURecorder mRecorder;
    private MotionRajawaliRenderer mRenderer;
    private org.rajawali3d.surface.RajawaliSurfaceView mSurfaceView;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mGravity;
    private Sensor mLinearAcce;
    private Sensor mOrientation;

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
    // Orientation
    private TextView mLabelOx;
    private TextView mLabelOy;
    private TextView mLabelOz;


    private Button mStartStopButton;
    private ToggleButton mTogglePoseButton;
    private ToggleButton mToggleFileButton;
//    private GLSurfaceView mVideoSurfaceView;
//    private HelloVideoRenderer mVideoRenderer;

    private int mCameraToDisplayRotation = 0;

    private Boolean mIsRecordingPose = true;
    private Boolean mIsWriteFile = true;
    private AtomicBoolean mIsConnected = new AtomicBoolean(false);
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);


//    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
//    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTangoUx = setupTangoUx();
        mSurfaceView = (RajawaliSurfaceView) findViewById(R.id.gl_surface_view);
        mRenderer = new MotionRajawaliRenderer(this);

        //mVideoSurfaceView = (GLSurfaceView) findViewById(R.id.video_surface_view);

        setupRenderer();

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
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

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
        mLabelOx = (TextView)findViewById(R.id.label_ox);
        mLabelOy = (TextView)findViewById(R.id.label_oy);
        mLabelOz = (TextView)findViewById(R.id.label_oz);
        mStartStopButton = (Button)findViewById(R.id.button_start_stop);
        mTogglePoseButton = (ToggleButton)findViewById(R.id.toggle_pose);
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
    }

    private void startNewRecording(){
        mTogglePoseButton.setEnabled(false);
        mToggleFileButton.setEnabled(false);
        if(mIsRecordingPose) {
            mTangoUx.start(new StartParams());
            // initialize tango service
            mTango = new Tango(MainActivity.this, new Runnable() {
                @Override
                public void run() {
                    synchronized (MainActivity.this) {
                        TangoSupport.initialize();
                        mTangoConfig = setupTangoConfig(mTango);
                        mTango.connect(mTangoConfig);
                        startupTango();
                        mIsConnected.set(true);
                    }
                }
            });
        }

        //initialize recorder
        if(mIsWriteFile) {
            try {
                String output_dir = setupOutputFolder();
                mRecorder = new PoseIMURecorder(output_dir, this);
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
        mIsRecording.set(true);
        // prevent screen lock
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void stopRecording(){
        mIsRecording.set(false);
        if(mRecorder != null) {
            mRecorder.endFiles();
        }
        if (mIsRecordingPose) {
            synchronized (this) {
                try {
                    mTangoUx.stop();
                    mTango.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mIsConnected.set(false);
        }
        mTogglePoseButton.setEnabled(true);
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

    public void tooglePoseRecording(View view){
        mIsRecordingPose = mTogglePoseButton.isChecked();
    }
    public void toogleFileWriting(View view) {
        mIsWriteFile = mToggleFileButton.isChecked();
    }

    private TangoConfig setupTangoConfig(Tango tango){
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_HIGH_RATE_POSE, true);
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);

        return config;
    }

    private TangoUx setupTangoUx(){
        TangoUx tangoUx = new TangoUx(this);
        tangoUx.setUxExceptionEventListener(mUxExceptionEventListener);
        TangoUxLayout uxLayout = (TangoUxLayout) findViewById(R.id.layout_tango);
        tangoUx.setLayout(uxLayout);
        return tangoUx;
    }

    private String setupOutputFolder() throws FileNotFoundException{
        Calendar current_time = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        File external_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File output_dir = new File(external_dir.getAbsolutePath() + "/" + formatter.format(current_time.getTime()));
        if(!output_dir.exists()) {
            if (!output_dir.mkdir()) {
                Log.e(LOG_TAG, "Can not create output directory");
                throw new FileNotFoundException();
            }
        }
        Log.i(LOG_TAG, "Output directory: " + output_dir.getAbsolutePath());
        return output_dir.getAbsolutePath();
    }

    private void setupRenderer(){
        // motion renderer
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                synchronized (MainActivity.this){
                    // Don't execute any tango API actions if we're not connected to the service
                    if (!mIsConnected.get()){
                        return;
                    }

                    // Update current camera pose
                    try{
                        TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(0,
                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                TangoPoseData.COORDINATE_FRAME_DEVICE,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL, mCameraToDisplayRotation);
                        mRenderer.updateCameraPose(lastFramePose);
                    }catch (TangoErrorException e){
                        Log.e(LOG_TAG, "Could not get valid transform");
                    }
                }
            }

            @Override
            public boolean callPreFrame(){
                return true;
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }
        });

        mSurfaceView.setSurfaceRenderer(mRenderer);

//        // camera renderer
//        mVideoSurfaceView.setEGLContextClientVersion(2);
//        mVideoRenderer = new HelloVideoRenderer(new HelloVideoRenderer.RenderCallback() {
//            @Override
//            public void preRender() {
//                if(!mIsConnected.get()){
//                    return;
//                }
//
//                try{
//                    synchronized (MainActivity.this) {
//                        if (mConnectedTextureIdGlThread == INVALID_TEXTURE_ID) {
//                            mConnectedTextureIdGlThread = mVideoRenderer.getTextureId();
//                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, mVideoRenderer.getTextureId());
//                        }
//
//                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
//                            mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
//                        }
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        });
//        mVideoSurfaceView.setRenderer(mVideoRenderer);

    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        mRenderer.onTouchEvent(motionEvent);
        return true;
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

    private void startupTango(){
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE
        ));

        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(TangoPoseData tangoPoseData) {
                if(mTangoUx != null){
                    mTangoUx.updatePoseStatus(tangoPoseData.statusCode);
                }
                if(mIsRecording.get() && mIsWriteFile) {
                    mRecorder.addPoseRecord(tangoPoseData);
                }
            }

            @Override
            public void onTangoEvent(TangoEvent tangoEvent) {
                if(mTangoUx != null){
                    mTangoUx.updateTangoEvent(tangoEvent);
                }
            }
        });
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
                mRecorder.addAcclerometerRecord(event);
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
                mRecorder.addGyroscopeRecord(event);
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
                mRecorder.addLinerAccelerationRecord(event);
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
                mRecorder.addGravityRecord(event);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLabelOx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelOy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelOz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                mRecorder.addOrientationRecord(event);
            }
        }
    }
}
