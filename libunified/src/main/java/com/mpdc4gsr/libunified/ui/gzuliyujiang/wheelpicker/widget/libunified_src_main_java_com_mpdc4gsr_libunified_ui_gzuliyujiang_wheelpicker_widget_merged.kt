// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget' directory and its subdirectories.
// Total files: 9 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\BaseWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.*;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.DialogLog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.OnWheelChangedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class BaseWheelLayout extends LinearLayout implements OnWheelChangedListener {
    private final List<WheelView> wheelViews = new ArrayList<>();
    private AttributeSet attrs;

    public BaseWheelLayout(Context context) {
        super(context);
        init(context, null);
        TypedArray a = context.obtainStyledAttributes(null, provideStyleableRes(),
                R.attr.WheelStyle, R.style.WheelDefault);
        onAttributeSet(context, a);
    }

    public BaseWheelLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, provideStyleableRes(),
                R.attr.WheelStyle, R.style.WheelDefault);
        onAttributeSet(context, a);
        a.recycle();
    }

    public BaseWheelLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, provideStyleableRes(),
                defStyleAttr, R.style.WheelDefault);
        onAttributeSet(context, a);
        a.recycle();
    }

    public BaseWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, provideStyleableRes(),
                defStyleAttr, defStyleRes);
        onAttributeSet(context, a);
        a.recycle();
    }

    private void init(Context context, AttributeSet attrs) {
        this.attrs = attrs;
        setOrientation(VERTICAL);
        inflate(context, provideLayoutRes(), this);
        onInit(context);
        wheelViews.addAll(provideWheelViews());
        for (WheelView wheelView : wheelViews) {
            wheelView.setOnWheelChangedListener(this);
        }
    }

    protected void onInit(@NonNull Context context) {

    }

    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {

    }

    @LayoutRes
    protected abstract int provideLayoutRes();

    @StyleableRes
    protected abstract int[] provideStyleableRes();

    protected abstract List<WheelView> provideWheelViews();

    public void setStyle(@StyleRes int style) {
        if (attrs == null) {
            DialogLog.print("Please use " + getClass().getSimpleName() + " in xml");
            return;
        }
        TypedArray a = getContext().obtainStyledAttributes(attrs, provideStyleableRes(), R.attr.WheelStyle, style);
        onAttributeSet(getContext(), a);
        a.recycle();
        requestLayout();
        invalidate();
    }

    @Override
    public void onWheelScrolled(WheelView view, int offset) {

    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {

    }

    @Override
    public void onWheelLoopFinished(WheelView view) {

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (WheelView wheelView : wheelViews) {
            wheelView.setEnabled(enabled);
        }
    }

    public void setVisibleItemCount(int visibleItemCount) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setVisibleItemCount(visibleItemCount);
        }
    }

    public void setItemSpace(@Px int space) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setItemSpace(space);
        }
    }

    public void setSameWidthEnabled(boolean sameWidthEnabled) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSameWidthEnabled(sameWidthEnabled);
        }
    }

    public void setDefaultItemPosition(int position) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setDefaultPosition(position);
        }
    }

    public void setCurtainEnabled(boolean hasCurtain) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainEnabled(hasCurtain);
        }
    }

    public void setCurtainColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainColor(color);
        }
    }

    public void setCurtainCorner(@CurtainCorner int corner) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainCorner(corner);
        }
    }

    public void setCurtainRadius(@Px float radius) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainRadius(radius);
        }
    }

    public void setAtmosphericEnabled(boolean hasAtmospheric) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setAtmosphericEnabled(hasAtmospheric);
        }
    }

    public void setCurvedEnabled(boolean curved) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurvedEnabled(curved);
        }
    }

    public void setCurvedMaxAngle(int curvedMaxAngle) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurvedMaxAngle(curvedMaxAngle);
        }
    }

    public void setCurvedIndicatorSpace(@Px int space) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurvedIndicatorSpace(space);
        }
    }

    public void setCyclicEnabled(boolean cyclic) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCyclicEnabled(cyclic);
        }
    }

    public void setIndicatorEnabled(boolean hasIndicator) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setIndicatorEnabled(hasIndicator);
        }
    }

    public void setIndicatorSize(@Px float size) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setIndicatorSize(size);
        }
    }

    public void setIndicatorColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setIndicatorColor(color);
        }
    }

    public void setMaxWidthText(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        for (WheelView wheelView : wheelViews) {
            wheelView.setMaxWidthText(text);
        }
    }

    public void setTextSize(@Px float textSize) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setTextSize(textSize);
        }
    }

    public void setSelectedTextSize(@Px float textSize) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSelectedTextSize(textSize);
        }
    }

    public void setTextColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setTextColor(color);
        }
    }

    public void setSelectedTextColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSelectedTextColor(color);
        }
    }

    public void setSelectedTextBold(boolean bold) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSelectedTextBold(bold);
        }
    }

    public void setTextAlign(@ItemTextAlign int align) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setTextAlign(align);
        }
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\CarPlateWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.CarPlateProvider;

public class CarPlateWheelLayout extends LinkageWheelLayout {
    private CarPlateProvider provider;

    public CarPlateWheelLayout(Context context) {
        super(context);
    }

