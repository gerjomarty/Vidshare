package com.gm375.vidshare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.haggle.Attribute;
import org.haggle.DataObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Stream {
    private ConcurrentHashMap<Integer, String> chunks;
    private ArrayList<String> tags;
    private String id;
    private String androidId;
    private String startTime;
    private long startTimeLong;
    private Bitmap thumbnail;
    
    private boolean isBeingViewed = false;
    private boolean streamEnding = false;
    private boolean streamEnded = false;
    
    // These vars used for the sequenceChunks() algorithm.
    private Set<Integer> expected = new HashSet<Integer>();
    private Set<Integer> toFire = new TreeSet<Integer>();
    private Integer lastFiredNumber = new Integer(com.gm375.vidshare.util.Counter.INITIAL_NUMBER - 1);
    
    
    Stream(DataObject dObj) {
        Log.d(Vidshare.LOG_TAG, "*** Stream constructor *** creating stream ***");
        chunks = new ConcurrentHashMap<Integer, String>();
        id = dObj.getAttribute("id", 0).getValue();
        Log.d(Vidshare.LOG_TAG, "*** Stream constructor *** dObj ID = "+ id +" ***");
        
        // ID example: 0123456789abcdef
        // ID length:  0123456789012345 length 16
        androidId = id.substring(0, 16);
        Log.d(Vidshare.LOG_TAG, "*** Stream constructor *** Android ID = "+ androidId +" ***");
        // Start time example: 1267652761287
        // Start time length:  6789012345678 length 13
        startTime = id.substring(16, 29);
        Log.d(Vidshare.LOG_TAG, "*** Stream constructor *** start Time = "+ startTime +" ***");
        
        startTimeLong = Long.parseLong(startTime);
        
        tags = new ArrayList<String>();
        Attribute[] attributes = dObj.getAttributes().clone();
        
        for (Attribute attr : attributes) {
            if (attr.getName() == "tag") {
                tags.add(attr.getValue());
            }
        }
        
        /*
        int size = (int) dObj.getThumbnailSize();
        byte[] data = new byte[size];
        dObj.getThumbnail(data);
        thumbnail = BitmapFactory.decodeByteArray(data, 0, size);
        */
        
        addDataObject(dObj);
    }
    
    public void addDataObject(DataObject dObj) {
        
        Integer seqNumber = Integer.decode(dObj.getAttribute("seqNumber", 0).getValue());
        String filepath = dObj.getFilePath();
        
        if (dObj.getAttribute("isLast", 0).getValue() != null) {
            streamEnding = true;
        }
        
        if (!streamEnding) {
            chunks.put(seqNumber, filepath);
            sequenceChunks(seqNumber, filepath);
        } else {
            if (!toFire.isEmpty()) {
                chunks.put(seqNumber, filepath);
                sequenceChunks(seqNumber, filepath);
            } else {
                streamEnded = true;
            }
        }
        
    }
    
    private void sequenceChunks(Integer seqNumber, String filepath) {
        
        if (seqNumber == (lastFiredNumber + 1)) {
            
            expected.remove(seqNumber);
            if (isBeingViewed) {
                // TODO: Fire object no. seqNumber
            }
            int current = seqNumber;
            for (Integer toFireNumber : toFire) {
                if (toFireNumber == (current + 1)) {
                    if (isBeingViewed) {
                        // TODO: Fire object no. toFireNumber
                    }
                    toFire.remove(toFireNumber);
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
            
        } else {
            Log.e(Vidshare.LOG_TAG, "***!!! We really should never be in this situation. !!!***");
        }
        
    }
    
    public ArrayList<String> getTags() {
        return tags;
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
    
    public Bitmap getThumbnail() {
        return thumbnail;
    }
    
    public boolean getBeingViewed() {
        return isBeingViewed;
    }
    
    public boolean hasStreamEnded() {
        return streamEnded;
    }
    
    public void setBeingViewed(boolean b) {
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
}
