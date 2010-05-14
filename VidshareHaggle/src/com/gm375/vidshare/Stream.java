package com.gm375.vidshare;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.haggle.Attribute;
import org.haggle.DataObject;

import android.util.Log;

import com.gm375.vidshare.listenerstuff.DataObjectEvent;

public class Stream {
    
    // Max number of dObjs that will be held.
    private final static int MAX_LENGTH_OF_TOFIRE_LIST = 3;
    private final static long TIMEOUT_IN_MILLISECONDS = 15000L;
    
    private Vidshare vs = null;
    
    private ConcurrentHashMap<Integer, String> chunks;
    private String[] tags;
    private String id;
    private String androidId;
    private String startTime;
    private long startTimeLong;
    
    private Timer timeoutTimer;
    private TimeoutTask timeoutTask;
    
    private boolean isBeingViewed = false;
    private boolean streamEnding = false;
    private boolean streamEnded = false;
    
    // These vars used for the sequenceChunks() algorithm.
    private HashSet<Integer> expected = new HashSet<Integer>();
    private TreeSet<Integer> toFire = new TreeSet<Integer>();
    private Integer lastFiredNumber = new Integer(com.gm375.vidshare.util.Counter.INITIAL_NUMBER - 1);
    
    
    Stream(DataObject dObj, Vidshare vs) {
        Log.d(Vidshare.LOG_TAG, "*** Stream constructor *** creating stream ***");
        chunks = new ConcurrentHashMap<Integer, String>();
        id = dObj.getAttribute("id", 0).getValue();
        Log.d(Vidshare.LOG_TAG, "*** Stream constructor *** dObj ID = "+ id +" ***");
        startTime = dObj.getAttribute("startTime", 0).getValue();
        Log.d(Vidshare.LOG_TAG, "*** Stream constructor *** start Time = "+ startTime +" ***");
        
        startTimeLong = Long.parseLong(startTime);
        
        Attribute[] attributes = dObj.getAttributes();
        
        int arrayLength = 0;
        for (Attribute attr : attributes) {
            if (attr.getName().contentEquals("tag")) {
                arrayLength++;
            }
        }
        tags = new String[arrayLength];
        int i = 0;
        for (Attribute attr : attributes) {
            if (attr.getName().contentEquals("tag")) {
                tags[i++] = attr.getValue();
            }
        }
        
        this.vs = vs;
        
        timeoutTimer = new Timer(true);
        timeoutTask = new TimeoutTask();
        
        Log.d("EVAL", "******** Time data object "+ dObj.getAttribute("seqNumber", 0).getValue()
                +" created STREAM CONSTRUCTOR: "+ System.currentTimeMillis());
        
        addDataObject(dObj);
    }
    
    public void addDataObject(DataObject dObj) {
        
        Integer seqNumber = Integer.decode(dObj.getAttribute("seqNumber", 0).getValue());
        
        String filepath = dObj.getFilePath();
        
        if (dObj.getAttribute("isLast", 0).getValue().contentEquals("true")) {
            streamEnding = true;
        }
        
        Log.d(Vidshare.LOG_TAG, "*** seqNumber: "+ seqNumber +" ***");
        Log.d(Vidshare.LOG_TAG, "*** filepath: "+ filepath +" ***");
        
        dObj.dispose();
        
        if (!streamEnding) {
            Log.d("EVAL", "Stream not ending.");
            if (filepath != null) {
                chunks.put(seqNumber, filepath);
            }
            sequenceChunks(seqNumber, filepath);
        } else {
            Log.d("EVAL", "Stream IS ending.");
            if (!expected.isEmpty()) {
                Log.d("EVAL", "Expected list IS NOT empty.");
                if (filepath != null) {
                    chunks.put(seqNumber, filepath);
                }
                sequenceChunks(seqNumber, filepath);
            } else {
                Log.d("EVAL", "Expected list IS empty. Fire stream ended.");
                streamEnded = true;
                fireStreamEnded();
            }
        }
        
        timeoutTask.cancel();
        if (!streamEnded) {
            timeoutTask = new TimeoutTask();
            timeoutTimer.schedule(timeoutTask, TIMEOUT_IN_MILLISECONDS);
        }
        Log.d("EVAL", "******** Time data object "+ seqNumber +" fully sequenced: "+ System.currentTimeMillis());
        
    }
    
