package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.impl;

public class BirthdayFormatter extends SimpleDateFormatter {

    @Override
    public String formatYear(int year) {
        return super.formatYear(year) + "[ph]";
    }

    @Override
    public String formatMonth(int month) {
        return super.formatMonth(month) + "[ph]";
    }

    @Override
    public String formatDay(int day) {
        return super.formatDay(day) + "[ph]";
    }

}


