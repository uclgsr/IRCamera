package com.mpdc4gsr.component.shared.ui.components;

import android.graphics.DashPathEffect;

import com.mpdc4gsr.component.shared.ui.utils.ColorTemplate;

public class LegendEntry {
    public String label;
    public Legend.LegendForm form = Legend.LegendForm.DEFAULT;
    public float formSize = Float.NaN;
    public float formLineWidth = Float.NaN;
    public DashPathEffect formLineDashEffect = null;
    public int formColor = ColorTemplate.COLOR_NONE;

    public LegendEntry() {

    }

    public LegendEntry(String label,
                       Legend.LegendForm form,
                       float formSize,
                       float formLineWidth,
                       DashPathEffect formLineDashEffect,
                       int formColor) {
        this.label = label;
        this.form = form;
        this.formSize = formSize;
        this.formLineWidth = formLineWidth;
        this.formLineDashEffect = formLineDashEffect;
        this.formColor = formColor;
    }

}


