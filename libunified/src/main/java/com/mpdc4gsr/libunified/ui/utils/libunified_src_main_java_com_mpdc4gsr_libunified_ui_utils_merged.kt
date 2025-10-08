// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils' directory and its subdirectories.
// Total files: 11 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\ColorTemplate.java =====

package com.mpdc4gsr.libunified.ui.utils;

import android.content.res.Resources;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class ColorTemplate {

    public static final int COLOR_NONE = 0x00112233;

    public static final int COLOR_SKIP = 0x00112234;

    public static final int[] LIBERTY_COLORS = {
            Color.rgb(207, 248, 246), Color.rgb(148, 212, 212), Color.rgb(136, 180, 187),
            Color.rgb(118, 174, 175), Color.rgb(42, 109, 130)
    };
    public static final int[] JOYFUL_COLORS = {
            Color.rgb(217, 80, 138), Color.rgb(254, 149, 7), Color.rgb(254, 247, 120),
            Color.rgb(106, 167, 134), Color.rgb(53, 194, 209)
    };
    public static final int[] PASTEL_COLORS = {
            Color.rgb(64, 89, 128), Color.rgb(149, 165, 124), Color.rgb(217, 184, 162),
            Color.rgb(191, 134, 134), Color.rgb(179, 48, 80)
    };
    public static final int[] COLORFUL_COLORS = {
            Color.rgb(193, 37, 82), Color.rgb(255, 102, 0), Color.rgb(245, 199, 0),
            Color.rgb(106, 150, 31), Color.rgb(179, 100, 53)
    };
    public static final int[] VORDIPLOM_COLORS = {
            Color.rgb(192, 255, 140), Color.rgb(255, 247, 140), Color.rgb(255, 208, 140),
            Color.rgb(140, 234, 255), Color.rgb(255, 140, 157)
    };
    public static final int[] MATERIAL_COLORS = {
            rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db")
    };

    public static int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }

    public static int getHoloBlue() {
        return Color.rgb(51, 181, 229);
    }

    public static int colorWithAlpha(int color, int alpha) {
        return (color & 0xffffff) | ((alpha & 0xff) << 24);
    }

    public static List<Integer> createColors(Resources r, int[] colors) {

        List<Integer> result = new ArrayList<Integer>();

        for (int i : colors) {
            result.add(r.getColor(i));
        }

        return result;
    }

    public static List<Integer> createColors(int[] colors) {

        List<Integer> result = new ArrayList<Integer>();

        for (int i : colors) {
            result.add(i);
        }

        return result;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\EntryXComparator.java =====

package com.mpdc4gsr.libunified.ui.utils;

import com.mpdc4gsr.libunified.ui.data.Entry;

import java.util.Comparator;

public class EntryXComparator implements Comparator<Entry> {
    @Override
    public int compare(Entry entry1, Entry entry2) {
        float diff = entry1.getX() - entry2.getX();

        if (diff == 0f) return 0;
        else {
            if (diff > 0f) return 1;
            else return -1;
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\FSize.java =====

package com.mpdc4gsr.libunified.ui.utils;

import java.util.List;

public final class FSize extends ObjectPool.Poolable {

    private static ObjectPool<FSize> pool;

    static {
        pool = ObjectPool.create(256, new FSize(0, 0));
        pool.setReplenishPercentage(0.5f);
    }

    public float width;
    public float height;

    public FSize() {
    }

    public FSize(final float width, final float height) {
        this.width = width;
        this.height = height;
    }

    public static FSize getInstance(final float width, final float height) {
        FSize result = pool.get();
        result.width = width;
        result.height = height;
        return result;
    }

    public static void recycleInstance(FSize instance) {
        pool.recycle(instance);
    }

    public static void recycleInstances(List<FSize> instances) {
        pool.recycle(instances);
    }

    protected ObjectPool.Poolable instantiate() {
        return new FSize(0, 0);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof FSize) {
            final FSize other = (FSize) obj;
            return width == other.width && height == other.height;
        }
        return false;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(width) ^ Float.floatToIntBits(height);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\HorizontalViewPortHandler.java =====

package com.mpdc4gsr.libunified.ui.utils;

public class HorizontalViewPortHandler extends ViewPortHandler {

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\MPPointD.java =====

package com.mpdc4gsr.libunified.ui.utils;

import java.util.List;

public class MPPointD extends ObjectPool.Poolable {

    private static ObjectPool<MPPointD> pool;

    static {
        pool = ObjectPool.create(64, new MPPointD(0, 0));
        pool.setReplenishPercentage(0.5f);
    }

    public double x;
    public double y;

    private MPPointD(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static MPPointD getInstance(double x, double y) {
        MPPointD result = pool.get();
        result.x = x;
        result.y = y;
        return result;
    }

    public static void recycleInstance(MPPointD instance) {
        pool.recycle(instance);
    }

    public static void recycleInstances(List<MPPointD> instances) {
        pool.recycle(instances);
    }

    protected ObjectPool.Poolable instantiate() {
        return new MPPointD(0, 0);
    }

    public String toString() {
        return "MPPointD, x: " + x + ", y: " + y;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\MPPointF.java =====

package com.mpdc4gsr.libunified.ui.utils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class MPPointF extends ObjectPool.Poolable {

    public static final Parcelable.Creator<MPPointF> CREATOR = new Parcelable.Creator<MPPointF>() {

        public MPPointF createFromParcel(Parcel in) {
            MPPointF r = new MPPointF(0, 0);
            r.my_readFromParcel(in);
            return r;
        }

        public MPPointF[] newArray(int size) {
            return new MPPointF[size];
        }
    };
    private static ObjectPool<MPPointF> pool;

    static {
        pool = ObjectPool.create(32, new MPPointF(0, 0));
        pool.setReplenishPercentage(0.5f);
    }

    public float x;
    public float y;

    public MPPointF() {
    }

    public MPPointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static MPPointF getInstance(float x, float y) {
        MPPointF result = pool.get();
        result.x = x;
        result.y = y;
        return result;
    }

    public static MPPointF getInstance() {
        return pool.get();
    }

    public static MPPointF getInstance(MPPointF copy) {
        MPPointF result = pool.get();
        result.x = copy.x;
        result.y = copy.y;
        return result;
    }

    public static void recycleInstance(MPPointF instance) {
        pool.recycle(instance);
    }

    public static void recycleInstances(List<MPPointF> instances) {
        pool.recycle(instances);
    }

    public void my_readFromParcel(Parcel in) {
        x = in.readFloat();
        y = in.readFloat();
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    @Override
    protected ObjectPool.Poolable instantiate() {
        return new MPPointF(0, 0);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\ObjectPool.java =====

package com.mpdc4gsr.libunified.ui.utils;

import java.util.List;

public class ObjectPool<T extends ObjectPool.Poolable> {

    private static int ids = 0;

    private int poolId;
    private int desiredCapacity;
    private Object[] objects;
    private int objectsPointer;
    private T modelObject;
    private float replenishPercentage;

    private ObjectPool(int withCapacity, T object) {
        if (withCapacity <= 0) {
            throw new IllegalArgumentException("Object Pool must be instantiated with a capacity greater than 0!");
        }
        this.desiredCapacity = withCapacity;
        this.objects = new Object[this.desiredCapacity];
        this.objectsPointer = 0;
        this.modelObject = object;
        this.replenishPercentage = 1.0f;
        this.refillPool();
    }

    public static synchronized ObjectPool create(int withCapacity, Poolable object) {
        ObjectPool result = new ObjectPool(withCapacity, object);
        result.poolId = ids;
        ids++;

        return result;
    }

    public int getPoolId() {
        return poolId;
    }

    public float getReplenishPercentage() {
        return replenishPercentage;
    }

    public void setReplenishPercentage(float percentage) {
        float p = percentage;
        if (p > 1) {
            p = 1;
        } else if (p < 0f) {
            p = 0f;
        }
        this.replenishPercentage = p;
    }

    private void refillPool() {
        this.refillPool(this.replenishPercentage);
    }

    private void refillPool(float percentage) {
        int portionOfCapacity = (int) (desiredCapacity * percentage);

        if (portionOfCapacity < 1) {
            portionOfCapacity = 1;
        } else if (portionOfCapacity > desiredCapacity) {
            portionOfCapacity = desiredCapacity;
        }

        for (int i = 0; i < portionOfCapacity; i++) {
            this.objects[i] = modelObject.instantiate();
        }
        objectsPointer = portionOfCapacity - 1;
    }

    public synchronized T get() {

        if (this.objectsPointer == -1 && this.replenishPercentage > 0.0f) {
            this.refillPool();
        }

        T result = (T) objects[this.objectsPointer];
        result.currentOwnerId = Poolable.NO_OWNER;
        this.objectsPointer--;

        return result;
    }

    public synchronized void recycle(T object) {
        if (object.currentOwnerId != Poolable.NO_OWNER) {
            if (object.currentOwnerId == this.poolId) {
                throw new IllegalArgumentException("The object passed is already stored in this pool!");
            } else {
                throw new IllegalArgumentException("The object to recycle already belongs to poolId " + object.currentOwnerId + ".  Object cannot belong to two different pool instances simultaneously!");
            }
        }

        this.objectsPointer++;
        if (this.objectsPointer >= objects.length) {
            this.resizePool();
        }

        object.currentOwnerId = this.poolId;
        objects[this.objectsPointer] = object;

    }

    public synchronized void recycle(List<T> objects) {
        while (objects.size() + this.objectsPointer + 1 > this.desiredCapacity) {
            this.resizePool();
        }
        final int objectsListSize = objects.size();

        for (int i = 0; i < objectsListSize; i++) {
            T object = objects.get(i);
            if (object.currentOwnerId != Poolable.NO_OWNER) {
                if (object.currentOwnerId == this.poolId) {
                    throw new IllegalArgumentException("The object passed is already stored in this pool!");
                } else {
                    throw new IllegalArgumentException("The object to recycle already belongs to poolId " + object.currentOwnerId + ".  Object cannot belong to two different pool instances simultaneously!");
                }
            }
            object.currentOwnerId = this.poolId;
            this.objects[this.objectsPointer + 1 + i] = object;
        }
        this.objectsPointer += objectsListSize;
    }

    private void resizePool() {
        final int oldCapacity = this.desiredCapacity;
        this.desiredCapacity *= 2;
        Object[] temp = new Object[this.desiredCapacity];
        for (int i = 0; i < oldCapacity; i++) {
            temp[i] = this.objects[i];
        }
        this.objects = temp;
    }

    public int getPoolCapacity() {
        return this.objects.length;
    }

    public int getPoolCount() {
        return this.objectsPointer + 1;
    }

    public static abstract class Poolable {

        public static int NO_OWNER = -1;
        int currentOwnerId = NO_OWNER;

        protected abstract Poolable instantiate();

    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\Transformer.java =====

package com.mpdc4gsr.libunified.ui.utils;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;

import com.elvishew.xlog.XLog;
import com.mpdc4gsr.libunified.ui.data.CandleEntry;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBubbleDataSet;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.ICandleDataSet;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineDataSet;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;

import java.util.List;

public class Transformer {

    protected Matrix mMatrixValueToPx = new Matrix();

    protected Matrix mMatrixOffset = new Matrix();

    protected ViewPortHandler mViewPortHandler;
    protected float[] valuePointsForGenerateTransformedValuesScatter = new float[1];
    protected float[] valuePointsForGenerateTransformedValuesBubble = new float[1];
    protected float[] valuePointsForGenerateTransformedValuesLine = new float[1];
    protected float[] valuePointsForGenerateTransformedValuesCandle = new float[1];
    protected Matrix mPixelToValueMatrixBuffer = new Matrix();
    float[] ptsBuffer = new float[2];
    private Matrix mMBuffer1 = new Matrix();
    private Matrix mMBuffer2 = new Matrix();

    public Transformer(ViewPortHandler viewPortHandler) {
        this.mViewPortHandler = viewPortHandler;
    }

    public void prepareMatrixValuePx(float xChartMin, float deltaX, float deltaY, float yChartMin) {

        float scaleX = (float) ((mViewPortHandler.contentWidth()) / deltaX);
        float scaleY = (float) ((mViewPortHandler.contentHeight()) / deltaY);

        if (Float.isInfinite(scaleX)) {
            scaleX = 0;
        }
        if (Float.isInfinite(scaleY)) {
            scaleY = 0;
        }

        mMatrixValueToPx.reset();
        mMatrixValueToPx.postTranslate(-xChartMin, -yChartMin);
        mMatrixValueToPx.postScale(scaleX, -scaleY);
    }

    public void prepareMatrixOffset(boolean inverted) {

        mMatrixOffset.reset();

        if (!inverted)
            mMatrixOffset.postTranslate(mViewPortHandler.offsetLeft(),
                    mViewPortHandler.getChartHeight() - mViewPortHandler.offsetBottom());
        else {
            mMatrixOffset
                    .setTranslate(mViewPortHandler.offsetLeft(), -mViewPortHandler.offsetTop());
            mMatrixOffset.postScale(1.0f, -1.0f);
        }
    }

    public float[] generateTransformedValuesScatter(IScatterDataSet data, float phaseX,
                                                    float phaseY, int from, int to) {

        final int count = (int) ((to - from) * phaseX + 1) * 2;

        if (valuePointsForGenerateTransformedValuesScatter.length != count) {
            valuePointsForGenerateTransformedValuesScatter = new float[count];
        }
        float[] valuePoints = valuePointsForGenerateTransformedValuesScatter;

        for (int j = 0; j < count; j += 2) {

            Entry e = data.getEntryForIndex(j / 2 + from);

            if (e != null) {
                valuePoints[j] = e.getX();
                valuePoints[j + 1] = e.getY() * phaseY;
            } else {
                valuePoints[j] = 0;
                valuePoints[j + 1] = 0;
            }
        }

        getValueToPixelMatrix().mapPoints(valuePoints);

        return valuePoints;
    }

    public float[] generateTransformedValuesBubble(IBubbleDataSet data, float phaseY, int from, int to) {

        final int count = (to - from + 1) * 2;

        if (valuePointsForGenerateTransformedValuesBubble.length != count) {
            valuePointsForGenerateTransformedValuesBubble = new float[count];
        }
        float[] valuePoints = valuePointsForGenerateTransformedValuesBubble;

        for (int j = 0; j < count; j += 2) {

            Entry e = data.getEntryForIndex(j / 2 + from);

            if (e != null) {
                valuePoints[j] = e.getX();
                valuePoints[j + 1] = e.getY() * phaseY;
            } else {
                valuePoints[j] = 0;
                valuePoints[j + 1] = 0;
            }
        }

        getValueToPixelMatrix().mapPoints(valuePoints);

        return valuePoints;
    }

    public float[] generateTransformedValuesLine(ILineDataSet data,
                                                 float phaseX, float phaseY,
                                                 int min, int max) {

        if (max < min) {
            XLog.w("generateTransformedValuesLine error: max:" + max + ", min:" + min + ", phaseX:" + phaseX);
            return new float[0];
        }
        final int count = ((int) ((max - min) * phaseX) + 1) * 2;

        if (valuePointsForGenerateTransformedValuesLine.length != count) {
            valuePointsForGenerateTransformedValuesLine = new float[count];
        }
        float[] valuePoints = valuePointsForGenerateTransformedValuesLine;

        for (int j = 0; j < count; j += 2) {

            Entry e = data.getEntryForIndex(j / 2 + min);

            if (e != null) {
                valuePoints[j] = e.getX();
                valuePoints[j + 1] = e.getY() * phaseY;
            } else {
                valuePoints[j] = 0;
                valuePoints[j + 1] = 0;
            }
        }

        getValueToPixelMatrix().mapPoints(valuePoints);

        return valuePoints;
    }

    public float[] generateTransformedValuesCandle(ICandleDataSet data,
                                                   float phaseX, float phaseY, int from, int to) {

        final int count = (int) ((to - from) * phaseX + 1) * 2;

        if (valuePointsForGenerateTransformedValuesCandle.length != count) {
            valuePointsForGenerateTransformedValuesCandle = new float[count];
        }
        float[] valuePoints = valuePointsForGenerateTransformedValuesCandle;

        for (int j = 0; j < count; j += 2) {

            CandleEntry e = data.getEntryForIndex(j / 2 + from);

            if (e != null) {
                valuePoints[j] = e.getX();
                valuePoints[j + 1] = e.getHigh() * phaseY;
            } else {
                valuePoints[j] = 0;
                valuePoints[j + 1] = 0;
            }
        }

        getValueToPixelMatrix().mapPoints(valuePoints);

        return valuePoints;
    }

    public void pathValueToPixel(Path path) {

        path.transform(mMatrixValueToPx);
        path.transform(mViewPortHandler.getMatrixTouch());
        path.transform(mMatrixOffset);
    }

    public void pathValuesToPixel(List<Path> paths) {

        for (int i = 0; i < paths.size(); i++) {
            pathValueToPixel(paths.get(i));
        }
    }

    public void pointValuesToPixel(float[] pts) {

        mMatrixValueToPx.mapPoints(pts);
        mViewPortHandler.getMatrixTouch().mapPoints(pts);
        mMatrixOffset.mapPoints(pts);
    }

    public void rectValueToPixel(RectF r) {

        mMatrixValueToPx.mapRect(r);
        mViewPortHandler.getMatrixTouch().mapRect(r);
        mMatrixOffset.mapRect(r);
    }

    public void rectToPixelPhase(RectF r, float phaseY) {

        r.top *= phaseY;
        r.bottom *= phaseY;

        mMatrixValueToPx.mapRect(r);
        mViewPortHandler.getMatrixTouch().mapRect(r);
        mMatrixOffset.mapRect(r);
    }

    public void rectToPixelPhaseHorizontal(RectF r, float phaseY) {

        r.left *= phaseY;
        r.right *= phaseY;

        mMatrixValueToPx.mapRect(r);
        mViewPortHandler.getMatrixTouch().mapRect(r);
        mMatrixOffset.mapRect(r);
    }

    public void rectValueToPixelHorizontal(RectF r) {

        mMatrixValueToPx.mapRect(r);
        mViewPortHandler.getMatrixTouch().mapRect(r);
        mMatrixOffset.mapRect(r);
    }

    public void rectValueToPixelHorizontal(RectF r, float phaseY) {

        r.left *= phaseY;
        r.right *= phaseY;

        mMatrixValueToPx.mapRect(r);
        mViewPortHandler.getMatrixTouch().mapRect(r);
        mMatrixOffset.mapRect(r);
    }

    public void rectValuesToPixel(List<RectF> rects) {

        Matrix m = getValueToPixelMatrix();

        for (int i = 0; i < rects.size(); i++)
            m.mapRect(rects.get(i));
    }

    public void pixelsToValue(float[] pixels) {

        Matrix tmp = mPixelToValueMatrixBuffer;
        tmp.reset();

        mMatrixOffset.invert(tmp);
        tmp.mapPoints(pixels);

        mViewPortHandler.getMatrixTouch().invert(tmp);
        tmp.mapPoints(pixels);

        mMatrixValueToPx.invert(tmp);
        tmp.mapPoints(pixels);
    }

    public MPPointD getValuesByTouchPoint(float x, float y) {

        MPPointD result = MPPointD.getInstance(0, 0);
        getValuesByTouchPoint(x, y, result);
        return result;
    }

    public void getValuesByTouchPoint(float x, float y, MPPointD outputPoint) {

        ptsBuffer[0] = x;
        ptsBuffer[1] = y;

        pixelsToValue(ptsBuffer);

        outputPoint.x = ptsBuffer[0];
        outputPoint.y = ptsBuffer[1];
    }

    public MPPointD getPixelForValues(float x, float y) {

        ptsBuffer[0] = x;
        ptsBuffer[1] = y;

        pointValuesToPixel(ptsBuffer);

        double xPx = ptsBuffer[0];
        double yPx = ptsBuffer[1];

        return MPPointD.getInstance(xPx, yPx);
    }

    public Matrix getValueMatrix() {
        return mMatrixValueToPx;
    }

    public Matrix getOffsetMatrix() {
        return mMatrixOffset;
    }

    public Matrix getValueToPixelMatrix() {
        mMBuffer1.set(mMatrixValueToPx);
        mMBuffer1.postConcat(mViewPortHandler.mMatrixTouch);
        mMBuffer1.postConcat(mMatrixOffset);
        return mMBuffer1;
    }

    public Matrix getPixelToValueMatrix() {
        getValueToPixelMatrix().invert(mMBuffer2);
        return mMBuffer2;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\TransformerHorizontalBarChart.java =====

package com.mpdc4gsr.libunified.ui.utils;

public class TransformerHorizontalBarChart extends Transformer {

    public TransformerHorizontalBarChart(ViewPortHandler viewPortHandler) {
        super(viewPortHandler);
    }

    public void prepareMatrixOffset(boolean inverted) {

        mMatrixOffset.reset();

        if (!inverted)
            mMatrixOffset.postTranslate(mViewPortHandler.offsetLeft(),
                    mViewPortHandler.getChartHeight() - mViewPortHandler.offsetBottom());
        else {
            mMatrixOffset
                    .setTranslate(
                            -(mViewPortHandler.getChartWidth() - mViewPortHandler.offsetRight()),
                            mViewPortHandler.getChartHeight() - mViewPortHandler.offsetBottom());
            mMatrixOffset.postScale(-1.0f, 1.0f);
        }

    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\Utils.java =====

package com.mpdc4gsr.libunified.ui.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import com.mpdc4gsr.libunified.ui.formatter.DefaultValueFormatter;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;

import java.util.List;

public abstract class Utils {

    public final static double DEG2RAD = (Math.PI / 180.0);
    public final static float FDEG2RAD = ((float) Math.PI / 180.f);
    @SuppressWarnings("unused")
    public final static double DOUBLE_EPSILON = Double.longBitsToDouble(1);
    @SuppressWarnings("unused")
    public final static float FLOAT_EPSILON = Float.intBitsToFloat(1);
    private static final int POW_10[] = {
            1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    };
    private static DisplayMetrics mMetrics;
    private static int mMinimumFlingVelocity = 50;
    private static int mMaximumFlingVelocity = 8000;
    private static Rect mCalcTextHeightRect = new Rect();
    private static Paint.FontMetrics mFontMetrics = new Paint.FontMetrics();
    private static Rect mCalcTextSizeRect = new Rect();
    private static ValueFormatter mDefaultValueFormatter = generateDefaultValueFormatter();
    private static Rect mDrawableBoundsCache = new Rect();
    private static Rect mDrawTextRectBuffer = new Rect();
    private static Paint.FontMetrics mFontMetricsBuffer = new Paint.FontMetrics();

    @SuppressWarnings("deprecation")
    public static void init(Context context) {

        if (context == null) {

            mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();

            mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();

            Log.e("MPChartLib-Utils"
                    , "Utils.init(...) PROVIDED CONTEXT OBJECT IS NULL");

        } else {
            ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
            mMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
            mMaximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();

            Resources res = context.getResources();
            mMetrics = res.getDisplayMetrics();
        }
    }

    @Deprecated
    public static void init(Resources res) {

        mMetrics = res.getDisplayMetrics();

        mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();

        mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
    }

    public static float convertDpToPixel(float dp) {

        if (mMetrics == null) {

            Log.e("MPChartLib-Utils",
                    "Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before" +
                            " calling Utils.convertDpToPixel(...). Otherwise conversion does not " +
                            "take place.");
            return dp;
        }

        return dp * mMetrics.density;
    }

    public static float convertPixelsToDp(float px) {

        if (mMetrics == null) {

            Log.e("MPChartLib-Utils",
                    "Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before" +
                            " calling Utils.convertPixelsToDp(...). Otherwise conversion does not" +
                            " take place.");
            return px;
        }

        return px / mMetrics.density;
    }

    public static int calcTextWidth(Paint paint, String demoText) {
        return (int) paint.measureText(demoText);
    }

    public static int calcTextHeight(Paint paint, String demoText) {

        Rect r = mCalcTextHeightRect;
        r.set(0, 0, 0, 0);
        paint.getTextBounds(demoText, 0, demoText.length(), r);
        return r.height();
    }

    public static float getLineHeight(Paint paint) {
        return getLineHeight(paint, mFontMetrics);
    }

    public static float getLineHeight(Paint paint, Paint.FontMetrics fontMetrics) {
        paint.getFontMetrics(fontMetrics);
        return fontMetrics.descent - fontMetrics.ascent;
    }

    public static float getLineSpacing(Paint paint) {
        return getLineSpacing(paint, mFontMetrics);
    }

    public static float getLineSpacing(Paint paint, Paint.FontMetrics fontMetrics) {
        paint.getFontMetrics(fontMetrics);
        return fontMetrics.ascent - fontMetrics.top + fontMetrics.bottom;
    }

    public static FSize calcTextSize(Paint paint, String demoText) {

        FSize result = FSize.getInstance(0, 0);
        calcTextSize(paint, demoText, result);
        return result;
    }

    public static void calcTextSize(Paint paint, String demoText, FSize outputFSize) {

        Rect r = mCalcTextSizeRect;
        r.set(0, 0, 0, 0);
        paint.getTextBounds(demoText, 0, demoText.length(), r);
        outputFSize.width = r.width();
        outputFSize.height = r.height();

    }

    private static ValueFormatter generateDefaultValueFormatter() {
        return new DefaultValueFormatter(1);
    }

    public static ValueFormatter getDefaultValueFormatter() {
        return mDefaultValueFormatter;
    }

    public static String formatNumber(float number, int digitCount, boolean separateThousands) {
        return formatNumber(number, digitCount, separateThousands, '.');
    }

    public static String formatNumber(float number, int digitCount, boolean separateThousands,
                                      char separateChar) {

        char[] out = new char[35];

        boolean neg = false;
        if (number == 0) {
            return "0";
        }

        boolean zero = false;
        if (number < 1 && number > -1) {
            zero = true;
        }

        if (number < 0) {
            neg = true;
            number = -number;
        }

        if (digitCount > POW_10.length) {
            digitCount = POW_10.length - 1;
        }

        number *= POW_10[digitCount];
        long lval = Math.round(number);
        int ind = out.length - 1;
        int charCount = 0;
        boolean decimalPointAdded = false;

        while (lval != 0 || charCount < (digitCount + 1)) {
            int digit = (int) (lval % 10);
            lval = lval / 10;
            out[ind--] = (char) (digit + '0');
            charCount++;

            if (charCount == digitCount) {
                out[ind--] = ',';
                charCount++;
                decimalPointAdded = true;

            } else if (separateThousands && lval != 0 && charCount > digitCount) {

                if (decimalPointAdded) {

                    if ((charCount - digitCount) % 4 == 0) {
                        out[ind--] = separateChar;
                        charCount++;
                    }

                } else {

                    if ((charCount - digitCount) % 4 == 3) {
                        out[ind--] = separateChar;
                        charCount++;
                    }
                }
            }
        }

        if (zero) {
            out[ind--] = '0';
            charCount += 1;
        }

        if (neg) {
            out[ind--] = '-';
            charCount += 1;
        }

        int start = out.length - charCount;

        return String.valueOf(out, start, out.length - start);
    }

    public static float roundToNextSignificant(double number) {
        if (Double.isInfinite(number) ||
                Double.isNaN(number) ||
                number == 0.0)
            return 0;

        final float d = (float) Math.ceil((float) Math.log10(number < 0 ? -number : number));
        final int pw = 1 - (int) d;
        final float magnitude = (float) Math.pow(10, pw);
        final long shifted = Math.round(number * magnitude);
        return shifted / magnitude;
    }

    public static int getDecimals(float number) {

        float i = roundToNextSignificant(number);

        if (Float.isInfinite(i))
            return 0;

        return (int) Math.ceil(-Math.log10(i)) + 2;
    }

    public static int[] convertIntegers(List<Integer> integers) {

        int[] ret = new int[integers.size()];

        copyIntegers(integers, ret);

        return ret;
    }

    public static void copyIntegers(List<Integer> from, int[] to) {
        int count = to.length < from.size() ? to.length : from.size();
        for (int i = 0; i < count; i++) {
            to[i] = from.get(i);
        }
    }

    public static String[] convertStrings(List<String> strings) {

        String[] ret = new String[strings.size()];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = strings.get(i);
        }

        return ret;
    }

    public static void copyStrings(List<String> from, String[] to) {
        int count = to.length < from.size() ? to.length : from.size();
        for (int i = 0; i < count; i++) {
            to[i] = from.get(i);
        }
    }

    public static double nextUp(double d) {
        if (d == Double.POSITIVE_INFINITY)
            return d;
        else {
            d += 0.0d;
            return Double.longBitsToDouble(Double.doubleToRawLongBits(d) +
                    ((d >= 0.0d) ? +1L : -1L));
        }
    }

    public static MPPointF getPosition(MPPointF center, float dist, float angle) {

        MPPointF p = MPPointF.getInstance(0, 0);
        getPosition(center, dist, angle, p);
        return p;
    }

    public static void getPosition(MPPointF center, float dist, float angle, MPPointF outputPoint) {
        outputPoint.x = (float) (center.x + dist * Math.cos(Math.toRadians(angle)));
        outputPoint.y = (float) (center.y + dist * Math.sin(Math.toRadians(angle)));
    }

    public static void velocityTrackerPointerUpCleanUpIfNecessary(MotionEvent ev,
                                                                  VelocityTracker tracker) {

        tracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
        final int upIndex = ev.getActionIndex();
        final int id1 = ev.getPointerId(upIndex);
        final float x1 = tracker.getXVelocity(id1);
        final float y1 = tracker.getYVelocity(id1);
        for (int i = 0, count = ev.getPointerCount(); i < count; i++) {
            if (i == upIndex)
                continue;

            final int id2 = ev.getPointerId(i);
            final float x = x1 * tracker.getXVelocity(id2);
            final float y = y1 * tracker.getYVelocity(id2);

            final float dot = x + y;
            if (dot < 0) {
                tracker.clear();
                break;
            }
        }
    }

    @SuppressLint("NewApi")
    public static void postInvalidateOnAnimation(View view) {
        if (Build.VERSION.SDK_INT >= 16)
            view.postInvalidateOnAnimation();
        else
            view.postInvalidateDelayed(10);
    }

    public static int getMinimumFlingVelocity() {
        return mMinimumFlingVelocity;
    }

    public static int getMaximumFlingVelocity() {
        return mMaximumFlingVelocity;
    }

    public static float getNormalizedAngle(float angle) {
        while (angle < 0.f)
            angle += 360.f;

        return angle % 360.f;
    }

    public static void drawImage(Canvas canvas,
                                 Drawable drawable,
                                 int x, int y,
                                 int width, int height) {

        MPPointF drawOffset = MPPointF.getInstance();
        drawOffset.x = x - (width / 2);
        drawOffset.y = y - (height / 2);

        drawable.copyBounds(mDrawableBoundsCache);
        drawable.setBounds(
                mDrawableBoundsCache.left,
                mDrawableBoundsCache.top,
                mDrawableBoundsCache.left + width,
                mDrawableBoundsCache.top + width);

        int saveId = canvas.save();

        canvas.translate(drawOffset.x, drawOffset.y);
        drawable.draw(canvas);
        canvas.restoreToCount(saveId);
    }

    public static void drawXAxisValue(Canvas c, String text, float x, float y,
                                      Paint paint,
                                      MPPointF anchor, float angleDegrees) {

        float drawOffsetX = 0.f;
        float drawOffsetY = 0.f;

        final float lineHeight = paint.getFontMetrics(mFontMetricsBuffer);
        paint.getTextBounds(text, 0, text.length(), mDrawTextRectBuffer);

        drawOffsetX -= mDrawTextRectBuffer.left;

        drawOffsetY += -mFontMetricsBuffer.ascent;

        Paint.Align originalTextAlign = paint.getTextAlign();
        paint.setTextAlign(Paint.Align.LEFT);

        if (angleDegrees != 0.f) {

            drawOffsetX -= mDrawTextRectBuffer.width() * 0.5f;
            drawOffsetY -= lineHeight * 0.5f;

            float translateX = x;
            float translateY = y;

            if (anchor.x != 0.5f || anchor.y != 0.5f) {
                final FSize rotatedSize = getSizeOfRotatedRectangleByDegrees(
                        mDrawTextRectBuffer.width(),
                        lineHeight,
                        angleDegrees);

                translateX -= rotatedSize.width * (anchor.x - 0.5f);
                translateY -= rotatedSize.height * (anchor.y - 0.5f);
                FSize.recycleInstance(rotatedSize);
            }

            c.save();
            c.translate(translateX, translateY);
            c.rotate(angleDegrees);

            c.drawText(text, drawOffsetX, drawOffsetY, paint);

            c.restore();
        } else {
            if (anchor.x != 0.f || anchor.y != 0.f) {

                drawOffsetX -= mDrawTextRectBuffer.width() * anchor.x;
                drawOffsetY -= lineHeight * anchor.y;
            }

            drawOffsetX += x;
            drawOffsetY += y;

            c.drawText(text, drawOffsetX, drawOffsetY, paint);
        }

        paint.setTextAlign(originalTextAlign);
    }

    public static void drawMultilineText(Canvas c, StaticLayout textLayout,
                                         float x, float y,
                                         TextPaint paint,
                                         MPPointF anchor, float angleDegrees) {

        float drawOffsetX = 0.f;
        float drawOffsetY = 0.f;
        float drawWidth;
        float drawHeight;

        final float lineHeight = paint.getFontMetrics(mFontMetricsBuffer);

        drawWidth = textLayout.getWidth();
        drawHeight = textLayout.getLineCount() * lineHeight;

        drawOffsetX -= mDrawTextRectBuffer.left;

        drawOffsetY += drawHeight;

        Paint.Align originalTextAlign = paint.getTextAlign();
        paint.setTextAlign(Paint.Align.LEFT);

        if (angleDegrees != 0.f) {

            drawOffsetX -= drawWidth * 0.5f;
            drawOffsetY -= drawHeight * 0.5f;

            float translateX = x;
            float translateY = y;

            if (anchor.x != 0.5f || anchor.y != 0.5f) {
                final FSize rotatedSize = getSizeOfRotatedRectangleByDegrees(
                        drawWidth,
                        drawHeight,
                        angleDegrees);

                translateX -= rotatedSize.width * (anchor.x - 0.5f);
                translateY -= rotatedSize.height * (anchor.y - 0.5f);
                FSize.recycleInstance(rotatedSize);
            }

            c.save();
            c.translate(translateX, translateY);
            c.rotate(angleDegrees);

            c.translate(drawOffsetX, drawOffsetY);
            textLayout.draw(c);

            c.restore();
        } else {
            if (anchor.x != 0.f || anchor.y != 0.f) {

                drawOffsetX -= drawWidth * anchor.x;
                drawOffsetY -= drawHeight * anchor.y;
            }

            drawOffsetX += x;
            drawOffsetY += y;

            c.save();

            c.translate(drawOffsetX, drawOffsetY);
            textLayout.draw(c);

            c.restore();
        }

        paint.setTextAlign(originalTextAlign);
    }

    public static void drawMultilineText(Canvas c, String text,
                                         float x, float y,
                                         TextPaint paint,
                                         FSize constrainedToSize,
                                         MPPointF anchor, float angleDegrees) {

        StaticLayout textLayout = new StaticLayout(
                text, 0, text.length(),
                paint,
                (int) Math.max(Math.ceil(constrainedToSize.width), 1.f),
                Layout.Alignment.ALIGN_NORMAL, 1.f, 0.f, false);

        drawMultilineText(c, textLayout, x, y, paint, anchor, angleDegrees);
    }

    public static FSize getSizeOfRotatedRectangleByDegrees(FSize rectangleSize, float degrees) {
        final float radians = degrees * FDEG2RAD;
        return getSizeOfRotatedRectangleByRadians(rectangleSize.width, rectangleSize.height,
                radians);
    }

    public static FSize getSizeOfRotatedRectangleByRadians(FSize rectangleSize, float radians) {
        return getSizeOfRotatedRectangleByRadians(rectangleSize.width, rectangleSize.height,
                radians);
    }

    public static FSize getSizeOfRotatedRectangleByDegrees(float rectangleWidth, float
            rectangleHeight, float degrees) {
        final float radians = degrees * FDEG2RAD;
        return getSizeOfRotatedRectangleByRadians(rectangleWidth, rectangleHeight, radians);
    }

    public static FSize getSizeOfRotatedRectangleByRadians(float rectangleWidth, float
            rectangleHeight, float radians) {
        return FSize.getInstance(
                Math.abs(rectangleWidth * (float) Math.cos(radians)) + Math.abs(rectangleHeight *
                        (float) Math.sin(radians)),
                Math.abs(rectangleWidth * (float) Math.sin(radians)) + Math.abs(rectangleHeight *
                        (float) Math.cos(radians))
        );
    }

    public static int getSDKInt() {
        return Build.VERSION.SDK_INT;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\utils\ViewPortHandler.java =====

package com.mpdc4gsr.libunified.ui.utils;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;

public class ViewPortHandler {

    protected final Matrix mMatrixTouch = new Matrix();
    protected final float[] matrixBuffer = new float[9];
    protected RectF mContentRect = new RectF();
    protected float mChartWidth = 0f;
    protected float mChartHeight = 0f;
    protected float[] valsBufferForFitScreen = new float[9];
    protected Matrix mCenterViewPortMatrixBuffer = new Matrix();
    private float mMinScaleY = 1f;
    private float mMaxScaleY = Float.MAX_VALUE;
    private float mMinScaleX = 1f;
    private float mMaxScaleX = Float.MAX_VALUE;
    private float mScaleX = 1f;
    private float mScaleY = 1f;
    private float mTransX = 0f;
    private float mTransY = 0f;
    private float mTransOffsetX = 0f;
    private float mTransOffsetY = 0f;

    public ViewPortHandler() {

    }

    public void setChartDimens(float width, float height) {

        float offsetLeft = this.offsetLeft();
        float offsetTop = this.offsetTop();
        float offsetRight = this.offsetRight();
        float offsetBottom = this.offsetBottom();

        mChartHeight = height;
        mChartWidth = width;

        restrainViewPort(offsetLeft, offsetTop, offsetRight, offsetBottom);
    }

    public boolean hasChartDimens() {
        if (mChartHeight > 0 && mChartWidth > 0)
            return true;
        else
            return false;
    }

    public void restrainViewPort(float offsetLeft, float offsetTop, float offsetRight,
                                 float offsetBottom) {
        mContentRect.set(offsetLeft, offsetTop, mChartWidth - offsetRight, mChartHeight
                - offsetBottom);
    }

    public float offsetLeft() {
        return mContentRect.left;
    }

    public float offsetRight() {
        return mChartWidth - mContentRect.right;
    }

    public float offsetTop() {
        return mContentRect.top;
    }

    public float offsetBottom() {
        return mChartHeight - mContentRect.bottom;
    }

    public float contentTop() {
        return mContentRect.top;
    }

    public float contentLeft() {
        return mContentRect.left;
    }

    public float contentRight() {
        return mContentRect.right;
    }

    public float contentBottom() {
        return mContentRect.bottom;
    }

    public float contentWidth() {
        return mContentRect.width();
    }

    public float contentHeight() {
        return mContentRect.height();
    }

    public RectF getContentRect() {
        return mContentRect;
    }

    public MPPointF getContentCenter() {
        return MPPointF.getInstance(mContentRect.centerX(), mContentRect.centerY());
    }

    public float getChartHeight() {
        return mChartHeight;
    }

    public float getChartWidth() {
        return mChartWidth;
    }

    public float getSmallestContentExtension() {
        return Math.min(mContentRect.width(), mContentRect.height());
    }

    public Matrix zoomIn(float x, float y) {

        Matrix save = new Matrix();
        zoomIn(x, y, save);
        return save;
    }

    public void zoomIn(float x, float y, Matrix outputMatrix) {
        outputMatrix.reset();
        outputMatrix.set(mMatrixTouch);
        outputMatrix.postScale(1.4f, 1.4f, x, y);
    }

    public Matrix zoomOut(float x, float y) {

        Matrix save = new Matrix();
        zoomOut(x, y, save);
        return save;
    }

    public void zoomOut(float x, float y, Matrix outputMatrix) {
        outputMatrix.reset();
        outputMatrix.set(mMatrixTouch);
        outputMatrix.postScale(0.7f, 0.7f, x, y);
    }

    public void resetZoom(Matrix outputMatrix) {
        outputMatrix.reset();
        outputMatrix.set(mMatrixTouch);
        outputMatrix.postScale(1.0f, 1.0f, 0.0f, 0.0f);
    }

    public Matrix zoom(float scaleX, float scaleY) {

        Matrix save = new Matrix();
        zoom(scaleX, scaleY, save);
        return save;
    }

    public void zoom(float scaleX, float scaleY, Matrix outputMatrix) {
        outputMatrix.reset();
        outputMatrix.set(mMatrixTouch);
        outputMatrix.postScale(scaleX, scaleY);
    }

    public Matrix zoom(float scaleX, float scaleY, float x, float y) {

        Matrix save = new Matrix();
        zoom(scaleX, scaleY, x, y, save);
        return save;
    }

    public void zoom(float scaleX, float scaleY, float x, float y, Matrix outputMatrix) {
        outputMatrix.reset();
        outputMatrix.set(mMatrixTouch);
        outputMatrix.postScale(scaleX, scaleY, x, y);
    }

    public Matrix setZoom(float scaleX, float scaleY) {

        Matrix save = new Matrix();
        setZoom(scaleX, scaleY, save);
        return save;
    }

    public void setZoom(float scaleX, float scaleY, Matrix outputMatrix) {
        outputMatrix.reset();
        outputMatrix.set(mMatrixTouch);
        outputMatrix.setScale(scaleX, scaleY);
    }

    public Matrix setZoom(float scaleX, float scaleY, float x, float y) {

        Matrix save = new Matrix();
        save.set(mMatrixTouch);

        save.setScale(scaleX, scaleY, x, y);

        return save;
    }

    public Matrix fitScreen() {

        Matrix save = new Matrix();
        fitScreen(save);
        return save;
    }

    public void fitScreen(Matrix outputMatrix) {
        mMinScaleX = 1f;
        mMinScaleY = 1f;

        outputMatrix.set(mMatrixTouch);

        float[] vals = valsBufferForFitScreen;
        for (int i = 0; i < 9; i++) {
            vals[i] = 0;
        }

        outputMatrix.getValues(vals);

        vals[Matrix.MTRANS_X] = 0f;
        vals[Matrix.MTRANS_Y] = 0f;
        vals[Matrix.MSCALE_X] = 1f;
        vals[Matrix.MSCALE_Y] = 1f;

        outputMatrix.setValues(vals);
    }

    public Matrix translate(final float[] transformedPts) {

        Matrix save = new Matrix();
        translate(transformedPts, save);
        return save;
    }

    public void translate(final float[] transformedPts, Matrix outputMatrix) {
        outputMatrix.reset();
        outputMatrix.set(mMatrixTouch);
        final float x = transformedPts[0] - offsetLeft();
        final float y = transformedPts[1] - offsetTop();
        outputMatrix.postTranslate(-x, -y);
    }

    public void centerViewPort(final float[] transformedPts, final View view) {

        Matrix save = mCenterViewPortMatrixBuffer;
        save.reset();
        save.set(mMatrixTouch);

        final float x = transformedPts[0] - offsetLeft();
        final float y = transformedPts[1] - offsetTop();

        save.postTranslate(-x, -y);

        refresh(save, view, true);
    }

    public Matrix refresh(Matrix newMatrix, View chart, boolean invalidate) {

        mMatrixTouch.set(newMatrix);

        limitTransAndScale(mMatrixTouch, mContentRect);

        if (invalidate)
            chart.invalidate();

        newMatrix.set(mMatrixTouch);
        return newMatrix;
    }

    public void limitTransAndScale(Matrix matrix, RectF content) {

        matrix.getValues(matrixBuffer);

        float curTransX = matrixBuffer[Matrix.MTRANS_X];
        float curScaleX = matrixBuffer[Matrix.MSCALE_X];

        float curTransY = matrixBuffer[Matrix.MTRANS_Y];
        float curScaleY = matrixBuffer[Matrix.MSCALE_Y];

        mScaleX = Math.min(Math.max(mMinScaleX, curScaleX), mMaxScaleX);

        mScaleY = Math.min(Math.max(mMinScaleY, curScaleY), mMaxScaleY);

        float width = 0f;
        float height = 0f;

        if (content != null) {
            width = content.width();
            height = content.height();
        }

        float maxTransX = -width * (mScaleX - 1f);
        mTransX = Math.min(Math.max(curTransX, maxTransX - mTransOffsetX), mTransOffsetX);

        float maxTransY = height * (mScaleY - 1f);
        mTransY = Math.max(Math.min(curTransY, maxTransY + mTransOffsetY), -mTransOffsetY);

        matrixBuffer[Matrix.MTRANS_X] = mTransX;
        matrixBuffer[Matrix.MSCALE_X] = mScaleX;

        matrixBuffer[Matrix.MTRANS_Y] = mTransY;
        matrixBuffer[Matrix.MSCALE_Y] = mScaleY;

        matrix.setValues(matrixBuffer);
    }

    public void setMinimumScaleX(float xScale) {

        if (xScale < 1f)
            xScale = 1f;

        mMinScaleX = xScale;

        limitTransAndScale(mMatrixTouch, mContentRect);
    }

    public void setMaximumScaleX(float xScale) {

        if (xScale == 0.f)
            xScale = Float.MAX_VALUE;

        mMaxScaleX = xScale;

        limitTransAndScale(mMatrixTouch, mContentRect);
    }

    public void setMinMaxScaleX(float minScaleX, float maxScaleX) {

        if (minScaleX < 1f)
            minScaleX = 1f;

        if (maxScaleX == 0.f)
            maxScaleX = Float.MAX_VALUE;

        mMinScaleX = minScaleX;
        mMaxScaleX = maxScaleX;

        limitTransAndScale(mMatrixTouch, mContentRect);
    }

    public void setMinimumScaleY(float yScale) {

        if (yScale < 1f)
            yScale = 1f;

        mMinScaleY = yScale;

        limitTransAndScale(mMatrixTouch, mContentRect);
    }

    public void setMaximumScaleY(float yScale) {

        if (yScale == 0.f)
            yScale = Float.MAX_VALUE;

        mMaxScaleY = yScale;

        limitTransAndScale(mMatrixTouch, mContentRect);
    }

    public void setMinMaxScaleY(float minScaleY, float maxScaleY) {

        if (minScaleY < 1f)
            minScaleY = 1f;

        if (maxScaleY == 0.f)
            maxScaleY = Float.MAX_VALUE;

        mMinScaleY = minScaleY;
        mMaxScaleY = maxScaleY;

        limitTransAndScale(mMatrixTouch, mContentRect);
    }

    public Matrix getMatrixTouch() {
        return mMatrixTouch;
    }

    public boolean isInBoundsX(float x) {
        return isInBoundsLeft(x) && isInBoundsRight(x);
    }

    public boolean isInBoundsY(float y) {
        return isInBoundsTop(y) && isInBoundsBottom(y);
    }

    public boolean isInBounds(float x, float y) {
        return isInBoundsX(x) && isInBoundsY(y);
    }

    public boolean isInBoundsLeft(float x) {
        return mContentRect.left <= x + 1;
    }

    public boolean isInBoundsRight(float x) {
        x = (float) ((int) (x * 100.f)) / 100.f;
        return mContentRect.right >= x - 1;
    }

    public boolean isInBoundsTop(float y) {
        return mContentRect.top <= y;
    }

    public boolean isInBoundsBottom(float y) {
        y = (float) ((int) (y * 100.f)) / 100.f;
        return mContentRect.bottom >= y;
    }

    public float getScaleX() {
        return mScaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }

    public float getMinScaleX() {
        return mMinScaleX;
    }

    public float getMaxScaleX() {
        return mMaxScaleX;
    }

    public float getMinScaleY() {
        return mMinScaleY;
    }

    public float getMaxScaleY() {
        return mMaxScaleY;
    }

    public float getTransX() {
        return mTransX;
    }

    public float getTransY() {
        return mTransY;
    }

    public boolean isFullyZoomedOut() {

        return isFullyZoomedOutX() && isFullyZoomedOutY();
    }

    public boolean isFullyZoomedOutY() {
        return !(mScaleY > mMinScaleY || mMinScaleY > 1f);
    }

    public boolean isFullyZoomedOutX() {
        return !(mScaleX > mMinScaleX || mMinScaleX > 1f);
    }

    public void setDragOffsetX(float offset) {
        mTransOffsetX = Utils.convertDpToPixel(offset);
    }

    public void setDragOffsetY(float offset) {
        mTransOffsetY = Utils.convertDpToPixel(offset);
    }

    public boolean hasNoDragOffset() {
        return mTransOffsetX <= 0 && mTransOffsetY <= 0;
    }

    public boolean canZoomOutMoreX() {
        return mScaleX > mMinScaleX;
    }

    public boolean canZoomInMoreX() {
        return mScaleX < mMaxScaleX;
    }

    public boolean canZoomOutMoreY() {
        return mScaleY > mMinScaleY;
    }

    public boolean canZoomInMoreY() {
        return mScaleY < mMaxScaleY;
    }
}