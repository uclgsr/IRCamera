package com.mpdc4gsr.lib.core.widget.seekbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class RangeSeekBar extends View {
    
    private OnRangeChangedListener onRangeChangedListener;
    private float progress = 0f;
    
    public RangeSeekBar(Context context) {
        super(context);
    }
    
    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        this.onRangeChangedListener = listener;
    }
    
    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
        
        // Notify listener if set
        if (onRangeChangedListener != null) {
            onRangeChangedListener.onRangeChanged(this, progress, progress, false);
        }
    }
    
    public float getProgress() {
        return progress;
    }
}