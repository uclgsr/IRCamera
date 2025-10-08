// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\annotation' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\annotation\DateMode.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface DateMode {

    int NONE = -1;

    int YEAR_MONTH_DAY = 0;

    int YEAR_MONTH = 1;

    int MONTH_DAY = 2;

    int YEAR = 3;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\annotation\EthnicSpec.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation;

public @interface EthnicSpec {
    int DEFAULT = 1;
    int GB3304_91 = 2;
    int SEVENTH_NATIONAL_CENSUS = 3;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\annotation\TimeMode.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface TimeMode {

    int NONE = -1;

    int HOUR_24_NO_SECOND = 0;

    int HOUR_24_HAS_SECOND = 1;

    int HOUR_12_NO_SECOND = 2;

    int HOUR_12_HAS_SECOND = 3;
}