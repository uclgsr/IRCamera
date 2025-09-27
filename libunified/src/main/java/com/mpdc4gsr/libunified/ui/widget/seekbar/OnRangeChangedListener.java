package com.mpdc4gsr.libunified.ui.widget.seekbar;

import androidx.annotation.NonNull;

public interface OnRangeChangedListener {
    void onRangeChanged(@NonNull RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser, int tempMode);

    void onStartTrackingTouch(@NonNull RangeSeekBar view, boolean isLeft);

    void onStopTrackingTouch(@NonNull RangeSeekBar view, boolean isLeft);
}
