package com.gm375.vidshare;

import java.util.concurrent.ConcurrentHashMap;

import org.haggle.Attribute;
import org.haggle.DataObject;
import org.haggle.Handle;
import org.haggle.Node;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.util.Log;

public class Vidshare extends Application implements org.haggle.EventHandler {
    
    public static final String LOG_TAG = "Vidshare";
    
    public static final int NO_OF_TRIES = 2;
    
    public static final int STATUS_OK = 0;
    public static final int STATUS_ERROR = -1;
    public static final int STATUS_REGISTRATION_FAILED = -2;
    public static final int STATUS_SPAWN_DAEMON_FAILED = -3;
    
    public static final int ADD_STREAM_ATTRIBUTES_REQUEST = 10;
    public static final int STREAM_VIDEO_REQUEST = 11;
    public static final int ADD_INTEREST_REQUEST = 12;
    public static final int WATCH_STREAM_REQUEST = 20;
    
    
    private VSActivity act = null;
    private VideoStream vidStream = null;
    private StreamViewer streamViewer = null;
    private org.haggle.Handle hh = null;
    ConcurrentHashMap<String, Stream> mStreamMap;
    ConcurrentHashMap<String, Boolean> mStreamAliveMap;
    
    private int status = STATUS_OK;
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(Vidshare.LOG_TAG, "Vidshare: ***onCreate() Thread ID = "+ Thread.currentThread().getId() +" ***");
        
        mStreamMap = new ConcurrentHashMap<String, Stream>();
        mStreamAliveMap = new ConcurrentHashMap<String, Boolean>();
        
