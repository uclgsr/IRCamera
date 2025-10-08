// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\contract' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\contract\OnWheelChangedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

public interface OnWheelChangedListener {

    void onWheelScrolled(WheelView view, int offset);

    void onWheelSelected(WheelView view, int position);

    void onWheelScrollStateChanged(WheelView view, @ScrollState int state);

    void onWheelLoopFinished(WheelView view);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\contract\TextProvider.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract;

public interface TextProvider {

    String provideText();

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\contract\WheelFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract;

import androidx.annotation.NonNull;

public interface WheelFormatter {

    String formatItem(@NonNull Object item);

}