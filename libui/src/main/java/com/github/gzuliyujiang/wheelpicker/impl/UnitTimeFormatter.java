package com.github.gzuliyujiang.wheelpicker.impl;

import com.github.gzuliyujiang.wheelpicker.contract.TimeFormatter;

public class UnitTimeFormatter implements TimeFormatter {

    @Override
    public String formatHour(int hour) {
        return hour + "[ph]";
    }

    @Override
    public String formatMinute(int minute) {
        return minute + "[ph]";
    }

    @Override
    public String formatSecond(int second) {
        return second + "[ph]";
    }

}
