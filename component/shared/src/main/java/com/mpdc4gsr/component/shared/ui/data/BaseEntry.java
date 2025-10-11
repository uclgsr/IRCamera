package com.mpdc4gsr.component.shared.ui.data;

import android.graphics.drawable.Drawable;

public abstract class BaseEntry {

    private float y = 0f;

    private Object mData = null;

    private Drawable mIcon = null;

    public BaseEntry() {

    }

    public BaseEntry(float y) {
        this.y = y;
    }

    public BaseEntry(float y, Object data) {
        this(y);
        this.mData = data;
    }

    public BaseEntry(float y, Drawable icon) {
        this(y);
        this.mIcon = icon;
    }

    public BaseEntry(float y, Drawable icon, Object data) {
        this(y);
        this.mIcon = icon;
        this.mData = data;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public Object getData() {
        return mData;
    }

    public void setData(Object data) {
        this.mData = data;
    }
}


