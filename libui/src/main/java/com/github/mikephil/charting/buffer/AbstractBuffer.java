package com.github.mikephil.charting.buffer;

public abstract class AbstractBuffer<T> {

    /**
     * float-buffer that holds the data points to draw, order: x,y,x,y,...
     */
    public final float[] buffer;
    /**
     * index in the buffer
     */
    protected int index = 0;
    /**
     * animation phase x-axis
     */
    protected float phaseX = 1f;

    /**
     * animation phase y-axis
     */
    protected float phaseY = 1f;

    /**
     * indicates from which x-index the visible data begins
     */
    protected int mFrom = 0;

    /**
     * indicates to which x-index the visible data ranges
     */
    protected int mTo = 0;

    public AbstractBuffer(int size) {
        index = 0;
        buffer = new float[size];
    }

    /**
     * limits the drawing on the x-axis
     */
    public void limitFrom(int from) {
        if (from < 0)
            from = 0;
        mFrom = from;
    }

    /**
     * limits the drawing on the x-axis
     */
    public void limitTo(int to) {
        if (to < 0)
            to = 0;
        mTo = to;
    }

    public void reset() {
        index = 0;
    }

    public int size() {
        return buffer.length;
    }

    public void setPhases(float phaseX, float phaseY) {
        this.phaseX = phaseX;
        this.phaseY = phaseY;
    }

    public abstract void feed(T data);
}
