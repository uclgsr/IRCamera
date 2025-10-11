package com.mpdc4gsr.component.shared.ui.interfaces.datasets;

import com.mpdc4gsr.component.shared.ui.data.Entry;
import com.mpdc4gsr.component.shared.ui.renderer.scatter.IShapeRenderer;

public interface IScatterDataSet extends ILineScatterCandleRadarDataSet<Entry> {

    float getScatterShapeSize();

    float getScatterShapeHoleRadius();

    int getScatterShapeHoleColor();

    IShapeRenderer getShapeRenderer();
}


