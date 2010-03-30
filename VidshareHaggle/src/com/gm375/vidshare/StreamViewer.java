package com.gm375.vidshare;

import android.app.Activity;
import android.os.Bundle;
import android.widget.VideoView;

import com.gm375.vidshare.listenerstuff.DataObjectEvent;
import com.gm375.vidshare.listenerstuff.DataObjectListener;

public class StreamViewer extends Activity implements DataObjectListener {
    
    private Vidshare vs = null;
    private VideoView video;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        vs = (Vidshare) getApplication();
        vs.setStreamViewer(this);
        
        video = (VideoView) findViewById(R.id.stream_viewer_surface);
    }
    
    public void onStop() {
        super.onStop();
        vs.setStreamViewer(null);
    }

    @Override
    public void dataObjectAlert(DataObjectEvent dObjEvent) {
        // TODO: This is executed whenever a new Data Object (or timeout message) arrives.
        // They *should* arrive in order, so no sequencing should have to happen here.
        // There may be indeterminable delays between each data object though.
    }
    
}
