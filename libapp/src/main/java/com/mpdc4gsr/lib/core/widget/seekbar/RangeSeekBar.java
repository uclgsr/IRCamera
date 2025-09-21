package com.mpdc4gsr.libunified.app.widget.seekbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class RangeSeekBar extends androidx.appcompat.widget.AppCompatSeekBar implements SeekBar.OnSeekBarChangeListener {

    private OnRangeChangedListener onRangeChangedListener;

    public RangeSeekBar(Context context) {
        super(context);
        init();
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnSeekBarChangeListener(this);
    }

    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        this.onRangeChangedListener = listener;
    }

    public void setProgress(float progress) {
        // Bridge to AppCompatSeekBar's integer progress
        super.setProgress((int) progress);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (onRangeChangedListener != null) {
            // Bridge to listener's float values
            onRangeChangedListener.onRangeChanged(this, (float) progress, (float) progress, fromUser);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (onRangeChangedListener != null) {
            // Treat as "left" thumb for compatibility
            onRangeChangedListener.onStartTrackingTouch(this, true);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (onRangeChangedListener != null) {
            // Treat as "left" thumb for compatibility
            onRangeChangedListener.onStopTrackingTouch(this, true);
        }
    }
}