package com.mpdc4gsr.component.thermal.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.mpdc4gsr.component.shared.app.db.entity.ThermalEntity;
import com.mpdc4gsr.component.shared.app.tools.NumberTools;
import com.mpdc4gsr.component.shared.app.tools.TimeTools;
import com.mpdc4gsr.component.shared.ui.components.MarkerView;
import com.mpdc4gsr.component.shared.ui.data.CandleEntry;
import com.mpdc4gsr.component.shared.ui.data.Entry;
import com.mpdc4gsr.component.shared.ui.highlight.Highlight;
import com.mpdc4gsr.component.shared.ui.utils.MPPointF;
import com.mpdc4gsr.component.shared.ui.utils.Utils;
import com.mpdc4gsr.component.thermal.R;

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
            timeText.setText(TimeTools.INSTANCE.showTimeSecond(data.getCreateTime()));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}



