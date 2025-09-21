package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;

public class SimpleWheelFormatter implements WheelFormatter {

    @Override
    public String formatItem(@NonNull Object item) {
        return item.toString();
    }

}
