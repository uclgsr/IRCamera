package com.mpdc4gsr.component.shared.ui.charts;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mpdc4gsr.component.shared.ui.components.XAxis.XAxisPosition;
import com.mpdc4gsr.component.shared.ui.components.YAxis;
import com.mpdc4gsr.component.shared.ui.components.YAxis.AxisDependency;
import com.mpdc4gsr.component.shared.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.component.shared.ui.data.Entry;
import com.mpdc4gsr.component.shared.ui.highlight.ChartHighlighter;
import com.mpdc4gsr.component.shared.ui.highlight.Highlight;
import com.mpdc4gsr.component.shared.ui.interfaces.dataprovider.BarLineScatterCandleBubbleDataProvider;
import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;
import com.mpdc4gsr.component.shared.ui.jobs.AnimatedMoveViewJob;
import com.mpdc4gsr.component.shared.ui.jobs.AnimatedZoomJob;
import com.mpdc4gsr.component.shared.ui.jobs.MoveViewJob;
import com.mpdc4gsr.component.shared.ui.jobs.ZoomJob;
import com.mpdc4gsr.component.shared.ui.listener.BarLineChartTouchListener;
import com.mpdc4gsr.component.shared.ui.listener.OnDrawListener;
import com.mpdc4gsr.component.shared.ui.renderer.XAxisRenderer;
import com.mpdc4gsr.component.shared.ui.renderer.YAxisRenderer;
import com.mpdc4gsr.component.shared.ui.utils.MPPointD;
import com.mpdc4gsr.component.shared.ui.utils.MPPointF;
import com.mpdc4gsr.component.shared.ui.utils.Transformer;
import com.mpdc4gsr.component.shared.ui.utils.Utils;

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
        }
    }

    public void resetTracking() {
        totalTime = 0;
        drawCycles = 0;
    }

    protected void prepareValuePxMatrix() {

        logDebug("Preparing value to pixel matrices");
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
            logDebug("notifyDataSetChanged skipped: data is null");
            return;
        } else {
            logDebug("notifyDataSetChanged executing");
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

            logDebug("Viewport offsets - L:" + offsetLeft + " T:" + offsetTop + " R:" + offsetRight + " B:" + offsetBottom);
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


