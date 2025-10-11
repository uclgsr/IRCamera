package com.mpdc4gsr.component.shared.ui.jobs;

import android.view.View;

import com.mpdc4gsr.component.shared.ui.utils.ObjectPool;
import com.mpdc4gsr.component.shared.ui.utils.Transformer;
import com.mpdc4gsr.component.shared.ui.utils.ViewPortHandler;

public class MoveViewJob extends ViewPortJob {

    private static ObjectPool<MoveViewJob> pool;

    static {
        pool = ObjectPool.create(2, new MoveViewJob(null, 0, 0, null, null));
        pool.setReplenishPercentage(0.5f);
    }

    public MoveViewJob(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v) {
        super(viewPortHandler, xValue, yValue, trans, v);
    }

    public static MoveViewJob getInstance(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v) {
        MoveViewJob result = pool.get();
        result.mViewPortHandler = viewPortHandler;
        result.xValue = xValue;
        result.yValue = yValue;
        result.mTrans = trans;
        result.view = v;
        return result;
    }

    public static void recycleInstance(MoveViewJob instance) {
        pool.recycle(instance);
    }

    @Override
    public void run() {

        pts[0] = xValue;
        pts[1] = yValue;

        mTrans.pointValuesToPixel(pts);
        mViewPortHandler.centerViewPort(pts, view);

        this.recycleInstance(this);
    }

    @Override
    protected ObjectPool.Poolable instantiate() {
        return new MoveViewJob(mViewPortHandler, xValue, yValue, mTrans, view);
    }
}


