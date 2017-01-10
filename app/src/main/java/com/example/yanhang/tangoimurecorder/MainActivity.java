package com.example.yanhang.tangoimurecorder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.UxExceptionEvent;
import com.google.atap.tango.ux.UxExceptionEventListener;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.TangoUx.StartParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.rajawali3d.view.SurfaceView;
import org.rajawali3d.scene.ASceneFrameCallback;


public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();
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
    private SurfaceView mSurfaceView;

    private boolean mIsConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTangoUx = setupTangoUx();
        mSurfaceView = (SurfaceView) findViewById(R.id.gl_surface_view);
        mRenderer = new MotionRajawaliRenderer(this);
        setupRenderer();
    }

    @Override
    protected void onPause(){
        super.onPause();

        synchronized (this){
            try{
                mTangoUx.stop();
                mTango.disconnect();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        mTangoUx.start(new StartParams());

        // initialize tango service
        mTango = new Tango(MainActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (MainActivity.this){
                    mTangoConfig = setupTangoConfig(mTango);
                    mTango.connect(mTangoConfig);
                    startupTango();
                }
            }
        });

        //initialize recorder
        try{
            String output_dir = setupOutputFolder();
            mRecorder = new PoseIMURecorder(output_dir, this);
        }catch (FileNotFoundException e){
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.alert_title)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
            e.printStackTrace();
        }
    }

    private TangoConfig setupTangoConfig(Tango tango){
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);

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
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                synchronized (MainActivity.this){
                    // Don't execute any tango API actions if we're not connected to the service
                    if (!mIsConnected){
                        return;
                    }

                    // Update current camera pose
                    try{
                        TangoPoseData lastFramePose = mTango.getPoseAtTime(0,
                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                TangoPoseData.COORDINATE_FRAME_DEVICE,)
                    }catch (TangoErrorException e){
                        Log.e(LOG_TAG, "Could not get valid transform");
                    }


                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }
        });
    }


    private void startupTango(){
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE
        ));

        mTango.connectListener(framePairs, new OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData tangoPoseData) {
                if(mTangoUx != null){
                    mTangoUx.updatePoseStatus(tangoPoseData.statusCode);
                }
                mRecorder.addPoseRecord(tangoPoseData);
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData tangoXyzIjData) {
            }

            @Override
            public void onFrameAvailable(int i) {
            }

            @Override
            public void onTangoEvent(TangoEvent tangoEvent) {
                if(mTangoUx != null){
                    mTangoUx.updateTangoEvent(tangoEvent);
                }
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData tangoPointCloudData) {
                if(mTangoUx != null){
                    mTangoUx.updatePointCloud(tangoPointCloudData);
                }
            }
        });
    }

}
