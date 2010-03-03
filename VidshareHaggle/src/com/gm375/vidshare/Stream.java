package com.gm375.vidshare;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.haggle.DataObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Stream {
    private ConcurrentHashMap<Integer, String> chunks;
    private ArrayList<String> tags;
    private String id;
    private String macAddress;
    private String startTime;
    private long startTimeLong;
    private Bitmap thumbnail;
    
    Stream(DataObject dObj) {
        chunks = new ConcurrentHashMap<Integer, String>();
        id = dObj.getAttribute("id", 0).getValue();
        
        // MAC Address example: 00:23:76:07:e8:b5
        // MAC address length:  01234567890123456 length 17
        macAddress = id.substring(0, 17);
        // Start time example: 1267652761287
        // Start time length:  7890123456789 length 13
        startTime = id.substring(17, 30);
        
        startTimeLong = Long.parseLong(startTime);
        
        for (int i = 0; i < dObj.getAttributes().length; i++) {
            if (dObj.getAttributes()[i].getName() == "tag") {
                tags.add(dObj.getAttributes()[i].getValue());
            }
        }
        
        int size = (int) dObj.getThumbnailSize();
        byte[] data = new byte[size];
        dObj.getThumbnail(data);
        thumbnail = BitmapFactory.decodeByteArray(data, 0, size);
    }
    
    public void addDataObject(DataObject dObj) {
        Integer seqNumber = Integer.decode(dObj.getAttribute("seqNumber", 0).getValue());
        String filepath = dObj.getFilePath();
        chunks.put(seqNumber, filepath);
    }
    
    public ArrayList<String> getTags() {
        return tags;
    }
    
    public String getMacAddress() {
        return macAddress;
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
