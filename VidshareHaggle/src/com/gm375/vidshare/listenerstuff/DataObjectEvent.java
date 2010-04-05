package com.gm375.vidshare.listenerstuff;

import java.util.EventObject;

public class DataObjectEvent extends EventObject {
    
    public static final int EVENT_TYPE_NEW_DATA_OBJECT = 0;
    public static final int EVENT_TYPE_TIMEOUT_REACHED = -1;
    public static final int EVENT_TYPE_STREAM_ENDED = -2;
    
    private Integer eventType;
    private Integer seqNumber;
    private String filepath;
    
    public DataObjectEvent(Object source, Integer eventType) {
        super(source);
        this.eventType = eventType;
        this.seqNumber = null;
        this.filepath = null;
    }
    
    public DataObjectEvent(Object source, Integer eventType, Integer seqNumber,
            String filepath) {
        super(source);
        this.eventType = eventType;
        this.seqNumber = seqNumber;
        this.filepath = filepath;
    }
    
    public Integer getEventType() {
        return eventType;
    }
    
    public Integer getseqNumber() {
        return seqNumber;
    }
    
    public String getFilepath() {
        return filepath;
    }
    
}
