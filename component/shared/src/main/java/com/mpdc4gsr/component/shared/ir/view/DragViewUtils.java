package com.mpdc4gsr.component.shared.ir.view;

import android.view.MotionEvent;
import android.view.View;

public enum DragViewUtils {
    ;

    public static void registerDragAction(View v) {
//        registerDragAction(v, 0);
    }

    public static void registerDragAction(View v, long delay) {
        v.setOnTouchListener(new TouchListener(delay));
    }

    private static class TouchListener implements View.OnTouchListener {
        private final long delay;
        private float downX;
        private float downY;
        private long downTime;
        private boolean isMove;
        private boolean canDrag;

        private TouchListener() {
            this(0);
        }

        private TouchListener(long delay) {
            this.delay = delay;
        }

        private boolean haveDelay() {
            return 0 < this.delay;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    isMove = false;
                    downTime = System.currentTimeMillis();
                    canDrag = !haveDelay();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (haveDelay() && !canDrag) {
                        long currMillis = System.currentTimeMillis();
                        if (currMillis - downTime >= delay) {
                            canDrag = true;
                        }
                    }
                    if (!canDrag) {
                        break;
                    }
                    final float xDistance = event.getX() - downX;
                    final float yDistance = event.getY() - downY;
                    if (0 != xDistance && 0 != yDistance) {
                        int l = (int) (v.getLeft() + xDistance);
                        int r = l + v.getWidth();
                        int t = (int) (v.getTop() + yDistance);
                        int b = t + v.getHeight();
//                        v.layout(l, t, r, b);
                        v.setLeft(l);
                        v.setTop(t);
                        v.setRight(r);
                        v.setBottom(b);
                        isMove = true;
                    }
                    break;
                default:
                    break;
            }
            return isMove;
        }

    }
}


