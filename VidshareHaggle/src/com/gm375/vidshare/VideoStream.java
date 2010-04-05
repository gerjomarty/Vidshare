package com.gm375.vidshare;

import java.io.File;
import java.io.IOException;

import org.haggle.DataObject;
import org.haggle.DataObject.DataObjectException;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
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

import com.gm375.vidshare.util.Counter;

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
    private Counter mCounter;
    private Vidshare vs = null;
    private String[] attributes;
    private long startTime;
    private String androidId;
    
    private JpegPictureCallback mJpegCallback = new JpegPictureCallback();
    private byte[] thumbnailData;
    
    int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    
    private static final int VIDEO_FRAME_RATE = 15;
    private static final int MILLISECONDS_PER_CHUNK = 3000;
    
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_STREAMING_VIDEO = 1;
    private static final int STATUS_STOPPING_STREAM = 2;
    private static final int STATUS_STREAM_STOPPED = 3;
    
    private volatile int mStatus = STATUS_IDLE;
    private volatile boolean isFinishedTakingThumbnail = false;
    private int seqNumber;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** onCreate() ***");
        
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
        vs.setVideoStream(this);
        
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.video_record);
        
        mVideoPreview = (VideoPreview) findViewById(R.id.video_record_surface);
        
        SurfaceHolder holder = mVideoPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mShutterButton = (ImageView) findViewById(R.id.shutter_button);
        mShutterButton.setOnClickListener(this);
        
        mCounter = new Counter();
        
        attributes = getIntent().getStringArrayExtra("attributes");
        
        startTime = System.currentTimeMillis();
        
        /*
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
         */
        androidId = android.provider.Settings.Secure.getString(getContentResolver(), 
                android.provider.Settings.Secure.ANDROID_ID);
        
        
    }
    
    public void onStart() {
        super.onStart();
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** onStart() ***");
        
        makeSureCameraIsOpen();
    }
    
    public void onStop() {
        super.onStop();
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** onStop() ***");
        
        closeCamera();
        vs.setVideoStream(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.shutter_button:
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Shutter button pressed! ***");
            switch (mStatus) {
            case STATUS_IDLE:
                Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Idle on shutter ***");
                Thread startStreamThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startStreamingVideo();                        
                    }
                });
                startStreamThread.start();
                break;
            case STATUS_STREAMING_VIDEO:
                Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Streaming on shutter ***");
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
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Surface changed! ***");
        startPreview(width, height, holder.isCreating());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Surface created! ***");
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Surface destroyed! ***");
        stopPreview();
        mSurfaceHolder = null;
    }
    
    private final class JpegPictureCallback implements PictureCallback {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
                thumbnailData = new byte[data.length];
                thumbnailData = data.clone();
                isFinishedTakingThumbnail = true;
        }
        
    }
    
    public void startPreview(int w, int h, boolean start) {
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** startPreview() ***");
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
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** IOException for setting preview display ***");
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
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** stopPreview() ***");
        if (mCamera == null || mIsPreviewing == false)
            return;
        
        mCamera.stopPreview();
        mIsPreviewing = false;
    }
    
    public void restartPreview() {
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** restartPreview() ***");
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
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Creating media recorder ***");
        MediaRecorder mr = new MediaRecorder();
        mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //mr.setCamera(mCamera);
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
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** startStreamingVideo() ***");
        Thread publishDObjThread = null;
        
        //stopPreview();
        
        // Take photo for thumbnail image.
        /*
        Camera.Parameters oldParams = mCamera.getParameters();
        Camera.Parameters thumbParams = mCamera.getParameters();
        
        final int latchedOrientation = roundOrientation(mLastOrientation + 90);
        thumbParams.set("picture-size", "80x60");
        thumbParams.set("jpeg-quality", 75);
        thumbParams.set("rotation", latchedOrientation);
        mCamera.takePicture(null, null, mJpegCallback);
        
        mCamera.setParameters(oldParams);
        */
        closeCamera();
        // TODO: See if we can remove this closeCamera() call without things breaking.
        
        mStatus = STATUS_STREAMING_VIDEO;
        
        while (mStatus == STATUS_STREAMING_VIDEO) {
            final int seqNumber = mCounter.getNext();
            final String filepath = recordChunk(seqNumber);
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Streaming sequence "+ seqNumber +" with filepath "+ filepath +" ***");
            
            publishDObjThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DataObject dObj = new DataObject(filepath);
                        synchronized(attributes) {
                            for (int i = 0; i < attributes.length; i++) {
                                dObj.addAttribute("tag", attributes[i], 1);
                                Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Attribue added: "+ attributes[i] +" ***");
                            }
                        }
                        dObj.addAttribute("seqNumber", String.valueOf(seqNumber), 1);
                        dObj.addAttribute("id", androidId+startTime, 1);
                        dObj.addAttribute("isLast", "false", 1);
                        /*
                        while (!isFinishedTakingThumbnail) {
                            // Busy wait.
                        }
                        dObj.setThumbnail(thumbnailData);
                        */
                        // TODO: Add more attributes here.
                        dObj.addHash();
                        if (dObj == null)
                            Log.d(Vidshare.LOG_TAG, "*** DOBJ WAS NULL!!! ***");
                        synchronized(vs) {
                            Log.d(Vidshare.LOG_TAG, "*** Publishing data object ***");
                            int ret = vs.getHaggleHandle().publishDataObject(dObj);
                            if (ret == -1) {
                                Log.e(Vidshare.LOG_TAG, "***!!! Data Object returned error code. !!!***");
                            }
                            Log.d(Vidshare.LOG_TAG, "*** Publish return code: "+ ret +" ***");
                        }
                    } catch (DataObjectException e) {
                        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** DataObjectException for sequence "+ seqNumber +" ***");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            
            publishDObjThread.start();
        }
        
        try {
            publishDObjThread.join();
            // Ensures streaming has completely stopped before dealing with next case.
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Last publish thread successfully joined ***");
            // Send last DataObject with indication the stream is ending.
            
            try {
                DataObject dObj = new DataObject();
                synchronized(attributes) {
                    for (int i = 0; i < attributes.length; i++) {
                        dObj.addAttribute("tag", attributes[i], 1);
                    }                
                }
                dObj.addAttribute("seqNumber", String.valueOf(mCounter.getNext()), 1);
                dObj.addAttribute("id", androidId+startTime, 1);
                dObj.addAttribute("isLast", "true", 1);
                dObj.addHash();
                if (dObj == null)
                    Log.d(Vidshare.LOG_TAG, "*** SENTINEL DOBJ WAS NULL!!! ***");
                synchronized(vs) {
                    Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Publishing sentinel final data object ***");
                    int ret = vs.getHaggleHandle().publishDataObject(dObj);
                    if (ret == -1) {
                        Log.e(Vidshare.LOG_TAG, "***!!! Data Object returned error code. !!!***");
                    }
                    Log.d(Vidshare.LOG_TAG, "*** Last object published return code: "+ ret +" ***");
                }
            } catch (DataObjectException e) {
                Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Data Object Exception ***");
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            
            synchronized(this) {
                // Notify the stopStreamingVideo() method to finish the Activity.
                notify();
            }
            
        } catch (InterruptedException e) {
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Thread interrupted ***");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    private String recordChunk(int seqNumber) {
        String filepath = null;
        try {
            // TODO: dir.mkdirs() etc. to ensure location exists before trying to make file.
            File chunkFile = new File("/sdcard/Vidshare/vs_"+ startTime +"_"+ seqNumber +".3gp");
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** file created ***");
            // TODO: possible bug here with possible file name clashes.
            filepath = chunkFile.getPath();
            mMediaRecorder = createMediaRecorder(filepath);
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Sleeping for "+ MILLISECONDS_PER_CHUNK +" ms ***");
            Thread.sleep(MILLISECONDS_PER_CHUNK);
            mMediaRecorder.stop();
        } catch (Exception e) {
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Exception in recordChunk() ***");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return filepath;
    }
    
    private void stopStreamingVideo() {
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** stopStreamingVideo() method STARTED!!! ***");
        Thread stopStreamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mStatus = STATUS_STOPPING_STREAM;
            }
        });
        stopStreamThread.start();
        try {
            stopStreamThread.join();
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Stop Stream thread successfully joined. ***");
            // Ensures thread is done before continuing.
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** wait() for startStreamingVideo() ***");
            synchronized(this) {
                // Wait for last data objects to be completely sent before being notified.
                wait();
            }
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** wait() over ***");
            setResult(Activity.RESULT_OK);
            finish();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Back key pressed ***");
            switch (mStatus) {
            case STATUS_STREAMING_VIDEO:
                stopStreamingVideo();
                break;
            }
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

}

