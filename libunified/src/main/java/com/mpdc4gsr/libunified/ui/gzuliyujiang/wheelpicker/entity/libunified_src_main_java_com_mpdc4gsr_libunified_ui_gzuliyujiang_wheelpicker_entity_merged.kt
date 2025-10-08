// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity' directory and its subdirectories.
// Total files: 7 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\ConstellationEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class ConstellationEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String id;
    private String startDate;
    private String endDate;
    private String name;
    private String english;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return english;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConstellationEntity that = (ConstellationEntity) o;
        return Objects.equals(id, that.id) ||
                Objects.equals(startDate, that.startDate) ||
                Objects.equals(endDate, that.endDate) ||
                Objects.equals(name, that.name) ||
                Objects.equals(english, that.english);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startDate, endDate, name, english);
    }

    @NonNull
    @Override
    public String toString() {
        return "ConstellationEntity{" +
                "id='" + id + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", name='" + name + '\'' +
                ", english" + english + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\DateEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;

@SuppressWarnings({"unused"})
public class DateEntity implements Serializable {
    private int year;
    private int month;
    private int day;

    public static DateEntity target(int year, int month, int day) {
        DateEntity entity = new DateEntity();
        entity.setYear(year);
        entity.setMonth(month);
        entity.setDay(day);
        return entity;
    }

    public static DateEntity today() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return target(year, month, day);
    }

    public static DateEntity dayOnFuture(int day) {
        DateEntity entity = today();
        entity.setDay(entity.getDay() + day);
        return entity;
    }

    public static DateEntity monthOnFuture(int month) {
        DateEntity entity = today();
        entity.setMonth(entity.getMonth() + month);
        return entity;
    }

    public static DateEntity yearOnFuture(int year) {
        DateEntity entity = today();
        entity.setYear(entity.getYear() + year);
        return entity;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public long toTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DateEntity that = (DateEntity) o;
        return year == that.year &&
                month == that.month &&
                day == that.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    @NonNull
    @Override
    public String toString() {
        return year + "-" + month + "-" + day;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\DatimeEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;

@SuppressWarnings({"unused"})
public class DatimeEntity implements Serializable {
    private DateEntity date;
    private TimeEntity time;

    public static DatimeEntity now() {
        DatimeEntity entity = new DatimeEntity();
        entity.setDate(DateEntity.today());
        entity.setTime(TimeEntity.now());
        return entity;
    }

    public static DatimeEntity minuteOnFuture(int minute) {
        DatimeEntity entity = now();
        entity.setTime(TimeEntity.minuteOnFuture(minute));
        return entity;
    }

    public static DatimeEntity hourOnFuture(int hour) {
        DatimeEntity entity = now();
        entity.setTime(TimeEntity.hourOnFuture(hour));
        return entity;
    }

    public static DatimeEntity dayOnFuture(int day) {
        DatimeEntity entity = now();
        entity.setDate(DateEntity.dayOnFuture(day));
        return entity;
    }

    public static DatimeEntity monthOnFuture(int month) {
        DatimeEntity entity = now();
        entity.setDate(DateEntity.monthOnFuture(month));
        return entity;
    }

    public static DatimeEntity yearOnFuture(int year) {
        DatimeEntity entity = now();
        entity.setDate(DateEntity.yearOnFuture(year));
        return entity;
    }

    public DateEntity getDate() {
        return date;
    }

    public void setDate(DateEntity date) {
        this.date = date;
    }

    public TimeEntity getTime() {
        return time;
    }

    public void setTime(TimeEntity time) {
        this.time = time;
    }

    public long toTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
        calendar.set(Calendar.MINUTE, time.getMinute());
        calendar.set(Calendar.SECOND, time.getSecond());
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @NonNull
    @Override
    public String toString() {
        return date.toString() + " " + time.toString();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\EthnicEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class EthnicEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String code;
    private String name;
    private String spelling;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpelling() {
        return spelling;
    }

    public void setSpelling(String spelling) {
        this.spelling = spelling;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return spelling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EthnicEntity that = (EthnicEntity) o;
        return Objects.equals(code, that.code) ||
                Objects.equals(name, that.name) ||
                Objects.equals(spelling, that.spelling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, spelling);
    }

    @NonNull
    @Override
    public String toString() {
        return "EthnicEntity{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", spelling='" + spelling + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\PhoneCodeEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class PhoneCodeEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String code;
    private String name;
    private String english;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return english;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhoneCodeEntity that = (PhoneCodeEntity) o;
        return Objects.equals(code, that.code) ||
                Objects.equals(name, that.name) ||
                Objects.equals(english, that.english);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, english);
    }

    @NonNull
    @Override
    public String toString() {
        return "PhoneCodeEntity{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", english" + english + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\SexEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class SexEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String id;
    private String name;
    private String english;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return english;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SexEntity that = (SexEntity) o;
        return Objects.equals(id, that.id) ||
                Objects.equals(name, that.name) ||
                Objects.equals(english, that.english);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, english);
    }

    @NonNull
    @Override
    public String toString() {
        return "SexEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", english" + english + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\TimeEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;

@SuppressWarnings({"unused"})
public class TimeEntity implements Serializable {
    private int hour;
    private int minute;
    private int second;

    public static TimeEntity target(int hour, int minute, int second) {
        TimeEntity entity = new TimeEntity();
        entity.setHour(hour);
        entity.setMinute(minute);
        entity.setSecond(second);
        return entity;
    }

    public static TimeEntity now() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return target(hour, minute, second);
    }

    public static TimeEntity minuteOnFuture(int minute) {
        TimeEntity entity = now();
        entity.setMinute(entity.getMinute() + minute);
        return entity;
    }

    public static TimeEntity hourOnFuture(int hour) {
        TimeEntity entity = now();
        entity.setHour(entity.getHour() + hour);
        return entity;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public long toTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @NonNull
    @Override
    public String toString() {
        return hour + ":" + minute + ":" + second;
    }

}