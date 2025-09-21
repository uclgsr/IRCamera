package com.mpdc4gsr.libunified.ui.charting.renderer;

import com.mpdc4gsr.libunified.ui.charting.utils.ViewPortHandler;

public abstract class Renderer {

    protected ViewPortHandler mViewPortHandler;

    public Renderer(ViewPortHandler viewPortHandler) {
        this.mViewPortHandler = viewPortHandler;
    }
}