        // TODO: Get vibrator here, when implementing.
        
    }
    
    @Override
    public void onLowMemory() {
        // TODO Auto-generated method stub
        super.onLowMemory();
        Log.d(Vidshare.LOG_TAG, "***onLowMemory()***");  
    }

    @Override
    public void onTerminate() {
        // TODO Auto-generated method stub
        super.onTerminate();
        Log.d(Vidshare.LOG_TAG, "***onTerminate()***");  
        //finiHaggle();
    }
    
    public void setVSActivity(VSActivity act) {
        Log.d(Vidshare.LOG_TAG, "***Setting Activity***");
        this.act = act;
    }
    
    public void setVideoStream(VideoStream vidStream) {
        Log.d(Vidshare.LOG_TAG, "***Setting video stream***");
        this.vidStream = vidStream;
    }
    
    public void setStreamViewer(StreamViewer streamViewer) {
        Log.d(Vidshare.LOG_TAG, "***Setting stream viewer***");
        this.streamViewer = streamViewer;
    }
    
    public VSActivity getVSActivity() {
        return act;
    }
    
    public VideoStream getVideoStream() {
        return vidStream;
    }
    
    public StreamViewer getStreamViewer() {
        return streamViewer;
    }
    
    public Handle getHaggleHandle() {
        return hh;
    }
    
    public void tryDeregisterHaggleHandle() {
        finiHaggle();
    }
    
    public int initHaggle() {
        
        if (hh != null)
            return STATUS_OK;
        
        long pid = Handle.getDaemonPid();
        
        Log.d(Vidshare.LOG_TAG, "***Attempting to spawn Haggle daemon***");
        
        if (!Handle.spawnDaemon()) {
            Log.d(Vidshare.LOG_TAG, "***Spawning failed...***");
            return STATUS_SPAWN_DAEMON_FAILED;
        }
        pid = Handle.getDaemonPid();

        Log.d(Vidshare.LOG_TAG, "***Haggle daemon pid is "+ pid +" ***");
        
        int tries = NO_OF_TRIES;

        while (tries > 0) {
            try {
                hh = new Handle("Vidshare");

            } catch (Handle.RegistrationFailedException e) {
                Log.e(Vidshare.LOG_TAG, "***Registration failed*** " + e.getMessage());

                if (--tries > 0) {
                    Handle.unregister("Vidshare");
                    continue;
                }

                Log.e(Vidshare.LOG_TAG, "***Registration failed, giving up***");
                return STATUS_REGISTRATION_FAILED;
            }
            break;
        }
        
        hh.registerEventInterest(EVENT_NEIGHBOR_UPDATE, this);
        hh.registerEventInterest(EVENT_NEW_DATAOBJECT, this);
        hh.registerEventInterest(EVENT_INTEREST_LIST_UPDATE, this);
        hh.registerEventInterest(EVENT_HAGGLE_SHUTDOWN, this);
        
        hh.eventLoopRunAsync();

        hh.getApplicationInterestsAsync();
        
        Log.d(Vidshare.LOG_TAG, "***Haggle event loop started***");   

        return STATUS_OK;
        
    }
    
    public synchronized void finiHaggle() {
        if (hh != null) {
            hh.eventLoopStop();
            hh.dispose();
            hh = null;
        }
    }
    
    public int getStatus() {
        return status;
    }
    
    public void shutdownHaggle() {
        hh.shutdown();
    }
    
    public boolean registerInterest(Attribute interest) {
        if (hh.registerInterest(interest) == 0) 
            return true;
        return false;
    }
    
    @Override
    public void onInterestListUpdate(Attribute[] interests) {
        Log.d(Vidshare.LOG_TAG, "***Setting interests (size=" + interests.length + ")***");
        InterestView.setInterests(interests);
    }

    
    @Override
    public void onNewDataObject(DataObject dObj) {
        Log.d("EVAL", "******** Time data object "+ dObj.getAttribute("seqNumber", 0).getValue()
                +" arrived at Vidshare: "+ System.currentTimeMillis());
        
        if (act == null && vidStream == null && streamViewer == null) {
            Log.d(Vidshare.LOG_TAG, "***Activity was null***");
            return;
        }
        
        Log.d(Vidshare.LOG_TAG, "***Got new data object, thread ID = "+ Thread.currentThread().getId() +" ***");
        
        if (dObj.getAttribute("tag", 0) == null) {
            Log.d(Vidshare.LOG_TAG, "***Received data object has no tags***");
            return;
        }
        
        String isLast = dObj.getAttribute("isLast", 0).getValue();
        
        if (isLast.contentEquals("false")) {
            // CASE: Proper video packet, video still streaming.
            
            Log.d(Vidshare.LOG_TAG, "***Attempting to get file path***");
            String filepath = dObj.getFilePath();
            
            if (filepath == null || filepath.length() == 0) {
                Log.d(Vidshare.LOG_TAG, "***Bad filepath***");
                return;
            }
            
            String seqNumber = dObj.getAttribute("seqNumber", 0).getValue();
            Log.d(Vidshare.LOG_TAG, "***Success! Filepath = "+ filepath +" *** sequence number: "+ seqNumber);
            
        } else {
            // CASE: Sentinel packet to signify stream is ending.
            
            String seqNumber = dObj.getAttribute("seqNumber", 0).getValue();
            Log.d(Vidshare.LOG_TAG, "***Success! FINAL DATA OBJECT, STREAM ENDING *** sequence number: "+ seqNumber);
        }

        Log.d(Vidshare.LOG_TAG, "Updating UI dobj");
        act.runOnUiThread(act.new DataUpdater(dObj));
        
    }

    @Override
    public void onNeighborUpdate(Node[] neighbors) {
        
        if (act == null)
            return;
        
        Log.d(Vidshare.LOG_TAG, "***Received neighbour update, Thread ID = "+ Thread.currentThread().getId() +" ***");
        
        act.runOnUiThread(act.new DataUpdater(neighbors));
    }
    
    @Override
    public void onShutdown(int reason) {
        
        Log.d(Vidshare.LOG_TAG, "***Shutdown event, reason = "+ reason +" ***");
        
        act.runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder
                .setMessage("Haggle was shutdown and is no longer running. Press Quit to exit Vidshare.")
                .setCancelable(false)
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        act.finish();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }
    
}