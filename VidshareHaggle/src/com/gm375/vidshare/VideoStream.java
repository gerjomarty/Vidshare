package com.gm375.vidshare;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class VideoStream extends Activity implements View.OnClickListener, SurfaceHolder.Callback, Camera.ErrorCallback {
    
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
    private String hashedId;
    
    private volatile int currentListIndex;
    private List<MediaRecorder> mrList;
    private List<String> filepathList;
    private List<Integer> seqNumberList;
    
    private ArrayList<DataObject> sentDObjs;
    
    int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    
    private static final int VIDEO_FRAME_RATE = 15;
    private static final int MILLISECONDS_PER_CHUNK = 3000;
    
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_STREAMING_VIDEO = 1;
    private static final int STATUS_STOPPING_STREAM = 2;
    private static final int STATUS_STREAM_STOPPED = 3;
    
    private volatile int mStatus = STATUS_IDLE;
    private volatile boolean isFinishedTakingThumbnail = false;
    
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
        
        sentDObjs = new ArrayList<DataObject>();
        
        currentListIndex = 1;
        mrList = Collections.synchronizedList(new ArrayList<MediaRecorder>(2));
        mrList.add(0, new MediaRecorder());
        mrList.add(1, new MediaRecorder());
        filepathList = Collections.synchronizedList(new ArrayList<String>(2));
        filepathList.add(0, null);
        filepathList.add(1, null);
        seqNumberList = Collections.synchronizedList(new ArrayList<Integer>(2));
        seqNumberList.add(0, null);
        seqNumberList.add(1, null);
        
        startTime = System.currentTimeMillis();
        
        androidId = android.provider.Settings.Secure.getString(getContentResolver(), 
                android.provider.Settings.Secure.ANDROID_ID);
        
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] shaDigest = sha.digest((androidId.concat(String.valueOf(startTime))).getBytes());
        BigInteger shaInt = new BigInteger(1, shaDigest);
        hashedId = shaInt.toString(16);
        
        
    }
    
    public void onStart() {
        super.onStart();
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** onStart() ***");
        vs = (Vidshare) getApplication();
        vs.setVideoStream(this);
        
        makeSureCameraIsOpen();
        //mCamera.setErrorCallback(this);
    }
    
    public void onStop() {
        super.onStop();
        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** onStop() ***");
        
        //closeCamera();
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
    /*
    private final class JpegPictureCallback implements PictureCallback {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
                thumbnailData = new byte[data.length];
                thumbnailData = data.clone();
                isFinishedTakingThumbnail = true;
        }
        
    }
    */
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
    
    private void setUpNextRecording() {
        int nextListIndex = currentListIndex ^ 1;
        MediaRecorder mr = mrList.get(nextListIndex);
        mr.reset();
        int nextSeqNumber = mCounter.getNext();
        String nextFilepath =
            new File("/sdcard/Vidshare/vs_"+startTime+"_"+nextSeqNumber+".3gp").getPath();
        seqNumberList.set(nextListIndex, nextSeqNumber);
        filepathList.set(nextListIndex, nextFilepath);
        mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
        mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mr.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        mr.setVideoFrameRate(VIDEO_FRAME_RATE);
        mr.setPreviewDisplay(mSurfaceHolder.getSurface());
        mr.setOutputFile(nextFilepath);
    }
    
    private void recordPart() {
        MediaRecorder mr = mrList.get(currentListIndex);
        try {
            mr.prepare();
            mr.start();
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Sleeping for "+ MILLISECONDS_PER_CHUNK +" ms ***");
            Thread.sleep(MILLISECONDS_PER_CHUNK);
            mr.stop();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
        Thread prepareNextRecordingThread = null;
        Thread publishDObjThread = null;
        
        //stopPreview();
        closeCamera();
        
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
        
        // Do the initial setup for the first MediaRecorder here, then can do in
        // in thread for all subsequent times.
        
        // These two commands set up MediaRecorder index 0, and then
        // set the List Index to 0.
        setUpNextRecording();
        currentListIndex ^= 1;
        
        mStatus = STATUS_STREAMING_VIDEO;
        
        while (mStatus == STATUS_STREAMING_VIDEO) {
            final int currSeqNumber = seqNumberList.get(currentListIndex);
            final String currFilepath = filepathList.get(currentListIndex);
            
            prepareNextRecordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    setUpNextRecording();
                }
            });
            
            prepareNextRecordingThread.start();
            recordPart();
            try {
                prepareNextRecordingThread.join();
                currentListIndex ^= 1;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                Log.d(Vidshare.LOG_TAG, "*** VideoStream *** prepareNextRecording interrupted. ***");
                e.printStackTrace();
                return;
            }
            
            publishDObjThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Streaming sequence "+ currSeqNumber +" with filepath "+ currFilepath +" ***");
                        DataObject dObj = new DataObject(currFilepath);
                        synchronized(attributes) {
                            for (int i = 0; i < attributes.length; i++) {
                                dObj.addAttribute("tag", attributes[i], 1);
                                Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Attribute added: "+ attributes[i] +" ***");
                            }
                        }
                        dObj.addAttribute("seqNumber", String.valueOf(currSeqNumber), 1);
                        dObj.addAttribute("id", hashedId, 1);
                        dObj.addAttribute("startTime", String.valueOf(startTime), 1);
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
                            sentDObjs.add(dObj);
                        }
                    } catch (DataObjectException e) {
                        Log.d(Vidshare.LOG_TAG, "*** VideoStream *** DataObjectException for sequence "+ currSeqNumber +" ***");
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
                int finalSeqNumber = seqNumberList.get(currentListIndex);
                dObj.addAttribute("seqNumber", String.valueOf(finalSeqNumber), 1);
                dObj.addAttribute("id", hashedId, 1);
                dObj.addAttribute("startTime", String.valueOf(startTime), 1);
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
                    sentDObjs.add(dObj);
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
            
            Log.d(Vidshare.LOG_TAG, "*** VideoStream *** Deleting data objects from Haggle ***");
            for (DataObject dObj : sentDObjs) {
                synchronized(vs) {
                    int ret = vs.getHaggleHandle().deleteDataObject(dObj);
                    if (ret == -1) {
                        Log.e(Vidshare.LOG_TAG, "***!!! Data Object deletion returned error code. !!!***");
                    }
                    Log.d(Vidshare.LOG_TAG, "*** Last object delete return code: "+ ret +" ***");
                }
            }
            
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

    @Override
    public void onError(int error, Camera camera) {
        Log.e(Vidshare.LOG_TAG, "***!!!  VideoStream  ***  Error code: "+ error +"  !!!***");
    }

}

