package com.gm375.vidshare;

import java.io.File;
import java.io.IOException;

import com.gm375.vidshare.util.Lollipop;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class VideoStream extends Activity implements View.OnClickListener, SurfaceHolder.Callback {
    
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
    
    public String prepareChunk() throws IOException {
        File chunkFile = File.createTempFile("vs-"+ hh.getSessionId() +"-"+ mCounter.getNext(), null);
        String filepath = chunkFile.getPath();
        mMediaRecorder = createMediaRecorder(filepath);
        return filepath;
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
        // TODO Auto-generated method stub
        
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        
    }
    
    public MediaRecorder createMediaRecorder(String filename) {
        MediaRecorder mr = new MediaRecorder();
        mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mr.setCamera(mCamera);
        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
        mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mr.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
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
        // TODO: Complete method.
    }
    
    private void stopStreamingVideo() {
        // TODO: Complete method.
    }

}
