package com.github.gzuliyujiang.wheelpicker.impl;

import com.github.gzuliyujiang.wheelpicker.contract.DateFormatter;

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
