package com.mpdc4gsr.module.thermal.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;
import com.mpdc4gsr.lib.core.db.entity.ThermalEntity;
import com.mpdc4gsr.lib.core.tools.NumberTools;
import com.mpdc4gsr.lib.core.tools.TimeTool;
import com.mpdc4gsr.module.thermal.R;

@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private final TextView tvContent;
    private final TextView timeText;

    public MyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        timeText = findViewById(R.id.time_text);
    }


    @SuppressLint("DefaultLocale")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = highlight.getDataIndex();
        ThermalEntity data = (ThermalEntity) e.getData();
        if (e instanceof CandleEntry) {
            CandleEntry ce = (CandleEntry) e;
            tvContent.setText(Utils.formatNumber(ce.getHigh(), 0, true));
        } else {
            StringBuilder str = new StringBuilder();
            String thermalStr = NumberTools.INSTANCE.to02(data.getThermal());
            String thermalMaxStr = NumberTools.INSTANCE.to02(data.getThermalMax());
            String thermalMinStr = NumberTools.INSTANCE.to02(data.getThermalMin());
            if (index == 0) {
                str.append("[CHINESE_TEXT]:").append(thermalStr);
            } else if (index == 1) {
                str.append("[CHINESE_TEXT]:").append(thermalMaxStr);
                str.append(System.getProperty("line.separator")).append("[CHINESE_TEXT]:").append(thermalMinStr);
            } else {
                str.append("[CHINESE_TEXT]:").append(thermalMaxStr);
                str.append(System.getProperty("line.separator")).append("[CHINESE_TEXT]:").append(thermalMinStr);
            }
            tvContent.setText(str.toString());
            timeText.setText(TimeTool.INSTANCE.showTimeSecond(data.getCreateTime()));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
