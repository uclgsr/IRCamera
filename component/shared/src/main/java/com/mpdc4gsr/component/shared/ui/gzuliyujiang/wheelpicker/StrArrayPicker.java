package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class StrArrayPicker extends OptionPicker {

    @NonNull
    private final List<String> optionList;

    public StrArrayPicker(Activity activity, @NonNull String[] optionArray, int defaultPosition) {
        super(activity);
        this.optionList = Arrays.asList(optionArray);
        this.defaultPosition = defaultPosition;
    }

    @Override
    protected List<?> provideData() {
        return optionList;
    }

}


