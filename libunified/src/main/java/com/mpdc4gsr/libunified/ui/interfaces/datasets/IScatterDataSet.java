package com.mpdc4gsr.libunified.ui.charting.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.charting.data.Entry;
import com.mpdc4gsr.libunified.ui.charting.renderer.scatter.IShapeRenderer;

public interface IScatterDataSet extends ILineScatterCandleRadarDataSet<Entry> {

    float getScatterShapeSize();

    float getScatterShapeHoleRadius();

    int getScatterShapeHoleColor();

    IShapeRenderer getShapeRenderer();
}
