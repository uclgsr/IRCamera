package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.TimeFormatter;

public class UnitTimeFormatter implements TimeFormatter {

    @Override
    public String formatHour(int hour) {
        return hour + "点";
    }

    @Override
    public String formatMinute(int minute) {
        return minute + "分";
    }

    @Override
    public String formatSecond(int second) {
        return second + "秒";
    }

}
