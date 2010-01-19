package com.gm375.vidshare;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

public class VideoPreview extends SurfaceView {
    private float mAspectRatio;
    private int mHorizontalTileSize = 1;
    private int mVerticalTileSize = 1;
    
    public static float DONT_CARE = 0.0f;
    
    public VideoPreview(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }
    
    public VideoPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }
    
    public VideoPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }
    
    public void setAspectRatio(int width, int height) {
        setAspectRatio(((float) width) / ((float) height));
    }

    public void setAspectRatio(float aspectRatio) {
        if (mAspectRatio != aspectRatio) {
            mAspectRatio = aspectRatio;
            requestLayout();
            invalidate();
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAspectRatio != DONT_CARE) {
            int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

            int width = widthSpecSize;
            int height = heightSpecSize;

            if (width > 0 && height > 0) {
                float defaultRatio = ((float) width) / ((float) height);
                if (defaultRatio < mAspectRatio) {
                    // Need to reduce height
                    height = (int) (width / mAspectRatio);
                } else if (defaultRatio > mAspectRatio) {
                    width = (int) (height * mAspectRatio);
                }
                width = roundUpToTile(width, mHorizontalTileSize, widthSpecSize);
                height = roundUpToTile(height, mVerticalTileSize, heightSpecSize);
                Log.d(Vidshare.LOG_TAG, "ar " + mAspectRatio + " setting size: " + width + 'x' + height);
                setMeasuredDimension(width, height);
                return;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    
    private int roundUpToTile(int dimension, int tileSize, int maxDimension) {
        return Math.min(((dimension + tileSize - 1) / tileSize) * tileSize, maxDimension);
    }
    
}
