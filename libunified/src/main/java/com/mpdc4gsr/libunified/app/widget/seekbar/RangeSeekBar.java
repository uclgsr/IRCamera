package com.mpdc4gsr.libunified.app.widget.seekbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * Thermal imaging specific RangeSeekBar that provides temperature-specific functionality.
 * This is a simplified implementation for backward compatibility.
 */
public class RangeSeekBar extends androidx.appcompat.widget.AppCompatSeekBar implements SeekBar.OnSeekBarChangeListener {

    // Temperature mode constants for backward compatibility
    public static final int TEMP_MODE_CLOSE = 0;
    public static final int TEMP_MODE_MIN = 1;
    public static final int TEMP_MODE_MAX = 2;
    public static final int TEMP_MODE_INTERVAL = 3;

    private int tempMode = TEMP_MODE_INTERVAL;
    private OnRangeChangedListener thermalRangeChangedListener;

    public RangeSeekBar(Context context) {
        super(context);
        initThermalSeekBar();
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initThermalSeekBar();
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initThermalSeekBar();
    }

    private void initThermalSeekBar() {
        // Set up the seekbar change listener
        super.setOnSeekBarChangeListener(this);
    }

    // Override to use our thermal-specific listener
    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        this.thermalRangeChangedListener = listener;
    }

    // Temperature mode methods for backward compatibility
    public int getTempMode() {
        return tempMode;
    }

    public void setTempMode(int tempMode) {
        this.tempMode = tempMode;
    }

    // SeekBar.OnSeekBarChangeListener implementation
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (thermalRangeChangedListener != null) {
            float progressValue = (float) progress;
            // For backward compatibility, treat as both left and right values
            thermalRangeChangedListener.onRangeChanged(this, progressValue, progressValue, fromUser, tempMode);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (thermalRangeChangedListener != null) {
            thermalRangeChangedListener.onStartTrackingTouch(this, true);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (thermalRangeChangedListener != null) {
            thermalRangeChangedListener.onStopTrackingTouch(this, true);
        }
    }

    // Thermal-specific methods (stubs for now to maintain compatibility)
    public void setColorList(int[] colors) {
        // Color list functionality would be implemented here
        // For now, just ignore to maintain compatibility
    }

    public void setPlaces(String[] places) {
        // Places functionality would be implemented here
        // For now, just ignore to maintain compatibility
    }

    public void setPseudocode(int pseudoColorMode) {
        // Pseudocolor functionality would be implemented here
        // For now, just ignore to maintain compatibility
    }

    public void setRangeAndPro(float min, float max, float leftValue, float rightValue) {
        // For a single seekbar, use the left value as progress
        setMax((int) max);
        setProgress((int) leftValue);
    }

    public void setIndicatorTextDecimalFormat(String format) {
        // Indicator text format would be implemented here
        // This is a simplified implementation
    }

    // Mock seekbar objects for backward compatibility
    public SeekBarProxy leftSeekBar = new SeekBarProxy();
    public SeekBarProxy rightSeekBar = new SeekBarProxy();

    // Helper class for seekbar color properties
    public static class SeekBarProxy {
        private int indicatorBackgroundColor = 0;

        public int getIndicatorBackgroundColor() {
            return indicatorBackgroundColor;
        }

        public void setIndicatorBackgroundColor(int color) {
            this.indicatorBackgroundColor = color;
            // In a full implementation, this would update the seekbar colors
        }
    }
}