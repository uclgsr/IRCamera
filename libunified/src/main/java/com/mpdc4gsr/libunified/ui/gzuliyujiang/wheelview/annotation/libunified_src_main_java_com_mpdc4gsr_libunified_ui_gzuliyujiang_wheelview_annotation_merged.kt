// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\annotation' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\annotation\CurtainCorner.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface CurtainCorner {
    int NONE = 0;
    int ALL = 1;
    int TOP = 2;
    int BOTTOM = 3;
    int LEFT = 4;
    int RIGHT = 5;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\annotation\ItemTextAlign.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface ItemTextAlign {
    int CENTER = 0;
    int LEFT = 1;
    int RIGHT = 2;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\annotation\ScrollState.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface ScrollState {
    int IDLE = 0;
    int DRAGGING = 1;
    int SCROLLING = 2;
}