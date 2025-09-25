package com.mpdc4gsr.libunified.ui.renderer;

import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public abstract class Renderer {

    protected ViewPortHandler mViewPortHandler;

    public Renderer(ViewPortHandler viewPortHandler) {
        this.mViewPortHandler = viewPortHandler;
    }
}
