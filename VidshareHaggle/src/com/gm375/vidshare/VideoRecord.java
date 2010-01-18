package com.gm375.vidshare;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.graphics.PixelFormat;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class VideoRecord extends Activity implements View.OnClickListener, SurfaceHolder.Callback {
    
    private boolean isPreviewing = false;
    private VideoPreview mVideoPreview;
    private MediaRecorder mMediaRecorder;
    private int mSavedWidth, mSavedHeight;
    private android.hardware.Camera mCamera;
    private android.hardware.Camera.Parameters mParameters;
    private ImageView mShutterButton;
    private SurfaceHolder mSurfaceHolder = null;
    int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_RECORDING_VIDEO = 1;
    private int mStatus = STATUS_IDLE;
    static final int SAVE_VIDEO_PROGRESS_DIALOG = 0;
    ProgressDialog progressDialog;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Throw camera opening out to another thread, as it's slow.
        Thread openCameraThread = new Thread(new Runnable() {
            public void run() {
                mCamera = android.hardware.Camera.open();
            }
        });
        openCameraThread.start();
        
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.video_record);
        
        mVideoPreview = (VideoPreview) findViewById(R.id.video_record_surface);
        
        SurfaceHolder holder = mVideoPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mShutterButton = (ImageView) findViewById(R.id.shutter_button);
        mShutterButton.setOnClickListener(this);
        
        mMediaRecorder = new MediaRecorder();
        
        try {
            openCameraThread.join();
        } catch (InterruptedException e) {
            // TODO: Handle exception
        }
        
    }
    
    public void onStart() {
        super.onStart();
        
        makeSureCameraIsOpen();
    }
    
    public void onStop() {
        super.onStop();
        
        closeCamera();
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
    
    public void startPreview(int w, int h, boolean start) {
        mVideoPreview.setVisibility(View.VISIBLE);
        
        if (mSavedWidth == w && mSavedHeight == h && isPreviewing)
            return;
        
        if (isFinishing())
            return;
        
        if (!start)
            return;
        
        if (isPreviewing)
            stopPreview();
        
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            // TODO: Add more to exception.
            mCamera.release();
            mCamera = null;
        }
        
        mParameters = mCamera.getParameters();
        mParameters.setPreviewSize(w, h);
        mSavedWidth = w;
        mSavedHeight = h;
        //mParameters.setPreviewFormat(PixelFormat.YCbCr_422_SP);
        
        mCamera.setParameters(mParameters);
        
        try {
            mCamera.startPreview();
            isPreviewing = true;
        } catch (Exception e) {
            // TODO: Handle exception
        }
        
    }
    
    public void stopPreview() {
        if (mCamera == null || isPreviewing == false)
            return;
        
        mCamera.stopPreview();
        isPreviewing = false;
    }
    
    
    public void restartPreview() {
        startPreview(mSavedWidth, mSavedHeight, true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.shutter_button:
            Log.d(Vidshare.LOG_TAG, "***Clicked on shutter button***");
            if (mStatus == STATUS_IDLE) {
                startRecordingVideo();                
            } else if (mStatus == STATUS_RECORDING_VIDEO) {
                //stopRecordingVideo();
            } else {
                // nothing
            }
            break;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
    
    
    private final class ShutterCallback implements android.hardware.Camera.ShutterCallback {
        public void onShutter() {

            mVideoPreview.setVisibility(View.INVISIBLE);
            // Resize the SurfaceView to the aspect-ratio of the still image
            // and so that we can see the full image that was taken.
            Size pictureSize = mParameters.getPictureSize();
            mVideoPreview.setAspectRatio(pictureSize.width, pictureSize.height);
        }
    };
    
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case SAVE_VIDEO_PROGRESS_DIALOG:
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Saving video...");
            return progressDialog;
        default:
            return null;
        }
    }
    
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            //progressDialog.setProgress(total);
            dismissDialog(SAVE_VIDEO_PROGRESS_DIALOG);
        }
    };
    
    private final class AutoFocusCallback implements android.hardware.Camera.AutoFocusCallback {
        public void onAutoFocus(boolean focused, android.hardware.Camera camera) {

        }
    };
    
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
    
    public void startRecordingVideo() {
        stopPreview();
        mStatus = STATUS_RECORDING_VIDEO;
        final int latchedOrientation = roundOrientation(mLastOrientation + 90);
        
        // Quality 75 has visible artifacts, and quality 90 looks great but the files begin to
        // get large. 85 is a good compromise between the two.
        mParameters.set("jpeg-quality", 85);
        mParameters.set("rotation", latchedOrientation);
        mCamera.setParameters(mParameters);
        
        //start recording video
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        
        File dir = new File("/sdcard/Vidshare");
        final String filename = "vidshare-"+ System.currentTimeMillis() +".3gp";
        
        dir.mkdirs();
        
        if (dir.canWrite() == false)
            dir = getFilesDir();
        
        final String filepath = dir +"/"+ filename;
        
        mMediaRecorder.setOutputFile(filepath);
        mMediaRecorder.setVideoSize(mSavedWidth, mSavedHeight); // TODO: Size???
        mMediaRecorder.setVideoFrameRate(15); // TODO: Framerate???
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        
        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: Handle exception
        }
        
        mMediaRecorder.start();
        
    }
    
    public void stopRecordingVideo() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mStatus = STATUS_IDLE;
        restartPreview();
    }
    
}
