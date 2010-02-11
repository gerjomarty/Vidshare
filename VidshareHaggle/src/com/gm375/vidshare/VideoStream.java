package com.gm375.vidshare;

import java.io.File;
import java.io.IOException;

import com.gm375.vidshare.util.Lollipop;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class VideoStream extends Activity implements View.OnClickListener, SurfaceHolder.Callback, MediaRecorder.OnInfoListener {
    
    private boolean mIsPreviewing = false;
    private VideoPreview mVideoPreview;
    private MediaRecorder mMediaRecorder;
    private int mSavedWidth, mSavedHeight;
    OrientationEventListener mOrientationListener;
    private android.hardware.Camera mCamera;
    private android.hardware.Camera.Parameters mParameters;
    private ImageView mShutterButton;
    private SurfaceHolder mSurfaceHolder = null;
    private Lollipop mCounter;
    private Vidshare vs = null;
    private org.haggle.Handle hh = null;
    
    int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    
    //File dir;
    //String filepath;
    
    private static final int VIDEO_FRAME_RATE = 15;
    private static final int MILLISECONDS_PER_CHUNK = 3000;
    
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_STREAMING_VIDEO = 1;
    private static final int STATUS_STOPPING_STREAM = 2;
    
    private int mStatus = STATUS_IDLE;
    private int seqNumber;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mCamera = android.hardware.Camera.open();
        // TODO: Find some way to quicken this up by shifting to another thread, while preserving locks.
        
        this.mOrientationListener = new OrientationEventListener(this) {
            public void onOrientationChanged(int orientation) {
                if (orientation != ORIENTATION_UNKNOWN) {
                    mLastOrientation = orientation;
                }
            }
        };
        
        vs = (Vidshare) getApplication();
        hh = vs.getHaggleHandle();
        
        if (hh == null) {
            Log.e(Vidshare.LOG_TAG, "*** Haggle handle was null. ***");
        }
        
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.video_record);
        
        mVideoPreview = (VideoPreview) findViewById(R.id.video_record_surface);
        
        SurfaceHolder holder = mVideoPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mShutterButton = (ImageView) findViewById(R.id.shutter_button);
        mShutterButton.setOnClickListener(this);
        
        mCounter = new Lollipop();
        
        
        
    }
    
    public void prepareChunk() throws IOException {
        File chunkFile = File.createTempFile("vs-"+ hh.getSessionId() +"-"+ mCounter.getNext(), null);
        mMediaRecorder = createMediaRecorder(chunkFile.getPath());
    }
    
    public void onStart() {
        super.onStart();
        
        makeSureCameraIsOpen();
    }
    
    public void onStop() {
        super.onStop();
        
        closeCamera();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.shutter_button:
            switch (mStatus) {
            case STATUS_IDLE:
                startStreamingVideo();
                break;
            case STATUS_STREAMING_VIDEO:
                stopStreamingVideo();
                break;
            }
            break;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // This is called immediately after any structural changes 
        // (format or size) have been made to the surface.
        startPreview(width, height, holder.isCreating());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        mSurfaceHolder = null;
    }
    
    public void startPreview(int w, int h, boolean start) {
        mVideoPreview.setVisibility(View.VISIBLE);
        
        if (mSavedWidth == w && mSavedHeight == h && mIsPreviewing)
            return;
        
        if (isFinishing())
            return;
        
        if (!start)
            return;
        
        if (mIsPreviewing)
            stopPreview();
        
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            // TODO: Add more to exception.
            mCamera.release();
            mCamera = null;
            return;
        }
        
        mParameters = mCamera.getParameters();
        mParameters.setPreviewSize(w, h);
        mSavedWidth = w;
        mSavedHeight = h;
        
        mCamera.setParameters(mParameters);
        
        try {
            mCamera.startPreview();
            mIsPreviewing = true;
        } catch (Exception e) {
            // TODO: Handle exception
        }
        
    }
    
    public void stopPreview() {
        if (mCamera == null || mIsPreviewing == false)
            return;
        
        mCamera.stopPreview();
        mIsPreviewing = false;
    }
    
    public void restartPreview() {
        startPreview(mSavedWidth, mSavedHeight, true);
    }
    
    public static int roundOrientation(int orientationInput) {
        int orientation = orientationInput;
        if (orientation == -1)
            orientation = 0;

        orientation = orientation % 360;
        int retVal;
        if (orientation < (0*90) + 45) {
            retVal = 0;
        } else if (orientation < (1*90) + 45) {
            retVal = 90;
        } else if (orientation < (2*90) + 45) {
            retVal = 180;
        } else if (orientation < (3*90) + 45) {
            retVal = 270;
        } else {
            retVal = 0;
        }

        return retVal;
    }
    
    public MediaRecorder createMediaRecorder(String filename) {
        MediaRecorder mr = new MediaRecorder();
        mr.setOnInfoListener(this);
        mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mr.setCamera(mCamera);
        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
        mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mr.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        mr.setMaxDuration(MILLISECONDS_PER_CHUNK);
        mr.setOutputFile(filename);
        mr.setVideoSize(mSavedWidth, mSavedHeight);
        mr.setVideoFrameRate(VIDEO_FRAME_RATE);
        mr.setPreviewDisplay(mSurfaceHolder.getSurface());
        return mr;
    }
    
    private void makeSureCameraIsOpen() {
        if (mCamera == null)
            mCamera = android.hardware.Camera.open();
    }
    
    private void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
    
    private void startStreamingVideo() {
        
        stopPreview();
        
        try {
            prepareChunk();
            mMediaRecorder.prepare();
            mMediaRecorder.start();            
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: Handle exception.
        }
        
    }
    
    private void stopStreamingVideo() {
        // TODO: Complete method.
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            switch (mStatus) {
            case STATUS_STREAMING_VIDEO:
                stopStreamingVideo();
                break;
            }
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
        case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
            // TODO: 11/02/2010 CONTINUE HERE
            // Find some way of getting the filename down to this callback (make it global to class?)
            // and spawn a thread to publish chunkFile to Haggle. Do stuff with numbering and attributes too.
            break;
        }
    }

}
