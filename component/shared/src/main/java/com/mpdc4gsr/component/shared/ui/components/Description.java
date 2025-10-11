package com.mpdc4gsr.component.shared.ui.components;

import android.graphics.Paint;

import com.mpdc4gsr.component.shared.ui.utils.MPPointF;
import com.mpdc4gsr.component.shared.ui.utils.Utils;

public class Description extends ComponentBase {

    private String text = "Description Label";

    private MPPointF mPosition;

    private Paint.Align mTextAlign = Paint.Align.RIGHT;

    public Description() {
        super();

        mTextSize = Utils.convertDpToPixel(8f);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setPosition(float x, float y) {
        if (mPosition == null) {
            mPosition = MPPointF.getInstance(x, y);
        } else {
            mPosition.x = x;
            mPosition.y = y;
        }
    }

    public MPPointF getPosition() {
        return mPosition;
    }

    public Paint.Align getTextAlign() {
        return mTextAlign;
    }

    public void setTextAlign(Paint.Align align) {
        this.mTextAlign = align;
    }
}


