package com.mpdc4gsr.libunified.ir.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.energy.iruvc.dual.DualUVCCamera;
import com.energy.iruvc.sdkisp.LibIRTemp;
import com.energy.iruvc.utils.DualCameraParams;
import com.energy.iruvc.utils.Line;
import com.energy.iruvc.utils.SynchronizedBitmap;
import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.app.common.SharedManager;
import com.mpdc4gsr.libunified.app.tools.UnitTools;
import com.mpdc4gsr.libunified.app.utils.UnifiedScreenUtils;
import com.mpdc4gsr.libunified.app.utils.UnifiedTemperatureUtils;
import com.mpdc4gsr.libunified.ir.inf.ILiteListener;
import com.mpdc4gsr.libunified.ir.usbdual.Const;
import com.mpdc4gsr.libunified.ir.usbdual.camera.BaseDualView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TemperatureView extends SurfaceView implements SurfaceHolder.Callback,
        View.OnTouchListener, BaseDualView.OnFrameCallback {

    public static final int REGION_MODE_RESET = -1;
    public static final int REGION_MODE_POINT = 0;
    public static final int REGION_MODE_LINE = 1;
    public static final int REGION_MODE_RECTANGLE = 2;
    public static final int REGION_MODE_CENTER = 3;
    public static final int REGION_NODE_TREND = 4;
    public static final int REGION_MODE_CLEAN = 5;
    private static final String TAG = "TemperatureView";
    private final int TOUCH_TOLERANCE;
    private final int POINT_MAX_COUNT;
    private final int LINE_MAX_COUNT;
    private final int RECTANGLE_MAX_COUNT;
    private final ArrayList<Point> pointList = new ArrayList<>();
    private final ArrayList<Line> lineList = new ArrayList<>();
    private final ArrayList<Rect> rectList = new ArrayList<>();
    // Paint objects for drawing temperature elements
    private final Paint tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<LibIRTemp.TemperatureSampleResult> pointResultList = new ArrayList<>(3);
    private final ArrayList<LibIRTemp.TemperatureSampleResult> lineResultList = new ArrayList<>(3);
    private final ArrayList<LibIRTemp.TemperatureSampleResult> rectangleResultList = new ArrayList<>(3);
    private final Runnable runnable;
    private final Object regionLock = new Object();
    private final boolean isShowC = SharedManager.INSTANCE.getTemperature() == 1;
    public int productType = Const.TYPE_IR;
    private int drawCount = 3;
    @Nullable
    private LibIRTemp irtemp;
    private float xScale = 0;
    private float yScale = 0;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private int temperatureWidth;
    private int temperatureHeight;
    @RegionMode
    private int temperatureRegionMode = REGION_MODE_CLEAN;
    private boolean isShowFull;
    @Nullable
    private OnTrendChangeListener onTrendChangeListener = null;
    @Nullable
    private Runnable onTrendAddListener = null;
    @Nullable
    private Runnable onTrendRemoveListener = null;
    private ILiteListener iLiteListener = null;
    private TempListener listener;
    private boolean isMonitor = false;
    private boolean isUserHighTemp = false;
    private boolean isUserLowTemp = false;
    private SynchronizedBitmap syncimage;
    private byte[] temperature;
    @Nullable
    private Line trendLine;
    private Bitmap regionBitmap;
    private Bitmap regionAndValueBitmap;
    private Thread temperatureThread;
    private volatile boolean runflag = false;
    private WeakReference<ITsTempListener> iTsTempListenerWeakReference;
    private boolean isShow = false;
    private boolean isAddAction = true;
    private int downX = 0;
    private int downY = 0;
    private Line movingLine;
    private LineMoveType lineMoveType = LineMoveType.ALL;
    private Rect movingRect;
    private RectMoveType rectMoveType = RectMoveType.ALL;
    private RectMoveEdge rectMoveEdge = RectMoveEdge.LEFT;
    private RectMoveCorner rectMoveCorner = RectMoveCorner.LT;
    private DualCameraParams.FusionType mCurrentFusionType;
    private byte[] remapTempData;
    private DualUVCCamera dualUVCCamera;
    private byte[] llTempData;

    {
        tempPaint.setStyle(Paint.Style.FILL);
        tempPaint.setTextSize(24f);
        tempPaint.setColor(Color.WHITE);
    }

    public TemperatureView(final Context context) {
        this(context, null, 0);
    }

    public TemperatureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TemperatureView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        TOUCH_TOLERANCE = (int) (7f * context.getResources().getDisplayMetrics().scaledDensity);

        setZOrderOnTop(true);

        getHolder().addCallback(this);
        setOnTouchListener(this);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TemperatureView);
        try {
            drawCount = ta.getInteger(R.styleable.TemperatureView_temperature_count, 3);
        } catch (Exception e) {

        } finally {
            ta.recycle();
        }

        POINT_MAX_COUNT = drawCount;
        LINE_MAX_COUNT = drawCount;
        RECTANGLE_MAX_COUNT = drawCount;

        runnable = () -> {
            while (!temperatureThread.isInterrupted() && runflag) {
                byte[] tempArray;
                if (productType == Const.TYPE_IR_DUAL) {
                    try {
                        if (remapTempData == null) {
                            if (dualUVCCamera != null && llTempData != null
                                    && dualUVCCamera.getTempData(llTempData) != 0) {

                                SystemClock.sleep(1000);
                                continue;
                            }
                        } else {
                            System.arraycopy(remapTempData, 0, llTempData, 0,
                                    temperatureHeight * temperatureWidth * 2);
                        }
                        if (llTempData == null) {
                            continue;
                        } else {
                            tempArray = llTempData;
                            irtemp.setTempData(llTempData);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                } else {
                    try {
                        synchronized (syncimage.dataLock) {

                            irtemp.setTempData(temperature);
                            if (syncimage.type == 1) irtemp.setScale(16);
                        }
                    } catch (Exception e) {
                    }
                    tempArray = temperature;
                }
                try {
                    if (iLiteListener != null) {
                        iLiteListener.getDeltaNucAndVTemp();
                    }
                    if (isMonitor && (viewWidth != getMeasuredWidth() || viewHeight != getMeasuredHeight())) {
                        viewWidth = getMeasuredWidth();
                        xScale = (float) viewWidth / (float) temperatureWidth;
                        viewHeight = getMeasuredHeight();
                        yScale = (float) viewHeight / (float) temperatureHeight;
                    }
                    LibIRTemp.TemperatureSampleResult temperatureSampleResult = irtemp.getTemperatureOfRect(new Rect(0, 0, temperatureWidth / 2, temperatureHeight - 1));

                    if (regionAndValueBitmap != null) {
                        synchronized (regionLock) {
                            Canvas canvas = new Canvas(regionAndValueBitmap);
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            canvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);

                            float fullMaxTemp;
                            float fullMinTemp;
                            LibIRTemp.TemperatureSampleResult fullResult = irtemp.getTemperatureOfRect(new Rect(0, 0, temperatureWidth - 1, temperatureHeight - 1));
                            fullMaxTemp = getTSTemp(fullResult.maxTemperature);
                            fullMinTemp = getTSTemp(fullResult.minTemperature);
                            if (listener != null) {
                                listener.getTemp((int) (fullMaxTemp * 100) / 100f, (int) (fullMinTemp * 100) / 100f, temperature);
                            }

                            if (isShowFull) {
                                String minTem = UnitTools.showC(fullMinTemp, isShowC);
                                int x = UnifiedScreenUtils.correct(fullResult.minTemperaturePixel.x * xScale, getWidth());
                                int y = UnifiedScreenUtils.correct(fullResult.minTemperaturePixel.y * yScale, getHeight());
                                drawCircle(canvas, x, y, false);
                                drawTempText(canvas, minTem, x, y);
                            }
                            if (isUserLowTemp) {
                                int x = UnifiedScreenUtils.correctPoint(fullResult.minTemperaturePixel.x * xScale, getWidth());
                                int y = UnifiedScreenUtils.correctPoint(fullResult.minTemperaturePixel.y * yScale, getHeight());
                                drawPoint(canvas, x, y);
                                drawCircle(canvas, x, y, false);
                            }

                            if (isShowFull) {
                                String maxTem = UnitTools.showC(fullMaxTemp, isShowC);
                                int x = UnifiedScreenUtils.correct(fullResult.maxTemperaturePixel.x * xScale, getWidth());
                                int y = UnifiedScreenUtils.correct(fullResult.maxTemperaturePixel.y * yScale, getHeight());
                                drawCircle(canvas, x, y, true);
                                drawTempText(canvas, maxTem, x, y);
                            }
                            if (isUserHighTemp) {
                                int x = UnifiedScreenUtils.correctPoint(fullResult.maxTemperaturePixel.x * xScale, getWidth());
                                int y = UnifiedScreenUtils.correctPoint(fullResult.maxTemperaturePixel.y * yScale, getHeight());
                                drawPoint(canvas, x, y);
                                drawCircle(canvas, x, y, true);
                            }

                            Line trendLine = this.trendLine;
                            if (trendLine != null) {
                                int startX = (int) (trendLine.start.x / xScale);
                                int startY = (int) (trendLine.start.y / yScale);
                                int endX = (int) (trendLine.end.x / xScale);
                                int endY = (int) (trendLine.end.y / yScale);
                                int minX = Math.min(startX, endX);
                                int maxX = Math.max(startX, endX);
                                int minY = Math.min(startY, endY);
                                int maxY = Math.max(startY, endY);
                                if (maxX < temperatureWidth && minX > 0 && maxY < temperatureHeight && minY > 0) {
                                    temperatureSampleResult = irtemp.getTemperatureOfLine(new Line(new Point(startX, startY), new Point(endX, endY)));
                                    String min = UnitTools.showC(getTSTemp(temperatureSampleResult.minTemperature), isShowC);
                                    String max = UnitTools.showC(getTSTemp(temperatureSampleResult.maxTemperature), isShowC);
                                    drawDot(canvas, temperatureSampleResult.minTemperaturePixel, false);
                                    drawTempText(canvas, min, temperatureSampleResult.minTemperaturePixel);
                                    drawDot(canvas, temperatureSampleResult.maxTemperaturePixel, true);
                                    drawTempText(canvas, max, temperatureSampleResult.maxTemperaturePixel);
                                    if (onTrendChangeListener != null) {
                                        List<Float> tempList = UnifiedTemperatureUtils.INSTANCE.getLineTemperatures(new Point(startX, startY), new Point(endX, endY), tempArray, temperatureWidth, temperatureHeight);
                                        onTrendChangeListener.onChange(tempList);
                                    }
                                }
                            }
                            for (int index = 0; index < rectList.size(); index++) {
                                Rect tempRectangle = rectList.get(index);
                                int left = (int) (tempRectangle.left / xScale);
                                int top = (int) (tempRectangle.top / yScale);
                                int right = (int) (tempRectangle.right / xScale);
                                int bottom = (int) (tempRectangle.bottom / yScale);
                                if (right > left && bottom > top && left < temperatureWidth && top < temperatureHeight && right > 0 && bottom > 0) {
                                    int tempLeft = Math.max(left, 0);
                                    int tempTop = Math.max(top, 0);
                                    int tempRight = Math.min(right, temperatureWidth);
                                    int tempBottom = Math.min(bottom, temperatureHeight);
                                    temperatureSampleResult = irtemp.getTemperatureOfRect(new Rect(tempLeft, tempTop, tempRight, tempBottom));
                                    String min = UnitTools.showC(getTSTemp(temperatureSampleResult.minTemperature), isShowC);
                                    String max = UnitTools.showC(getTSTemp(temperatureSampleResult.maxTemperature), isShowC);
                                    drawDot(canvas, temperatureSampleResult.minTemperaturePixel, false);
                                    drawTempText(canvas, min, temperatureSampleResult.minTemperaturePixel);
                                    drawDot(canvas, temperatureSampleResult.maxTemperaturePixel, true);
                                    drawTempText(canvas, max, temperatureSampleResult.maxTemperaturePixel);
                                }
                            }
                            for (Line line : lineList) {
                                int startX = (int) (line.start.x / xScale);
                                int startY = (int) (line.start.y / yScale);
                                int endX = (int) (line.end.x / xScale);
                                int endY = (int) (line.end.y / yScale);
                                int minX = Math.min(startX, endX);
                                int maxX = Math.max(startX, endX);
                                int minY = Math.min(startY, endY);
                                int maxY = Math.max(startY, endY);
                                if (maxX < temperatureWidth && minX > 0 && maxY < temperatureHeight && minY > 0) {
                                    temperatureSampleResult = irtemp.getTemperatureOfLine(new Line(new Point(startX, startY), new Point(endX, endY)));
                                    String min = UnitTools.showC(getTSTemp(temperatureSampleResult.minTemperature), isShowC);
                                    String max = UnitTools.showC(getTSTemp(temperatureSampleResult.maxTemperature), isShowC);
                                    drawDot(canvas, temperatureSampleResult.minTemperaturePixel, false);
                                    drawTempText(canvas, min, temperatureSampleResult.minTemperaturePixel);
                                    drawDot(canvas, temperatureSampleResult.maxTemperaturePixel, true);
                                    drawTempText(canvas, max, temperatureSampleResult.maxTemperaturePixel);
                                }
                            }
                            for (Point point : pointList) {
                                int x = (int) (point.x / xScale);
                                int y = (int) (point.y / yScale);
                                if (x < temperatureWidth && x > 0 && y < temperatureHeight && y > 0) {
                                    temperatureSampleResult = irtemp.getTemperatureOfPoint(new Point(x, y));
                                    String max = UnitTools.showC(getTSTemp(temperatureSampleResult.maxTemperature), isShowC);
                                    drawCircle(canvas, point.x, point.y, true);
                                    drawTempText(canvas, max, point.x, point.y);
                                }
                            }

                            if (isShowFull || (!lineList.isEmpty() || !pointList.isEmpty() || !rectList.isEmpty())) {
                                drawPoint(canvas, getWidth() / 2, getHeight() / 2);
                                temperatureSampleResult = irtemp.getTemperatureOfPoint(new Point(temperatureWidth / 2, temperatureHeight / 2));
                                String max = UnitTools.showC(getTSTemp(temperatureSampleResult.maxTemperature), isShowC);
                                drawTempText(canvas, max, temperatureSampleResult.maxTemperaturePixel);
                            }
                        }
                        Canvas surfaceViewCanvas = getHolder().lockCanvas();
                        if (surfaceViewCanvas == null) {
                            SystemClock.sleep(1000);
                            continue;
                        }
                        try {
                            surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            surfaceViewCanvas.drawBitmap(regionAndValueBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                            getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                }
                SystemClock.sleep(1000);
            }
        };
    }

    private boolean isLineConcat(@NonNull Line line, int x, int y) {
        int tempDistance = ((line.end.y - line.start.y) * x - (line.end.x - line.start.x) * y + line.end.x * line.start.y - line.start.x * line.end.y);
        tempDistance = (int) (tempDistance / Math.sqrt(Math.pow(line.end.y - line.start.y, 2) + Math.pow(line.end.x - line.start.x, 2)));
        return Math.abs(tempDistance) < TOUCH_TOLERANCE && x > Math.min(line.start.x, line.end.x) - TOUCH_TOLERANCE && x < Math.max(line.start.x, line.end.x) + TOUCH_TOLERANCE;
    }

    @RegionMode
    public int getTemperatureRegionMode() {
        return this.temperatureRegionMode;
    }

    public void setTemperatureRegionMode(@RegionMode int temperatureRegionMode) {
        this.temperatureRegionMode = temperatureRegionMode;
        if (temperatureRegionMode == REGION_MODE_CENTER) {
            isShowFull = true;
        } else if (temperatureRegionMode == REGION_MODE_CLEAN) {
            isShowFull = false;
        }
    }

    public boolean isShowFull() {
        return isShowFull;
    }

    public void setShowFull(boolean showFull) {
        isShowFull = showFull;
        if (temperatureRegionMode == REGION_MODE_CLEAN) {
            temperatureRegionMode = REGION_MODE_CENTER;
        }
    }

    public void setTextSize(int textSize) {
        tempPaint.setTextSize(textSize);
        refreshRegion();
    }

    public void setLinePaintColor(@ColorInt int color) {
        tempPaint.setColor(color);
        refreshRegion();
    }

    private void refreshRegion() {
        Canvas surfaceViewCanvas = getHolder().lockCanvas();
        if (surfaceViewCanvas != null) {
            surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            setBitmap();
            surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
            getHolder().unlockCanvasAndPost(surfaceViewCanvas);
        }
    }

    public void setOnTrendChangeListener(@Nullable OnTrendChangeListener onTrendChangeListener) {
        this.onTrendChangeListener = onTrendChangeListener;
    }

    public void setOnTrendAddListener(@Nullable Runnable onTrendAddListener) {
        this.onTrendAddListener = onTrendAddListener;
    }

    public void setOnTrendRemoveListener(@Nullable Runnable onTrendRemoveListener) {
        this.onTrendRemoveListener = onTrendRemoveListener;
    }

    public void setiLiteListener(ILiteListener iLiteListener) {
        this.iLiteListener = iLiteListener;
    }

    public TempListener getListener() {
        return listener;
    }

    public void setListener(TempListener listener) {
        this.listener = listener;
    }

    public void setMonitor(boolean monitor) {
        isMonitor = monitor;
    }

    public boolean isUserHighTemp() {
        return isUserHighTemp;
    }

    public void setUserHighTemp(boolean isUserHighTemp) {
        this.isUserHighTemp = isUserHighTemp;
    }

    public boolean isUserLowTemp() {
        return isUserLowTemp;
    }

    public void setUserLowTemp(boolean isUserLowTemp) {
        this.isUserLowTemp = isUserLowTemp;
    }

    public void setSyncimage(SynchronizedBitmap syncimage) {
        this.syncimage = syncimage;
    }

    public void setTemperature(byte[] temperature) {
        this.temperature = temperature;
    }

    private void setDefPoint(Point point) {
        if (point.x > temperatureWidth && point.x > 0) {
            point.x = temperatureWidth;
        }
        if (point.x <= 0) {
            point.x = 0;
        }
        if (point.y > temperatureHeight) {
            point.y = temperatureHeight;
        }
        if (point.y < 0) {
            point.y = 0;
        }
    }

    public LibIRTemp.TemperatureSampleResult getPointTemp(Point point) {
        if (irtemp == null) {
            return null;
        } else {
            setDefPoint(point);
            return irtemp.getTemperatureOfPoint(point);
        }
    }

    public LibIRTemp.TemperatureSampleResult getLineTemp(Line line) {
        if (irtemp == null) {
            return null;
        } else {
            setDefPoint(line.start);
            setDefPoint(line.end);
            return irtemp.getTemperatureOfLine(line);
        }
    }

    public LibIRTemp.TemperatureSampleResult getRectTemp(Rect rect) {
        if (irtemp == null) {
            return null;
        } else {
            if (rect.top < 0) {
                rect.top = 0;
            }
            if (rect.bottom > temperatureHeight) {
                rect.bottom = temperatureHeight;
            }
            if (rect.left < 0) {
                rect.left = 0;
            }
            if (rect.right > temperatureWidth) {
                rect.right = temperatureWidth;
            }
            return irtemp.getTemperatureOfRect(rect);
        }
    }

    public Bitmap getRegionBitmap() {
        return regionAndValueBitmap;
    }

    public Bitmap getRegionAndValueBitmap() {
        synchronized (regionLock) {
            return regionAndValueBitmap;
        }
    }

    public void setImageSize(int imageWidth, int imageHeight, ITsTempListener iTsTempListener) {
        if (iTsTempListener != null) {
            iTsTempListenerWeakReference = new WeakReference<>(iTsTempListener);
        }
        this.temperatureWidth = imageWidth;
        this.temperatureHeight = imageHeight;
        if (viewWidth == 0) {
            viewWidth = getMeasuredWidth();
        }
        if (viewHeight == 0) {
            viewHeight = getMeasuredHeight();
        }
        xScale = (float) viewWidth / (float) imageWidth;
        yScale = (float) viewHeight / (float) imageHeight;
        irtemp = new LibIRTemp(imageWidth, imageHeight);
        llTempData = new byte[imageHeight * imageWidth * 2];
        for (int i = 0; i < drawCount; i++) {
            pointResultList.add(irtemp.new TemperatureSampleResult());
            lineResultList.add(irtemp.new TemperatureSampleResult());
            rectangleResultList.add(irtemp.new TemperatureSampleResult());
        }
    }

    public void restView() {
        viewWidth = 0;
        viewHeight = 0;
        viewWidth = getMeasuredWidth();
        xScale = (float) viewWidth / (float) temperatureWidth;
        viewHeight = getMeasuredHeight();
        yScale = (float) viewHeight / (float) temperatureHeight;
    }

    public void start() {
        if (!runflag) {
            runflag = true;
            temperatureThread = new Thread(runnable);
            if (isShow) {
                setVisibility(VISIBLE);
            } else {
                setVisibility(INVISIBLE);
            }
            temperatureThread.start();
        }
    }

    public void stop() {
        runflag = false;
        isShow = getVisibility() == View.VISIBLE;
        try {
            if (temperatureThread != null) {
                temperatureThread.interrupt();
                temperatureThread.join();
                temperatureThread = null;
            }
        } catch (InterruptedException ignored) {

        }
    }

    public void clear() {
        if (onTrendRemoveListener != null) {
            onTrendRemoveListener.run();
        }
        trendLine = null;
        pointList.clear();
        lineList.clear();
        rectList.clear();
        if (regionBitmap != null) {
            regionBitmap.eraseColor(0);
        }
        Canvas surfaceViewCanvas = getHolder().lockCanvas();
        if (surfaceViewCanvas != null) {
            surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
            getHolder().unlockCanvasAndPost(surfaceViewCanvas);
        }
        for (int i = 0; i < pointResultList.size(); i++) {
            pointResultList.get(i).index = 0;
        }
        for (int i = 0; i < lineResultList.size(); i++) {
            lineResultList.get(i).index = 0;
        }
        for (int i = 0; i < rectangleResultList.size(); i++) {
            rectangleResultList.get(i).index = 0;
        }
    }

    public void addScalePoint(Point point) {
        float sx = getMeasuredWidth() / (float) temperatureWidth;
        float sy = getMeasuredHeight() / (float) temperatureHeight;
        int viewX = UnifiedScreenUtils.correctPoint(point.x * sx, getMeasuredWidth());
        int viewY = UnifiedScreenUtils.correctPoint(point.y * sy, getMeasuredHeight());
        if (pointList.size() == POINT_MAX_COUNT) {
            pointList.remove(0);
        }
        pointList.add(new Point(viewX, viewY));
    }

    public void addScaleLine(Line l) {
        float sx = getMeasuredWidth() / (float) temperatureWidth;
        float sy = getMeasuredHeight() / (float) temperatureHeight;
        Line line = new Line(new Point(), new Point());
        line.start.x = UnifiedScreenUtils.correct(l.start.x * sx, getMeasuredWidth());
        line.start.y = UnifiedScreenUtils.correct(l.start.y * sy, getMeasuredHeight());
        line.end.x = UnifiedScreenUtils.correct(l.end.x * sx, getMeasuredWidth());
        line.end.y = UnifiedScreenUtils.correct(l.end.y * sy, getMeasuredHeight());
        if (pointList.size() == POINT_MAX_COUNT) {
            pointList.remove(0);
        }
        lineList.add(line);
    }

    public void addScaleRectangle(Rect r) {
        float sx = getMeasuredWidth() / (float) temperatureWidth;
        float sy = getMeasuredHeight() / (float) temperatureHeight;
        Rect rectangle = new Rect();
        rectangle.left = (int) (r.left * sx);
        rectangle.top = (int) (r.top * sy);
        rectangle.right = (int) (r.right * sx);
        rectangle.bottom = (int) (r.bottom * sy);
        if (rectList.size() < RECTANGLE_MAX_COUNT) {
            rectList.add(rectangle);
        } else {
            for (int index = 0; index < rectList.size() - 1; index++) {
                Rect tempRectangle = rectList.get(index + 1);
                rectList.set(index, tempRectangle);
            }
            rectList.set(rectList.size() - 1, rectangle);
        }
    }

    public Point getPoint() {
        if (pointList.isEmpty()) {
            return null;
        }
        return new Point((int) (pointList.get(0).x / xScale), (int) (pointList.get(0).y / yScale));
    }

    public Line getLine() {
        if (!lineList.isEmpty()) {
            Line line = new Line(new Point(), new Point());
            line.start.x = (int) (lineList.get(0).start.x / xScale);
            line.start.y = (int) (lineList.get(0).start.y / yScale);
            line.end.x = (int) (lineList.get(0).end.x / xScale);
            line.end.y = (int) (lineList.get(0).end.y / yScale);
            return line;
        } else {
            return null;
        }
    }

    public Rect getRectangle() {
        if (!rectList.isEmpty()) {
            Rect rect = new Rect();
            rect.left = (int) (rectList.get(0).left / xScale);
            rect.top = (int) (rectList.get(0).top / yScale);
            rect.right = (int) (rectList.get(0).right / xScale);
            rect.bottom = (int) (rectList.get(0).bottom / yScale);
            return rect;
        } else {
            return null;
        }
    }

    public void drawLine() {
        setBitmap();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        holder.setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        viewWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        viewHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        xScale = (float) viewWidth / (float) temperatureWidth;
        yScale = (float) viewHeight / (float) temperatureHeight;

        if (regionBitmap == null || regionBitmap.getWidth() != viewWidth || regionBitmap.getHeight() != viewHeight) {
            regionBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        }
        regionAndValueBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (temperatureRegionMode) {
            case REGION_MODE_POINT:
                return handleTouchPoint(event);
            case REGION_MODE_LINE:
                return handleTouchLine(event, false);
            case REGION_MODE_RECTANGLE:
                return handleTouchRect(event);
            case REGION_NODE_TREND:
                return handleTouchLine(event, true);
            default:
                return false;
        }
    }

    private boolean handleTouchPoint(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = UnifiedScreenUtils.correctPoint(event.getX(), getWidth());
                downY = UnifiedScreenUtils.correctPoint(event.getY(), getHeight());
                Point point = getPoint(downX, downY);
                if (point == null) {
                    isAddAction = true;
                    if (pointList.size() == POINT_MAX_COUNT) {
                        synchronized (regionLock) {
                            pointList.remove(0);
                        }
                        setBitmap();
                    }
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    drawPoint(surfaceViewCanvas, downX, downY);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                } else {
                    isAddAction = false;
                    synchronized (regionLock) {
                        pointList.remove(point);
                    }
                    setBitmap();
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    drawPoint(surfaceViewCanvas, point.x, point.y);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int x = UnifiedScreenUtils.correctPoint(event.getX(), getWidth());
                int y = UnifiedScreenUtils.correctPoint(event.getY(), getHeight());
                Canvas surfaceViewCanvas = getHolder().lockCanvas();
                surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                drawPoint(surfaceViewCanvas, x, y);
                getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                return true;
            }
            case MotionEvent.ACTION_UP: {
                int x = UnifiedScreenUtils.correctPoint(event.getX(), getWidth());
                int y = UnifiedScreenUtils.correctPoint(event.getY(), getHeight());
                if (isAddAction) {
                    synchronized (regionLock) {
                        if (pointList.size() == POINT_MAX_COUNT) {
                            pointList.remove(0);
                        }
                        pointList.add(new Point(x, y));
                    }
                } else {
                    if (Math.abs(x - downX) > TOUCH_TOLERANCE || Math.abs(y - downY) > TOUCH_TOLERANCE) {
                        synchronized (regionLock) {
                            if (pointList.size() == POINT_MAX_COUNT) {
                                pointList.remove(0);
                            }
                            pointList.add(new Point(x, y));
                        }
                    }
                }
                setBitmap();
                Canvas surfaceViewCanvas = getHolder().lockCanvas();
                surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                return true;
            }
            default:
                return false;
        }
    }

    @Nullable
    private Point getPoint(int x, int y) {
        for (int i = pointList.size() - 1; i >= 0; i--) {
            Point point = pointList.get(i);
            if (point.x > x - TOUCH_TOLERANCE && point.x < x + TOUCH_TOLERANCE && point.y > y - TOUCH_TOLERANCE && point.y < y + TOUCH_TOLERANCE) {
                return point;
            }
        }
        return null;
    }

    private boolean handleTouchLine(MotionEvent event, boolean isTrend) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = UnifiedScreenUtils.correct(event.getX(), getWidth());
                downY = UnifiedScreenUtils.correct(event.getY(), getHeight());
                Line line = getLine(downX, downY, isTrend);
                if (line == null) {
                    isAddAction = true;
                } else {
                    isAddAction = false;
                    movingLine = line;
                    if (downX > line.start.x - TOUCH_TOLERANCE && downX < line.start.x + TOUCH_TOLERANCE && downY > line.start.y - TOUCH_TOLERANCE && downY < line.start.y + TOUCH_TOLERANCE) {
                        lineMoveType = LineMoveType.START;
                    } else if (downX > line.end.x - TOUCH_TOLERANCE && downX < line.end.x + TOUCH_TOLERANCE && downY > line.end.y - TOUCH_TOLERANCE && downY < line.end.y + TOUCH_TOLERANCE) {
                        lineMoveType = LineMoveType.END;
                    } else {
                        lineMoveType = LineMoveType.ALL;
                    }
                    if (isTrend) {
                        synchronized (regionLock) {
                            trendLine = null;
                        }
                        if (onTrendRemoveListener != null) {
                            onTrendRemoveListener.run();
                        }
                    } else {
                        synchronized (regionLock) {

                            lineList.remove(line);
                        }
                    }
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    setBitmap();
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    drawLine(surfaceViewCanvas, line.start.x, line.start.y, line.end.x, line.end.y, isTrend);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int x = UnifiedScreenUtils.correct(event.getX(), getWidth());
                int y = UnifiedScreenUtils.correct(event.getY(), getHeight());
                if (isAddAction) {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    drawLine(surfaceViewCanvas, downX, downY, x, y, isTrend);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                } else {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);

                    Point start = new Point();
                    Point end = new Point();
                    switch (lineMoveType) {
                        case ALL:
                            Rect rect = UnifiedScreenUtils.getRect(getWidth(), getHeight());
                            int minX = Math.min(movingLine.start.x, movingLine.end.x);
                            int maxX = Math.max(movingLine.start.x, movingLine.end.x);
                            int minY = Math.min(movingLine.start.y, movingLine.end.y);
                            int maxY = Math.max(movingLine.start.y, movingLine.end.y);
                            int biasX = x < downX ? Math.max(x - downX, rect.left - minX) : Math.min(x - downX, rect.right - maxX);
                            int biasY = y < downY ? Math.max(y - downY, rect.top - minY) : Math.min(y - downY, rect.bottom - maxY);
                            start = new Point(movingLine.start.x + biasX, movingLine.start.y + biasY);
                            end = new Point(movingLine.end.x + biasX, movingLine.end.y + biasY);
                            break;
                        case START:
                            start = new Point(x, y);
                            end = movingLine.end;
                            break;
                        case END:
                            start = movingLine.start;
                            end = new Point(x, y);
                            break;
                    }
                    drawLine(surfaceViewCanvas, start.x, start.y, end.x, end.y, isTrend);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                int x = UnifiedScreenUtils.correct(event.getX(), getWidth());
                int y = UnifiedScreenUtils.correct(event.getY(), getHeight());
                if (isAddAction) {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    if (Math.abs(x - downX) > TOUCH_TOLERANCE || Math.abs(y - downY) > TOUCH_TOLERANCE) {
                        if (isTrend) {
                            synchronized (regionLock) {
                                trendLine = new Line(new Point(downX, downY), new Point(x, y));
                            }
                            if (onTrendAddListener != null) {
                                onTrendAddListener.run();
                            }
                        } else {
                            synchronized (regionLock) {
                                if (lineList.size() == LINE_MAX_COUNT) {
                                    lineList.remove(0);
                                }
                                lineList.add(new Line(new Point(downX, downY), new Point(x, y)));
                            }
                        }
                        setBitmap();
                    }
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                } else {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    Canvas bitmapCanvas = new Canvas(regionBitmap);

                    if (Math.abs(x - downX) > TOUCH_TOLERANCE || Math.abs(y - downY) > TOUCH_TOLERANCE) {
                        Point start = new Point();
                        Point end = new Point();
                        switch (lineMoveType) {
                            case ALL:
                                Rect rect = UnifiedScreenUtils.getRect(getWidth(), getHeight());
                                int minX = Math.min(movingLine.start.x, movingLine.end.x);
                                int maxX = Math.max(movingLine.start.x, movingLine.end.x);
                                int minY = Math.min(movingLine.start.y, movingLine.end.y);
                                int maxY = Math.max(movingLine.start.y, movingLine.end.y);
                                int biasX = x < downX ? Math.max(x - downX, rect.left - minX) : Math.min(x - downX, rect.right - maxX);
                                int biasY = y < downY ? Math.max(y - downY, rect.top - minY) : Math.min(y - downY, rect.bottom - maxY);
                                start = new Point(movingLine.start.x + biasX, movingLine.start.y + biasY);
                                end = new Point(movingLine.end.x + biasX, movingLine.end.y + biasY);
                                break;
                            case START:
                                start = new Point(x, y);
                                end = movingLine.end;
                                break;
                            case END:
                                start = movingLine.start;
                                end = new Point(x, y);
                                break;
                        }
                        drawLine(bitmapCanvas, start.x, start.y, end.x, end.y, isTrend);

                        if (isTrend) {
                            synchronized (regionLock) {
                                trendLine = new Line(start, end);
                            }
                            if (onTrendAddListener != null) {
                                onTrendAddListener.run();
                            }
                        } else {
                            synchronized (regionLock) {
                                if (lineList.size() == LINE_MAX_COUNT) {
                                    lineList.remove(0);
                                }
                                lineList.add(new Line(start, end));
                            }
                        }
                    }
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                }
                return true;
            }
            default:
                return false;
        }
    }

    @Nullable
    private Line getLine(int x, int y, boolean isTrend) {
        if (isTrend) {
            if (trendLine != null && isLineConcat(trendLine, x, y)) {
                return trendLine;
            }
        } else {
            for (int i = lineList.size() - 1; i >= 0; i--) {
                Line line = lineList.get(i);
                if (isLineConcat(line, x, y)) {
                    return line;
                }
            }
        }
        return null;
    }

    private boolean handleTouchRect(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = UnifiedScreenUtils.correct(event.getX(), getWidth());
                downY = UnifiedScreenUtils.correct(event.getY(), getHeight());
                Rect rect = getRect(downX, downY);
                if (rect == null) {
                    isAddAction = true;
                } else {
                    isAddAction = false;
                    movingRect = rect;

                    if (isIn(downX, rect.left)) {
                        if (isIn(downY, rect.top)) {
                            rectMoveType = RectMoveType.CORNER;
                            rectMoveCorner = RectMoveCorner.LT;
                        } else if (isIn(downY, rect.bottom)) {
                            rectMoveType = RectMoveType.CORNER;
                            rectMoveCorner = RectMoveCorner.LB;
                        } else {
                            rectMoveType = RectMoveType.EDGE;
                            rectMoveEdge = RectMoveEdge.LEFT;
                        }
                    } else if (isIn(downX, rect.right)) {
                        if (isIn(downY, rect.top)) {
                            rectMoveType = RectMoveType.CORNER;
                            rectMoveCorner = RectMoveCorner.RT;
                        } else if (isIn(downY, rect.bottom)) {
                            rectMoveType = RectMoveType.CORNER;
                            rectMoveCorner = RectMoveCorner.RB;
                        } else {
                            rectMoveType = RectMoveType.EDGE;
                            rectMoveEdge = RectMoveEdge.RIGHT;
                        }
                    } else if (isIn(downY, rect.top)) {
                        rectMoveType = RectMoveType.EDGE;
                        rectMoveEdge = RectMoveEdge.TOP;
                    } else if (isIn(downY, rect.bottom)) {
                        rectMoveType = RectMoveType.EDGE;
                        rectMoveEdge = RectMoveEdge.BOTTOM;
                    } else {
                        rectMoveType = RectMoveType.ALL;
                    }
                    synchronized (regionLock) {
                        rectList.remove(rect);
                    }
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    setBitmap();
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    drawRect(surfaceViewCanvas, rect.left, rect.top, rect.right, rect.bottom);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int x = UnifiedScreenUtils.correct(event.getX(), getWidth());
                int y = UnifiedScreenUtils.correct(event.getY(), getHeight());
                if (isAddAction) {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    drawRect(surfaceViewCanvas, downX, downY, x, y);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                } else {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    switch (rectMoveType) {
                        case ALL:
                            Rect rect = UnifiedScreenUtils.getRect(getWidth(), getHeight());
                            int biasX = x < downX ? Math.max(x - downX, rect.left - movingRect.left) : Math.min(x - downX, rect.right - movingRect.right);
                            int biasY = y < downY ? Math.max(y - downY, rect.top - movingRect.top) : Math.min(y - downY, rect.bottom - movingRect.bottom);
                            drawRect(surfaceViewCanvas, movingRect.left + biasX, movingRect.top + biasY, movingRect.right + biasX, movingRect.bottom + biasY);
                            break;
                        case EDGE:
                            switch (rectMoveEdge) {
                                case LEFT:
                                    drawRect(surfaceViewCanvas, x, movingRect.top, movingRect.right, movingRect.bottom);
                                    break;
                                case TOP:
                                    drawRect(surfaceViewCanvas, movingRect.left, y, movingRect.right, movingRect.bottom);
                                    break;
                                case RIGHT:
                                    drawRect(surfaceViewCanvas, movingRect.left, movingRect.top, x, movingRect.bottom);
                                    break;
                                case BOTTOM:
                                    drawRect(surfaceViewCanvas, movingRect.left, movingRect.top, movingRect.right, y);
                                    break;
                            }
                            break;
                        case CORNER:
                            switch (rectMoveCorner) {
                                case LT:
                                    drawRect(surfaceViewCanvas, x, y, movingRect.right, movingRect.bottom);
                                    break;
                                case LB:
                                    drawRect(surfaceViewCanvas, x, movingRect.top, movingRect.right, y);
                                    break;
                                case RT:
                                    drawRect(surfaceViewCanvas, movingRect.left, y, x, movingRect.bottom);
                                    break;
                                case RB:
                                    drawRect(surfaceViewCanvas, movingRect.left, movingRect.top, x, y);
                                    break;
                            }
                            break;
                    }
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                int x = UnifiedScreenUtils.correct(event.getX(), getWidth());
                int y = UnifiedScreenUtils.correct(event.getY(), getHeight());
                if (isAddAction) {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    if (Math.abs(x - downX) > TOUCH_TOLERANCE || Math.abs(y - downY) > TOUCH_TOLERANCE) {
                        synchronized (regionLock) {
                            if (rectList.size() == RECTANGLE_MAX_COUNT) {
                                rectList.remove(0);
                            }
                            rectList.add(new Rect(Math.min(downX, x), Math.min(downY, y), Math.max(downX, x), Math.max(downY, y)));
                        }
                        setBitmap();
                    }
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                } else {
                    Canvas surfaceViewCanvas = getHolder().lockCanvas();
                    surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    Canvas bitmapCanvas = new Canvas(regionBitmap);

                    if (Math.abs(x - downX) > TOUCH_TOLERANCE || Math.abs(y - downY) > TOUCH_TOLERANCE) {
                        switch (rectMoveType) {
                            case ALL:
                                Rect rect = UnifiedScreenUtils.getRect(getWidth(), getHeight());
                                int biasX = x < downX ? Math.max(x - downX, rect.left - movingRect.left) : Math.min(x - downX, rect.right - movingRect.right);
                                int biasY = y < downY ? Math.max(y - downY, rect.top - movingRect.top) : Math.min(y - downY, rect.bottom - movingRect.bottom);
                                movingRect.offset(biasX, biasY);
                                break;
                            case EDGE:
                                switch (rectMoveEdge) {
                                    case LEFT:
                                        movingRect.left = Math.min(x, movingRect.right);
                                        movingRect.right = Math.max(x, movingRect.right);
                                        break;
                                    case TOP:
                                        movingRect.top = Math.min(y, movingRect.bottom);
                                        movingRect.bottom = Math.max(y, movingRect.bottom);
                                        break;
                                    case RIGHT:
                                        movingRect.right = Math.max(x, movingRect.left);
                                        movingRect.left = Math.min(x, movingRect.left);
                                        break;
                                    case BOTTOM:
                                        movingRect.bottom = Math.max(y, movingRect.top);
                                        movingRect.top = Math.min(y, movingRect.top);
                                        break;
                                }
                                break;
                            case CORNER:
                                switch (rectMoveCorner) {
                                    case LT:
                                        movingRect.left = Math.min(x, movingRect.right);
                                        movingRect.right = Math.max(x, movingRect.right);
                                        movingRect.top = Math.min(y, movingRect.bottom);
                                        movingRect.bottom = Math.max(y, movingRect.bottom);
                                        break;
                                    case RT:
                                        movingRect.right = Math.max(x, movingRect.left);
                                        movingRect.left = Math.min(x, movingRect.left);
                                        movingRect.top = Math.min(y, movingRect.bottom);
                                        movingRect.bottom = Math.max(y, movingRect.bottom);
                                        break;
                                    case RB:
                                        movingRect.right = Math.max(x, movingRect.left);
                                        movingRect.left = Math.min(x, movingRect.left);
                                        movingRect.bottom = Math.max(y, movingRect.top);
                                        movingRect.top = Math.min(y, movingRect.top);
                                        break;
                                    case LB:
                                        movingRect.left = Math.min(x, movingRect.right);
                                        movingRect.right = Math.max(x, movingRect.right);
                                        movingRect.bottom = Math.max(y, movingRect.top);
                                        movingRect.top = Math.min(y, movingRect.top);
                                        break;
                                }
                                break;
                        }

                        drawRect(bitmapCanvas, movingRect.left, movingRect.top, movingRect.right, movingRect.bottom);
                        synchronized (regionLock) {
                            if (rectList.size() == RECTANGLE_MAX_COUNT) {
                                rectList.remove(0);
                            }
                            rectList.add(movingRect);
                        }
                    }
                    surfaceViewCanvas.drawBitmap(regionBitmap, new Rect(0, 0, viewWidth, viewHeight), new Rect(0, 0, viewWidth, viewHeight), null);
                    getHolder().unlockCanvasAndPost(surfaceViewCanvas);
                }
                return true;
            }
            default:
                return false;
        }
    }

    @Nullable
    private Rect getRect(int x, int y) {
        for (int i = rectList.size() - 1; i >= 0; i--) {
            Rect rect = rectList.get(i);
            if (x > rect.left - TOUCH_TOLERANCE && x < rect.right + TOUCH_TOLERANCE
                    && y > rect.top - TOUCH_TOLERANCE && y < rect.bottom + TOUCH_TOLERANCE) {
                return rect;
            }
        }
        return null;
    }

    private boolean isIn(int a, int b) {
        return a > b - TOUCH_TOLERANCE && a < b + TOUCH_TOLERANCE;
    }

    private void drawPoint(Canvas canvas, int x, int y) {
        // Draw point
        tempPaint.setColor(Color.GREEN);
        canvas.drawCircle(x, y, 8f, tempPaint);
    }

    private void drawLine(Canvas canvas, int x1, int y1, int x2, int y2, boolean isTrend) {

        int startX = (int) ((int) (x1 / xScale) * xScale);
        int startY = (int) ((int) (y1 / yScale) * yScale);
        int stopX = (int) ((int) (x2 / xScale) * xScale);
        int stopY = (int) ((int) (y2 / yScale) * yScale);
        // Draw line
        tempPaint.setColor(Color.YELLOW);
        tempPaint.setStyle(Paint.Style.STROKE);
        tempPaint.setStrokeWidth(3f);
        canvas.drawLine(startX, startY, stopX, stopY, tempPaint);
        tempPaint.setStyle(Paint.Style.FILL);

        if (isTrend) {
            // Draw trend text
            String text = "Trend";
            int centerX = (startX + stopX) / 2;
            int centerY = (startY + stopY) / 2;
            tempPaint.setColor(Color.WHITE);
            tempPaint.setStyle(Paint.Style.FILL);
            float textWidth = tempPaint.measureText(text);
            int adjustedX = centerX + (int) textWidth > getWidth() ? (int) (getWidth() - textWidth) : centerX;
            canvas.drawText(text, adjustedX, centerY - 15, tempPaint);
        }
    }

    private void drawRect(Canvas canvas, float x1, float y1, float x2, float y2) {
        int left = (int) ((int) (x1 / xScale) * xScale);
        int top = (int) ((int) (y1 / yScale) * yScale);
        int right = (int) ((int) (x2 / xScale) * xScale);
        int bottom = (int) ((int) (y2 / yScale) * yScale);
        // Draw rectangle
        tempPaint.setColor(Color.CYAN);
        tempPaint.setStyle(Paint.Style.STROKE);
        tempPaint.setStrokeWidth(3f);
        canvas.drawRect(left, top, right, bottom, tempPaint);
        tempPaint.setStyle(Paint.Style.FILL);
    }

    private void drawCircle(Canvas canvas, int x, int y, boolean isMax) {
        tempPaint.setColor(isMax ? Color.RED : Color.BLUE);
        canvas.drawCircle(x, y, 10f, tempPaint);
    }

    private void drawDot(Canvas canvas, Point point, boolean isMax) {

        int x = UnifiedScreenUtils.correct(point.x * xScale, getWidth());
        int y = UnifiedScreenUtils.correct(point.y * yScale, getHeight());
        tempPaint.setColor(isMax ? Color.RED : Color.BLUE);
        canvas.drawCircle(x, y, 10f, tempPaint);
    }

    private void drawTempText(Canvas canvas, String text, int x, int y) {
        tempPaint.setColor(Color.WHITE);
        tempPaint.setStyle(Paint.Style.FILL);
        float textWidth = tempPaint.measureText(text);
        int adjustedX = x + (int) textWidth > getWidth() ? (int) (getWidth() - textWidth) : x;
        canvas.drawText(text, adjustedX, y - 15, tempPaint);
    }

    private void drawTempText(Canvas canvas, String text, Point point) {
        int x = UnifiedScreenUtils.correct(point.x * xScale, getWidth());
        int y = UnifiedScreenUtils.correct(point.y * yScale, getHeight());
        tempPaint.setColor(Color.WHITE);
        tempPaint.setStyle(Paint.Style.FILL);
        float textWidth = tempPaint.measureText(text);
        int adjustedX = x + (int) textWidth > getWidth() ? (int) (getWidth() - textWidth) : x;
        canvas.drawText(text, adjustedX, y - 15, tempPaint);
    }

    private void setBitmap() {
        regionBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(regionBitmap);
        for (Point point : pointList) {
            drawPoint(canvas, point.x, point.y);
        }
        for (Line line : lineList) {
            drawLine(canvas, line.start.x, line.start.y, line.end.x, line.end.y, false);
        }
        for (Rect rect : rectList) {
            drawRect(canvas, rect.left, rect.top, rect.right, rect.bottom);
        }
        if (trendLine != null) {
            drawLine(canvas, trendLine.start.x, trendLine.start.y, trendLine.end.x, trendLine.end.y, true);
        }
    }

    public float getCompensateTemp(float temp) {
        if (iLiteListener != null) {
            return iLiteListener.compensateTemp(temp);
        } else {
            return temp;
        }
    }

    public float getTSTemp(float temp) {
        if (iTsTempListenerWeakReference != null && iTsTempListenerWeakReference.get() != null) {
            return iTsTempListenerWeakReference.get().tempCorrectByTs(getCompensateTemp(temp));
        } else {
            return getCompensateTemp(temp);
        }
    }

    public void setUseIRISP(boolean useIRISP) {
        if (irtemp != null) {
            irtemp.setScale(useIRISP ? 16 : 64);
        }
    }

    public void setCurrentFusionType(@NonNull DualCameraParams.FusionType currentFusionType) {
        this.mCurrentFusionType = currentFusionType;
    }

    public void setDualUVCCamera(@NonNull DualUVCCamera dualUVCCamera) {
        this.dualUVCCamera = dualUVCCamera;

    }

    @Override
    public void onFame(byte[] mixData, byte[] tempData, double fpsText) {
        if (Const.TYPE_IR_DUAL == productType) {
            if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) {
                if (this.remapTempData == null) {
                    this.remapTempData = new byte[Const.IR_WIDTH * Const.IR_HEIGHT * 2];
                }
                System.arraycopy(tempData, 0, this.remapTempData, 0, Const.IR_WIDTH * Const.IR_HEIGHT * 2);
            } else {
                if (this.remapTempData == null) {
                    this.remapTempData = new byte[Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2];
                }
                System.arraycopy(tempData, 0, this.remapTempData, 0, Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2);
            }
        }
    }

    // Additional method for compatibility
    public void updateMagnifier() {
        // Trigger a redraw to update magnifier display
        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    private enum LineMoveType {ALL, START, END}

    private enum RectMoveType {ALL, EDGE, CORNER}

    private enum RectMoveEdge {LEFT, TOP, RIGHT, BOTTOM}

    private enum RectMoveCorner {LT, RT, RB, LB}

    @IntDef({REGION_MODE_RESET, REGION_MODE_POINT, REGION_MODE_LINE, REGION_MODE_RECTANGLE, REGION_MODE_CENTER, REGION_NODE_TREND, REGION_MODE_CLEAN})
    @Retention(RetentionPolicy.SOURCE)
    private @interface RegionMode {
    }

    public interface OnTrendChangeListener {
        void onChange(List<Float> temps);
    }

    public interface TempListener {
        void getTemp(float max, float min, byte[] tempData);
    }
}
