// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract' directory and its subdirectories.
// Total files: 19 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\DateFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface DateFormatter {

    String formatYear(int year);

    String formatMonth(int month);

    String formatDay(int day);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\LinkageProvider.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

import androidx.annotation.NonNull;

import java.util.List;

public interface LinkageProvider {
    int INDEX_NO_FOUND = -1;

    boolean firstLevelVisible();

    boolean thirdLevelVisible();

    @NonNull
    List<?> provideFirstData();

    @NonNull
    List<?> linkageSecondData(int firstIndex);

    @NonNull
    List<?> linkageThirdData(int firstIndex, int secondIndex);

    int findFirstIndex(Object firstValue);

    int findSecondIndex(int firstIndex, Object secondValue);

    int findThirdIndex(int firstIndex, int secondIndex, Object thirdValue);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnCarPlatePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnCarPlatePickedListener {

    void onCarNumberPicked(String province, String letter);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDatePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDatePickedListener {

    void onDatePicked(int year, int month, int day);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDateSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDateSelectedListener {

    void onDateSelected(int year, int month, int day);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDatimePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDatimePickedListener {

    void onDatimePicked(int year, int month, int day, int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDatimeSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDatimeSelectedListener {

    void onDatimeSelected(int year, int month, int day, int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnLinkagePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnLinkagePickedListener {

    void onLinkagePicked(Object first, Object second, Object third);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnLinkageSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnLinkageSelectedListener {

    void onLinkageSelected(Object first, Object second, Object third);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnNumberPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnNumberPickedListener {

    void onNumberPicked(int position, Number item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnNumberSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnNumberSelectedListener {

    void onNumberSelected(int position, Number item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnOptionPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnOptionPickedListener {

    void onOptionPicked(int position, Object item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnOptionSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnOptionSelectedListener {

    void onOptionSelected(int position, Object item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimeMeridiemPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimeMeridiemPickedListener {

    void onTimePicked(int hour, int minute, int second, boolean isAnteMeridiem);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimeMeridiemSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimeMeridiemSelectedListener {

    void onTimeSelected(int hour, int minute, int second, boolean isAnteMeridiem);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimePickedListener {

    void onTimePicked(int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimeSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimeSelectedListener {

    void onTimeSelected(int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnYearPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnYearPickedListener {

    void onYearPicked(int year);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\TimeFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface TimeFormatter {

    String formatHour(int hour);

    String formatMinute(int minute);

    String formatSecond(int second);

}