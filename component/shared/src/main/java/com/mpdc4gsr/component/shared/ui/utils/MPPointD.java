package com.mpdc4gsr.component.shared.ui.utils;

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


