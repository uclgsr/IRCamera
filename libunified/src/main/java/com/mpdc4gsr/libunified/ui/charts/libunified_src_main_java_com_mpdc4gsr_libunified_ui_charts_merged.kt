// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts' directory and its subdirectories.
// Total files: 12 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\BarChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.data.BarData;
import com.mpdc4gsr.libunified.ui.data.BarEntry;
import com.mpdc4gsr.libunified.ui.highlight.BarHighlighter;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.BarDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;
import com.mpdc4gsr.libunified.ui.renderer.BarChartRenderer;

public class BarChart extends BarLineChartBase<BarData> implements BarDataProvider {

    protected boolean mHighlightFullBarEnabled = false;

    private boolean mDrawValueAboveBar = true;

    private boolean mDrawBarShadow = false;

    private boolean mFitBars = false;

    public BarChart(Context context) {
        super(context);
    }

    public BarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new BarChartRenderer(this, mAnimator, mViewPortHandler);

        setHighlighter(new BarHighlighter(this));

        getXAxis().setSpaceMin(0.5f);
        getXAxis().setSpaceMax(0.5f);
    }

    @Override
    protected void calcMinMax() {

        if (mFitBars) {
            mXAxis.calculate(mData.getXMin() - mData.getBarWidth() / 2f, mData.getXMax() + mData.getBarWidth() / 2f);
        } else {
            mXAxis.calculate(mData.getXMin(), mData.getXMax());
        }

        mAxisLeft.calculate(mData.getYMin(YAxis.AxisDependency.LEFT), mData.getYMax(YAxis.AxisDependency.LEFT));
        mAxisRight.calculate(mData.getYMin(YAxis.AxisDependency.RIGHT), mData.getYMax(YAxis.AxisDependency
                .RIGHT));
    }

    @Override
    public Highlight getHighlightByTouchPoint(float x, float y) {

        if (mData == null) {
            Log.e(LOG_TAG, "Can't select by touch. No data set.");
            return null;
        } else {
            Highlight h = getHighlighter().getHighlight(x, y);
            if (h == null || !isHighlightFullBarEnabled()) return h;

            return new Highlight(h.getX(), h.getY(),
                    h.getXPx(), h.getYPx(),
                    h.getDataSetIndex(), -1, h.getAxis());
        }
    }

    public RectF getBarBounds(BarEntry e) {

        RectF bounds = new RectF();
        getBarBounds(e, bounds);

        return bounds;
    }

    public void getBarBounds(BarEntry e, RectF outputRect) {

        RectF bounds = outputRect;

        IBarDataSet set = mData.getDataSetForEntry(e);

        if (set == null) {
            bounds.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
            return;
        }

        float y = e.getY();
        float x = e.getX();

        float barWidth = mData.getBarWidth();

        float left = x - barWidth / 2f;
        float right = x + barWidth / 2f;
        float top = y >= 0 ? y : 0;
        float bottom = y <= 0 ? y : 0;

        bounds.set(left, top, right, bottom);

        getTransformer(set.getAxisDependency()).rectValueToPixel(outputRect);
    }

    public void setDrawValueAboveBar(boolean enabled) {
        mDrawValueAboveBar = enabled;
    }

    public boolean isDrawValueAboveBarEnabled() {
        return mDrawValueAboveBar;
    }

    public void setDrawBarShadow(boolean enabled) {
        mDrawBarShadow = enabled;
    }

    public boolean isDrawBarShadowEnabled() {
        return mDrawBarShadow;
    }

    @Override
    public boolean isHighlightFullBarEnabled() {
        return mHighlightFullBarEnabled;
    }

    public void setHighlightFullBarEnabled(boolean enabled) {
        mHighlightFullBarEnabled = enabled;
    }

    public void highlightValue(float x, int dataSetIndex, int stackIndex) {
        highlightValue(new Highlight(x, dataSetIndex, stackIndex), false);
    }

    @Override
    public BarData getBarData() {
        return mData;
    }

    public void setFitBars(boolean enabled) {
        mFitBars = enabled;
    }

    public void groupBars(float fromX, float groupSpace, float barSpace) {

        if (getBarData() == null) {
            throw new RuntimeException("You need to set data for the chart before grouping bars.");
        } else {
            getBarData().groupBars(fromX, groupSpace, barSpace);
            notifyDataSetChanged();
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\BarLineChartBase.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.mpdc4gsr.libunified.ui.components.XAxis.XAxisPosition;
import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.components.YAxis.AxisDependency;
import com.mpdc4gsr.libunified.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.ChartHighlighter;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.BarLineScatterCandleBubbleDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;
import com.mpdc4gsr.libunified.ui.jobs.AnimatedMoveViewJob;
import com.mpdc4gsr.libunified.ui.jobs.AnimatedZoomJob;
import com.mpdc4gsr.libunified.ui.jobs.MoveViewJob;
import com.mpdc4gsr.libunified.ui.jobs.ZoomJob;
import com.mpdc4gsr.libunified.ui.listener.BarLineChartTouchListener;
import com.mpdc4gsr.libunified.ui.listener.OnDrawListener;
import com.mpdc4gsr.libunified.ui.renderer.XAxisRenderer;
import com.mpdc4gsr.libunified.ui.renderer.YAxisRenderer;
import com.mpdc4gsr.libunified.ui.utils.MPPointD;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Transformer;
import com.mpdc4gsr.libunified.ui.utils.Utils;

@SuppressLint("RtlHardcoded")
public abstract class BarLineChartBase<T extends BarLineScatterCandleBubbleData<? extends
        IBarLineScatterCandleBubbleDataSet<? extends Entry>>>
        extends Chart<T> implements BarLineScatterCandleBubbleDataProvider {

    protected int mMaxVisibleCount = 100;

    protected boolean mAutoScaleMinMaxEnabled = false;

    protected boolean mPinchZoomEnabled = false;

    protected boolean mDoubleTapToZoomEnabled = true;

    protected boolean mHighlightPerDragEnabled = true;
    protected Paint mGridBackgroundPaint;
    protected Paint mBorderPaint;
    protected boolean mDrawGridBackground = false;
    protected boolean mDrawBorders = false;
    protected boolean mClipValuesToContent = false;
    protected float mMinOffset = 15.f;
    protected boolean mKeepPositionOnRotation = false;
    protected OnDrawListener mDrawListener;
    protected YAxis mAxisLeft;
    protected YAxis mAxisRight;
    protected YAxisRenderer mAxisRendererLeft;
    protected YAxisRenderer mAxisRendererRight;
    protected Transformer mLeftAxisTransformer;
    protected Transformer mRightAxisTransformer;
    protected XAxisRenderer mXAxisRenderer;
    protected Matrix mZoomMatrixBuffer = new Matrix();
    protected Matrix mFitScreenMatrixBuffer = new Matrix();
    protected float[] mGetPositionBuffer = new float[2];
    protected MPPointD posForGetLowestVisibleX = MPPointD.getInstance(0, 0);
    protected MPPointD posForGetHighestVisibleX = MPPointD.getInstance(0, 0);
    protected float[] mOnSizeChangedBuffer = new float[2];
    private boolean mDragXEnabled = true;
    private boolean mDragYEnabled = true;
    private boolean mScaleXEnabled = true;
    private boolean mScaleYEnabled = true;
    private long totalTime = 0;
    private long drawCycles = 0;
    private RectF mOffsetsBuffer = new RectF();
    private boolean mCustomViewPortEnabled = false;

    public BarLineChartBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BarLineChartBase(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarLineChartBase(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        super.init();

        mAxisLeft = new YAxis(AxisDependency.LEFT);
        mAxisRight = new YAxis(AxisDependency.RIGHT);

        mLeftAxisTransformer = new Transformer(mViewPortHandler);
        mRightAxisTransformer = new Transformer(mViewPortHandler);

        mAxisRendererLeft = new YAxisRenderer(mViewPortHandler, mAxisLeft, mLeftAxisTransformer);
        mAxisRendererRight = new YAxisRenderer(mViewPortHandler, mAxisRight, mRightAxisTransformer);

        mXAxisRenderer = new XAxisRenderer(mViewPortHandler, mXAxis, mLeftAxisTransformer);

        setHighlighter(new ChartHighlighter(this));

        mChartTouchListener = new BarLineChartTouchListener(this, mViewPortHandler.getMatrixTouch(), 3f);

        mGridBackgroundPaint = new Paint();
        mGridBackgroundPaint.setStyle(Style.FILL);

        mGridBackgroundPaint.setColor(Color.rgb(240, 240, 240));

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Style.STROKE);
        mBorderPaint.setColor(Color.BLACK);
        mBorderPaint.setStrokeWidth(Utils.convertDpToPixel(1f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mData == null)
            return;

        long starttime = System.currentTimeMillis();

        drawGridBackground(canvas);

        if (mAutoScaleMinMaxEnabled) {
            autoScale();
        }

        if (mAxisLeft.isEnabled())
            mAxisRendererLeft.computeAxis(mAxisLeft.mAxisMinimum, mAxisLeft.mAxisMaximum, mAxisLeft.isInverted());

        if (mAxisRight.isEnabled())
            mAxisRendererRight.computeAxis(mAxisRight.mAxisMinimum, mAxisRight.mAxisMaximum, mAxisRight.isInverted());

        if (mXAxis.isEnabled())
            mXAxisRenderer.computeAxis(mXAxis.mAxisMinimum, mXAxis.mAxisMaximum, false);

        mXAxisRenderer.renderAxisLine(canvas);
        mAxisRendererLeft.renderAxisLine(canvas);
        mAxisRendererRight.renderAxisLine(canvas);

        if (mXAxis.isDrawGridLinesBehindDataEnabled())
            mXAxisRenderer.renderGridLines(canvas);

        if (mAxisLeft.isDrawGridLinesBehindDataEnabled())
            mAxisRendererLeft.renderGridLines(canvas);

        if (mAxisRight.isDrawGridLinesBehindDataEnabled())
            mAxisRendererRight.renderGridLines(canvas);

        if (mXAxis.isEnabled() && mXAxis.isDrawLimitLinesBehindDataEnabled())
            mXAxisRenderer.renderLimitLines(canvas);

        if (mAxisLeft.isEnabled() && mAxisLeft.isDrawLimitLinesBehindDataEnabled())
            mAxisRendererLeft.renderLimitLines(canvas);

        if (mAxisRight.isEnabled() && mAxisRight.isDrawLimitLinesBehindDataEnabled())
            mAxisRendererRight.renderLimitLines(canvas);

        int clipRestoreCount = canvas.save();
        canvas.clipRect(mViewPortHandler.getContentRect());

        mRenderer.drawData(canvas);

        if (!mXAxis.isDrawGridLinesBehindDataEnabled())
            mXAxisRenderer.renderGridLines(canvas);

        if (!mAxisLeft.isDrawGridLinesBehindDataEnabled())
            mAxisRendererLeft.renderGridLines(canvas);

        if (!mAxisRight.isDrawGridLinesBehindDataEnabled())
            mAxisRendererRight.renderGridLines(canvas);

        if (valuesToHighlight())
            mRenderer.drawHighlighted(canvas, mIndicesToHighlight);

        canvas.restoreToCount(clipRestoreCount);

        mRenderer.drawExtras(canvas);

        if (mXAxis.isEnabled() && !mXAxis.isDrawLimitLinesBehindDataEnabled())
            mXAxisRenderer.renderLimitLines(canvas);

        if (mAxisLeft.isEnabled() && !mAxisLeft.isDrawLimitLinesBehindDataEnabled())
            mAxisRendererLeft.renderLimitLines(canvas);

        if (mAxisRight.isEnabled() && !mAxisRight.isDrawLimitLinesBehindDataEnabled())
            mAxisRendererRight.renderLimitLines(canvas);

        mXAxisRenderer.renderAxisLabels(canvas);
        mAxisRendererLeft.renderAxisLabels(canvas);
        mAxisRendererRight.renderAxisLabels(canvas);

        if (isClipValuesToContentEnabled()) {
            clipRestoreCount = canvas.save();
            canvas.clipRect(mViewPortHandler.getContentRect());

            mRenderer.drawValues(canvas);

            canvas.restoreToCount(clipRestoreCount);
        } else {
            mRenderer.drawValues(canvas);
        }

        mLegendRenderer.renderLegend(canvas);

        drawDescription(canvas);

        drawMarkers(canvas);

        if (mLogEnabled) {
            long drawtime = (System.currentTimeMillis() - starttime);
            totalTime += drawtime;
            drawCycles += 1;
            long average = totalTime / drawCycles;
            Log.i(LOG_TAG, "Drawtime: " + drawtime + " ms, average: " + average + " ms, cycles: "
                    + drawCycles);
        }
    }

    public void resetTracking() {
        totalTime = 0;
        drawCycles = 0;
    }

    protected void prepareValuePxMatrix() {

        if (mLogEnabled)
            Log.i(LOG_TAG, "Preparing Value-Px Matrix, xmin: " + mXAxis.mAxisMinimum + ", xmax: "
                    + mXAxis.mAxisMaximum + ", xdelta: " + mXAxis.mAxisRange);

        mRightAxisTransformer.prepareMatrixValuePx(mXAxis.mAxisMinimum,
                mXAxis.mAxisRange,
                mAxisRight.mAxisRange,
                mAxisRight.mAxisMinimum);
        mLeftAxisTransformer.prepareMatrixValuePx(mXAxis.mAxisMinimum,
                mXAxis.mAxisRange,
                mAxisLeft.mAxisRange,
                mAxisLeft.mAxisMinimum);
    }

    protected void prepareOffsetMatrix() {

        mRightAxisTransformer.prepareMatrixOffset(mAxisRight.isInverted());
        mLeftAxisTransformer.prepareMatrixOffset(mAxisLeft.isInverted());
    }

    @Override
    public void notifyDataSetChanged() {

        if (mData == null) {
            if (mLogEnabled)
                Log.i(LOG_TAG, "Preparing... DATA NOT SET.");
            return;
        } else {
            if (mLogEnabled)
                Log.i(LOG_TAG, "Preparing...");
        }

        if (mRenderer != null)
            mRenderer.initBuffers();

        calcMinMax();

        mAxisRendererLeft.computeAxis(mAxisLeft.mAxisMinimum, mAxisLeft.mAxisMaximum, mAxisLeft.isInverted());
        mAxisRendererRight.computeAxis(mAxisRight.mAxisMinimum, mAxisRight.mAxisMaximum, mAxisRight.isInverted());
        mXAxisRenderer.computeAxis(mXAxis.mAxisMinimum, mXAxis.mAxisMaximum, false);

        if (mLegend != null)
            mLegendRenderer.computeLegend(mData);

        calculateOffsets();
    }

    protected void autoScale() {

        final float fromX = getLowestVisibleX();
        final float toX = getHighestVisibleX();

        mData.calcMinMaxY(fromX, toX);

        mXAxis.calculate(mData.getXMin(), mData.getXMax());

        if (mAxisLeft.isEnabled())
            mAxisLeft.calculate(mData.getYMin(AxisDependency.LEFT),
                    mData.getYMax(AxisDependency.LEFT));

        if (mAxisRight.isEnabled())
            mAxisRight.calculate(mData.getYMin(AxisDependency.RIGHT),
                    mData.getYMax(AxisDependency.RIGHT));

        calculateOffsets();
    }

    @Override
    protected void calcMinMax() {

        mXAxis.calculate(mData.getXMin(), mData.getXMax());

        mAxisLeft.calculate(mData.getYMin(AxisDependency.LEFT), mData.getYMax(AxisDependency.LEFT));
        mAxisRight.calculate(mData.getYMin(AxisDependency.RIGHT), mData.getYMax(AxisDependency
                .RIGHT));
    }

    protected void calculateLegendOffsets(RectF offsets) {

        offsets.left = 0.f;
        offsets.right = 0.f;
        offsets.top = 0.f;
        offsets.bottom = 0.f;

        if (mLegend != null && mLegend.isEnabled() && !mLegend.isDrawInsideEnabled()) {
            switch (mLegend.getOrientation()) {
                case VERTICAL:

                    switch (mLegend.getHorizontalAlignment()) {
                        case LEFT:
                            offsets.left += Math.min(mLegend.mNeededWidth,
                                    mViewPortHandler.getChartWidth() * mLegend.getMaxSizePercent())
                                    + mLegend.getXOffset();
                            break;

                        case RIGHT:
                            offsets.right += Math.min(mLegend.mNeededWidth,
                                    mViewPortHandler.getChartWidth() * mLegend.getMaxSizePercent())
                                    + mLegend.getXOffset();
                            break;

                        case CENTER:

                            switch (mLegend.getVerticalAlignment()) {
                                case TOP:
                                    offsets.top += Math.min(mLegend.mNeededHeight,
                                            mViewPortHandler.getChartHeight() * mLegend.getMaxSizePercent())
                                            + mLegend.getYOffset();
                                    break;

                                case BOTTOM:
                                    offsets.bottom += Math.min(mLegend.mNeededHeight,
                                            mViewPortHandler.getChartHeight() * mLegend.getMaxSizePercent())
                                            + mLegend.getYOffset();
                                    break;

                                default:
                                    break;
                            }
                    }

                    break;

                case HORIZONTAL:

                    switch (mLegend.getVerticalAlignment()) {
                        case TOP:
                            offsets.top += Math.min(mLegend.mNeededHeight,
                                    mViewPortHandler.getChartHeight() * mLegend.getMaxSizePercent())
                                    + mLegend.getYOffset();
                            break;

                        case BOTTOM:
                            offsets.bottom += Math.min(mLegend.mNeededHeight,
                                    mViewPortHandler.getChartHeight() * mLegend.getMaxSizePercent())
                                    + mLegend.getYOffset();
                            break;

                        default:
                            break;
                    }
                    break;
            }
        }
    }

    @Override
    public void calculateOffsets() {

        if (!mCustomViewPortEnabled) {

            float offsetLeft = 0f, offsetRight = 0f, offsetTop = 0f, offsetBottom = 0f;

            calculateLegendOffsets(mOffsetsBuffer);

            offsetLeft += mOffsetsBuffer.left;
            offsetTop += mOffsetsBuffer.top;
            offsetRight += mOffsetsBuffer.right;
            offsetBottom += mOffsetsBuffer.bottom;

            if (mAxisLeft.needsOffset()) {
                offsetLeft += mAxisLeft.getRequiredWidthSpace(mAxisRendererLeft
                        .getPaintAxisLabels());
            }

            if (mAxisRight.needsOffset()) {
                offsetRight += mAxisRight.getRequiredWidthSpace(mAxisRendererRight
                        .getPaintAxisLabels());
            }

            if (mXAxis.isEnabled() && mXAxis.isDrawLabelsEnabled()) {

                float xLabelHeight = mXAxis.mLabelRotatedHeight + mXAxis.getYOffset();

                if (mXAxis.getPosition() == XAxisPosition.BOTTOM) {

                    offsetBottom += xLabelHeight;

                } else if (mXAxis.getPosition() == XAxisPosition.TOP) {

                    offsetTop += xLabelHeight;

                } else if (mXAxis.getPosition() == XAxisPosition.BOTH_SIDED) {

                    offsetBottom += xLabelHeight;
                    offsetTop += xLabelHeight;
                }
            }

            offsetTop += getExtraTopOffset();
            offsetRight += getExtraRightOffset();
            offsetBottom += getExtraBottomOffset();
            offsetLeft += getExtraLeftOffset();

            float minOffset = Utils.convertDpToPixel(mMinOffset);

            mViewPortHandler.restrainViewPort(
                    Math.max(minOffset, offsetLeft),
                    Math.max(minOffset, offsetTop),
                    Math.max(minOffset, offsetRight),
                    Math.max(minOffset, offsetBottom));

            if (mLogEnabled) {
                Log.i(LOG_TAG, "offsetLeft: " + offsetLeft + ", offsetTop: " + offsetTop
                        + ", offsetRight: " + offsetRight + ", offsetBottom: " + offsetBottom);
                Log.i(LOG_TAG, "Content: " + mViewPortHandler.getContentRect().toString());
            }
        }

        prepareOffsetMatrix();
        prepareValuePxMatrix();
    }

    protected void drawGridBackground(Canvas c) {

        if (mDrawGridBackground) {

            c.drawRect(mViewPortHandler.getContentRect(), mGridBackgroundPaint);
        }

        if (mDrawBorders) {
            c.drawRect(mViewPortHandler.getContentRect(), mBorderPaint);
        }
    }

    public Transformer getTransformer(AxisDependency which) {
        if (which == AxisDependency.LEFT)
            return mLeftAxisTransformer;
        else
            return mRightAxisTransformer;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (mChartTouchListener == null || mData == null)
            return false;

        if (!mTouchEnabled)
            return false;
        else
            return mChartTouchListener.onTouch(this, event);
    }

    @Override
    public void computeScroll() {

        if (mChartTouchListener instanceof BarLineChartTouchListener)
            ((BarLineChartTouchListener) mChartTouchListener).computeScroll();
    }

    public void zoomIn() {

        MPPointF center = mViewPortHandler.getContentCenter();

        mViewPortHandler.zoomIn(center.x, -center.y, mZoomMatrixBuffer);
        mViewPortHandler.refresh(mZoomMatrixBuffer, this, false);

        MPPointF.recycleInstance(center);

        calculateOffsets();
        postInvalidate();
    }

    public void zoomOut() {

        MPPointF center = mViewPortHandler.getContentCenter();

        mViewPortHandler.zoomOut(center.x, -center.y, mZoomMatrixBuffer);
        mViewPortHandler.refresh(mZoomMatrixBuffer, this, false);

        MPPointF.recycleInstance(center);

        calculateOffsets();
        postInvalidate();
    }

    public void resetZoom() {

        mViewPortHandler.resetZoom(mZoomMatrixBuffer);
        mViewPortHandler.refresh(mZoomMatrixBuffer, this, false);

        calculateOffsets();
        postInvalidate();
    }

    public void zoom(float scaleX, float scaleY, float x, float y) {

        mViewPortHandler.zoom(scaleX, scaleY, x, -y, mZoomMatrixBuffer);
        mViewPortHandler.refresh(mZoomMatrixBuffer, this, false);

        calculateOffsets();
        postInvalidate();
    }

    public void zoom(float scaleX, float scaleY, float xValue, float yValue, AxisDependency axis) {

        Runnable job = ZoomJob.getInstance(mViewPortHandler, scaleX, scaleY, xValue, yValue, getTransformer(axis), axis, this);
        addViewportJob(job);
    }

    public void zoomToCenter(float scaleX, float scaleY) {

        MPPointF center = getCenterOffsets();

        Matrix save = mZoomMatrixBuffer;
        mViewPortHandler.zoom(scaleX, scaleY, center.x, -center.y, save);
        mViewPortHandler.refresh(save, this, false);
    }

    @TargetApi(11)
    public void zoomAndCenterAnimated(float scaleX, float scaleY, float xValue, float yValue, AxisDependency axis,
                                      long duration) {

        MPPointD origin = getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop(), axis);

        Runnable job = AnimatedZoomJob.getInstance(mViewPortHandler, this, getTransformer(axis), getAxis(axis), mXAxis
                        .mAxisRange, scaleX, scaleY, mViewPortHandler.getScaleX(), mViewPortHandler.getScaleY(),
                xValue, yValue, (float) origin.x, (float) origin.y, duration);
        addViewportJob(job);

        MPPointD.recycleInstance(origin);
    }

    public void fitScreen() {
        Matrix save = mFitScreenMatrixBuffer;
        mViewPortHandler.fitScreen(save);
        mViewPortHandler.refresh(save, this, false);

        calculateOffsets();
        postInvalidate();
    }

    public void setScaleMinima(float scaleX, float scaleY) {
        mViewPortHandler.setMinimumScaleX(scaleX);
        mViewPortHandler.setMinimumScaleY(scaleY);
    }

    public void setVisibleXRangeMaximum(float maxXRange) {
        float xScale = mXAxis.mAxisRange / (maxXRange);
        mViewPortHandler.setMinimumScaleX(xScale);
    }

    public void setVisibleXRangeMinimum(float minXRange) {
        float xScale = mXAxis.mAxisRange / (minXRange);
        mViewPortHandler.setMaximumScaleX(xScale);
    }

    public void setVisibleXRange(float minXRange, float maxXRange) {
        float minScale = mXAxis.mAxisRange / minXRange;
        float maxScale = mXAxis.mAxisRange / maxXRange;
        mViewPortHandler.setMinMaxScaleX(minScale, maxScale);
    }

    public void setVisibleYRangeMaximum(float maxYRange, AxisDependency axis) {
        float yScale = getAxisRange(axis) / maxYRange;
        mViewPortHandler.setMinimumScaleY(yScale);
    }

    public void setVisibleYRangeMinimum(float minYRange, AxisDependency axis) {
        float yScale = getAxisRange(axis) / minYRange;
        mViewPortHandler.setMaximumScaleY(yScale);
    }

    public void setVisibleYRange(float minYRange, float maxYRange, AxisDependency axis) {
        float minScale = getAxisRange(axis) / minYRange;
        float maxScale = getAxisRange(axis) / maxYRange;
        mViewPortHandler.setMinMaxScaleY(minScale, maxScale);
    }

    public void moveViewToX(float xValue) {

        Runnable job = MoveViewJob.getInstance(mViewPortHandler, xValue, 0f,
                getTransformer(AxisDependency.LEFT), this);

        addViewportJob(job);
    }

    public void moveViewTo(float xValue, float yValue, AxisDependency axis) {

        float yInView = getAxisRange(axis) / mViewPortHandler.getScaleY();

        Runnable job = MoveViewJob.getInstance(mViewPortHandler, xValue, yValue + yInView / 2f,
                getTransformer(axis), this);

        addViewportJob(job);
    }

    @TargetApi(11)
    public void moveViewToAnimated(float xValue, float yValue, AxisDependency axis, long duration) {

        MPPointD bounds = getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop(), axis);

        float yInView = getAxisRange(axis) / mViewPortHandler.getScaleY();

        Runnable job = AnimatedMoveViewJob.getInstance(mViewPortHandler, xValue, yValue + yInView / 2f,
                getTransformer(axis), this, (float) bounds.x, (float) bounds.y, duration);

        addViewportJob(job);

        MPPointD.recycleInstance(bounds);
    }

    public void centerViewToY(float yValue, AxisDependency axis) {

        float valsInView = getAxisRange(axis) / mViewPortHandler.getScaleY();

        Runnable job = MoveViewJob.getInstance(mViewPortHandler, 0f, yValue + valsInView / 2f,
                getTransformer(axis), this);

        addViewportJob(job);
    }

    public void centerViewTo(float xValue, float yValue, AxisDependency axis) {

        float yInView = getAxisRange(axis) / mViewPortHandler.getScaleY();
        float xInView = getXAxis().mAxisRange / mViewPortHandler.getScaleX();

        Runnable job = MoveViewJob.getInstance(mViewPortHandler,
                xValue - xInView / 2f, yValue + yInView / 2f,
                getTransformer(axis), this);

        addViewportJob(job);
    }

    @TargetApi(11)
    public void centerViewToAnimated(float xValue, float yValue, AxisDependency axis, long duration) {

        MPPointD bounds = getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop(), axis);

        float yInView = getAxisRange(axis) / mViewPortHandler.getScaleY();
        float xInView = getXAxis().mAxisRange / mViewPortHandler.getScaleX();

        Runnable job = AnimatedMoveViewJob.getInstance(mViewPortHandler,
                xValue - xInView / 2f, yValue + yInView / 2f,
                getTransformer(axis), this, (float) bounds.x, (float) bounds.y, duration);

        addViewportJob(job);

        MPPointD.recycleInstance(bounds);
    }

    public void setViewPortOffsets(final float left, final float top,
                                   final float right, final float bottom) {

        mCustomViewPortEnabled = true;
        post(new Runnable() {

            @Override
            public void run() {

                mViewPortHandler.restrainViewPort(left, top, right, bottom);
                prepareOffsetMatrix();
                prepareValuePxMatrix();
            }
        });
    }

    public void resetViewPortOffsets() {
        mCustomViewPortEnabled = false;
        calculateOffsets();
    }

    protected float getAxisRange(AxisDependency axis) {
        if (axis == AxisDependency.LEFT)
            return mAxisLeft.mAxisRange;
        else
            return mAxisRight.mAxisRange;
    }

    public void setOnDrawListener(OnDrawListener drawListener) {
        this.mDrawListener = drawListener;
    }

    public OnDrawListener getDrawListener() {
        return mDrawListener;
    }

    public MPPointF getPosition(Entry e, AxisDependency axis) {

        if (e == null)
            return null;

        mGetPositionBuffer[0] = e.getX();
        mGetPositionBuffer[1] = e.getY();

        getTransformer(axis).pointValuesToPixel(mGetPositionBuffer);

        return MPPointF.getInstance(mGetPositionBuffer[0], mGetPositionBuffer[1]);
    }

    public void setMaxVisibleValueCount(int count) {
        this.mMaxVisibleCount = count;
    }

    public int getMaxVisibleCount() {
        return mMaxVisibleCount;
    }

    public boolean isHighlightPerDragEnabled() {
        return mHighlightPerDragEnabled;
    }

    public void setHighlightPerDragEnabled(boolean enabled) {
        mHighlightPerDragEnabled = enabled;
    }

    public void setGridBackgroundColor(int color) {
        mGridBackgroundPaint.setColor(color);
    }

    public boolean isDragEnabled() {
        return mDragXEnabled || mDragYEnabled;
    }

    public void setDragEnabled(boolean enabled) {
        this.mDragXEnabled = enabled;
        this.mDragYEnabled = enabled;
    }

    public boolean isDragXEnabled() {
        return mDragXEnabled;
    }

    public void setDragXEnabled(boolean enabled) {
        this.mDragXEnabled = enabled;
    }

    public boolean isDragYEnabled() {
        return mDragYEnabled;
    }

    public void setDragYEnabled(boolean enabled) {
        this.mDragYEnabled = enabled;
    }

    public void setScaleEnabled(boolean enabled) {
        this.mScaleXEnabled = enabled;
        this.mScaleYEnabled = enabled;
    }

    public boolean isScaleXEnabled() {
        return mScaleXEnabled;
    }

    public void setScaleXEnabled(boolean enabled) {
        mScaleXEnabled = enabled;
    }

    public boolean isScaleYEnabled() {
        return mScaleYEnabled;
    }

    public void setScaleYEnabled(boolean enabled) {
        mScaleYEnabled = enabled;
    }

    public boolean isDoubleTapToZoomEnabled() {
        return mDoubleTapToZoomEnabled;
    }

    public void setDoubleTapToZoomEnabled(boolean enabled) {
        mDoubleTapToZoomEnabled = enabled;
    }

    public void setDrawGridBackground(boolean enabled) {
        mDrawGridBackground = enabled;
    }

    public void setDrawBorders(boolean enabled) {
        mDrawBorders = enabled;
    }

    public boolean isDrawBordersEnabled() {
        return mDrawBorders;
    }

    public void setClipValuesToContent(boolean enabled) {
        mClipValuesToContent = enabled;
    }

    public boolean isClipValuesToContentEnabled() {
        return mClipValuesToContent;
    }

    public void setBorderWidth(float width) {
        mBorderPaint.setStrokeWidth(Utils.convertDpToPixel(width));
    }

    public void setBorderColor(int color) {
        mBorderPaint.setColor(color);
    }

    public float getMinOffset() {
        return mMinOffset;
    }

    public void setMinOffset(float minOffset) {
        mMinOffset = minOffset;
    }

    public boolean isKeepPositionOnRotation() {
        return mKeepPositionOnRotation;
    }

    public void setKeepPositionOnRotation(boolean keepPositionOnRotation) {
        mKeepPositionOnRotation = keepPositionOnRotation;
    }

    public MPPointD getValuesByTouchPoint(float x, float y, AxisDependency axis) {
        MPPointD result = MPPointD.getInstance(0, 0);
        getValuesByTouchPoint(x, y, axis, result);
        return result;
    }

    public void getValuesByTouchPoint(float x, float y, AxisDependency axis, MPPointD outputPoint) {
        getTransformer(axis).getValuesByTouchPoint(x, y, outputPoint);
    }

    public MPPointD getPixelForValues(float x, float y, AxisDependency axis) {
        return getTransformer(axis).getPixelForValues(x, y);
    }

    public Entry getEntryByTouchPoint(float x, float y) {
        Highlight h = getHighlightByTouchPoint(x, y);
        if (h != null) {
            return mData.getEntryForHighlight(h);
        }
        return null;
    }

    public IBarLineScatterCandleBubbleDataSet getDataSetByTouchPoint(float x, float y) {
        Highlight h = getHighlightByTouchPoint(x, y);
        if (h != null) {
            return mData.getDataSetByIndex(h.getDataSetIndex());
        }
        return null;
    }

    @Override
    public float getLowestVisibleX() {
        getTransformer(AxisDependency.LEFT).getValuesByTouchPoint(mViewPortHandler.contentLeft(),
                mViewPortHandler.contentBottom(), posForGetLowestVisibleX);
        float result = (float) Math.max(mXAxis.mAxisMinimum, posForGetLowestVisibleX.x);
        return result;
    }

    @Override
    public float getHighestVisibleX() {
        getTransformer(AxisDependency.LEFT).getValuesByTouchPoint(mViewPortHandler.contentRight(),
                mViewPortHandler.contentBottom(), posForGetHighestVisibleX);
        float result = (float) Math.min(mXAxis.mAxisMaximum, posForGetHighestVisibleX.x);
        return result;
    }

    public float getVisibleXRange() {
        return Math.abs(getHighestVisibleX() - getLowestVisibleX());
    }

    public float getScaleX() {
        if (mViewPortHandler == null)
            return 1f;
        else
            return mViewPortHandler.getScaleX();
    }

    public float getScaleY() {
        if (mViewPortHandler == null)
            return 1f;
        else
            return mViewPortHandler.getScaleY();
    }

    public boolean isFullyZoomedOut() {
        return mViewPortHandler.isFullyZoomedOut();
    }

    public YAxis getAxisLeft() {
        return mAxisLeft;
    }

    public YAxis getAxisRight() {
        return mAxisRight;
    }

    public YAxis getAxis(AxisDependency axis) {
        if (axis == AxisDependency.LEFT)
            return mAxisLeft;
        else
            return mAxisRight;
    }

    @Override
    public boolean isInverted(AxisDependency axis) {
        return getAxis(axis).isInverted();
    }

    public void setPinchZoom(boolean enabled) {
        mPinchZoomEnabled = enabled;
    }

    public boolean isPinchZoomEnabled() {
        return mPinchZoomEnabled;
    }

    public void setDragOffsetX(float offset) {
        mViewPortHandler.setDragOffsetX(offset);
    }

    public void setDragOffsetY(float offset) {
        mViewPortHandler.setDragOffsetY(offset);
    }

    public boolean hasNoDragOffset() {
        return mViewPortHandler.hasNoDragOffset();
    }

    public XAxisRenderer getRendererXAxis() {
        return mXAxisRenderer;
    }

    public void setXAxisRenderer(XAxisRenderer xAxisRenderer) {
        mXAxisRenderer = xAxisRenderer;
    }

    public YAxisRenderer getRendererLeftYAxis() {
        return mAxisRendererLeft;
    }

    public void setRendererLeftYAxis(YAxisRenderer rendererLeftYAxis) {
        mAxisRendererLeft = rendererLeftYAxis;
    }

    public YAxisRenderer getRendererRightYAxis() {
        return mAxisRendererRight;
    }

    public void setRendererRightYAxis(YAxisRenderer rendererRightYAxis) {
        mAxisRendererRight = rendererRightYAxis;
    }

    @Override
    public float getYChartMax() {
        return Math.max(mAxisLeft.mAxisMaximum, mAxisRight.mAxisMaximum);
    }

    @Override
    public float getYChartMin() {
        return Math.min(mAxisLeft.mAxisMinimum, mAxisRight.mAxisMinimum);
    }

    public boolean isAnyAxisInverted() {
        if (mAxisLeft.isInverted())
            return true;
        if (mAxisRight.isInverted())
            return true;
        return false;
    }

    public boolean isAutoScaleMinMaxEnabled() {
        return mAutoScaleMinMaxEnabled;
    }

    public void setAutoScaleMinMaxEnabled(boolean enabled) {
        mAutoScaleMinMaxEnabled = enabled;
    }

    @Override
    public void setPaint(Paint p, int which) {
        super.setPaint(p, which);

        switch (which) {
            case PAINT_GRID_BACKGROUND:
                mGridBackgroundPaint = p;
                break;
        }
    }

    @Override
    public Paint getPaint(int which) {
        Paint p = super.getPaint(which);
        if (p != null)
            return p;

        switch (which) {
            case PAINT_GRID_BACKGROUND:
                return mGridBackgroundPaint;
        }

        return null;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        mOnSizeChangedBuffer[0] = mOnSizeChangedBuffer[1] = 0;

        if (mKeepPositionOnRotation) {
            mOnSizeChangedBuffer[0] = mViewPortHandler.contentLeft();
            mOnSizeChangedBuffer[1] = mViewPortHandler.contentTop();
            getTransformer(AxisDependency.LEFT).pixelsToValue(mOnSizeChangedBuffer);
        }

        super.onSizeChanged(w, h, oldw, oldh);

        if (mKeepPositionOnRotation) {

            getTransformer(AxisDependency.LEFT).pointValuesToPixel(mOnSizeChangedBuffer);
            mViewPortHandler.centerViewPort(mOnSizeChangedBuffer, this);
        } else {
            mViewPortHandler.refresh(mViewPortHandler.getMatrixTouch(), this, true);
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\BubbleChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.mpdc4gsr.libunified.ui.data.BubbleData;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.BubbleDataProvider;
import com.mpdc4gsr.libunified.ui.renderer.BubbleChartRenderer;

public class BubbleChart extends BarLineChartBase<BubbleData> implements BubbleDataProvider {

    public BubbleChart(Context context) {
        super(context);
    }

    public BubbleChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BubbleChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new BubbleChartRenderer(this, mAnimator, mViewPortHandler);
    }

    public BubbleData getBubbleData() {
        return mData;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\CandleStickChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.mpdc4gsr.libunified.ui.data.CandleData;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.CandleDataProvider;
import com.mpdc4gsr.libunified.ui.renderer.CandleStickChartRenderer;

public class CandleStickChart extends BarLineChartBase<CandleData> implements CandleDataProvider {

    public CandleStickChart(Context context) {
        super(context);
    }

    public CandleStickChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CandleStickChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new CandleStickChartRenderer(this, mAnimator, mViewPortHandler);

        getXAxis().setSpaceMin(0.5f);
        getXAxis().setSpaceMax(0.5f);
    }

    @Override
    public CandleData getCandleData() {
        return mData;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\Chart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.*;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.RequiresApi;

import com.mpdc4gsr.libunified.ui.animation.ChartAnimator;
import com.mpdc4gsr.libunified.ui.animation.Easing.EasingFunction;
import com.mpdc4gsr.libunified.ui.components.Description;
import com.mpdc4gsr.libunified.ui.components.IMarker;
import com.mpdc4gsr.libunified.ui.components.Legend;
import com.mpdc4gsr.libunified.ui.components.XAxis;
import com.mpdc4gsr.libunified.ui.data.ChartData;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.formatter.DefaultValueFormatter;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.highlight.ChartHighlighter;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.highlight.IHighlighter;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.ChartInterface;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.listener.ChartTouchListener;
import com.mpdc4gsr.libunified.ui.listener.OnChartGestureListener;
import com.mpdc4gsr.libunified.ui.listener.OnChartValueSelectedListener;
import com.mpdc4gsr.libunified.ui.renderer.DataRenderer;
import com.mpdc4gsr.libunified.ui.renderer.LegendRenderer;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public abstract class Chart<T extends ChartData<? extends IDataSet<? extends Entry>>> extends
        ViewGroup
        implements ChartInterface {

    public static final String LOG_TAG = "MPAndroidChart";
    public static final int PAINT_GRID_BACKGROUND = 4;
    public static final int PAINT_INFO = 7;
    public static final int PAINT_DESCRIPTION = 11;
    public static final int PAINT_HOLE = 13;
    public static final int PAINT_CENTER_TEXT = 14;
    public static final int PAINT_LEGEND_LABEL = 18;
    protected boolean mLogEnabled = false;
    protected T mData = null;
    protected boolean mHighLightPerTapEnabled = true;
    protected DefaultValueFormatter mDefaultValueFormatter = new DefaultValueFormatter(0);
    protected Paint mDescPaint;
    protected Paint mInfoPaint;
    protected XAxis mXAxis;
    protected boolean mTouchEnabled = true;
    protected Description mDescription;
    protected Legend mLegend;
    protected OnChartValueSelectedListener mSelectionListener;
    protected ChartTouchListener mChartTouchListener;
    protected LegendRenderer mLegendRenderer;
    protected DataRenderer mRenderer;
    protected IHighlighter mHighlighter;
    protected ViewPortHandler mViewPortHandler = new ViewPortHandler();
    protected ChartAnimator mAnimator;

    protected Highlight[] mIndicesToHighlight;
    protected float mMaxHighlightDistance = 0f;

    protected boolean mDrawMarkers = true;
    protected IMarker mMarker;
    protected ArrayList<Runnable> mJobs = new ArrayList<Runnable>();
    private boolean mDragDecelerationEnabled = true;
    private float mDragDecelerationFrictionCoef = 0.9f;
    private String mNoDataText = "No chart data available.";
    private OnChartGestureListener mGestureListener;
    private float mExtraTopOffset = 0.f,
            mExtraRightOffset = 0.f,
            mExtraBottomOffset = 0.f,
            mExtraLeftOffset = 0.f;
    private boolean mOffsetsCalculated = false;
    private boolean mUnbind = false;

    public Chart(Context context) {
        super(context);
        init();
    }

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Chart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {

        setWillNotDraw(false);

        mAnimator = new ChartAnimator(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                postInvalidate();
            }
        });
        mAnimator.setView(this);

        Utils.init(getContext());
        mMaxHighlightDistance = Utils.convertDpToPixel(500f);

        mDescription = new Description();
        mLegend = new Legend();

        mLegendRenderer = new LegendRenderer(mViewPortHandler, mLegend);

        mXAxis = new XAxis();

        mDescPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mInfoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInfoPaint.setColor(Color.rgb(247, 189, 51));
        mInfoPaint.setTextAlign(Align.CENTER);
        mInfoPaint.setTextSize(Utils.convertDpToPixel(12f));

        if (mLogEnabled)
            Log.i("", "Chart.init()");
    }

    public void clear() {
        mData = null;
        mOffsetsCalculated = false;
        mIndicesToHighlight = null;
        mChartTouchListener.setLastHighlighted(null);
        invalidate();
    }

    public void clearValues() {
        mData.clearValues();
        invalidate();
    }

    public boolean isEmpty() {

        if (mData == null)
            return true;
        else {

            if (mData.getEntryCount() <= 0)
                return true;
            else
                return false;
        }
    }

    public abstract void notifyDataSetChanged();

    protected abstract void calculateOffsets();

    protected abstract void calcMinMax();

    protected void setupDefaultFormatter(float min, float max) {

        float reference = 0f;

        if (mData == null || mData.getEntryCount() < 2) {

            reference = Math.max(Math.abs(min), Math.abs(max));
        } else {
            reference = Math.abs(max - min);
        }

        int digits = Utils.getDecimals(reference);

        mDefaultValueFormatter.setup(digits);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mData == null) {

            boolean hasText = !TextUtils.isEmpty(mNoDataText);

            if (hasText) {
                MPPointF c = getCenter();
                canvas.drawText(mNoDataText, c.x, c.y, mInfoPaint);
            }

            return;
        }

        if (!mOffsetsCalculated) {

            calculateOffsets();
            mOffsetsCalculated = true;
        }
    }

    protected void drawDescription(Canvas c) {

        if (mDescription != null && mDescription.isEnabled()) {

            MPPointF position = mDescription.getPosition();

            mDescPaint.setTypeface(mDescription.getTypeface());
            mDescPaint.setTextSize(mDescription.getTextSize());
            mDescPaint.setColor(mDescription.getTextColor());
            mDescPaint.setTextAlign(mDescription.getTextAlign());

            float x, y;

            if (position == null) {
                x = getWidth() - mViewPortHandler.offsetRight() - mDescription.getXOffset();
                y = getHeight() - mViewPortHandler.offsetBottom() - mDescription.getYOffset();
            } else {
                x = position.x;
                y = position.y;
            }

            c.drawText(mDescription.getText(), x, y, mDescPaint);
        }
    }

    @Override
    public float getMaxHighlightDistance() {
        return mMaxHighlightDistance;
    }

    public void setMaxHighlightDistance(float distDp) {
        mMaxHighlightDistance = Utils.convertDpToPixel(distDp);
    }

    public Highlight[] getHighlighted() {
        return mIndicesToHighlight;
    }

    public boolean isHighlightPerTapEnabled() {
        return mHighLightPerTapEnabled;
    }

    public void setHighlightPerTapEnabled(boolean enabled) {
        mHighLightPerTapEnabled = enabled;
    }

    public boolean valuesToHighlight() {
        return mIndicesToHighlight == null || mIndicesToHighlight.length <= 0
                || mIndicesToHighlight[0] == null ? false
                : true;
    }

    protected void setLastHighlighted(Highlight[] highs) {

        if (highs == null || highs.length <= 0 || highs[0] == null) {
            mChartTouchListener.setLastHighlighted(null);
        } else {
            mChartTouchListener.setLastHighlighted(highs[0]);
        }
    }

    public void highlightValues(Highlight[] highs) {

        mIndicesToHighlight = highs;

        setLastHighlighted(highs);

        invalidate();
    }

    public void highlightValue(float x, int dataSetIndex) {
        highlightValue(x, dataSetIndex, true);
    }

    public void highlightValue(float x, float y, int dataSetIndex) {
        highlightValue(x, y, dataSetIndex, true);
    }

    public void highlightValue(float x, int dataSetIndex, boolean callListener) {
        highlightValue(x, Float.NaN, dataSetIndex, callListener);
    }

    public void highlightValue(float x, float y, int dataSetIndex, boolean callListener) {

        if (dataSetIndex < 0 || dataSetIndex >= mData.getDataSetCount()) {
            highlightValue(null, callListener);
        } else {
            highlightValue(new Highlight(x, y, dataSetIndex), callListener);
        }
    }

    public void highlightValue(Highlight highlight) {
        highlightValue(highlight, false);
    }

    public void highlightValue(Highlight high, boolean callListener) {

        Entry e = null;

        if (high == null)
            mIndicesToHighlight = null;
        else {

            if (mLogEnabled)
                Log.i(LOG_TAG, "Highlighted: " + high.toString());

            e = mData.getEntryForHighlight(high);
            if (e == null) {
                mIndicesToHighlight = null;
                high = null;
            } else {

                mIndicesToHighlight = new Highlight[]{
                        high
                };
            }
        }

        setLastHighlighted(mIndicesToHighlight);

        if (callListener && mSelectionListener != null) {

            if (!valuesToHighlight())
                mSelectionListener.onNothingSelected();
            else {

                mSelectionListener.onValueSelected(e, high);
            }
        }

        invalidate();
    }

    public Highlight getHighlightByTouchPoint(float x, float y) {

        if (mData == null) {
            Log.e(LOG_TAG, "Can't select by touch. No data set.");
            return null;
        } else
            return getHighlighter().getHighlight(x, y);
    }

    public ChartTouchListener getOnTouchListener() {
        return mChartTouchListener;
    }

    public void setOnTouchListener(ChartTouchListener l) {
        this.mChartTouchListener = l;
    }

    protected void drawMarkers(Canvas canvas) {

        if (mMarker == null || !isDrawMarkersEnabled() || !valuesToHighlight())
            return;

        for (int i = 0; i < mIndicesToHighlight.length; i++) {

            Highlight highlight = mIndicesToHighlight[i];

            IDataSet set = mData.getDataSetByIndex(highlight.getDataSetIndex());

            Entry e = mData.getEntryForHighlight(mIndicesToHighlight[i]);

            try {
                int entryIndex = set.getEntryIndex(e);

                if (e == null || entryIndex > set.getEntryCount() * mAnimator.getPhaseX())
                    continue;

                float[] pos = getMarkerPosition(highlight);

                if (!mViewPortHandler.isInBounds(pos[0], pos[1]))
                    continue;

                mMarker.refreshContent(e, highlight);

                mMarker.draw(canvas, pos[0], pos[1]);
            } catch (Exception exception) {
                Log.e("Test", exception.getMessage());
            }
        }
    }

    protected float[] getMarkerPosition(Highlight high) {
        return new float[]{high.getDrawX(), high.getDrawY()};
    }

    public ChartAnimator getAnimator() {
        return mAnimator;
    }

    public boolean isDragDecelerationEnabled() {
        return mDragDecelerationEnabled;
    }

    public void setDragDecelerationEnabled(boolean enabled) {
        mDragDecelerationEnabled = enabled;
    }

    public float getDragDecelerationFrictionCoef() {
        return mDragDecelerationFrictionCoef;
    }

    public void setDragDecelerationFrictionCoef(float newValue) {

        if (newValue < 0.f)
            newValue = 0.f;

        if (newValue >= 1f)
            newValue = 0.999f;

        mDragDecelerationFrictionCoef = newValue;
    }

    @RequiresApi(11)
    public void animateXY(int durationMillisX, int durationMillisY, EasingFunction easingX,
                          EasingFunction easingY) {
        if (isAttachedToWindow()) {
            mAnimator.animateXY(durationMillisX, durationMillisY, easingX, easingY);
        }
    }

    @RequiresApi(11)
    public void animateXY(int durationMillisX, int durationMillisY, EasingFunction easing) {
        if (isAttachedToWindow()) {
            mAnimator.animateXY(durationMillisX, durationMillisY, easing);
        }
    }

    @RequiresApi(11)
    public void animateX(int durationMillis, EasingFunction easing) {
        if (isAttachedToWindow()) {
            mAnimator.animateX(durationMillis, easing);
        }
    }

    @RequiresApi(11)
    public void animateY(int durationMillis, EasingFunction easing) {
        if (isAttachedToWindow()) {
            mAnimator.animateY(durationMillis, easing);
        }
    }

    @RequiresApi(11)
    public void animateX(int durationMillis) {
        if (isAttachedToWindow()) {
            mAnimator.animateX(durationMillis);
        }
    }

    @RequiresApi(11)
    public void animateY(int durationMillis) {
        if (isAttachedToWindow()) {
            mAnimator.animateY(durationMillis);
        }
    }

    @RequiresApi(11)
    public void animateXY(int durationMillisX, int durationMillisY) {
        if (isAttachedToWindow()) {
            mAnimator.animateXY(durationMillisX, durationMillisY);
        }
    }

    public XAxis getXAxis() {
        return mXAxis;
    }

    public ValueFormatter getDefaultValueFormatter() {
        return mDefaultValueFormatter;
    }

    public void setOnChartValueSelectedListener(OnChartValueSelectedListener l) {
        this.mSelectionListener = l;
    }

    public OnChartGestureListener getOnChartGestureListener() {
        return mGestureListener;
    }

    public void setOnChartGestureListener(OnChartGestureListener l) {
        this.mGestureListener = l;
    }

    public float getYMax() {
        return mData.getYMax();
    }

    public float getYMin() {
        return mData.getYMin();
    }

    @Override
    public float getXChartMax() {
        return mXAxis.mAxisMaximum;
    }

    @Override
    public float getXChartMin() {
        return mXAxis.mAxisMinimum;
    }

    @Override
    public float getXRange() {
        return mXAxis.mAxisRange;
    }

    public MPPointF getCenter() {
        return MPPointF.getInstance(getWidth() / 2f, getHeight() / 2f);
    }

    @Override
    public MPPointF getCenterOffsets() {
        return mViewPortHandler.getContentCenter();
    }

    public void setExtraOffsets(float left, float top, float right, float bottom) {
        setExtraLeftOffset(left);
        setExtraTopOffset(top);
        setExtraRightOffset(right);
        setExtraBottomOffset(bottom);
    }

    public float getExtraTopOffset() {
        return mExtraTopOffset;
    }

    public void setExtraTopOffset(float offset) {
        mExtraTopOffset = Utils.convertDpToPixel(offset);
    }

    public float getExtraRightOffset() {
        return mExtraRightOffset;
    }

    public void setExtraRightOffset(float offset) {
        mExtraRightOffset = Utils.convertDpToPixel(offset);
    }

    public float getExtraBottomOffset() {
        return mExtraBottomOffset;
    }

    public void setExtraBottomOffset(float offset) {
        mExtraBottomOffset = Utils.convertDpToPixel(offset);
    }

    public float getExtraLeftOffset() {
        return mExtraLeftOffset;
    }

    public void setExtraLeftOffset(float offset) {
        mExtraLeftOffset = Utils.convertDpToPixel(offset);
    }

    public boolean isLogEnabled() {
        return mLogEnabled;
    }

    public void setLogEnabled(boolean enabled) {
        mLogEnabled = enabled;
    }

    public void setNoDataText(String text) {
        mNoDataText = text;
    }

    public void setNoDataTextColor(int color) {
        mInfoPaint.setColor(color);
    }

    public void setNoDataTextTypeface(Typeface tf) {
        mInfoPaint.setTypeface(tf);
    }

    public void setTouchEnabled(boolean enabled) {
        this.mTouchEnabled = enabled;
    }

    public IMarker getMarker() {
        return mMarker;
    }

    public void setMarker(IMarker marker) {
        mMarker = marker;
    }

    @Deprecated
    public IMarker getMarkerView() {
        return getMarker();
    }

    @Deprecated
    public void setMarkerView(IMarker v) {
        setMarker(v);
    }

    public Description getDescription() {
        return mDescription;
    }

    public void setDescription(Description desc) {
        this.mDescription = desc;
    }

    public Legend getLegend() {
        return mLegend;
    }

    public LegendRenderer getLegendRenderer() {
        return mLegendRenderer;
    }

    @Override
    public RectF getContentRect() {
        return mViewPortHandler.getContentRect();
    }

    public void disableScroll() {
        ViewParent parent = getParent();
        if (parent != null)
            parent.requestDisallowInterceptTouchEvent(true);
    }

    public void enableScroll() {
        ViewParent parent = getParent();
        if (parent != null)
            parent.requestDisallowInterceptTouchEvent(false);
    }

    public void setPaint(Paint p, int which) {

        switch (which) {
            case PAINT_INFO:
                mInfoPaint = p;
                break;
            case PAINT_DESCRIPTION:
                mDescPaint = p;
                break;
        }
    }

    public Paint getPaint(int which) {
        switch (which) {
            case PAINT_INFO:
                return mInfoPaint;
            case PAINT_DESCRIPTION:
                return mDescPaint;
        }

        return null;
    }

    @Deprecated
    public boolean isDrawMarkerViewsEnabled() {
        return isDrawMarkersEnabled();
    }

    @Deprecated
    public void setDrawMarkerViews(boolean enabled) {
        setDrawMarkers(enabled);
    }

    public boolean isDrawMarkersEnabled() {
        return mDrawMarkers;
    }

    public void setDrawMarkers(boolean enabled) {
        mDrawMarkers = enabled;
    }

    public T getData() {
        return mData;
    }

    public void setData(T data) {

        mData = data;
        mOffsetsCalculated = false;

        if (data == null) {
            return;
        }

        setupDefaultFormatter(data.getYMin(), data.getYMax());

        for (IDataSet set : mData.getDataSets()) {
            if (set.needsFormatter() || set.getValueFormatter() == mDefaultValueFormatter)
                set.setValueFormatter(mDefaultValueFormatter);
        }

        notifyDataSetChanged();

        if (mLogEnabled)
            Log.i(LOG_TAG, "Data is set.");
    }

    public ViewPortHandler getViewPortHandler() {
        return mViewPortHandler;
    }

    public DataRenderer getRenderer() {
        return mRenderer;
    }

    public void setRenderer(DataRenderer renderer) {

        if (renderer != null)
            mRenderer = renderer;
    }

    public IHighlighter getHighlighter() {
        return mHighlighter;
    }

    public void setHighlighter(ChartHighlighter highlighter) {
        mHighlighter = highlighter;
    }

    @Override
    public MPPointF getCenterOfView() {
        return getCenter();
    }

    public Bitmap getChartBitmap() {

        Bitmap returnedBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(returnedBitmap);

        Drawable bgDrawable = getBackground();
        if (bgDrawable != null)

            bgDrawable.draw(canvas);
        else

            canvas.drawColor(Color.WHITE);

        draw(canvas);

        return returnedBitmap;
    }

    public boolean saveToPath(String title, String pathOnSD) {

        Bitmap b = getChartBitmap();

        OutputStream stream = null;
        try {
            stream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()
                    + pathOnSD + "/" + title
                    + ".png");

            b.compress(CompressFormat.PNG, 40, stream);

            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean saveToGallery(String fileName, String subFolderPath, String fileDescription, CompressFormat
            format, int quality) {

        if (quality < 0 || quality > 100)
            quality = 50;

        long currentTime = System.currentTimeMillis();

        File extBaseDir = Environment.getExternalStorageDirectory();
        File file = new File(extBaseDir.getAbsolutePath() + "/DCIM/" + subFolderPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return false;
            }
        }

        String mimeType = "";
        switch (format) {
            case PNG:
                mimeType = "image/png";
                if (!fileName.endsWith(".png"))
                    fileName += ".png";
                break;
            case WEBP:
                mimeType = "image/webp";
                if (!fileName.endsWith(".webp"))
                    fileName += ".webp";
                break;
            case JPEG:
            default:
                mimeType = "image/jpeg";
                if (!(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")))
                    fileName += ".jpg";
                break;
        }

        String filePath = file.getAbsolutePath() + "/" + fileName;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filePath);

            Bitmap b = getChartBitmap();
            b.compress(format, quality, out);

            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        long size = new File(filePath).length();

        ContentValues values = new ContentValues(8);

        values.put(Images.Media.TITLE, fileName);
        values.put(Images.Media.DISPLAY_NAME, fileName);
        values.put(Images.Media.DATE_ADDED, currentTime);
        values.put(Images.Media.MIME_TYPE, mimeType);
        values.put(Images.Media.DESCRIPTION, fileDescription);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, filePath);
        values.put(Images.Media.SIZE, size);

        return getContext().getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values) != null;
    }

    public boolean saveToGallery(String fileName, int quality) {
        return saveToGallery(fileName, "", "MPAndroidChart-Library Save", CompressFormat.PNG, quality);
    }

    public boolean saveToGallery(String fileName) {
        return saveToGallery(fileName, "", "MPAndroidChart-Library Save", CompressFormat.PNG, 40);
    }

    public void removeViewportJob(Runnable job) {
        mJobs.remove(job);
    }

    public void clearAllViewportJobs() {
        mJobs.clear();
    }

    public void addViewportJob(Runnable job) {

        if (mViewPortHandler.hasChartDimens()) {
            post(job);
        } else {
            mJobs.add(job);
        }
    }

    public ArrayList<Runnable> getJobs() {
        return mJobs;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(left, top, right, bottom);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = (int) Utils.convertDpToPixel(50f);
        setMeasuredDimension(
                Math.max(getSuggestedMinimumWidth(),
                        resolveSize(size,
                                widthMeasureSpec)),
                Math.max(getSuggestedMinimumHeight(),
                        resolveSize(size,
                                heightMeasureSpec)));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mLogEnabled)
            Log.i(LOG_TAG, "OnSizeChanged()");

        if (w > 0 && h > 0 && w < 10000 && h < 10000) {
            if (mLogEnabled)
                Log.i(LOG_TAG, "Setting chart dimens, width: " + w + ", height: " + h);
            mViewPortHandler.setChartDimens(w, h);
        } else {
            if (mLogEnabled)
                Log.w(LOG_TAG, "*Avoiding* setting chart dimens! width: " + w + ", height: " + h);
        }

        notifyDataSetChanged();

        for (Runnable r : mJobs) {
            post(r);
        }

        mJobs.clear();

        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void setHardwareAccelerationEnabled(boolean enabled) {

        if (enabled)
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mUnbind)
            unbindDrawables(this);
    }

    private void unbindDrawables(View view) {

        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    public void setUnbindEnabled(boolean enabled) {
        this.mUnbind = enabled;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\CombinedChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;

import com.mpdc4gsr.libunified.ui.data.*;
import com.mpdc4gsr.libunified.ui.highlight.CombinedHighlighter;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.CombinedDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.renderer.CombinedChartRenderer;

public class CombinedChart extends BarLineChartBase<CombinedData> implements CombinedDataProvider {

    protected boolean mHighlightFullBarEnabled = false;
    protected DrawOrder[] mDrawOrder;
    private boolean mDrawValueAboveBar = true;
    private boolean mDrawBarShadow = false;

    public CombinedChart(Context context) {
        super(context);
    }

    public CombinedChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CombinedChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mDrawOrder = new DrawOrder[]{
                DrawOrder.BAR, DrawOrder.BUBBLE, DrawOrder.LINE, DrawOrder.CANDLE, DrawOrder.SCATTER
        };

        setHighlighter(new CombinedHighlighter(this, this));

        setHighlightFullBarEnabled(true);

        mRenderer = new CombinedChartRenderer(this, mAnimator, mViewPortHandler);
    }

    @Override
    public CombinedData getCombinedData() {
        return mData;
    }

    @Override
    public void setData(CombinedData data) {
        super.setData(data);
        setHighlighter(new CombinedHighlighter(this, this));
        ((CombinedChartRenderer) mRenderer).createRenderers();
        mRenderer.initBuffers();
    }

    @Override
    public Highlight getHighlightByTouchPoint(float x, float y) {

        if (mData == null) {
            Log.e(LOG_TAG, "Can't select by touch. No data set.");
            return null;
        } else {
            Highlight h = getHighlighter().getHighlight(x, y);
            if (h == null || !isHighlightFullBarEnabled()) return h;

            return new Highlight(h.getX(), h.getY(),
                    h.getXPx(), h.getYPx(),
                    h.getDataSetIndex(), -1, h.getAxis());
        }
    }

    @Override
    public LineData getLineData() {
        if (mData == null)
            return null;
        return mData.getLineData();
    }

    @Override
    public BarData getBarData() {
        if (mData == null)
            return null;
        return mData.getBarData();
    }

    @Override
    public ScatterData getScatterData() {
        if (mData == null)
            return null;
        return mData.getScatterData();
    }

    @Override
    public CandleData getCandleData() {
        if (mData == null)
            return null;
        return mData.getCandleData();
    }

    @Override
    public BubbleData getBubbleData() {
        if (mData == null)
            return null;
        return mData.getBubbleData();
    }

    @Override
    public boolean isDrawBarShadowEnabled() {
        return mDrawBarShadow;
    }

    @Override
    public boolean isDrawValueAboveBarEnabled() {
        return mDrawValueAboveBar;
    }

    public void setDrawValueAboveBar(boolean enabled) {
        mDrawValueAboveBar = enabled;
    }

    public void setDrawBarShadow(boolean enabled) {
        mDrawBarShadow = enabled;
    }

    @Override
    public boolean isHighlightFullBarEnabled() {
        return mHighlightFullBarEnabled;
    }

    public void setHighlightFullBarEnabled(boolean enabled) {
        mHighlightFullBarEnabled = enabled;
    }

    public DrawOrder[] getDrawOrder() {
        return mDrawOrder;
    }

    public void setDrawOrder(DrawOrder[] order) {
        if (order == null || order.length <= 0)
            return;
        mDrawOrder = order;
    }

    protected void drawMarkers(Canvas canvas) {

        if (mMarker == null || !isDrawMarkersEnabled() || !valuesToHighlight())
            return;

        for (int i = 0; i < mIndicesToHighlight.length; i++) {

            Highlight highlight = mIndicesToHighlight[i];

            IDataSet set = mData.getDataSetByHighlight(highlight);

            Entry e = mData.getEntryForHighlight(highlight);
            if (e == null)
                continue;

            int entryIndex = set.getEntryIndex(e);

            if (entryIndex > set.getEntryCount() * mAnimator.getPhaseX())
                continue;

            float[] pos = getMarkerPosition(highlight);

            if (!mViewPortHandler.isInBounds(pos[0], pos[1]))
                continue;

            mMarker.refreshContent(e, highlight);

            mMarker.draw(canvas, pos[0], pos[1]);
        }
    }

    public enum DrawOrder {
        BAR, BUBBLE, LINE, CANDLE, SCATTER
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\HorizontalBarChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import com.mpdc4gsr.libunified.ui.components.XAxis.XAxisPosition;
import com.mpdc4gsr.libunified.ui.components.YAxis.AxisDependency;
import com.mpdc4gsr.libunified.ui.data.BarEntry;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.highlight.HorizontalBarHighlighter;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;
import com.mpdc4gsr.libunified.ui.renderer.HorizontalBarChartRenderer;
import com.mpdc4gsr.libunified.ui.renderer.XAxisRendererHorizontalBarChart;
import com.mpdc4gsr.libunified.ui.renderer.YAxisRendererHorizontalBarChart;
import com.mpdc4gsr.libunified.ui.utils.HorizontalViewPortHandler;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.TransformerHorizontalBarChart;
import com.mpdc4gsr.libunified.ui.utils.Utils;

public class HorizontalBarChart extends BarChart {

    protected float[] mGetPositionBuffer = new float[2];
    private RectF mOffsetsBuffer = new RectF();

    public HorizontalBarChart(Context context) {
        super(context);
    }

    public HorizontalBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {

        mViewPortHandler = new HorizontalViewPortHandler();

        super.init();

        mLeftAxisTransformer = new TransformerHorizontalBarChart(mViewPortHandler);
        mRightAxisTransformer = new TransformerHorizontalBarChart(mViewPortHandler);

        mRenderer = new HorizontalBarChartRenderer(this, mAnimator, mViewPortHandler);
        setHighlighter(new HorizontalBarHighlighter(this));

        mAxisRendererLeft = new YAxisRendererHorizontalBarChart(mViewPortHandler, mAxisLeft, mLeftAxisTransformer);
        mAxisRendererRight = new YAxisRendererHorizontalBarChart(mViewPortHandler, mAxisRight, mRightAxisTransformer);
        mXAxisRenderer = new XAxisRendererHorizontalBarChart(mViewPortHandler, mXAxis, mLeftAxisTransformer, this);
    }

    @Override
    public void calculateOffsets() {

        float offsetLeft = 0f, offsetRight = 0f, offsetTop = 0f, offsetBottom = 0f;

        calculateLegendOffsets(mOffsetsBuffer);

        offsetLeft += mOffsetsBuffer.left;
        offsetTop += mOffsetsBuffer.top;
        offsetRight += mOffsetsBuffer.right;
        offsetBottom += mOffsetsBuffer.bottom;

        if (mAxisLeft.needsOffset()) {
            offsetTop += mAxisLeft.getRequiredHeightSpace(mAxisRendererLeft.getPaintAxisLabels());
        }

        if (mAxisRight.needsOffset()) {
            offsetBottom += mAxisRight.getRequiredHeightSpace(mAxisRendererRight.getPaintAxisLabels());
        }

        float xlabelwidth = mXAxis.mLabelRotatedWidth;

        if (mXAxis.isEnabled()) {

            if (mXAxis.getPosition() == XAxisPosition.BOTTOM) {

                offsetLeft += xlabelwidth;

            } else if (mXAxis.getPosition() == XAxisPosition.TOP) {

                offsetRight += xlabelwidth;

            } else if (mXAxis.getPosition() == XAxisPosition.BOTH_SIDED) {

                offsetLeft += xlabelwidth;
                offsetRight += xlabelwidth;
            }
        }

        offsetTop += getExtraTopOffset();
        offsetRight += getExtraRightOffset();
        offsetBottom += getExtraBottomOffset();
        offsetLeft += getExtraLeftOffset();

        float minOffset = Utils.convertDpToPixel(mMinOffset);

        mViewPortHandler.restrainViewPort(
                Math.max(minOffset, offsetLeft),
                Math.max(minOffset, offsetTop),
                Math.max(minOffset, offsetRight),
                Math.max(minOffset, offsetBottom));

        if (mLogEnabled) {
            Log.i(LOG_TAG, "offsetLeft: " + offsetLeft + ", offsetTop: " + offsetTop + ", offsetRight: " +
                    offsetRight + ", offsetBottom: "
                    + offsetBottom);
            Log.i(LOG_TAG, "Content: " + mViewPortHandler.getContentRect().toString());
        }

        prepareOffsetMatrix();
        prepareValuePxMatrix();
    }

    @Override
    protected void prepareValuePxMatrix() {
        mRightAxisTransformer.prepareMatrixValuePx(mAxisRight.mAxisMinimum, mAxisRight.mAxisRange, mXAxis.mAxisRange,
                mXAxis.mAxisMinimum);
        mLeftAxisTransformer.prepareMatrixValuePx(mAxisLeft.mAxisMinimum, mAxisLeft.mAxisRange, mXAxis.mAxisRange,
                mXAxis.mAxisMinimum);
    }

    @Override
    protected float[] getMarkerPosition(Highlight high) {
        return new float[]{high.getDrawY(), high.getDrawX()};
    }

    @Override
    public void getBarBounds(BarEntry e, RectF outputRect) {

        RectF bounds = outputRect;
        IBarDataSet set = mData.getDataSetForEntry(e);

        if (set == null) {
            outputRect.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
            return;
        }

        float y = e.getY();
        float x = e.getX();

        float barWidth = mData.getBarWidth();

        float top = x - barWidth / 2f;
        float bottom = x + barWidth / 2f;
        float left = y >= 0 ? y : 0;
        float right = y <= 0 ? y : 0;

        bounds.set(left, top, right, bottom);

        getTransformer(set.getAxisDependency()).rectValueToPixel(bounds);

    }

    @Override
    public MPPointF getPosition(Entry e, AxisDependency axis) {

        if (e == null)
            return null;

        float[] vals = mGetPositionBuffer;
        vals[0] = e.getY();
        vals[1] = e.getX();

        getTransformer(axis).pointValuesToPixel(vals);

        return MPPointF.getInstance(vals[0], vals[1]);
    }

    @Override
    public Highlight getHighlightByTouchPoint(float x, float y) {

        if (mData == null) {
            if (mLogEnabled)
                Log.e(LOG_TAG, "Can't select by touch. No data set.");
            return null;
        } else
            return getHighlighter().getHighlight(y, x);
    }

    @Override
    public float getLowestVisibleX() {
        getTransformer(AxisDependency.LEFT).getValuesByTouchPoint(mViewPortHandler.contentLeft(),
                mViewPortHandler.contentBottom(), posForGetLowestVisibleX);
        float result = (float) Math.max(mXAxis.mAxisMinimum, posForGetLowestVisibleX.y);
        return result;
    }

    @Override
    public float getHighestVisibleX() {
        getTransformer(AxisDependency.LEFT).getValuesByTouchPoint(mViewPortHandler.contentLeft(),
                mViewPortHandler.contentTop(), posForGetHighestVisibleX);
        float result = (float) Math.min(mXAxis.mAxisMaximum, posForGetHighestVisibleX.y);
        return result;
    }

    @Override
    public void setVisibleXRangeMaximum(float maxXRange) {
        float xScale = mXAxis.mAxisRange / (maxXRange);
        mViewPortHandler.setMinimumScaleY(xScale);
    }

    @Override
    public void setVisibleXRangeMinimum(float minXRange) {
        float xScale = mXAxis.mAxisRange / (minXRange);
        mViewPortHandler.setMaximumScaleY(xScale);
    }

    @Override
    public void setVisibleXRange(float minXRange, float maxXRange) {
        float minScale = mXAxis.mAxisRange / minXRange;
        float maxScale = mXAxis.mAxisRange / maxXRange;
        mViewPortHandler.setMinMaxScaleY(minScale, maxScale);
    }

    @Override
    public void setVisibleYRangeMaximum(float maxYRange, AxisDependency axis) {
        float yScale = getAxisRange(axis) / maxYRange;
        mViewPortHandler.setMinimumScaleX(yScale);
    }

    @Override
    public void setVisibleYRangeMinimum(float minYRange, AxisDependency axis) {
        float yScale = getAxisRange(axis) / minYRange;
        mViewPortHandler.setMaximumScaleX(yScale);
    }

    @Override
    public void setVisibleYRange(float minYRange, float maxYRange, AxisDependency axis) {
        float minScale = getAxisRange(axis) / minYRange;
        float maxScale = getAxisRange(axis) / maxYRange;
        mViewPortHandler.setMinMaxScaleX(minScale, maxScale);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\LineChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.mpdc4gsr.libunified.ui.data.LineData;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.LineDataProvider;
import com.mpdc4gsr.libunified.ui.renderer.LineChartRenderer;

public class LineChart extends BarLineChartBase<LineData> implements LineDataProvider {

    public LineChart(Context context) {
        super(context);
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new LineChartRenderer(this, mAnimator, mViewPortHandler);
    }

    @Override
    public LineData getLineData() {
        return mData;
    }

    @Override
    protected void onDetachedFromWindow() {

        if (mRenderer != null && mRenderer instanceof LineChartRenderer) {
            ((LineChartRenderer) mRenderer).releaseBitmap();
        }
        super.onDetachedFromWindow();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\PieChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;

import com.mpdc4gsr.libunified.ui.components.XAxis;
import com.mpdc4gsr.libunified.ui.data.PieData;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.highlight.PieHighlighter;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IPieDataSet;
import com.mpdc4gsr.libunified.ui.renderer.PieChartRenderer;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.List;

public class PieChart extends PieRadarChartBase<PieData> {

    protected float mTransparentCircleRadiusPercent = 55f;
    protected float mMaxAngle = 360f;
    private RectF mCircleBox = new RectF();
    private boolean mDrawEntryLabels = true;
    private float[] mDrawAngles = new float[1];
    private float[] mAbsoluteAngles = new float[1];
    private boolean mDrawHole = true;
    private boolean mDrawSlicesUnderHole = false;
    private boolean mUsePercentValues = false;
    private boolean mDrawRoundedSlices = false;
    private CharSequence mCenterText = "";
    private MPPointF mCenterTextOffset = MPPointF.getInstance(0, 0);
    private float mHoleRadiusPercent = 50f;
    private boolean mDrawCenterText = true;
    private float mCenterTextRadiusPercent = 100.f;
    private float mMinAngleForSlices = 0f;

    public PieChart(Context context) {
        super(context);
    }

    public PieChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new PieChartRenderer(this, mAnimator, mViewPortHandler);
        mXAxis = null;

        mHighlighter = new PieHighlighter(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mData == null)
            return;

        mRenderer.drawData(canvas);

        if (valuesToHighlight())
            mRenderer.drawHighlighted(canvas, mIndicesToHighlight);

        mRenderer.drawExtras(canvas);

        mRenderer.drawValues(canvas);

        mLegendRenderer.renderLegend(canvas);

        drawDescription(canvas);

        drawMarkers(canvas);
    }

    @Override
    public void calculateOffsets() {
        super.calculateOffsets();

        if (mData == null)
            return;

        float diameter = getDiameter();
        float radius = diameter / 2f;

        MPPointF c = getCenterOffsets();

        float shift = mData.getDataSet().getSelectionShift();

        mCircleBox.set(c.x - radius + shift,
                c.y - radius + shift,
                c.x + radius - shift,
                c.y + radius - shift);

        MPPointF.recycleInstance(c);
    }

    @Override
    protected void calcMinMax() {
        calcAngles();
    }

    @Override
    protected float[] getMarkerPosition(Highlight highlight) {

        MPPointF center = getCenterCircleBox();
        float r = getRadius();

        float off = r / 10f * 3.6f;

        if (isDrawHoleEnabled()) {
            off = (r - (r / 100f * getHoleRadius())) / 2f;
        }

        r -= off;

        float rotationAngle = getRotationAngle();

        int entryIndex = (int) highlight.getX();

        float offset = mDrawAngles[entryIndex] / 2;

        float x = (float) (r
                * Math.cos(Math.toRadians((rotationAngle + mAbsoluteAngles[entryIndex] - offset)
                * mAnimator.getPhaseY())) + center.x);
        float y = (float) (r
                * Math.sin(Math.toRadians((rotationAngle + mAbsoluteAngles[entryIndex] - offset)
                * mAnimator.getPhaseY())) + center.y);

        MPPointF.recycleInstance(center);
        return new float[]{x, y};
    }

    private void calcAngles() {

        int entryCount = mData.getEntryCount();

        if (mDrawAngles.length != entryCount) {
            mDrawAngles = new float[entryCount];
        } else {
            for (int i = 0; i < entryCount; i++) {
                mDrawAngles[i] = 0;
            }
        }
        if (mAbsoluteAngles.length != entryCount) {
            mAbsoluteAngles = new float[entryCount];
        } else {
            for (int i = 0; i < entryCount; i++) {
                mAbsoluteAngles[i] = 0;
            }
        }

        float yValueSum = mData.getYValueSum();

        List<IPieDataSet> dataSets = mData.getDataSets();

        boolean hasMinAngle = mMinAngleForSlices != 0f && entryCount * mMinAngleForSlices <= mMaxAngle;
        float[] minAngles = new float[entryCount];

        int cnt = 0;
        float offset = 0f;
        float diff = 0f;

        for (int i = 0; i < mData.getDataSetCount(); i++) {

            IPieDataSet set = dataSets.get(i);

            for (int j = 0; j < set.getEntryCount(); j++) {

                float drawAngle = calcAngle(Math.abs(set.getEntryForIndex(j).getY()), yValueSum);

                if (hasMinAngle) {
                    float temp = drawAngle - mMinAngleForSlices;
                    if (temp <= 0) {
                        minAngles[cnt] = mMinAngleForSlices;
                        offset += -temp;
                    } else {
                        minAngles[cnt] = drawAngle;
                        diff += temp;
                    }
                }

                mDrawAngles[cnt] = drawAngle;

                if (cnt == 0) {
                    mAbsoluteAngles[cnt] = mDrawAngles[cnt];
                } else {
                    mAbsoluteAngles[cnt] = mAbsoluteAngles[cnt - 1] + mDrawAngles[cnt];
                }

                cnt++;
            }
        }

        if (hasMinAngle) {

            for (int i = 0; i < entryCount; i++) {
                minAngles[i] -= (minAngles[i] - mMinAngleForSlices) / diff * offset;
                if (i == 0) {
                    mAbsoluteAngles[0] = minAngles[0];
                } else {
                    mAbsoluteAngles[i] = mAbsoluteAngles[i - 1] + minAngles[i];
                }
            }

            mDrawAngles = minAngles;
        }
    }

    public boolean needsHighlight(int index) {

        if (!valuesToHighlight())
            return false;

        for (int i = 0; i < mIndicesToHighlight.length; i++)

            if ((int) mIndicesToHighlight[i].getX() == index)
                return true;

        return false;
    }

    private float calcAngle(float value) {
        return calcAngle(value, mData.getYValueSum());
    }

    private float calcAngle(float value, float yValueSum) {
        return value / yValueSum * mMaxAngle;
    }

    @Deprecated
    @Override
    public XAxis getXAxis() {
        throw new RuntimeException("PieChart has no XAxis");
    }

    @Override
    public int getIndexForAngle(float angle) {

        float a = Utils.getNormalizedAngle(angle - getRotationAngle());

        for (int i = 0; i < mAbsoluteAngles.length; i++) {
            if (mAbsoluteAngles[i] > a)
                return i;
        }

        return -1;
    }

    public int getDataSetIndexForIndex(int xIndex) {

        List<IPieDataSet> dataSets = mData.getDataSets();

        for (int i = 0; i < dataSets.size(); i++) {
            if (dataSets.get(i).getEntryForXValue(xIndex, Float.NaN) != null)
                return i;
        }

        return -1;
    }

    public float[] getDrawAngles() {
        return mDrawAngles;
    }

    public float[] getAbsoluteAngles() {
        return mAbsoluteAngles;
    }

    public void setHoleColor(int color) {
        ((PieChartRenderer) mRenderer).getPaintHole().setColor(color);
    }

    public void setDrawSlicesUnderHole(boolean enable) {
        mDrawSlicesUnderHole = enable;
    }

    public boolean isDrawSlicesUnderHoleEnabled() {
        return mDrawSlicesUnderHole;
    }

    public boolean isDrawHoleEnabled() {
        return mDrawHole;
    }

    public void setDrawHoleEnabled(boolean enabled) {
        this.mDrawHole = enabled;
    }

    public CharSequence getCenterText() {
        return mCenterText;
    }

    public void setCenterText(CharSequence text) {
        if (text == null)
            mCenterText = "";
        else
            mCenterText = text;
    }

    public void setDrawCenterText(boolean enabled) {
        this.mDrawCenterText = enabled;
    }

    public boolean isDrawCenterTextEnabled() {
        return mDrawCenterText;
    }

    @Override
    protected float getRequiredLegendOffset() {
        return mLegendRenderer.getLabelPaint().getTextSize() * 2.f;
    }

    @Override
    protected float getRequiredBaseOffset() {
        return 0;
    }

    @Override
    public float getRadius() {
        if (mCircleBox == null)
            return 0;
        else
            return Math.min(mCircleBox.width() / 2f, mCircleBox.height() / 2f);
    }

    public RectF getCircleBox() {
        return mCircleBox;
    }

    public MPPointF getCenterCircleBox() {
        return MPPointF.getInstance(mCircleBox.centerX(), mCircleBox.centerY());
    }

    public void setCenterTextTypeface(Typeface t) {
        ((PieChartRenderer) mRenderer).getPaintCenterText().setTypeface(t);
    }

    public void setCenterTextSize(float sizeDp) {
        ((PieChartRenderer) mRenderer).getPaintCenterText().setTextSize(
                Utils.convertDpToPixel(sizeDp));
    }

    public void setCenterTextSizePixels(float sizePixels) {
        ((PieChartRenderer) mRenderer).getPaintCenterText().setTextSize(sizePixels);
    }

    public void setCenterTextOffset(float x, float y) {
        mCenterTextOffset.x = Utils.convertDpToPixel(x);
        mCenterTextOffset.y = Utils.convertDpToPixel(y);
    }

    public MPPointF getCenterTextOffset() {
        return MPPointF.getInstance(mCenterTextOffset.x, mCenterTextOffset.y);
    }

    public void setCenterTextColor(int color) {
        ((PieChartRenderer) mRenderer).getPaintCenterText().setColor(color);
    }

    public float getHoleRadius() {
        return mHoleRadiusPercent;
    }

    public void setHoleRadius(final float percent) {
        mHoleRadiusPercent = percent;
    }

    public void setTransparentCircleColor(int color) {

        Paint p = ((PieChartRenderer) mRenderer).getPaintTransparentCircle();
        int alpha = p.getAlpha();
        p.setColor(color);
        p.setAlpha(alpha);
    }

    public float getTransparentCircleRadius() {
        return mTransparentCircleRadiusPercent;
    }

    public void setTransparentCircleRadius(final float percent) {
        mTransparentCircleRadiusPercent = percent;
    }

    public void setTransparentCircleAlpha(int alpha) {
        ((PieChartRenderer) mRenderer).getPaintTransparentCircle().setAlpha(alpha);
    }

    @Deprecated
    public void setDrawSliceText(boolean enabled) {
        mDrawEntryLabels = enabled;
    }

    public void setDrawEntryLabels(boolean enabled) {
        mDrawEntryLabels = enabled;
    }

    public boolean isDrawEntryLabelsEnabled() {
        return mDrawEntryLabels;
    }

    public void setEntryLabelColor(int color) {
        ((PieChartRenderer) mRenderer).getPaintEntryLabels().setColor(color);
    }

    public void setEntryLabelTypeface(Typeface tf) {
        ((PieChartRenderer) mRenderer).getPaintEntryLabels().setTypeface(tf);
    }

    public void setEntryLabelTextSize(float size) {
        ((PieChartRenderer) mRenderer).getPaintEntryLabels().setTextSize(Utils.convertDpToPixel(size));
    }

    public void setDrawRoundedSlices(boolean enabled) {
        mDrawRoundedSlices = enabled;
    }

    public boolean isDrawRoundedSlicesEnabled() {
        return mDrawRoundedSlices;
    }

    public void setUsePercentValues(boolean enabled) {
        mUsePercentValues = enabled;
    }

    public boolean isUsePercentValuesEnabled() {
        return mUsePercentValues;
    }

    public float getCenterTextRadiusPercent() {
        return mCenterTextRadiusPercent;
    }

    public void setCenterTextRadiusPercent(float percent) {
        mCenterTextRadiusPercent = percent;
    }

    public float getMaxAngle() {
        return mMaxAngle;
    }

    public void setMaxAngle(float maxangle) {

        if (maxangle > 360)
            maxangle = 360f;

        if (maxangle < 90)
            maxangle = 90f;

        this.mMaxAngle = maxangle;
    }

    public float getMinAngleForSlices() {
        return mMinAngleForSlices;
    }

    public void setMinAngleForSlices(float minAngle) {

        if (minAngle > (mMaxAngle / 2f))
            minAngle = mMaxAngle / 2f;
        else if (minAngle < 0)
            minAngle = 0f;

        this.mMinAngleForSlices = minAngle;
    }

    @Override
    protected void onDetachedFromWindow() {

        if (mRenderer != null && mRenderer instanceof PieChartRenderer) {
            ((PieChartRenderer) mRenderer).releaseBitmap();
        }
        super.onDetachedFromWindow();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\PieRadarChartBase.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.mpdc4gsr.libunified.ui.animation.Easing.EasingFunction;
import com.mpdc4gsr.libunified.ui.components.Legend;
import com.mpdc4gsr.libunified.ui.components.XAxis;
import com.mpdc4gsr.libunified.ui.data.ChartData;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.listener.PieRadarChartTouchListener;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;

public abstract class PieRadarChartBase<T extends ChartData<? extends IDataSet<? extends Entry>>>
        extends Chart<T> {

    protected boolean mRotateEnabled = true;
    protected float mMinOffset = 0.f;
    private float mRotationAngle = 270f;
    private float mRawRotationAngle = 270f;

    public PieRadarChartBase(Context context) {
        super(context);
    }

    public PieRadarChartBase(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieRadarChartBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mChartTouchListener = new PieRadarChartTouchListener(this);
    }

    @Override
    protected void calcMinMax() {

    }

    @Override
    public int getMaxVisibleCount() {
        return mData.getEntryCount();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mTouchEnabled && mChartTouchListener != null)
            return mChartTouchListener.onTouch(this, event);
        else
            return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {

        if (mChartTouchListener instanceof PieRadarChartTouchListener)
            ((PieRadarChartTouchListener) mChartTouchListener).computeScroll();
    }

    @Override
    public void notifyDataSetChanged() {
        if (mData == null)
            return;

        calcMinMax();

        if (mLegend != null)
            mLegendRenderer.computeLegend(mData);

        calculateOffsets();
    }

    @Override
    public void calculateOffsets() {

        float legendLeft = 0f, legendRight = 0f, legendBottom = 0f, legendTop = 0f;

        if (mLegend != null && mLegend.isEnabled() && !mLegend.isDrawInsideEnabled()) {

            float fullLegendWidth = Math.min(mLegend.mNeededWidth,
                    mViewPortHandler.getChartWidth() * mLegend.getMaxSizePercent());

            switch (mLegend.getOrientation()) {
                case VERTICAL: {
                    float xLegendOffset = 0.f;

                    if (mLegend.getHorizontalAlignment() == Legend.LegendHorizontalAlignment.LEFT
                            || mLegend.getHorizontalAlignment() == Legend.LegendHorizontalAlignment.RIGHT) {
                        if (mLegend.getVerticalAlignment() == Legend.LegendVerticalAlignment.CENTER) {

                            final float spacing = Utils.convertDpToPixel(13f);

                            xLegendOffset = fullLegendWidth + spacing;

                        } else {

                            float spacing = Utils.convertDpToPixel(8f);

                            float legendWidth = fullLegendWidth + spacing;
                            float legendHeight = mLegend.mNeededHeight + mLegend.mTextHeightMax;

                            MPPointF center = getCenter();

                            float bottomX = mLegend.getHorizontalAlignment() ==
                                    Legend.LegendHorizontalAlignment.RIGHT
                                    ? getWidth() - legendWidth + 15.f
                                    : legendWidth - 15.f;
                            float bottomY = legendHeight + 15.f;
                            float distLegend = distanceToCenter(bottomX, bottomY);

                            MPPointF reference = getPosition(center, getRadius(),
                                    getAngleForPoint(bottomX, bottomY));

                            float distReference = distanceToCenter(reference.x, reference.y);
                            float minOffset = Utils.convertDpToPixel(5f);

                            if (bottomY >= center.y && getHeight() - legendWidth > getWidth()) {
                                xLegendOffset = legendWidth;
                            } else if (distLegend < distReference) {

                                float diff = distReference - distLegend;
                                xLegendOffset = minOffset + diff;
                            }

                            MPPointF.recycleInstance(center);
                            MPPointF.recycleInstance(reference);
                        }
                    }

                    switch (mLegend.getHorizontalAlignment()) {
                        case LEFT:
                            legendLeft = xLegendOffset;
                            break;

                        case RIGHT:
                            legendRight = xLegendOffset;
                            break;

                        case CENTER:
                            switch (mLegend.getVerticalAlignment()) {
                                case TOP:
                                    legendTop = Math.min(mLegend.mNeededHeight,
                                            mViewPortHandler.getChartHeight() * mLegend.getMaxSizePercent());
                                    break;
                                case BOTTOM:
                                    legendBottom = Math.min(mLegend.mNeededHeight,
                                            mViewPortHandler.getChartHeight() * mLegend.getMaxSizePercent());
                                    break;
                            }
                            break;
                    }
                }
                break;

                case HORIZONTAL:
                    float yLegendOffset = 0.f;

                    if (mLegend.getVerticalAlignment() == Legend.LegendVerticalAlignment.TOP ||
                            mLegend.getVerticalAlignment() == Legend.LegendVerticalAlignment.BOTTOM) {

                        float yOffset = getRequiredLegendOffset();

                        yLegendOffset = Math.min(mLegend.mNeededHeight + yOffset,
                                mViewPortHandler.getChartHeight() * mLegend.getMaxSizePercent());

                        switch (mLegend.getVerticalAlignment()) {
                            case TOP:
                                legendTop = yLegendOffset;
                                break;
                            case BOTTOM:
                                legendBottom = yLegendOffset;
                                break;
                        }
                    }
                    break;
            }

            legendLeft += getRequiredBaseOffset();
            legendRight += getRequiredBaseOffset();
            legendTop += getRequiredBaseOffset();
            legendBottom += getRequiredBaseOffset();
        }

        float minOffset = Utils.convertDpToPixel(mMinOffset);

        if (this instanceof RadarChart) {
            XAxis x = this.getXAxis();

            if (x.isEnabled() && x.isDrawLabelsEnabled()) {
                minOffset = Math.max(minOffset, x.mLabelRotatedWidth);
            }
        }

        legendTop += getExtraTopOffset();
        legendRight += getExtraRightOffset();
        legendBottom += getExtraBottomOffset();
        legendLeft += getExtraLeftOffset();

        float offsetLeft = Math.max(minOffset, legendLeft);
        float offsetTop = Math.max(minOffset, legendTop);
        float offsetRight = Math.max(minOffset, legendRight);
        float offsetBottom = Math.max(minOffset, Math.max(getRequiredBaseOffset(), legendBottom));

        mViewPortHandler.restrainViewPort(offsetLeft, offsetTop, offsetRight, offsetBottom);

        if (mLogEnabled)
            Log.i(LOG_TAG, "offsetLeft: " + offsetLeft + ", offsetTop: " + offsetTop
                    + ", offsetRight: " + offsetRight + ", offsetBottom: " + offsetBottom);
    }

    public float getAngleForPoint(float x, float y) {

        MPPointF c = getCenterOffsets();

        double tx = x - c.x, ty = y - c.y;
        double length = Math.sqrt(tx * tx + ty * ty);
        double r = Math.acos(ty / length);

        float angle = (float) Math.toDegrees(r);

        if (x > c.x)
            angle = 360f - angle;

        angle = angle + 90f;

        if (angle > 360f)
            angle = angle - 360f;

        MPPointF.recycleInstance(c);

        return angle;
    }

    public MPPointF getPosition(MPPointF center, float dist, float angle) {

        MPPointF p = MPPointF.getInstance(0, 0);
        getPosition(center, dist, angle, p);
        return p;
    }

    public void getPosition(MPPointF center, float dist, float angle, MPPointF outputPoint) {
        outputPoint.x = (float) (center.x + dist * Math.cos(Math.toRadians(angle)));
        outputPoint.y = (float) (center.y + dist * Math.sin(Math.toRadians(angle)));
    }

    public float distanceToCenter(float x, float y) {

        MPPointF c = getCenterOffsets();

        float dist = 0f;

        float xDist = 0f;
        float yDist = 0f;

        if (x > c.x) {
            xDist = x - c.x;
        } else {
            xDist = c.x - x;
        }

        if (y > c.y) {
            yDist = y - c.y;
        } else {
            yDist = c.y - y;
        }

        dist = (float) Math.sqrt(Math.pow(xDist, 2.0) + Math.pow(yDist, 2.0));

        MPPointF.recycleInstance(c);

        return dist;
    }

    public abstract int getIndexForAngle(float angle);

    public float getRawRotationAngle() {
        return mRawRotationAngle;
    }

    public float getRotationAngle() {
        return mRotationAngle;
    }

    public void setRotationAngle(float angle) {
        mRawRotationAngle = angle;
        mRotationAngle = Utils.getNormalizedAngle(mRawRotationAngle);
    }

    public boolean isRotationEnabled() {
        return mRotateEnabled;
    }

    public void setRotationEnabled(boolean enabled) {
        mRotateEnabled = enabled;
    }

    public float getMinOffset() {
        return mMinOffset;
    }

    public void setMinOffset(float minOffset) {
        mMinOffset = minOffset;
    }

    public float getDiameter() {
        RectF content = mViewPortHandler.getContentRect();
        content.left += getExtraLeftOffset();
        content.top += getExtraTopOffset();
        content.right -= getExtraRightOffset();
        content.bottom -= getExtraBottomOffset();
        return Math.min(content.width(), content.height());
    }

    public abstract float getRadius();

    protected abstract float getRequiredLegendOffset();

    protected abstract float getRequiredBaseOffset();

    @Override
    public float getYChartMax() {

        return 0;
    }

    @Override
    public float getYChartMin() {

        return 0;
    }

    @SuppressLint("NewApi")
    public void spin(int durationmillis, float fromangle, float toangle, EasingFunction easing) {
        if (!isAttachedToWindow()) {
            return;
        }
        setRotationAngle(fromangle);

        ObjectAnimator spinAnimator = ObjectAnimator.ofFloat(this, "rotationAngle", fromangle,
                toangle);
        spinAnimator.setDuration(durationmillis);
        spinAnimator.setInterpolator(easing);

        spinAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                postInvalidate();
            }
        });
        if (isAttachedToWindow()) {
            spinAnimator.start();
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\RadarChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.components.YAxis.AxisDependency;
import com.mpdc4gsr.libunified.ui.data.RadarData;
import com.mpdc4gsr.libunified.ui.highlight.RadarHighlighter;
import com.mpdc4gsr.libunified.ui.renderer.RadarChartRenderer;
import com.mpdc4gsr.libunified.ui.renderer.XAxisRendererRadarChart;
import com.mpdc4gsr.libunified.ui.renderer.YAxisRendererRadarChart;
import com.mpdc4gsr.libunified.ui.utils.Utils;

public class RadarChart extends PieRadarChartBase<RadarData> {

    protected YAxisRendererRadarChart mYAxisRenderer;
    protected XAxisRendererRadarChart mXAxisRenderer;
    private float mWebLineWidth = 2.5f;
    private float mInnerWebLineWidth = 1.5f;
    private int mWebColor = Color.rgb(122, 122, 122);
    private int mWebColorInner = Color.rgb(122, 122, 122);
    private int mWebAlpha = 150;
    private boolean mDrawWeb = true;
    private int mSkipWebLineCount = 0;
    private YAxis mYAxis;

    public RadarChart(Context context) {
        super(context);
    }

    public RadarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RadarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mYAxis = new YAxis(AxisDependency.LEFT);

        mWebLineWidth = Utils.convertDpToPixel(1.5f);
        mInnerWebLineWidth = Utils.convertDpToPixel(0.75f);

        mRenderer = new RadarChartRenderer(this, mAnimator, mViewPortHandler);
        mYAxisRenderer = new YAxisRendererRadarChart(mViewPortHandler, mYAxis, this);
        mXAxisRenderer = new XAxisRendererRadarChart(mViewPortHandler, mXAxis, this);

        mHighlighter = new RadarHighlighter(this);
    }

    @Override
    protected void calcMinMax() {
        super.calcMinMax();

        mYAxis.calculate(mData.getYMin(AxisDependency.LEFT), mData.getYMax(AxisDependency.LEFT));
        mXAxis.calculate(0, mData.getMaxEntryCountSet().getEntryCount());
    }

    @Override
    public void notifyDataSetChanged() {
        if (mData == null)
            return;

        calcMinMax();

        mYAxisRenderer.computeAxis(mYAxis.mAxisMinimum, mYAxis.mAxisMaximum, mYAxis.isInverted());
        mXAxisRenderer.computeAxis(mXAxis.mAxisMinimum, mXAxis.mAxisMaximum, false);

        if (mLegend != null && !mLegend.isLegendCustom())
            mLegendRenderer.computeLegend(mData);

        calculateOffsets();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mData == null)
            return;

        if (mXAxis.isEnabled())
            mXAxisRenderer.computeAxis(mXAxis.mAxisMinimum, mXAxis.mAxisMaximum, false);

        mXAxisRenderer.renderAxisLabels(canvas);

        if (mDrawWeb)
            mRenderer.drawExtras(canvas);

        if (mYAxis.isEnabled() && mYAxis.isDrawLimitLinesBehindDataEnabled())
            mYAxisRenderer.renderLimitLines(canvas);

        mRenderer.drawData(canvas);

        if (valuesToHighlight())
            mRenderer.drawHighlighted(canvas, mIndicesToHighlight);

        if (mYAxis.isEnabled() && !mYAxis.isDrawLimitLinesBehindDataEnabled())
            mYAxisRenderer.renderLimitLines(canvas);

        mYAxisRenderer.renderAxisLabels(canvas);

        mRenderer.drawValues(canvas);

        mLegendRenderer.renderLegend(canvas);

        drawDescription(canvas);

        drawMarkers(canvas);
    }

    public float getFactor() {
        RectF content = mViewPortHandler.getContentRect();
        return Math.min(content.width() / 2f, content.height() / 2f) / mYAxis.mAxisRange;
    }

    public float getSliceAngle() {
        return 360f / (float) mData.getMaxEntryCountSet().getEntryCount();
    }

    @Override
    public int getIndexForAngle(float angle) {

        float a = Utils.getNormalizedAngle(angle - getRotationAngle());

        float sliceangle = getSliceAngle();

        int max = mData.getMaxEntryCountSet().getEntryCount();

        int index = 0;

        for (int i = 0; i < max; i++) {

            float referenceAngle = sliceangle * (i + 1) - sliceangle / 2f;

            if (referenceAngle > a) {
                index = i;
                break;
            }
        }

        return index;
    }

    public YAxis getYAxis() {
        return mYAxis;
    }

    public float getWebLineWidth() {
        return mWebLineWidth;
    }

    public void setWebLineWidth(float width) {
        mWebLineWidth = Utils.convertDpToPixel(width);
    }

    public float getWebLineWidthInner() {
        return mInnerWebLineWidth;
    }

    public void setWebLineWidthInner(float width) {
        mInnerWebLineWidth = Utils.convertDpToPixel(width);
    }

    public int getWebAlpha() {
        return mWebAlpha;
    }

    public void setWebAlpha(int alpha) {
        mWebAlpha = alpha;
    }

    public int getWebColor() {
        return mWebColor;
    }

    public void setWebColor(int color) {
        mWebColor = color;
    }

    public int getWebColorInner() {
        return mWebColorInner;
    }

    public void setWebColorInner(int color) {
        mWebColorInner = color;
    }

    public void setDrawWeb(boolean enabled) {
        mDrawWeb = enabled;
    }

    public int getSkipWebLineCount() {
        return mSkipWebLineCount;
    }

    public void setSkipWebLineCount(int count) {

        mSkipWebLineCount = Math.max(0, count);
    }

    @Override
    protected float getRequiredLegendOffset() {
        return mLegendRenderer.getLabelPaint().getTextSize() * 4.f;
    }

    @Override
    protected float getRequiredBaseOffset() {
        return mXAxis.isEnabled() && mXAxis.isDrawLabelsEnabled() ?
                mXAxis.mLabelRotatedWidth :
                Utils.convertDpToPixel(10f);
    }

    @Override
    public float getRadius() {
        RectF content = mViewPortHandler.getContentRect();
        return Math.min(content.width() / 2f, content.height() / 2f);
    }

    public float getYChartMax() {
        return mYAxis.mAxisMaximum;
    }

    public float getYChartMin() {
        return mYAxis.mAxisMinimum;
    }

    public float getYRange() {
        return mYAxis.mAxisRange;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\charts\ScatterChart.java =====

package com.mpdc4gsr.libunified.ui.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.mpdc4gsr.libunified.ui.data.ScatterData;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.ScatterDataProvider;
import com.mpdc4gsr.libunified.ui.renderer.ScatterChartRenderer;

public class ScatterChart extends BarLineChartBase<ScatterData> implements ScatterDataProvider {

    public ScatterChart(Context context) {
        super(context);
    }

    public ScatterChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScatterChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new ScatterChartRenderer(this, mAnimator, mViewPortHandler);

        getXAxis().setSpaceMin(0.5f);
        getXAxis().setSpaceMax(0.5f);
    }

    @Override
    public ScatterData getScatterData() {
        return mData;
    }

    public enum ScatterShape {

        SQUARE("SQUARE"),
        CIRCLE("CIRCLE"),
        TRIANGLE("TRIANGLE"),
        CROSS("CROSS"),
        X("X"),
        CHEVRON_UP("CHEVRON_UP"),
        CHEVRON_DOWN("CHEVRON_DOWN");

        private final String shapeIdentifier;

        ScatterShape(final String shapeIdentifier) {
            this.shapeIdentifier = shapeIdentifier;
        }

        public static ScatterShape[] getAllDefaultShapes() {
            return new ScatterShape[]{SQUARE, CIRCLE, TRIANGLE, CROSS, X, CHEVRON_UP, CHEVRON_DOWN};
        }

        @Override
        public String toString() {
            return shapeIdentifier;
        }
    }
}