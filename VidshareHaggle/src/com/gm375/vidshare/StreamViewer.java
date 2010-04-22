package com.gm375.vidshare;

import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.gm375.vidshare.listenerstuff.DataObjectEvent;
import com.gm375.vidshare.listenerstuff.DataObjectListener;

public class StreamViewer extends Activity implements DataObjectListener,
            SurfaceHolder.Callback, MediaPlayer.OnCompletionListener {
    
    private Vidshare vs = null;
    private VideoView mVideoView;
    private ImageView mLoadingScreen;
    private Stream currentStream = null;
    
    private ConcurrentLinkedQueue<String> dObjFilepaths = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Vidshare.LOG_TAG, "*** StreamViewer *** onCreate() ***");
        
        vs = (Vidshare) getApplication();
        
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.stream_viewer);
        
        dObjFilepaths = new ConcurrentLinkedQueue<String>();
        
        mVideoView = (VideoView) findViewById(R.id.stream_viewer_surface);
        mLoadingScreen = (ImageView) findViewById(R.id.loading_screen);
        
        mVideoView.setOnCompletionListener(this);
        
        SurfaceHolder holder = mVideoView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public void onStart() {
        super.onStart();
        Log.d(Vidshare.LOG_TAG, "*** StreamViewer *** onStart() ***");
        
        vs.setStreamViewer(this);
        
        String mapKey = getIntent().getStringExtra(VSActivity.STREAM_ID_KEY);
        currentStream = vs.mStreamMap.get(mapKey);
        currentStream.setIsBeingViewed(true);
    }
    
    public void onStop() {
        super.onStop();
        Log.d(Vidshare.LOG_TAG, "*** StreamViewer *** onStop() ***");
        
        currentStream.setIsBeingViewed(false);
        currentStream = null;
        
        vs.setStreamViewer(null);
    }

    @Override
    public void dataObjectAlert(DataObjectEvent dObjEvent) {
        // TODO: This is executed whenever a new Data Object (or timeout message) arrives.
        // They *should* arrive in order, so no sequencing should have to happen here.
        // There may be indeterminable delays between each data object though.
        
        Log.d(Vidshare.LOG_TAG, "*** StreamViewer *** DATA OBJECT EVENT! ***");
        
        switch(dObjEvent.getEventType()) {
        
        case DataObjectEvent.EVENT_TYPE_TIMEOUT_REACHED:
            Toast.makeText(getApplicationContext(),
                    "There was a problem with the stream. It timed out.",
                    Toast.LENGTH_LONG).show();
            finish();
            break;
            
        case DataObjectEvent.EVENT_TYPE_NEW_DATA_OBJECT:
            newDataObject(dObjEvent.getseqNumber(), dObjEvent.getFilepath());
            break;
            
        case DataObjectEvent.EVENT_TYPE_STREAM_ENDED:
            Intent i = new Intent();
            i.putExtra(VSActivity.IS_STREAM_OVER_KEY, true);
            i.putExtra(VSActivity.STREAM_ID_KEY, currentStream.getId());
            setResult(Activity.RESULT_OK, i);
            finish();
            break;
            
        }
        
    }
    
    private void newDataObject(Integer seqNumber, String filepath) {
        if (!mVideoView.isPlaying()) {
            if (mLoadingScreen.isShown()) {
                mLoadingScreen.setVisibility(View.GONE);
            }
            mVideoView.setVideoPath(filepath);
            mVideoView.start();
        } else {
            dObjFilepaths.add(filepath);            
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.d(Vidshare.LOG_TAG, "*** StreamViewer *** surface changed ***");

        // TODO Auto-generated method stub
        
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(Vidshare.LOG_TAG, "*** StreamViewer *** surface created ***");

        // TODO Auto-generated method stub
        
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Vidshare.LOG_TAG, "*** StreamViewer *** surface destroyed ***");

        // TODO Auto-generated method stub
        
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (dObjFilepaths.isEmpty()) {
            mLoadingScreen.setVisibility(View.VISIBLE);
        } else {
            mVideoView.setVideoPath(dObjFilepaths.poll());
            mVideoView.start();
        }
    }
    
}
