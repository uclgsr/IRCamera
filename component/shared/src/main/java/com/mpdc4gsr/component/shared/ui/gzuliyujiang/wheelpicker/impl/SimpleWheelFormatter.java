package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.impl;

import androidx.annotation.NonNull;

import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelview.contract.WheelFormatter;

public class SimpleWheelFormatter implements WheelFormatter {

    @Override
    public String formatItem(@NonNull Object item) {
        return item.toString();
    }

}


