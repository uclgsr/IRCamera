package com.mpdc4gsr.module.thermal.ir.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.mpdc4gsr.libunified.ui.charting.components.MarkerView;
import com.mpdc4gsr.libunified.ui.charting.data.CandleEntry;
import com.mpdc4gsr.libunified.ui.charting.data.Entry;
import com.mpdc4gsr.libunified.ui.charting.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.charting.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.charting.utils.Utils;
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity;
import com.mpdc4gsr.libunified.app.tools.TimeTool;
import com.mpdc4gsr.libunified.app.tools.UnitTools;
import com.mpdc4gsr.module.thermal.ir.R;

import java.util.Locale;

@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private final TextView tvContent;
    private final TextView timeText;

    public MyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        timeText = findViewById(R.id.time_text);
    }


    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        try {
            if (e instanceof CandleEntry) {
                CandleEntry ce = (CandleEntry) e;
                tvContent.setText(Utils.formatNumber(ce.getHigh(), 0, true));
            } else {
                if (e.getData() instanceof ThermalEntity) {
                    ThermalEntity data = (ThermalEntity) e.getData();
                    int index = highlight.getDataIndex();
                    StringBuilder str = new StringBuilder();
                    if (index == 0) {
                        str.append(com.blankj.utilcode.util.Utils.getApp().getString(R.string.chart_temperature) + ": ").append(UnitTools.showC(data.getThermal()));
                    } else if (index == 1) {
                        str.append(com.blankj.utilcode.util.Utils.getApp().getString(R.string.chart_temperature_high) + ": ").append(UnitTools.showC(data.getThermalMax()));
                        str.append(System.getProperty("line.separator")).append(com.blankj.utilcode.util.Utils.getApp().getString(R.string.chart_temperature_low) + ": ").append(UnitTools.showC(data.getThermalMin()));
                    } else {
                        str.append(com.blankj.utilcode.util.Utils.getApp().getString(R.string.chart_temperature_high) + ": ").append(UnitTools.showC(data.getThermalMax()));
                        str.append(System.getProperty("line.separator")).append(com.blankj.utilcode.util.Utils.getApp().getString(R.string.chart_temperature_low) + ": ").append(UnitTools.showC(data.getThermalMin()));
                    }
                    tvContent.setText(str.toString());
                    timeText.setText(TimeTool.INSTANCE.showTimeSecond(data.getCreateTime()));
                } else {
                    tvContent.setText(com.blankj.utilcode.util.Utils.getApp().getString(R.string.chart_temperature) + ": " + String.format(Locale.ENGLISH, "%.1f", e.getY()) + UnitTools.showUnit());
                    timeText.setVisibility(View.GONE);
                }
            }
        } catch (Exception ex) {
            XLog.e("MarkerView error: " + ex.getMessage());
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
