package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelview.contract;

import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelview.widget.WheelView;

public interface OnWheelChangedListener {

    void onWheelScrolled(WheelView view, int offset);

    void onWheelSelected(WheelView view, int position);

    void onWheelScrollStateChanged(WheelView view, @ScrollState int state);

    void onWheelLoopFinished(WheelView view);

}


