package com.mpdc4gsr.libunified.app.widget.seekbar;

/**
 * Thermal imaging specific OnRangeChangedListener that extends the basic interface
 * with temperature mode information.
 */
public interface OnRangeChangedListener {
    void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser, int tempMode);
    
    void onStartTrackingTouch(RangeSeekBar view, boolean isLeft);
    
    void onStopTrackingTouch(RangeSeekBar view, boolean isLeft);
}