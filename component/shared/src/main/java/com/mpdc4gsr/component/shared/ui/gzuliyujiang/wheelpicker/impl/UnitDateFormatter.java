package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.impl;

import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;

public class UnitDateFormatter implements DateFormatter {

    @Override
    public String formatYear(int year) {
        return year + "[ph]";
    }

    @Override
    public String formatMonth(int month) {
        return month + "[ph]";
    }

    @Override
    public String formatDay(int day) {
        return day + "[ph]";
    }

}