    private void sequenceChunks(Integer seqNumber, String filepath) {
        
        if (seqNumber == (lastFiredNumber + 1)) {
            
            expected.remove(seqNumber);
            if (isBeingViewed) {
                fireDataObject(seqNumber, filepath);
            }
            int current = seqNumber;
            for (Iterator<Integer> it = toFire.iterator(); it.hasNext(); ) {
                Integer toFireNumber = it.next();
                if (toFireNumber == (current + 1)) {
                    if (isBeingViewed) {
                        fireDataObject(toFireNumber, chunks.get(toFireNumber));
                    }
                    it.remove();
                    current++;
                } else {
                    break;
                }
            }
            lastFiredNumber = current;
            
        } else if (seqNumber > (lastFiredNumber + 1)) {
            
            expected.remove(seqNumber);
            toFire.add(seqNumber);
            // Need to work out expected packets that haven't arrived.
            for (int i = (lastFiredNumber + 1); i < seqNumber; i++) {
                if (!toFire.contains(i) && !expected.contains(i)) {
                    expected.add(i);
                }
            }
            
            // If toFire list gets too big, video will get behind broadcast too much.
            // Therefore, keep toFire list small.
            
            while (toFire.size() > MAX_LENGTH_OF_TOFIRE_LIST) {
                Integer firstDObj = toFire.first();
                toFire.remove(firstDObj);
                if (isBeingViewed) {
                    fireDataObject(firstDObj, chunks.get(firstDObj));
                }
                
                int current = firstDObj;
                for (Iterator<Integer> it = toFire.iterator(); it.hasNext(); ) {
                    Integer toFireNumber = it.next();
                    if (toFireNumber == (current + 1)) {
                        if (isBeingViewed) {
                            fireDataObject(toFireNumber, chunks.get(toFireNumber));
                        }
                        it.remove();
                        current++;
                    } else {
                        break;
                    }
                }
                lastFiredNumber = current;
                
            }
            
        } else {
            // seqNumber < (lastFiredNumber + 1)
            // We can get into the situation where we receive sequence numbers less
            // than the last fired number when the toFire list gets too big and we
            // send some later ones, only to receive the earlier ones after this.
            // In this case, we can only drop the packet and continue.
            expected.remove(seqNumber);
        }
        
    }
    
    private void fireDataObject(Integer seqNumber, String filepath) {
        StreamViewer streamViewer = vs.getStreamViewer();
        if (streamViewer == null) {
            Log.e(Vidshare.LOG_TAG, "***!!! stream viewer was NULL when OBJECT was fired !!!***");
            Log.e(Vidshare.LOG_TAG, "***!!! This really shouldn't happen. !!!***");
            return;
        }
        if (isBeingViewed()) {
            DataObjectEvent doe = new DataObjectEvent(this,
                    DataObjectEvent.EVENT_TYPE_NEW_DATA_OBJECT, seqNumber, filepath);
            streamViewer.dataObjectAlert(doe);
        }
    }
    
    private void fireTimeout() {
        vs.mStreamMap.remove(id);
        vs.mStreamAliveMap.put(id, true);
        StreamViewer streamViewer = vs.getStreamViewer();
        if (streamViewer == null) {
            Log.e(Vidshare.LOG_TAG, "***!!! stream viewer was NULL when TIMEOUT was fired !!!***");
            Log.e(Vidshare.LOG_TAG, "***!!! This really shouldn't happen. !!!***");
            return;
        }
        if (isBeingViewed()) {
            DataObjectEvent doe = new DataObjectEvent(this,
                    DataObjectEvent.EVENT_TYPE_TIMEOUT_REACHED);
            streamViewer.dataObjectAlert(doe);
        }
    }
    
    private void fireStreamEnded() {
        vs.mStreamAliveMap.put(id, false);
        vs.mStreamMap.remove(id);
        StreamViewer streamViewer = vs.getStreamViewer();
        if (streamViewer == null) {
            Log.e(Vidshare.LOG_TAG, "***!!! stream viewer was NULL when STREAM ENDED was fired !!!***");
            Log.e(Vidshare.LOG_TAG, "***!!! This really shouldn't happen. !!!***");
            return;
        }
        if (isBeingViewed()) {
            DataObjectEvent doe = new DataObjectEvent(this,
                    DataObjectEvent.EVENT_TYPE_STREAM_ENDED);
            streamViewer.dataObjectAlert(doe);
        }
    }
    
    public String[] getTags() {
        return tags;
    }
    
    public String getId() {
        return id;
    }
    
    public String getAndroidId() {
        return androidId;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public long getStartTimeLong() {
        return startTimeLong;
    }
    
    public boolean isBeingViewed() {
        return isBeingViewed;
    }
    
    public boolean hasStreamEnded() {
        return streamEnded;
    }
    
    public void setIsBeingViewed(boolean b) {
        isBeingViewed = b;
    }
    
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return 0;
    }
    
    public boolean equals(Object obj) {
        Stream object = (Stream) obj;
        if (id == object.id) {
            return true;
        }
        return false;
    }
    
    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            fireTimeout();
        }
    }
}
