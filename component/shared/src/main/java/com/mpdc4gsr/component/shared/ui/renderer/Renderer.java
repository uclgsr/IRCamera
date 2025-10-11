package com.mpdc4gsr.component.shared.ui.renderer;

import com.mpdc4gsr.component.shared.ui.utils.ViewPortHandler;

public abstract class Renderer {

    protected ViewPortHandler mViewPortHandler;

    public Renderer(ViewPortHandler viewPortHandler) {
        this.mViewPortHandler = viewPortHandler;
    }
}


