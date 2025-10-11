package com.mpdc4gsr.component.shared.ui.interfaces.datasets;

import com.mpdc4gsr.component.shared.ui.data.PieDataSet;
import com.mpdc4gsr.component.shared.ui.data.PieEntry;

public interface IPieDataSet extends IDataSet<PieEntry> {

    float getSliceSpace();

    boolean isAutomaticallyDisableSliceSpacingEnabled();

    float getSelectionShift();

    PieDataSet.ValuePosition getXValuePosition();

    PieDataSet.ValuePosition getYValuePosition();

    boolean isUsingSliceColorAsValueLineColor();

    int getValueLineColor();

    float getValueLineWidth();

    float getValueLinePart1OffsetPercentage();

    float getValueLinePart1Length();

    float getValueLinePart2Length();

    boolean isValueLineVariableLength();

}


