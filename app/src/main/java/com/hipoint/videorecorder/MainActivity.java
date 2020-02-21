package com.hipoint.videorecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements Callback, GestureDetector.OnGestureListener {
    private ImageButton mRecordImageButton;
    private ImageButton mStillImageButton;
    private boolean mIsRecording = false;
    private boolean mIsTimelapse = false;
    Chronometer mChronometer;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private File mVideoFolder;
    private String mVideoFileName;
    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    public MediaRecorder mrec = new MediaRecorder();
    private Camera mCamera;
    private GestureDetector gDetector;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChronometer = findViewById(R.id.chronometer);
        createVideoFolder();
        surfaceView =  findViewById(R.id.textureView);
        mRecordImageButton =  findViewById(R.id.videoOnlineImageButton);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        gDetector = new GestureDetector( getBaseContext(), this );
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mRecordImageButton.setOnClickListener(view1->{
            if(mIsRecording){

                stopRecording();
            }else {
                try {
                    startRecording();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    protected void startRecording() throws IOException
    {   mIsRecording=true;
        mIsTimelapse=true;
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.setVisibility(View.VISIBLE);
        mChronometer.start();
        mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
        if(mCamera==null)
            mCamera = Camera.open();
        String filename;
        String path;
        path= Environment.getExternalStorageDirectory()+"/AVDVideo/";
        filename=createVideoFileName();
        mrec = new MediaRecorder();
        mCamera.lock();
        mCamera.unlock();


        // Please maintain sequence of following code.

        // If you change sequence it will not work.
        mrec.setCamera(mCamera);
        mrec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
        mrec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mrec.setVideoEncodingBitRate(1000000);
        mrec.setVideoFrameRate(30);
        mrec.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mrec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mrec.setPreviewDisplay(surfaceHolder.getSurface());
        mrec.setOutputFile(path+filename);
        mrec.prepare();
        mrec.start();


    }

    protected void stopRecording() {
        mChronometer.stop();
        mChronometer.setVisibility(View.INVISIBLE);
        mRecordImageButton.setImageResource(R.mipmap.btn_video_online);
        mIsRecording=false;
        mIsTimelapse=false;
        if(mrec!=null)
        {
            mrec.stop();
            mrec.release();
            mCamera.stopPreview();
            /*mCamera.release();
            mCamera.lock();*/
        }
        try {
            startRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseMediaRecorder() {

        if (mrec != null) {
            mrec.reset(); // clear recorder configuration
            mrec.release(); // release the recorder object
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.setDisplayOrientation(0);
        mCamera.autoFocus(null);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (mCamera != null) {
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }else {
                Camera.Parameters params = mCamera.getParameters();
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(params);
                mCamera.setDisplayOrientation(0);
                mCamera = Camera.open();
                Log.i("Surface", "Created");
            }

        }
        else {
                mCamera=Camera.open();
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }else {
                Camera.Parameters params = mCamera.getParameters();
                mCamera.setParameters(params);
                mCamera.setDisplayOrientation(90);
                mCamera = Camera.open();
                Log.i("Surface", "Created");
            }
           /* Toast.makeText(getApplicationContext(), "Camera not available!",
                    Toast.LENGTH_LONG).show();

            finish();*/
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }

    }
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        Toast.makeText(getApplicationContext(),"Swiped LEFT TO RIGHT!",Toast.LENGTH_SHORT).show();
                        Log.e( "down", "Swiped LEFT TO RIGHT!" );
                        //startRecording();


                    } else {
                        Toast.makeText(getApplicationContext(),"Swiped RIGHT TO LEFT!",Toast.LENGTH_SHORT).show();
                        Log.e( "down", "Swiped RIGHT TO LEFT!" );
                        stopRecording();
                    }
                }
                result = true;
            }
            else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    Toast.makeText(getApplicationContext(),"Swiped UP TO DOWN!",Toast.LENGTH_SHORT).show();
                    Log.e( "down", "Swiped UP TO DOWN!" );
                } else {
                    Toast.makeText(getApplicationContext(),"Swiped DOWN TO Up!",Toast.LENGTH_SHORT).show();
                    Log.e( "up", "Swiped DOWN TO Up!" );
                }
            }
            result = true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    private void createVideoFolder() {
        File movieFile = Environment.getExternalStorageDirectory();
        mVideoFolder = new File(movieFile, "AVDVideo");
        if(!mVideoFolder.exists()) {
            mVideoFolder.mkdirs();
        }
    }

    private String createVideoFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + ".mp4";
        return prepend;
    }

  /*  @Override
    protected void onDestroy() {
        mCamera.stopPreview();
        mCamera.release();
        mrec.release();
        mCamera = null;
        super.onDestroy();
    }*/

}