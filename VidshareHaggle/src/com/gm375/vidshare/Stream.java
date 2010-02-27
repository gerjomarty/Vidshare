package com.gm375.vidshare;

import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Bitmap;

public class Stream {
    private ConcurrentHashMap<Integer, String> chunks;
    private String[] tags;
    private String macAddress;
    private long timeOfStream;
    private Bitmap thumbnail = null;
    
    Stream(String[] tags, String macAddress, long timeOfStream) {
        chunks = new ConcurrentHashMap<Integer, String>();
        this.tags = tags;
        this.macAddress = macAddress;
        this.timeOfStream = timeOfStream;
    }
    
    public String[] getTags() {
        return tags;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public long getTimeOfStream() {
        return timeOfStream;
    }
    
    public Bitmap getThumbnail() {
        return thumbnail;
    }
}