    public CarPlateWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarPlateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarPlateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        super.onInit(context);
        provider = new CarPlateProvider();
        setData(provider);
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        super.onAttributeSet(context, typedArray);
        setFirstVisible(provider.firstLevelVisible());
        setThirdVisible(provider.thirdLevelVisible());
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\DateWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnDateSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DateEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleDateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class DateWheelLayout extends BaseWheelLayout {
    private NumberWheelView yearWheelView;
    private NumberWheelView monthWheelView;
    private NumberWheelView dayWheelView;
    private TextView yearLabelView;
    private TextView monthLabelView;
    private TextView dayLabelView;
    private TextView spaceStartView;
    private TextView spaceEndView;
    private DateEntity startValue;
    private DateEntity endValue;
    private Integer selectedYear;
    private Integer selectedMonth;
    private Integer selectedDay;
    private OnDateSelectedListener onDateSelectedListener;
    private boolean resetWhenLinkage = true;

    public DateWheelLayout(Context context) {
        super(context);
    }

    public DateWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_date;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.DateWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(yearWheelView, monthWheelView, dayWheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        yearWheelView = findViewById(R.id.wheel_picker_date_year_wheel);
        monthWheelView = findViewById(R.id.wheel_picker_date_month_wheel);
        dayWheelView = findViewById(R.id.wheel_picker_date_day_wheel);
        yearLabelView = findViewById(R.id.wheel_picker_date_year_label);
        monthLabelView = findViewById(R.id.wheel_picker_date_month_label);
        dayLabelView = findViewById(R.id.wheel_picker_date_day_label);
        spaceStartView = findViewById(R.id.wheel_picker_date_start_view);
        spaceEndView = findViewById(R.id.wheel_picker_date_end_view);

        post(new Runnable() {
            @Override
            public void run() {
                yearLabelView.setHeight(monthWheelView.itemHeight);
                monthLabelView.setHeight(monthWheelView.itemHeight);
                dayLabelView.setHeight(monthWheelView.itemHeight);
                spaceStartView.setHeight(monthWheelView.itemHeight);
                spaceEndView.setHeight(monthWheelView.itemHeight);
            }
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.DateWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.DateWheelLayout_wheel_maxWidthText));
        setTextColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextSize(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.DateWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.DateWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.DateWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.DateWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.DateWheelLayout_wheel_curvedMaxAngle, 90));
        setDateMode(typedArray.getInt(R.styleable.DateWheelLayout_wheel_dateMode, DateMode.YEAR_MONTH_DAY));
        String yearLabel = typedArray.getString(R.styleable.DateWheelLayout_wheel_yearLabel);
        String monthLabel = typedArray.getString(R.styleable.DateWheelLayout_wheel_monthLabel);
        String dayLabel = typedArray.getString(R.styleable.DateWheelLayout_wheel_dayLabel);
        setDateLabel(yearLabel, monthLabel, dayLabel);
        setDateFormatter(new SimpleDateFormatter());
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(DateEntity.today(), DateEntity.yearOnFuture(30), DateEntity.today());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_date_year_wheel) {
            selectedYear = yearWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedMonth = null;
                selectedDay = null;
            }
            changeMonth(selectedYear);
            dateSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_date_month_wheel) {
            selectedMonth = monthWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedDay = null;
            }
            changeDay(selectedYear, selectedMonth);
            dateSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_date_day_wheel) {
            selectedDay = dayWheelView.getItem(position);
            dateSelectedCallback();
        }
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        int id = view.getId();
        if (id == R.id.wheel_picker_date_year_wheel) {
            monthWheelView.setEnabled(state == ScrollState.IDLE);
            dayWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_date_month_wheel) {
            yearWheelView.setEnabled(state == ScrollState.IDLE);
            dayWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_date_day_wheel) {
            yearWheelView.setEnabled(state == ScrollState.IDLE);
            monthWheelView.setEnabled(state == ScrollState.IDLE);
        }
    }

    private void dateSelectedCallback() {
        if (onDateSelectedListener == null) {
            return;
        }
        dayWheelView.post(new Runnable() {
            @Override
            public void run() {
                onDateSelectedListener.onDateSelected(selectedYear, selectedMonth, selectedDay);
            }
        });
    }

    public void setDateMode(@DateMode int dateMode) {
        yearWheelView.setVisibility(View.VISIBLE);
        yearLabelView.setVisibility(View.VISIBLE);
        monthWheelView.setVisibility(View.VISIBLE);
        monthLabelView.setVisibility(View.VISIBLE);
        dayWheelView.setVisibility(View.VISIBLE);
        dayLabelView.setVisibility(View.VISIBLE);
        if (dateMode == DateMode.NONE) {
            yearWheelView.setVisibility(View.GONE);
            yearLabelView.setVisibility(View.GONE);
            monthWheelView.setVisibility(View.GONE);
            monthLabelView.setVisibility(View.GONE);
            dayWheelView.setVisibility(View.GONE);
            dayLabelView.setVisibility(View.GONE);
            return;
        }
        if (dateMode == DateMode.MONTH_DAY) {
            yearWheelView.setVisibility(View.GONE);
            yearLabelView.setVisibility(View.GONE);
            return;
        }
        if (dateMode == DateMode.YEAR_MONTH) {
            dayWheelView.setVisibility(View.GONE);
            dayLabelView.setVisibility(View.GONE);
        }
        if (dateMode == DateMode.YEAR) {
            yearLabelView.setVisibility(View.GONE);
            monthWheelView.setVisibility(View.GONE);
            monthLabelView.setVisibility(View.GONE);
            dayWheelView.setVisibility(View.GONE);
            dayLabelView.setVisibility(View.GONE);
        }
    }

    public void setRange(DateEntity startValue, DateEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(DateEntity startValue, DateEntity endValue, DateEntity defaultValue) {
        if (startValue == null) {
            startValue = DateEntity.today();
        }
        if (endValue == null) {
            endValue = DateEntity.yearOnFuture(30);
        }
        if (endValue.toTimeInMillis() < startValue.toTimeInMillis()) {
            throw new IllegalArgumentException("Ensure the start date is less than the end date");
        }
        this.startValue = startValue;
        this.endValue = endValue;
        if (defaultValue != null) {
            selectedYear = defaultValue.getYear();
            selectedMonth = defaultValue.getMonth();
            selectedDay = defaultValue.getDay();
        } else {
            selectedYear = null;
            selectedMonth = null;
            selectedDay = null;
        }
        changeYear();
    }

    public void setDefaultValue(DateEntity defaultValue) {
        setRange(startValue, endValue, defaultValue);
    }

    public void setDateFormatter(final DateFormatter dateFormatter) {
        if (dateFormatter == null) {
            return;
        }
        yearWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return dateFormatter.formatYear((Integer) value);
            }
        });
        monthWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return dateFormatter.formatMonth((Integer) value);
            }
        });
        dayWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return dateFormatter.formatDay((Integer) value);
            }
        });
    }

    public void setDateLabel(CharSequence year, CharSequence month, CharSequence day) {
        yearLabelView.setText(year);
        monthLabelView.setText(month);
        dayLabelView.setText(day);

    }

    public void setOnDateSelectedListener(OnDateSelectedListener onDateSelectedListener) {
        this.onDateSelectedListener = onDateSelectedListener;
    }

    public void setResetWhenLinkage(boolean resetWhenLinkage) {
        this.resetWhenLinkage = resetWhenLinkage;
    }

    public final DateEntity getStartValue() {
        return startValue;
    }

    public final DateEntity getEndValue() {
        return endValue;
    }

    public final NumberWheelView getYearWheelView() {
        return yearWheelView;
    }

    public final NumberWheelView getMonthWheelView() {
        return monthWheelView;
    }

    public final NumberWheelView getDayWheelView() {
        return dayWheelView;
    }

    public final TextView getYearLabelView() {
        return yearLabelView;
    }

    public final TextView getMonthLabelView() {
        return monthLabelView;
    }

    public final TextView getDayLabelView() {
        return dayLabelView;
    }

    public final TextView getSpaceStartView() {
        return spaceStartView;
    }

    public final TextView getSpaceEndView() {
        return spaceEndView;
    }

    public final int getSelectedYear() {
        return yearWheelView.getCurrentItem();
    }

    public final int getSelectedMonth() {
        return monthWheelView.getCurrentItem();
    }

    public final int getSelectedDay() {
        return dayWheelView.getCurrentItem();
    }

    private void changeYear() {
        final int min = Math.min(startValue.getYear(), endValue.getYear());
        final int max = Math.max(startValue.getYear(), endValue.getYear());
        if (selectedYear == null) {
            selectedYear = min;
        } else {
            selectedYear = Math.max(selectedYear, min);
            selectedYear = Math.min(selectedYear, max);
        }
        yearWheelView.setRange(min, max, 1);
        yearWheelView.setDefaultValue(selectedYear);
        changeMonth(selectedYear);
    }

    private void changeMonth(int year) {
        final int min, max;

        if (startValue.getYear() == endValue.getYear()) {
            min = Math.min(startValue.getMonth(), endValue.getMonth());
            max = Math.max(startValue.getMonth(), endValue.getMonth());
        } else if (year == startValue.getYear()) {
            min = startValue.getMonth();
            max = 12;
        } else if (year == endValue.getYear()) {
            min = 1;
            max = endValue.getMonth();
        } else {
            min = 1;
            max = 12;
        }
        if (selectedMonth == null) {
            selectedMonth = min;
        } else {
            selectedMonth = Math.max(selectedMonth, min);
            selectedMonth = Math.min(selectedMonth, max);
        }
        monthWheelView.setRange(min, max, 1);
        monthWheelView.setDefaultValue(selectedMonth);
        changeDay(year, selectedMonth);
    }

    private void changeDay(int year, int month) {
        final int min, max;

        if (year == startValue.getYear() && month == startValue.getMonth()
                && year == endValue.getYear() && month == endValue.getMonth()) {
            min = startValue.getDay();
            max = endValue.getDay();
        } else if (year == startValue.getYear() && month == startValue.getMonth()) {
            min = startValue.getDay();
            max = getTotalDaysInMonth(year, month);
        } else if (year == endValue.getYear() && month == endValue.getMonth()) {
            min = 1;
            max = endValue.getDay();
        } else {
            min = 1;
            max = getTotalDaysInMonth(year, month);
        }
        if (selectedDay == null) {
            selectedDay = min;
        } else {
            selectedDay = Math.max(selectedDay, min);
            selectedDay = Math.min(selectedDay, max);
        }
        dayWheelView.setRange(min, max, 1);
        dayWheelView.setDefaultValue(selectedDay);
    }

    private int getTotalDaysInMonth(int year, int month) {
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:

                return 31;
            case 4:
            case 6:
            case 9:
            case 11:

                return 30;
            case 2:

                if (year <= 0) {
                    return 29;
                }

                boolean isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
                if (isLeap) {
                    return 29;
                } else {
                    return 28;
                }
            default:
                return 30;
        }
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\DatimeWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.TimeMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnDatimeSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.TimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DatimeEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleDateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleTimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class DatimeWheelLayout extends BaseWheelLayout {
    private DateWheelLayout dateWheelLayout;
    private TimeWheelLayout timeWheelLayout;
    private DatimeEntity startValue;
    private DatimeEntity endValue;
    private OnDatimeSelectedListener onDatimeSelectedListener;

    public DatimeWheelLayout(Context context) {
        super(context);
    }

    public DatimeWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DatimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DatimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_datime;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.DatimeWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        List<WheelView> list = new ArrayList<>();
        list.addAll(dateWheelLayout.provideWheelViews());
        list.addAll(timeWheelLayout.provideWheelViews());
        return list;
    }

    @Override
    protected void onInit(@NonNull Context context) {
        dateWheelLayout = findViewById(R.id.wheel_picker_date_wheel);
        timeWheelLayout = findViewById(R.id.wheel_picker_time_wheel);

        setCurtainEnabled(true);
        getMonthLabelView().setTextColor(0xffffffff);
        getYearLabelView().setTextColor(0xffffffff);
        getDayLabelView().setTextColor(0xffffffff);
        getHourLabelView().setTextColor(0xffffffff);
        getMinuteLabelView().setTextColor(0xffffffff);
        getSecondLabelView().setTextColor(0xffffffff);

        post(() -> {
            View view_select_bg = findViewById(R.id.view_select_bg);
            ViewGroup.LayoutParams params = view_select_bg.getLayoutParams();
            params.height = dateWheelLayout.getYearWheelView().itemHeight;
            view_select_bg.setLayoutParams(params);
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.DatimeWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColor, 0xFF888888));
        setTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_itemSpace,
                (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.DatimeWheelLayout_wheel_curvedMaxAngle, 90));
        setDateMode(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_dateMode, DateMode.YEAR_MONTH_DAY));
        setTimeMode(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_timeMode, TimeMode.HOUR_24_NO_SECOND));
        String yearLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_yearLabel);
        String monthLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_monthLabel);
        String dayLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_dayLabel);
        setDateLabel(yearLabel, monthLabel, dayLabel);
        String hourLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_hourLabel);
        String minuteLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_minuteLabel);
        String secondLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_secondLabel);
        setTimeLabel(hourLabel, minuteLabel, secondLabel);
        setDateFormatter(new SimpleDateFormatter());
        setTimeFormatter(new SimpleTimeFormatter(timeWheelLayout));
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(DatimeEntity.now(), DatimeEntity.yearOnFuture(30), DatimeEntity.now());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        dateWheelLayout.onWheelSelected(view, position);
        timeWheelLayout.onWheelSelected(view, position);
        if (onDatimeSelectedListener == null) {
            return;
        }
        timeWheelLayout.post(new Runnable() {
            @Override
            public void run() {
                onDatimeSelectedListener.onDatimeSelected(dateWheelLayout.getSelectedYear(),
                        dateWheelLayout.getSelectedMonth(), dateWheelLayout.getSelectedDay(),
                        timeWheelLayout.getSelectedHour(), timeWheelLayout.getSelectedMinute(),
                        timeWheelLayout.getSelectedSecond());
            }
        });
    }

    @Override
    public void onWheelScrolled(WheelView view, int offset) {
        dateWheelLayout.onWheelScrolled(view, offset);
        timeWheelLayout.onWheelScrolled(view, offset);
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        dateWheelLayout.onWheelScrollStateChanged(view, state);
        timeWheelLayout.onWheelScrollStateChanged(view, state);
    }

    @Override
    public void onWheelLoopFinished(WheelView view) {
        dateWheelLayout.onWheelLoopFinished(view);
        timeWheelLayout.onWheelLoopFinished(view);
    }

    public void setDateMode(@DateMode int dateMode) {
        dateWheelLayout.setDateMode(dateMode);
    }

    public void setTimeMode(@TimeMode int timeMode) {
        timeWheelLayout.setTimeMode(timeMode);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue, DatimeEntity defaultValue) {
        if (startValue == null) {
            startValue = DatimeEntity.now();
        }
        if (endValue == null) {
            endValue = DatimeEntity.yearOnFuture(10);
        }
        if (defaultValue == null) {
            defaultValue = startValue;
        }
        dateWheelLayout.setRange(startValue.getDate(), endValue.getDate(), defaultValue.getDate());
        timeWheelLayout.setRange(null, null, defaultValue.getTime());
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public void setDefaultValue(DatimeEntity defaultValue) {
        if (defaultValue == null) {
            defaultValue = DatimeEntity.now();
        }
        dateWheelLayout.setDefaultValue(defaultValue.getDate());
        timeWheelLayout.setDefaultValue(defaultValue.getTime());
    }

    public void setDateFormatter(DateFormatter dateFormatter) {
        dateWheelLayout.setDateFormatter(dateFormatter);
    }

    public void setTimeFormatter(TimeFormatter timeFormatter) {
        timeWheelLayout.setTimeFormatter(timeFormatter);
    }

    public void setDateLabel(CharSequence year, CharSequence month, CharSequence day) {
        dateWheelLayout.setDateLabel(year, month, day);
    }

    public void setTimeLabel(CharSequence hour, CharSequence minute, CharSequence second) {
        timeWheelLayout.setTimeLabel(hour, minute, second);
    }

    public void setOnDatimeSelectedListener(OnDatimeSelectedListener onDatimeSelectedListener) {
        this.onDatimeSelectedListener = onDatimeSelectedListener;
    }

    public void setResetWhenLinkage(boolean dateResetWhenLinkage, boolean timeResetWhenLinkage) {
        dateWheelLayout.setResetWhenLinkage(dateResetWhenLinkage);
        timeWheelLayout.setResetWhenLinkage(timeResetWhenLinkage);
    }

    public final DatimeEntity getStartValue() {
        return startValue;
    }

    public final DatimeEntity getEndValue() {
        return endValue;
    }

    public final DateWheelLayout getDateWheelLayout() {
        return dateWheelLayout;
    }

    public final TimeWheelLayout getTimeWheelLayout() {
        return timeWheelLayout;
    }

    public final NumberWheelView getYearWheelView() {
        return dateWheelLayout.getYearWheelView();
    }

    public final NumberWheelView getMonthWheelView() {
        return dateWheelLayout.getMonthWheelView();
    }

    public final NumberWheelView getDayWheelView() {
        return dateWheelLayout.getDayWheelView();
    }

    public final NumberWheelView getHourWheelView() {
        return timeWheelLayout.getHourWheelView();
    }

    public final NumberWheelView getMinuteWheelView() {
        return timeWheelLayout.getMinuteWheelView();
    }

    public final NumberWheelView getSecondWheelView() {
        return timeWheelLayout.getSecondWheelView();
    }

    public final WheelView getMeridiemWheelView() {
        return timeWheelLayout.getMeridiemWheelView();
    }

    public final TextView getYearLabelView() {
        return dateWheelLayout.getYearLabelView();
    }

    public final TextView getMonthLabelView() {
        return dateWheelLayout.getMonthLabelView();
    }

    public final TextView getDayLabelView() {
        return dateWheelLayout.getDayLabelView();
    }

    public final TextView getHourLabelView() {
        return timeWheelLayout.getHourLabelView();
    }

    public final TextView getMinuteLabelView() {
        return timeWheelLayout.getMinuteLabelView();
    }

    public final TextView getSecondLabelView() {
        return timeWheelLayout.getSecondLabelView();
    }

    public final TextView getSpaceStartView() {
        return dateWheelLayout.getSpaceStartView();
    }

    public final TextView getSpaceEndView() {
        return dateWheelLayout.getSpaceEndView();
    }

    public final int getSelectedYear() {
        return dateWheelLayout.getSelectedYear();
    }

    public final int getSelectedMonth() {
        return dateWheelLayout.getSelectedMonth();
    }

    public final int getSelectedDay() {
        return dateWheelLayout.getSelectedDay();
    }

    public final int getSelectedHour() {
        return timeWheelLayout.getSelectedHour();
    }

    public final int getSelectedMinute() {
        return timeWheelLayout.getSelectedMinute();
    }

    public final int getSelectedSecond() {
        return timeWheelLayout.getSelectedSecond();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\LinkageWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.LinkageProvider;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnLinkageSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class LinkageWheelLayout extends BaseWheelLayout {
    private WheelView firstWheelView, secondWheelView, thirdWheelView;
    private TextView firstLabelView, secondLabelView, thirdLabelView;
    private ProgressBar loadingView;
    private Object firstValue, secondValue, thirdValue;
    private int firstIndex, secondIndex, thirdIndex;
    private LinkageProvider dataProvider;
    private OnLinkageSelectedListener onLinkageSelectedListener;

    public LinkageWheelLayout(Context context) {
        super(context);
    }

    public LinkageWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkageWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LinkageWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_linkage;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.LinkageWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(firstWheelView, secondWheelView, thirdWheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        firstWheelView = findViewById(R.id.wheel_picker_linkage_first_wheel);
        secondWheelView = findViewById(R.id.wheel_picker_linkage_second_wheel);
        thirdWheelView = findViewById(R.id.wheel_picker_linkage_third_wheel);
        firstLabelView = findViewById(R.id.wheel_picker_linkage_first_label);
        secondLabelView = findViewById(R.id.wheel_picker_linkage_second_label);
        thirdLabelView = findViewById(R.id.wheel_picker_linkage_third_label);
        loadingView = findViewById(R.id.wheel_picker_linkage_loading);
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.LinkageWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.LinkageWheelLayout_wheel_maxWidthText));
        setTextSize(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setTextColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.LinkageWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.LinkageWheelLayout_wheel_itemSpace,
                (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.LinkageWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.LinkageWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.LinkageWheelLayout_wheel_curvedMaxAngle, 90));
        setFirstVisible(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_firstVisible, true));
        setThirdVisible(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_thirdVisible, true));
        String firstLabel = typedArray.getString(R.styleable.LinkageWheelLayout_wheel_firstLabel);
        String secondLabel = typedArray.getString(R.styleable.LinkageWheelLayout_wheel_secondLabel);
        String thirdLabel = typedArray.getString(R.styleable.LinkageWheelLayout_wheel_thirdLabel);
        setLabel(firstLabel, secondLabel, thirdLabel);
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_linkage_first_wheel) {
            firstIndex = position;
            secondIndex = 0;
            thirdIndex = 0;
            changeSecondData();
            changeThirdData();
            selectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_linkage_second_wheel) {
            secondIndex = position;
            thirdIndex = 0;
            changeThirdData();
            selectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_linkage_third_wheel) {
            thirdIndex = position;
            selectedCallback();
        }
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        int id = view.getId();
        if (id == R.id.wheel_picker_linkage_first_wheel) {
            secondWheelView.setEnabled(state == ScrollState.IDLE);
            thirdWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_linkage_second_wheel) {
            firstWheelView.setEnabled(state == ScrollState.IDLE);
            thirdWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_linkage_third_wheel) {
            firstWheelView.setEnabled(state == ScrollState.IDLE);
            secondWheelView.setEnabled(state == ScrollState.IDLE);
        }
    }

    public void setData(@NonNull LinkageProvider provider) {
        setFirstVisible(provider.firstLevelVisible());
        setThirdVisible(provider.thirdLevelVisible());
        if (firstValue != null) {
            firstIndex = provider.findFirstIndex(firstValue);
        }
        if (secondValue != null) {
            secondIndex = provider.findSecondIndex(firstIndex, secondValue);
        }
        if (thirdValue != null) {
            thirdIndex = provider.findThirdIndex(firstIndex, secondIndex, thirdValue);
        }
        dataProvider = provider;
        changeFirstData();
        changeSecondData();
        changeThirdData();
    }

    public void setDefaultValue(Object first, Object second, Object third) {
        if (dataProvider != null) {
            firstIndex = dataProvider.findFirstIndex(first);
            secondIndex = dataProvider.findSecondIndex(firstIndex, second);
            thirdIndex = dataProvider.findThirdIndex(firstIndex, secondIndex, third);
            changeFirstData();
            changeSecondData();
            changeThirdData();
        } else {
            this.firstValue = first;
            this.secondValue = second;
            this.thirdValue = third;
        }
    }

    public void setFormatter(WheelFormatter first, WheelFormatter second, WheelFormatter third) {
        firstWheelView.setFormatter(first);
        secondWheelView.setFormatter(second);
        thirdWheelView.setFormatter(third);
    }

    public void setLabel(CharSequence first, CharSequence second, CharSequence third) {
        firstLabelView.setText(first);
        secondLabelView.setText(second);
        thirdLabelView.setText(third);
    }

    public void showLoading() {
        loadingView.setVisibility(VISIBLE);
    }

    public void hideLoading() {
        loadingView.setVisibility(GONE);
    }

    public void setOnLinkageSelectedListener(OnLinkageSelectedListener onLinkageSelectedListener) {
        this.onLinkageSelectedListener = onLinkageSelectedListener;
    }

    public void setFirstVisible(boolean visible) {
        if (visible) {
            firstWheelView.setVisibility(VISIBLE);
            firstLabelView.setVisibility(VISIBLE);
        } else {
            firstWheelView.setVisibility(GONE);
            firstLabelView.setVisibility(GONE);
        }
    }

    public void setThirdVisible(boolean visible) {
        if (visible) {
            thirdWheelView.setVisibility(VISIBLE);
            thirdLabelView.setVisibility(VISIBLE);
        } else {
            thirdWheelView.setVisibility(GONE);
            thirdLabelView.setVisibility(GONE);
        }
    }

    private void selectedCallback() {
        if (onLinkageSelectedListener == null) {
            return;
        }
        thirdWheelView.post(new Runnable() {
            @Override
            public void run() {
                Object first = firstWheelView.getCurrentItem();
                Object second = secondWheelView.getCurrentItem();
                Object third = thirdWheelView.getCurrentItem();
                onLinkageSelectedListener.onLinkageSelected(first, second, third);
            }
        });
    }

    private void changeFirstData() {
        firstWheelView.setData(dataProvider.provideFirstData());
        firstWheelView.setDefaultPosition(firstIndex);
    }

    private void changeSecondData() {
        secondWheelView.setData(dataProvider.linkageSecondData(firstIndex));
        secondWheelView.setDefaultPosition(secondIndex);
    }

    private void changeThirdData() {
        if (!dataProvider.thirdLevelVisible()) {
            return;
        }
        thirdWheelView.setData(dataProvider.linkageThirdData(firstIndex, secondIndex));
        thirdWheelView.setDefaultPosition(thirdIndex);
    }

    public final WheelView getFirstWheelView() {
        return firstWheelView;
    }

    public final WheelView getSecondWheelView() {
        return secondWheelView;
    }

    public final WheelView getThirdWheelView() {
        return thirdWheelView;
    }

    public final TextView getFirstLabelView() {
        return firstLabelView;
    }

    public final TextView getSecondLabelView() {
        return secondLabelView;
    }

    public final TextView getThirdLabelView() {
        return thirdLabelView;
    }

    public final ProgressBar getLoadingView() {
        return loadingView;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\NumberWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnNumberSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnOptionSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

public class NumberWheelLayout extends OptionWheelLayout {
    private OnNumberSelectedListener onNumberSelectedListener;

    public NumberWheelLayout(Context context) {
        super(context);
    }

    public NumberWheelLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberWheelLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NumberWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.NumberWheelLayout;
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.NumberWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.NumberWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_itemTextColor, 0xFF888888));
        setTextSize(typedArray.getDimension(R.styleable.NumberWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.NumberWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.NumberWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.NumberWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimensionPixelSize(R.styleable.NumberWheelLayout_wheel_indicatorSize, (int) (1 * density)));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.NumberWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.NumberWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.NumberWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.NumberWheelLayout_wheel_curvedMaxAngle, 90));
        getLabelView().setText(typedArray.getString(R.styleable.NumberWheelLayout_wheel_label));
        float minNumber = typedArray.getFloat(R.styleable.NumberWheelLayout_wheel_minNumber, 0);
        float maxNumber = typedArray.getFloat(R.styleable.NumberWheelLayout_wheel_maxNumber, 10);
        float stepNumber = typedArray.getFloat(R.styleable.NumberWheelLayout_wheel_stepNumber, 1);
        boolean isDecimal = typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_isDecimal, false);
        if (isDecimal) {
            setRange(minNumber, maxNumber, stepNumber);
        } else {
            setRange((int) minNumber, (int) maxNumber, (int) stepNumber);
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        if (onNumberSelectedListener != null) {
            Object item = getWheelView().getItem(position);
            onNumberSelectedListener.onNumberSelected(position, (Number) item);
        }
    }

    @Deprecated
    @Override
    public void setData(List<?> data) {
        throw new UnsupportedOperationException("Use setRange instead");
    }

    @Deprecated
    @Override
    public void setOnOptionSelectedListener(OnOptionSelectedListener onOptionSelectedListener) {
        throw new UnsupportedOperationException("Use setOnNumberSelectedListener instead");
    }

    public void setOnNumberSelectedListener(OnNumberSelectedListener onNumberSelectedListener) {
        this.onNumberSelectedListener = onNumberSelectedListener;
    }

    public void setRange(int min, int max, int step) {
        int minValue = Math.min(min, max);
        int maxValue = Math.max(min, max);

        int capacity = (maxValue - minValue) / step;
        List<Integer> data = new ArrayList<>(capacity);
        for (int i = minValue; i <= maxValue; i = i + step) {
            data.add(i);
        }
        super.setData(data);
    }

    public void setRange(float min, float max, float step) {
        float minValue = Math.min(min, max);
        float maxValue = Math.max(min, max);

        int capacity = (int) ((maxValue - minValue) / step);
        List<Float> data = new ArrayList<>(capacity);
        for (float i = minValue; i <= maxValue; i = i + step) {
            data.add(i);
        }
        super.setData(data);
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\OptionWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnOptionSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class OptionWheelLayout extends BaseWheelLayout {
    private WheelView wheelView;
    private TextView labelView;
    private OnOptionSelectedListener onOptionSelectedListener;

    public OptionWheelLayout(Context context) {
        super(context);
    }

    public OptionWheelLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OptionWheelLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OptionWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_option;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.OptionWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Collections.singletonList(wheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        wheelView = findViewById(R.id.wheel_picker_option_wheel);
        labelView = findViewById(R.id.wheel_picker_option_label);

        post(() -> {
            View view_select_bg = findViewById(R.id.view_select_bg);
            ViewGroup.LayoutParams params = view_select_bg.getLayoutParams();
            params.height = wheelView.itemHeight;
            view_select_bg.setLayoutParams(params);
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.OptionWheelLayout_wheel_maxWidthText));
        setTextColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextSize(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.OptionWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.OptionWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.OptionWheelLayout_wheel_curvedMaxAngle, 90));
        labelView.setText(typedArray.getString(R.styleable.OptionWheelLayout_wheel_label));
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        if (onOptionSelectedListener != null) {
            onOptionSelectedListener.onOptionSelected(position, wheelView.getItem(position));
        }
    }

    public void setData(List<?> data) {
        wheelView.setData(data);
    }

    public void setDefaultValue(Object value) {
        wheelView.setDefaultValue(value);
    }

    public void setDefaultPosition(int position) {
        wheelView.setDefaultPosition(position);
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener onOptionSelectedListener) {
        this.onOptionSelectedListener = onOptionSelectedListener;
    }

    public final WheelView getWheelView() {
        return wheelView;
    }

    public final TextView getLabelView() {
        return labelView;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\TimeWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.TimeMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnTimeMeridiemSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnTimeSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.TimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.TimeEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleTimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class TimeWheelLayout extends BaseWheelLayout {
    private NumberWheelView hourWheelView;
    private NumberWheelView minuteWheelView;
    private NumberWheelView secondWheelView;
    private TextView hourLabelView;
    private TextView minuteLabelView;
    private TextView secondLabelView;
    private TextView spaceEndView;
    private WheelView meridiemWheelView;
    private TimeEntity startValue;
    private TimeEntity endValue;
    private Integer selectedHour;
    private Integer selectedMinute;
    private Integer selectedSecond;
    private boolean isAnteMeridiem;
    private int timeMode;
    private int hourStep = 1;
    private int minuteStep = 1;
    private int secondStep = 1;
    private OnTimeSelectedListener onTimeSelectedListener;
    private OnTimeMeridiemSelectedListener onTimeMeridiemSelectedListener;
    private boolean resetWhenLinkage = true;

    public TimeWheelLayout(Context context) {
        super(context);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_time;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.TimeWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(hourWheelView, minuteWheelView, secondWheelView, meridiemWheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        hourWheelView = findViewById(R.id.wheel_picker_time_hour_wheel);
        minuteWheelView = findViewById(R.id.wheel_picker_time_minute_wheel);
        secondWheelView = findViewById(R.id.wheel_picker_time_second_wheel);
        hourLabelView = findViewById(R.id.wheel_picker_time_hour_label);
        minuteLabelView = findViewById(R.id.wheel_picker_time_minute_label);
        secondLabelView = findViewById(R.id.wheel_picker_time_second_label);
        meridiemWheelView = findViewById(R.id.wheel_picker_time_meridiem_wheel);
        spaceEndView = findViewById(R.id.wheel_picker_time_end_view);

        post(new Runnable() {
            @Override
            public void run() {
                hourLabelView.setHeight(minuteWheelView.itemHeight);
                minuteLabelView.setHeight(minuteWheelView.itemHeight);
                secondLabelView.setHeight(minuteWheelView.itemHeight);
                spaceEndView.setHeight(minuteWheelView.itemHeight);
            }
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.TimeWheelLayout_wheel_maxWidthText));
        setTextColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextSize(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.TimeWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.TimeWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_curtainColor, 0));
        setCurtainCorner(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.TimeWheelLayout_wheel_curvedMaxAngle, 90));
        setTimeMode(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_timeMode, TimeMode.HOUR_24_NO_SECOND));
        String hourLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_hourLabel);
        String minuteLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_minuteLabel);
        String secondLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_secondLabel);
        setTimeLabel(hourLabel, minuteLabel, secondLabel);
        setTimeFormatter(new SimpleTimeFormatter(this));
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(TimeEntity.target(0, 0, 0),
                    TimeEntity.target(23, 59, 59), TimeEntity.now());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_time_hour_wheel) {
            selectedHour = hourWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedMinute = null;
                selectedSecond = null;
            }
            changeMinute(selectedHour);
            timeSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_time_minute_wheel) {
            selectedMinute = minuteWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedSecond = null;
            }
            changeSecond();
            timeSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_time_second_wheel) {
            selectedSecond = secondWheelView.getItem(position);
            timeSelectedCallback();
        }
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        int id = view.getId();
        if (id == R.id.wheel_picker_time_hour_wheel) {
            minuteWheelView.setEnabled(state == ScrollState.IDLE);
            secondWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_time_minute_wheel) {
            hourWheelView.setEnabled(state == ScrollState.IDLE);
            secondWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_time_second_wheel) {
            hourWheelView.setEnabled(state == ScrollState.IDLE);
            minuteWheelView.setEnabled(state == ScrollState.IDLE);
        }
    }

    private void timeSelectedCallback() {
        if (onTimeSelectedListener != null) {
            secondWheelView.post(new Runnable() {
                @Override
                public void run() {
                    onTimeSelectedListener.onTimeSelected(selectedHour, selectedMinute, selectedSecond);
                }
            });
        }
        if (onTimeMeridiemSelectedListener != null) {
            secondWheelView.post(new Runnable() {
                @Override
                public void run() {
                    onTimeMeridiemSelectedListener.onTimeSelected(selectedHour, selectedMinute, selectedSecond, isAnteMeridiem());
                }
            });
        }
    }

    public void setTimeMode(@TimeMode int timeMode) {
        this.timeMode = timeMode;
        hourWheelView.setVisibility(View.VISIBLE);
        hourLabelView.setVisibility(View.VISIBLE);
        minuteWheelView.setVisibility(View.VISIBLE);
        minuteLabelView.setVisibility(View.VISIBLE);
        secondWheelView.setVisibility(View.VISIBLE);
        secondLabelView.setVisibility(View.VISIBLE);
        meridiemWheelView.setVisibility(View.GONE);
        if (timeMode == TimeMode.NONE) {
            hourWheelView.setVisibility(View.GONE);
            hourLabelView.setVisibility(View.GONE);
            minuteWheelView.setVisibility(View.GONE);
            minuteLabelView.setVisibility(View.GONE);
            secondWheelView.setVisibility(View.GONE);
            secondLabelView.setVisibility(View.GONE);
            this.timeMode = timeMode;
            return;
        }
        if (timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_24_NO_SECOND) {
            secondWheelView.setVisibility(View.GONE);
            secondLabelView.setVisibility(View.GONE);
            minuteLabelView.setVisibility(View.GONE);
        }
        if (isHour12Mode()) {
            meridiemWheelView.setVisibility(View.VISIBLE);
            meridiemWheelView.setData(Arrays.asList("AM", "PM"));
        }
    }

    public boolean isHour12Mode() {
        return timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_12_HAS_SECOND;
    }

    public void setRange(TimeEntity startValue, TimeEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(TimeEntity startValue, TimeEntity endValue, TimeEntity defaultValue) {
        if (startValue == null) {
            startValue = TimeEntity.target(isHour12Mode() ? 1 : 0, 0, 0);
        }
        if (endValue == null) {
            endValue = TimeEntity.target(isHour12Mode() ? 12 : 23, 59, 59);
        }
        if (endValue.toTimeInMillis() < startValue.toTimeInMillis()) {
            throw new IllegalArgumentException("Ensure the start time is less than the time date");
        }
        this.startValue = startValue;
        this.endValue = endValue;
        if (defaultValue != null) {
            isAnteMeridiem = defaultValue.getHour() <= 12;
            defaultValue.setHour(wrapHour(defaultValue.getHour()));
            selectedHour = defaultValue.getHour();
            selectedMinute = defaultValue.getMinute();
            selectedSecond = defaultValue.getSecond();
        } else {
            selectedHour = null;
            selectedMinute = null;
            selectedSecond = null;
        }
        changeHour();
        changeAnteMeridiem();
    }

    public void setDefaultValue(@NonNull final TimeEntity defaultValue) {
        setRange(startValue, endValue, defaultValue);
    }

    public void setTimeFormatter(final TimeFormatter timeFormatter) {
        if (timeFormatter == null) {
            return;
        }
        hourWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatHour((Integer) value);
            }
        });
        minuteWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatMinute((Integer) value);
            }
        });
        secondWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatSecond((Integer) value);
            }
        });
    }

    public void setTimeLabel(CharSequence hour, CharSequence minute, CharSequence second) {
        hourLabelView.setText(hour);
        minuteLabelView.setText(minute);
        secondLabelView.setText(second);
    }

    public void setOnTimeSelectedListener(OnTimeSelectedListener onTimeSelectedListener) {
        this.onTimeSelectedListener = onTimeSelectedListener;
    }

    public void setOnTimeMeridiemSelectedListener(OnTimeMeridiemSelectedListener onTimeMeridiemSelectedListener) {
        this.onTimeMeridiemSelectedListener = onTimeMeridiemSelectedListener;
    }

    public void setResetWhenLinkage(boolean resetWhenLinkage) {
        this.resetWhenLinkage = resetWhenLinkage;
    }

    public void setTimeStep(int hourStep, int minuteStep, int secondStep) {
        this.hourStep = hourStep;
        this.minuteStep = minuteStep;
        this.secondStep = secondStep;
        if (isDataAlready()) {
            setRange(startValue, endValue, TimeEntity.target(selectedHour, selectedMinute, selectedSecond));
        }
    }

    public boolean isDataAlready() {
        return startValue != null && endValue != null;
    }

    public final TimeEntity getStartValue() {
        return startValue;
    }

    public final TimeEntity getEndValue() {
        return endValue;
    }

    public final NumberWheelView getHourWheelView() {
        return hourWheelView;
    }

    public final NumberWheelView getMinuteWheelView() {
        return minuteWheelView;
    }

    public final NumberWheelView getSecondWheelView() {
        return secondWheelView;
    }

    public final TextView getHourLabelView() {
        return hourLabelView;
    }

    public final TextView getMinuteLabelView() {
        return minuteLabelView;
    }

    public final TextView getSecondLabelView() {
        return secondLabelView;
    }

    public final WheelView getMeridiemWheelView() {
        return meridiemWheelView;
    }

    @Deprecated
    public final TextView getMeridiemLabelView() {
        throw new UnsupportedOperationException("Use getMeridiemWheelView instead");
    }

    public final int getSelectedHour() {
        int hour = hourWheelView.getCurrentItem();
        return wrapHour(hour);
    }

    private int wrapHour(int hour) {
        if (isHour12Mode() && hour > 12) {
            hour = hour - 12;
        }
        return hour;
    }

    public final int getSelectedMinute() {
        return minuteWheelView.getCurrentItem();
    }

    public final int getSelectedSecond() {
        if (timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_24_NO_SECOND) {
            return 0;
        }
        return secondWheelView.getCurrentItem();
    }

    public final boolean isAnteMeridiem() {
        return meridiemWheelView.getCurrentItem().toString().equalsIgnoreCase("AM");
    }

    private void changeHour() {
        int min = Math.min(startValue.getHour(), endValue.getHour());
        int max = Math.max(startValue.getHour(), endValue.getHour());
        int minHour = isHour12Mode() ? 1 : 0;
        int maxHour = isHour12Mode() ? 12 : 23;
        min = Math.max(minHour, min);
        max = Math.min(maxHour, max);
        if (selectedHour == null) {
            selectedHour = min;
        } else {
            selectedHour = Math.max(selectedHour, min);
            selectedHour = Math.min(selectedHour, max);
        }
        hourWheelView.setRange(min, max, hourStep);
        hourWheelView.setDefaultValue(selectedHour);
        changeMinute(selectedHour);
    }

    private void changeMinute(int hour) {
        final int min, max;

        if (hour == startValue.getHour() && hour == endValue.getHour()) {
            min = startValue.getMinute();
            max = endValue.getMinute();
        } else if (hour == startValue.getHour()) {
            min = startValue.getMinute();
            max = 59;
        } else if (hour == endValue.getHour()) {
            min = 0;
            max = endValue.getMinute();
        } else {
            min = 0;
            max = 59;
        }
        if (selectedMinute == null) {
            selectedMinute = min;
        } else {
            selectedMinute = Math.max(selectedMinute, min);
            selectedMinute = Math.min(selectedMinute, max);
        }
        minuteWheelView.setRange(min, max, minuteStep);
        minuteWheelView.setDefaultValue(selectedMinute);
        changeSecond();
    }

    private void changeSecond() {
        if (selectedSecond == null) {
            selectedSecond = 0;
        }
        secondWheelView.setRange(0, 59, secondStep);
        secondWheelView.setDefaultValue(selectedSecond);
    }

    private void changeAnteMeridiem() {
        meridiemWheelView.setDefaultValue(isAnteMeridiem ? "AM" : "PM");
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\YearWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DatimeEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleDateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class YearWheelLayout extends BaseWheelLayout {
    private DateWheelLayout dateWheelLayout;
    private DatimeEntity startValue;
    private DatimeEntity endValue;

    public YearWheelLayout(Context context) {
        super(context);
    }

    public YearWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public YearWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public YearWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_year;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.DatimeWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        List<WheelView> list = new ArrayList<>();
        list.addAll(dateWheelLayout.provideWheelViews());
        return list;
    }

    @Override
    protected void onInit(@NonNull Context context) {
        dateWheelLayout = findViewById(R.id.wheel_picker_date_wheel);

        setCurtainEnabled(true);
        getMonthLabelView().setBackgroundColor(0x1A2B79D7);
        getYearLabelView().setBackgroundColor(0x1A2B79D7);
        getDayLabelView().setBackgroundColor(0x1A2B79D7);

        post(() -> {
            View view_select_bg = findViewById(R.id.view_select_bg);
            ViewGroup.LayoutParams params = view_select_bg.getLayoutParams();
            params.height = dateWheelLayout.getYearWheelView().itemHeight;
            view_select_bg.setLayoutParams(params);
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.DatimeWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColor, 0xFF888888));
        setTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_itemSpace,
                (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.DatimeWheelLayout_wheel_curvedMaxAngle, 90));
        setDateMode(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_dateMode, DateMode.YEAR));
        String yearLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_yearLabel);
        String monthLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_monthLabel);
        String dayLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_dayLabel);
        setDateLabel(yearLabel, monthLabel, dayLabel);
        setDateFormatter(new SimpleDateFormatter());
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(DatimeEntity.now(), DatimeEntity.yearOnFuture(30), DatimeEntity.now());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        dateWheelLayout.onWheelSelected(view, position);
    }

    @Override
    public void onWheelScrolled(WheelView view, int offset) {
        dateWheelLayout.onWheelScrolled(view, offset);
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        dateWheelLayout.onWheelScrollStateChanged(view, state);
    }

    @Override
    public void onWheelLoopFinished(WheelView view) {
        dateWheelLayout.onWheelLoopFinished(view);
    }

    public void setDateMode(@DateMode int dateMode) {
        dateWheelLayout.setDateMode(dateMode);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue, DatimeEntity defaultValue) {
        if (startValue == null) {
            startValue = DatimeEntity.now();
        }
        if (endValue == null) {
            endValue = DatimeEntity.yearOnFuture(10);
        }
        if (defaultValue == null) {
            defaultValue = startValue;
        }
        dateWheelLayout.setRange(startValue.getDate(), endValue.getDate(), defaultValue.getDate());
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public void setDefaultValue(DatimeEntity defaultValue) {
        if (defaultValue == null) {
            defaultValue = DatimeEntity.now();
        }
        dateWheelLayout.setDefaultValue(defaultValue.getDate());
    }

    public void setDateFormatter(DateFormatter dateFormatter) {
        dateWheelLayout.setDateFormatter(dateFormatter);
    }

    public void setDateLabel(CharSequence year, CharSequence month, CharSequence day) {
        dateWheelLayout.setDateLabel(year, month, day);
    }

    public void setResetWhenLinkage(boolean dateResetWhenLinkage, boolean timeResetWhenLinkage) {
        dateWheelLayout.setResetWhenLinkage(dateResetWhenLinkage);
    }

    public final DatimeEntity getStartValue() {
        return startValue;
    }

    public final DatimeEntity getEndValue() {
        return endValue;
    }

    public final DateWheelLayout getDateWheelLayout() {
        return dateWheelLayout;
    }

    public final NumberWheelView getYearWheelView() {
        return dateWheelLayout.getYearWheelView();
    }

    public final NumberWheelView getMonthWheelView() {
        return dateWheelLayout.getMonthWheelView();
    }

    public final NumberWheelView getDayWheelView() {
        return dateWheelLayout.getDayWheelView();
    }

    public final TextView getYearLabelView() {
        return dateWheelLayout.getYearLabelView();
    }

    public final TextView getMonthLabelView() {
        return dateWheelLayout.getMonthLabelView();
    }

    public final TextView getDayLabelView() {
        return dateWheelLayout.getDayLabelView();
    }

    public final TextView getSpaceStartView() {
        return dateWheelLayout.getSpaceStartView();
    }

    public final TextView getSpaceEndView() {
        return dateWheelLayout.getSpaceEndView();
    }

    public final int getSelectedYear() {
        return dateWheelLayout.getSelectedYear();
    }

    public final int getSelectedMonth() {
        return dateWheelLayout.getSelectedMonth();
    }

    public final int getSelectedDay() {
        return dateWheelLayout.getSelectedDay();
    }

}