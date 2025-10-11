package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;

import com.mpdc4gsr.component.shared.R;
import com.mpdc4gsr.component.shared.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.annotation.TimeMode;
import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.contract.OnDatimePickedListener;
import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.widget.DatimeWheelLayout;

@SuppressWarnings({"unused", "WeakerAccess"})
public class DatimePicker extends ModalDialog {
    protected DatimeWheelLayout wheelLayout;
    private OnDatimePickedListener onDatimePickedListener;

    public DatimePicker(@NonNull Activity activity) {
        super(activity);
    }

    public DatimePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new DatimeWheelLayout(activity);
        wheelLayout.setDateMode(DateMode.YEAR_MONTH_DAY);
        wheelLayout.setTimeMode(TimeMode.HOUR_24_NO_SECOND);
        wheelLayout.setDateLabel("/", "/", "");
        wheelLayout.setTimeLabel(":", ":", "");
        wheelLayout.setCurtainEnabled(true);
        wheelLayout.setCurtainColor(ContextCompat.getColor(getContext(), R.color.wheel_select_bg));
        wheelLayout.setSelectedTextColor(ContextCompat.getColor(getContext(), R.color.wheel_select_text));
        wheelLayout.setTextColor(ContextCompat.getColor(getContext(), R.color.wheel_unselect_text));
        wheelLayout.setResetWhenLinkage(false, false);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onDatimePickedListener != null) {
            int year = wheelLayout.getSelectedYear();
            int month = wheelLayout.getSelectedMonth();
            int day = wheelLayout.getSelectedDay();
            int hour = wheelLayout.getSelectedHour();
            int minute = wheelLayout.getSelectedMinute();
            int second = wheelLayout.getSelectedSecond();
            onDatimePickedListener.onDatimePicked(year, month, day, hour, minute, second);
        }
    }

    public void setOnDatimePickedListener(OnDatimePickedListener onDatimePickedListener) {
        this.onDatimePickedListener = onDatimePickedListener;
    }

    public final DatimeWheelLayout getWheelLayout() {
        return wheelLayout;
    }

}


