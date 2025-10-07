package com.mpdc4gsr.libunified.app.utils;

import android.graphics.*;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.app.listener.BitmapViewListener;
import com.mpdc4gsr.libunified.compat.ContextProvider;

import java.io.*;

public enum BitmapUtils {
    ;

    public static Bitmap mirror(Bitmap rawBitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);
    }

    public static Bitmap rotateBitmap(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // ，
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // ，
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (null == returnBm) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    public static byte[] bitmapToBytes(Bitmap bitmap, int quality) {
        if (null == bitmap) {
            return null;
        }
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean saveBitmap(Bitmap bitmap, File file, File path) {
        boolean success = false;
        byte[] bytes = bitmapToBytes(bitmap, 100);
        OutputStream out = null;
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
            out = new FileOutputStream(path);
            out.write(bytes);
            out.flush();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    public static Bitmap imageZoom(Bitmap bitmap, double width) {
        // bitmap，bitmap（）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 、、
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] b = baos.toByteArray();
        Bitmap newBitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
        // bitmap 
        return scaleWithWH(newBitmap, width,
                width * newBitmap.getHeight() / newBitmap.getWidth());
    }

    public static Bitmap scaleWithWH(Bitmap bitmap, double w, double h) {
        if (0 == w || 0 == h || null == bitmap) {
            return bitmap;
        } else {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Matrix matrix = new Matrix();
            float scaleWidth = (float) (w / width);
            float scaleHeight = (float) (h / height);

            matrix.postScale(scaleWidth, scaleHeight);
            return Bitmap.createBitmap(bitmap, 0, 0, width, height,
                    matrix, true);
        }
    }

    public static boolean saveFile(String file, Bitmap bmp) {
        if (TextUtils.isEmpty(file) || null == bmp) return false;

        File f = new File(file);
        if (f.exists()) {
            f.delete();
        } else {
            File p = f.getParentFile();
            if (!p.exists()) {
                p.mkdirs();
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Bitmap mergeBitmap(Bitmap backBitmap, Bitmap frontBitmap, int leftFront, int topFront) {
        if (null == backBitmap || backBitmap.isRecycled()
                || null == frontBitmap || frontBitmap.isRecycled()) {
            return null;
        }
        Bitmap bitmap = backBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(backBitmap, 0, 0, null);
        canvas.drawBitmap(frontBitmap, leftFront, topFront, null);
//        if (!frontBitmap.isRecycled()){
//            frontBitmap.recycle();
//        }
        return bitmap;
    }

    public static Bitmap mergeBitmapAlpha(Bitmap backBitmap, Bitmap frontBitmap, Paint paint, int leftFront, int topFront) {
        if (null == backBitmap || backBitmap.isRecycled()
                || null == frontBitmap || frontBitmap.isRecycled()) {
            return null;
        }
        Bitmap bitmap = backBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(backBitmap, 0, 0, null);
        canvas.drawBitmap(frontBitmap, leftFront, topFront, paint);
//        if (!frontBitmap.isRecycled()){
//            frontBitmap.recycle();
//        }
        return bitmap;
    }

    public static Bitmap mergeBitmapByView(Bitmap backBitmap, Bitmap frontBitmap, BitmapViewListener view) {
        if (null == backBitmap || backBitmap.isRecycled()
                || null == frontBitmap || frontBitmap.isRecycled()) {
            return null;
        }
        Paint paint = new Paint();
        paint.setAlpha((int) (view.getViewAlpha() * 255));
        Bitmap bitmap = backBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(backBitmap, 0, 0, null);
        if (1 != view.getViewScale()) {
            frontBitmap = scaleWithWH(frontBitmap, view.getViewWidth(), view.getViewHeight());
        }
        canvas.drawBitmap(frontBitmap, view.getViewX(), view.getViewY(), paint);
        frontBitmap.recycle();
        return bitmap;
    }

    @NonNull
    public static Bitmap mergeBitmapByViewNonNull(@NonNull Bitmap backBitmap, @Nullable Bitmap frontBitmap, BitmapViewListener view) {
        if (null == frontBitmap || frontBitmap.isRecycled()) {
            return backBitmap;
        }

        Bitmap bitmap;
        if (backBitmap.isRecycled()) {
            bitmap = Bitmap.createBitmap(backBitmap.getWidth(), backBitmap.getHeight(), backBitmap.getConfig());
        } else {
            bitmap = backBitmap;
        }
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAlpha((int) (view.getViewAlpha() * 255));

        if (1 != view.getViewScale()) {
            frontBitmap = scaleWithWH(frontBitmap, view.getViewWidth(), view.getViewHeight());
        }
        canvas.drawBitmap(frontBitmap, view.getViewX(), view.getViewY(), paint);
        frontBitmap.recycle();
        return bitmap;
    }

    public static void mergeBitmapByView(Bitmap frontBitmap, BitmapViewListener view, Canvas canvas) {
        if (null == frontBitmap || frontBitmap.isRecycled()) {
            return;
        }
        Paint paint = new Paint();
        paint.setAlpha((int) (view.getViewAlpha() * 255));
        if (1 != view.getViewScale()) {
            frontBitmap = scaleWithWH(frontBitmap, view.getViewWidth(), view.getViewHeight());
        }
        canvas.drawBitmap(frontBitmap, view.getViewX(), view.getViewY(), paint);
    }

    public static void savaRawFile(byte[] bytes, byte[] bytes2) {
        try {
            File path = new File("/sdcard");
            if (!path.exists() && path.isDirectory()) {
                path.mkdirs();
            }
            File file = new File("/sdcard/", "xxx.raw");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.write(bytes2);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap drawCenterLable(Bitmap bmp, String title, String address, String time, int seekBarWidth) {
        //
        Bitmap newBmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        //
        Canvas canvas = new Canvas(newBmp);
        canvas.drawBitmap(bmp, 0, 0, null);  //
        canvas.save();
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE); //
        paint.setTextSize(((int) (12 * ContextProvider.getContext().getResources().getDisplayMetrics().scaledDensity)));
        paint.setDither(true);
        paint.setFilterBitmap(true);
        Rect rectText = new Rect();  //text， ：
        paint.getTextBounds("", 0, "".length(), rectText);
        double beginX = ((int) (10 * ContextProvider.getContext().getResources().getDisplayMetrics().density));  //451.414
        double beginY = bmp.getHeight() - ((int) (10 * ContextProvider.getContext().getResources().getDisplayMetrics().density));
        if (!TextUtils.isEmpty(time)) {
            beginY = beginY - (rectText.bottom - rectText.top);
            canvas.drawText(time, (int) beginX, (int) beginY, paint);
            beginY -= ((int) (6 * ContextProvider.getContext().getResources().getDisplayMetrics().density));
        }
        int lineWidth = bmp.getWidth() - ((int) (20 * ContextProvider.getContext().getResources().getDisplayMetrics().density)) - seekBarWidth;//
        if (!TextUtils.isEmpty(address)) {
            int textHeight = (rectText.bottom - rectText.top);
            paint.getTextBounds(address, 0, address.length(), rectText);
            if (rectText.width() > lineWidth) {
                //，
                StaticLayout staticLayout = new StaticLayout(address,
                        paint, lineWidth,
                        Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                beginY = beginY - (textHeight + ((int) (1.0f * ContextProvider.getContext().getResources().getDisplayMetrics().density))) * staticLayout.getLineCount();
                canvas.save();
                canvas.translate((int) beginX, (int) beginY - textHeight);
                staticLayout.draw(canvas);
                canvas.restore();
            } else {
                beginY = beginY - textHeight;
                canvas.drawText(address, (int) beginX, (int) beginY, paint);
            }
            beginY -= ((int) (6 * ContextProvider.getContext().getResources().getDisplayMetrics().density));
        }
        if (!TextUtils.isEmpty(title)) {
            int textHeight = (rectText.bottom - rectText.top);
            paint.getTextBounds(title, 0, title.length(), rectText);
            if (rectText.width() > lineWidth) {
                //，
                StaticLayout staticLayout = new StaticLayout(title,
                        paint, lineWidth,
                        Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                beginY = beginY - textHeight * staticLayout.getLineCount();
                canvas.save();
                canvas.translate((int) beginX, (int) beginY - textHeight);
                staticLayout.draw(canvas);
                canvas.restore();
            } else {
                beginY = beginY - textHeight;
                canvas.drawText(title, (int) beginX, (int) beginY, paint);
            }
            beginY -= ((int) (6 * ContextProvider.getContext().getResources().getDisplayMetrics().density));
        }
        canvas.restore();
        return newBmp;
    }
}
