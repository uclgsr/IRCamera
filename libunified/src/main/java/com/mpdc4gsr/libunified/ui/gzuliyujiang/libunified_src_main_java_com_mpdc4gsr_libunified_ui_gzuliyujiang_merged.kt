// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang' directory and its subdirectories.
// Total files: 75 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\BaseDialog.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.*;

import com.mpdc4gsr.libunified.R;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class BaseDialog extends Dialog implements DialogInterface.OnShowListener, DialogInterface.OnDismissListener {
    public static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    protected Activity activity;
    protected View contentView;

    public BaseDialog(@NonNull Activity activity) {
        this(activity, R.style.DialogTheme_Base);
    }

    public BaseDialog(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
        init(activity);
    }

    public final View getContentView() {
        return contentView;
    }

    private void init(Activity activity) {
        this.activity = activity;
        setOwnerActivity(activity);

        setCanceledOnTouchOutside(false);

        setCancelable(false);
        super.setOnShowListener(this);
        super.setOnDismissListener(this);
        Window window = super.getWindow();
        if (window != null) {

            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(activity.getResources().getDisplayMetrics().widthPixels, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.getDecorView().setPadding(0, 0, 0, 0);
        }
        onInit(null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.create();
        } else {
            readyView();
        }
    }

    @Deprecated
    @CallSuper
    protected void onInit(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        DialogLog.print("dialog onInit");
    }

    @CallSuper
    protected void onInit(@Nullable Bundle savedInstanceState) {

        onInit(activity, savedInstanceState);
    }

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogLog.print("dialog onCreate");
        if (contentView == null) {
            readyView();
        }
    }

    private void readyView() {
        contentView = createContentView();
        contentView.setFocusable(true);
        contentView.setFocusableInTouchMode(true);
        setContentView(contentView);
        initView();
    }

    @NonNull
    protected abstract View createContentView();

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @CallSuper
    protected void initView(View contentView) {
        DialogLog.print("dialog initView");
    }

    @CallSuper
    protected void initView() {

        initView(contentView);
    }

    public final void disableCancel() {
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public final void setBackgroundColor(@ColorInt int color) {
        setBackgroundColor(CornerRound.No, color);
    }

    public final void setBackgroundColor(@CornerRound int cornerRound, @ColorInt int color) {
        setBackgroundColor(cornerRound, 20, color);
    }

    public final void setBackgroundColor(@CornerRound int cornerRound, @Dimension(unit = Dimension.DP) int radius, @ColorInt int color) {
        if (contentView == null) {
            return;
        }
        float radiusInPX = contentView.getResources().getDisplayMetrics().density * radius;
        Drawable drawable;
        switch (cornerRound) {
            case CornerRound.Top:
                float[] outerRadii = new float[]{radiusInPX, radiusInPX, radiusInPX, radiusInPX, 0, 0, 0, 0};
                ShapeDrawable shapeDrawable = new ShapeDrawable(new RoundRectShape(outerRadii, null, null));
                shapeDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                drawable = shapeDrawable;
                break;
            case CornerRound.All:
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setCornerRadius(radiusInPX);
                gradientDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                drawable = gradientDrawable;
                break;
            default:
                drawable = new ColorDrawable(color);
                break;
        }
        contentView.setBackground(drawable);
    }

    public final void setBackgroundResource(@DrawableRes int resId) {
        if (contentView == null) {
            return;
        }
        contentView.setBackgroundResource(resId);
    }

    public final void setBackgroundDrawable(Drawable drawable) {
        if (contentView == null) {
            return;
        }
        contentView.setBackground(drawable);
    }

    public final void setLayout(int width, int height) {
        getWindow().setLayout(width, height);
    }

    public final void setWidth(int width) {
        getWindow().setLayout(width, getWindow().getAttributes().height);
    }

    public final void setHeight(int height) {
        getWindow().setLayout(getWindow().getAttributes().width, height);
    }

    public final void setGravity(int gravity) {
        getWindow().setGravity(gravity);
    }

    public final void setDimAmount(@FloatRange(from = 0, to = 1) float amount) {
        getWindow().setDimAmount(amount);
    }

    public final void setAnimationStyle(@StyleRes int animRes) {
        getWindow().setWindowAnimations(animRes);
    }

    @Override
    public void setOnShowListener(@Nullable OnShowListener listener) {
        if (listener == null) {
            return;
        }
        final OnShowListener current = this;
        super.setOnShowListener(dialog -> {
            current.onShow(dialog);
            listener.onShow(dialog);
        });
    }

    @Override
    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        if (listener == null) {
            return;
        }
        final OnDismissListener current = this;
        super.setOnDismissListener(dialog -> {
            current.onDismiss(dialog);
            listener.onDismiss(dialog);
        });
    }

    @CallSuper
    @Override
    public void show() {
        if (isShowing()) {
            return;
        }
        try {
            super.show();
            DialogLog.print("dialog show");
        } catch (Exception e) {

            DialogLog.print(e);
        }
    }

    @CallSuper
    @Override
    public void dismiss() {
        if (!isShowing()) {
            return;
        }
        try {
            super.dismiss();
            DialogLog.print("dialog dismiss");
        } catch (Exception e) {

            DialogLog.print(e);
        }
    }

    @CallSuper
    @Override
    public void onAttachedToWindow() {
        DialogLog.print("dialog attached to window");
        super.onAttachedToWindow();
        initData();
    }

    @CallSuper
    protected void initData() {
        DialogLog.print("dialog initData");
    }

    @CallSuper
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        DialogLog.print("dialog detached from window");
    }

    @CallSuper
    @Override
    public void onShow(DialogInterface dialog) {
        DialogLog.print("dialog onShow");
    }

    @CallSuper
    @Override
    public void onDismiss(DialogInterface dialog) {
        DialogLog.print("dialog onDismiss");
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\BottomDialog.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.R;

public abstract class BottomDialog extends BaseDialog {
    protected View maskView;

    public BottomDialog(@NonNull Activity activity) {
        super(activity, R.style.DialogTheme_Sheet);
    }

    public BottomDialog(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @Override
    public void onInit(@Nullable Bundle savedInstanceState) {
        super.onInit(savedInstanceState);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setWidth(activity.getResources().getDisplayMetrics().widthPixels);
        setGravity(Gravity.BOTTOM);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        super.onShow(dialog);
        if (enableMaskView()) {
            addMaskView();
        }
    }

    protected boolean enableMaskView() {
        return true;
    }

    protected void addMaskView() {

        try {

            getWindow().setDimAmount(0);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            Point screenRealSize = new Point();
            activity.getWindowManager().getDefaultDisplay().getRealSize(screenRealSize);
            int navBarIdentifier = activity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            params.height = screenRealSize.y - activity.getResources().getDimensionPixelSize(navBarIdentifier);
            params.gravity = Gravity.TOP;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            params.format = PixelFormat.TRANSLUCENT;
            params.token = activity.getWindow().getDecorView().getWindowToken();
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
            maskView = new View(activity);
            maskView.setBackgroundColor(0x7F000000);
            maskView.setFitsSystemWindows(false);
            maskView.setOnKeyListener((view, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dismiss();
                    return true;
                }
                return false;
            });
            activity.getWindowManager().addView(maskView, params);
            DialogLog.print("dialog add mask view");
        } catch (Exception e) {
            DialogLog.print(e);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        removeMaskView();
        super.onDismiss(dialog);
    }

    protected void removeMaskView() {
        if (maskView == null) {
            DialogLog.print("mask view is null");
            return;
        }
        try {
            activity.getWindowManager().removeViewImmediate(maskView);
            DialogLog.print("dialog remove mask view");
        } catch (Exception e) {
            DialogLog.print(e);
        }
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\CornerRound.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

public @interface CornerRound {
    int No = 0;
    int Top = 1;
    int All = 2;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\DialogColor.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

import androidx.annotation.ColorInt;

import java.io.Serializable;

public class DialogColor implements Serializable {
    private int contentBackgroundColor = 0xff3b3e44;
    private int topLineColor = 0x33ffffff;
    private int titleTextColor = 0xffffffff;
    private int cancelTextColor = 0x99ffffff;
    private int okTextColor = 0xffffba42;
    private int cancelEllipseColor = 0xFFF4F4F4;
    private int okEllipseColor = 0xFF0081FF;

    public DialogColor contentBackgroundColor(@ColorInt int color) {
        this.contentBackgroundColor = color;
        return this;
    }

    @ColorInt
    public int contentBackgroundColor() {
        return contentBackgroundColor;
    }

    public DialogColor topLineColor(@ColorInt int color) {
        this.topLineColor = color;
        return this;
    }

    @ColorInt
    public int topLineColor() {
        return topLineColor;
    }

    public DialogColor titleTextColor(@ColorInt int color) {
        this.titleTextColor = color;
        return this;
    }

    @ColorInt
    public int titleTextColor() {
        return titleTextColor;
    }

    public DialogColor cancelTextColor(@ColorInt int color) {
        this.cancelTextColor = color;
        return this;
    }

    @ColorInt
    public int cancelTextColor() {
        return cancelTextColor;
    }

    public DialogColor okTextColor(@ColorInt int color) {
        this.okTextColor = color;
        return this;
    }

    @ColorInt
    public int okTextColor() {
        return okTextColor;
    }

    public DialogColor cancelEllipseColor(@ColorInt int color) {
        this.cancelEllipseColor = color;
        return this;
    }

    @ColorInt
    public int cancelEllipseColor() {
        return cancelEllipseColor;
    }

    public DialogColor okEllipseColor(@ColorInt int color) {
        this.okEllipseColor = color;
        return this;
    }

    @ColorInt
    public int okEllipseColor() {
        return okEllipseColor;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\DialogConfig.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

public final class DialogConfig {
    private static int dialogStyle = DialogStyle.Default;
    private static DialogColor dialogColor = new DialogColor();

    private DialogConfig() {
        super();
    }

    @DialogStyle
    public static int getDialogStyle() {
        return dialogStyle;
    }

    public static void setDialogStyle(@DialogStyle int style) {
        dialogStyle = style;
    }

    public static DialogColor getDialogColor() {
        if (dialogColor == null) {
            dialogColor = new DialogColor();
        }
        return dialogColor;
    }

    public static void setDialogColor(DialogColor color) {
        dialogColor = color;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\DialogLog.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

import android.util.Log;

import androidx.annotation.NonNull;

public final class DialogLog {
    private static final String TAG = "AndroidPicker";
    private static boolean enable = false;

    private DialogLog() {
        super();
    }

    public static void enable() {
        enable = true;
    }

    public static void print(@NonNull Object log) {
        if (!enable) {
            return;
        }
        Log.d(TAG, log.toString());
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\DialogStyle.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface DialogStyle {
    int Default = 0;
    int One = 1;
    int Two = 2;
    int Three = 3;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog\ModalDialog.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.*;

import com.mpdc4gsr.libunified.R;

@SuppressWarnings("unused")
public abstract class ModalDialog extends BottomDialog implements View.OnClickListener {
    protected View headerView;
    protected TextView cancelView;
    protected TextView titleView;
    protected TextView okView;
    protected View topLineView;
    protected View bodyView;
    protected View footerView;

    public ModalDialog(@NonNull Activity activity) {
        super(activity, DialogConfig.getDialogStyle() == DialogStyle.Three
                ? R.style.DialogTheme_Fade : R.style.DialogTheme_Sheet);
    }

    public ModalDialog(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @Override
    public void onInit(@Nullable Bundle savedInstanceState) {
        super.onInit(savedInstanceState);
        if (DialogConfig.getDialogStyle() == DialogStyle.Three) {
            setWidth((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.8f));
            setGravity(Gravity.CENTER);
        }
    }

    @Override
    protected boolean enableMaskView() {
        return DialogConfig.getDialogStyle() != DialogStyle.Three;
    }

    @NonNull
    @Override
    protected View createContentView() {
        LinearLayout rootLayout = new LinearLayout(activity);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setPadding(0, 0, 0, 0);
        headerView = createHeaderView();
        if (headerView == null) {
            headerView = new View(activity);
            headerView.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        }
        rootLayout.addView(headerView);
        topLineView = createTopLineView();
        if (topLineView == null) {
            topLineView = new View(activity);
            topLineView.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        }
        rootLayout.addView(topLineView);
        bodyView = createBodyView();
        rootLayout.addView(bodyView, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f));
        footerView = createFooterView();
        if (footerView == null) {
            footerView = new View(activity);
            footerView.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        }
        rootLayout.addView(footerView);
        return rootLayout;
    }

    @Nullable
    protected View createHeaderView() {
        switch (DialogConfig.getDialogStyle()) {
            case DialogStyle.One:
                return View.inflate(activity, R.layout.dialog_header_style_1, null);
            case DialogStyle.Two:
                return View.inflate(activity, R.layout.dialog_header_style_2, null);
            case DialogStyle.Three:
                return View.inflate(activity, R.layout.dialog_header_style_3, null);
            default:
                return View.inflate(activity, R.layout.dialog_header_style_default, null);
        }
    }

    @Nullable
    protected View createTopLineView() {
        if (DialogConfig.getDialogStyle() == DialogStyle.Default) {
            View view = new View(activity);
            view.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, (int) (1 * activity.getResources().getDisplayMetrics().density)));
            view.setBackgroundColor(DialogConfig.getDialogColor().topLineColor());
            return view;
        }
        return null;
    }

    @NonNull
    protected abstract View createBodyView();

    @Nullable
    protected View createFooterView() {
        switch (DialogConfig.getDialogStyle()) {
            case DialogStyle.One:
                return View.inflate(activity, R.layout.dialog_footer_style_1, null);
            case DialogStyle.Two:
                return View.inflate(activity, R.layout.dialog_footer_style_2, null);
            case DialogStyle.Three:
                return View.inflate(activity, R.layout.dialog_footer_style_3, null);
            default:
                return null;
        }
    }

    @CallSuper
    @Override
    protected void initView() {
        super.initView();
        int color = DialogConfig.getDialogColor().contentBackgroundColor();
        switch (DialogConfig.getDialogStyle()) {
            case DialogStyle.One:
            case DialogStyle.Two:
                setBackgroundColor(CornerRound.Top, color);
                break;
            case DialogStyle.Three:
                setBackgroundColor(CornerRound.All, color);
                break;
            default:
                setBackgroundColor(CornerRound.Top, 15, color);
                break;
        }
        cancelView = contentView.findViewById(R.id.dialog_modal_cancel);
        if (cancelView == null) {
            throw new IllegalArgumentException("Cancel view id not found");
        }
        titleView = contentView.findViewById(R.id.dialog_modal_title);
        if (titleView == null) {
            throw new IllegalArgumentException("Title view id not found");
        }
        okView = contentView.findViewById(R.id.dialog_modal_ok);
        if (okView == null) {
            throw new IllegalArgumentException("Ok view id not found");
        }
        titleView.setTextColor(DialogConfig.getDialogColor().titleTextColor());
        cancelView.setTextColor(DialogConfig.getDialogColor().cancelTextColor());
        okView.setTextColor(DialogConfig.getDialogColor().okTextColor());
        cancelView.setOnClickListener(this);
        okView.setOnClickListener(this);
        maybeBuildEllipseButton();
    }

    private void maybeBuildEllipseButton() {
        if (DialogConfig.getDialogStyle() != DialogStyle.One && DialogConfig.getDialogStyle() != DialogStyle.Two) {
            return;
        }
        if (DialogConfig.getDialogStyle() == DialogStyle.Two) {
            Drawable background = cancelView.getBackground();
            if (background != null) {
                background.setColorFilter(new PorterDuffColorFilter(DialogConfig.getDialogColor().cancelEllipseColor(), PorterDuff.Mode.SRC_IN));
                cancelView.setBackground(background);
            } else {
                cancelView.setBackgroundResource(R.mipmap.dialog_close_icon);
            }
        } else {
            GradientDrawable cancelDrawable = new GradientDrawable();
            cancelDrawable.setCornerRadius(okView.getResources().getDisplayMetrics().density * 999);
            cancelDrawable.setColor(DialogConfig.getDialogColor().cancelEllipseColor());
            cancelView.setBackground(cancelDrawable);

        }
        GradientDrawable okDrawable = new GradientDrawable();
        okDrawable.setCornerRadius(okView.getResources().getDisplayMetrics().density * 999);
        okDrawable.setColor(DialogConfig.getDialogColor().okEllipseColor());
        okView.setBackground(okDrawable);

    }

    @Override
    public void setTitle(final @Nullable CharSequence title) {
        if (titleView != null) {
            titleView.post(new Runnable() {
                @Override
                public void run() {
                    titleView.setText(title);
                }
            });
        } else {
            super.setTitle(title);
        }
    }

    @Override
    public void setTitle(final int titleId) {
        if (titleView != null) {
            titleView.post(new Runnable() {
                @Override
                public void run() {
                    titleView.setText(titleId);
                }
            });
        } else {
            super.setTitle(titleId);
        }
    }

    @CallSuper
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.dialog_modal_cancel) {
            DialogLog.print("cancel clicked");
            onCancel();
            dismiss();
        } else if (id == R.id.dialog_modal_ok) {
            DialogLog.print("ok clicked");
            onOk();
            dismiss();
        }
    }

    protected abstract void onCancel();

    protected abstract void onOk();

    public final void setBodyWidth(@Dimension(unit = Dimension.DP) @IntRange(from = 50) int bodyWidth) {
        ViewGroup.LayoutParams layoutParams = bodyView.getLayoutParams();
        int width = WRAP_CONTENT;
        if (bodyWidth != WRAP_CONTENT && bodyWidth != MATCH_PARENT) {
            width = (int) (bodyView.getResources().getDisplayMetrics().density * bodyWidth);
        }
        layoutParams.width = width;
        bodyView.setLayoutParams(layoutParams);
    }

    public final void setBodyHeight(@Dimension(unit = Dimension.DP) @IntRange(from = 50) int bodyHeight) {
        ViewGroup.LayoutParams layoutParams = bodyView.getLayoutParams();
        int height = WRAP_CONTENT;
        if (bodyHeight != WRAP_CONTENT && bodyHeight != MATCH_PARENT) {
            height = (int) (bodyView.getResources().getDisplayMetrics().density * bodyHeight);
        }
        layoutParams.height = height;
        bodyView.setLayoutParams(layoutParams);
    }

    public final View getHeaderView() {
        if (headerView == null) {
            headerView = new View(activity);
        }
        return headerView;
    }

    public final View getTopLineView() {
        return topLineView;
    }

    public final View getBodyView() {
        return bodyView;
    }

    public final View getFooterView() {
        return footerView;
    }

    public final TextView getCancelView() {
        return cancelView;
    }

    public final TextView getTitleView() {
        return titleView;
    }

    public final TextView getOkView() {
        return okView;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\annotation\DateMode.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface DateMode {

    int NONE = -1;

    int YEAR_MONTH_DAY = 0;

    int YEAR_MONTH = 1;

    int MONTH_DAY = 2;

    int YEAR = 3;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\annotation\EthnicSpec.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation;

public @interface EthnicSpec {
    int DEFAULT = 1;
    int GB3304_91 = 2;
    int SEVENTH_NATIONAL_CENSUS = 3;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\annotation\TimeMode.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface TimeMode {

    int NONE = -1;

    int HOUR_24_NO_SECOND = 0;

    int HOUR_24_HAS_SECOND = 1;

    int HOUR_12_NO_SECOND = 2;

    int HOUR_12_HAS_SECOND = 3;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\BirthdayPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DateEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.BirthdayFormatter;

import java.util.Calendar;

@SuppressWarnings("unused")
public class BirthdayPicker extends DatePicker {
    private static final int MAX_AGE = 100;
    private DateEntity defaultValue;
    private boolean initialized = false;

    public BirthdayPicker(@NonNull Activity activity) {
        super(activity);
    }

    public BirthdayPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @Override
    protected void initData() {
        super.initData();
        initialized = true;
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        DateEntity startValue = DateEntity.target(currentYear - MAX_AGE, 1, 1);
        DateEntity endValue = DateEntity.target(currentYear, currentMonth, currentDay);
        wheelLayout.setRange(startValue, endValue, defaultValue);
        wheelLayout.setDateMode(DateMode.YEAR_MONTH_DAY);
        wheelLayout.setDateFormatter(new BirthdayFormatter());
    }

    public void setDefaultValue(int year, int month, int day) {
        defaultValue = DateEntity.target(year, month, day);
        if (initialized) {
            wheelLayout.setDefaultValue(defaultValue);
        }
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\CarPlatePicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.LinkageProvider;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnCarPlatePickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnLinkagePickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.CarPlateWheelLayout;

@SuppressWarnings({"unused"})
public class CarPlatePicker extends LinkagePicker {
    private OnCarPlatePickedListener onCarPlatePickedListener;

    public CarPlatePicker(@NonNull Activity activity) {
        super(activity);
    }

    public CarPlatePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @Deprecated
    @Override
    public void setData(@NonNull LinkageProvider data) {
        throw new UnsupportedOperationException("Data already preset");
    }

    @Deprecated
    @Override
    public void setOnLinkagePickedListener(OnLinkagePickedListener onLinkagePickedListener) {
        throw new UnsupportedOperationException("Use setOnCarPlatePickedListener instead");
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new CarPlateWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onOk() {
        if (onCarPlatePickedListener != null) {
            String province = wheelLayout.getFirstWheelView().getCurrentItem();
            String letter = wheelLayout.getSecondWheelView().getCurrentItem();
            onCarPlatePickedListener.onCarNumberPicked(province, letter);
        }
    }

    public void setOnCarPlatePickedListener(OnCarPlatePickedListener onCarPlatePickedListener) {
        this.onCarPlatePickedListener = onCarPlatePickedListener;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\ConstellationPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.DialogLog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.ConstellationEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DateEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ConstellationPicker extends OptionPicker {
    public static String JSON = "[{\"id\":0,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"\",\"endDate\":\"\",\"english\":\"Unlimited\"},\n" +
            "{\"id\":1,\"name\":\"[CHINESE_TEXT]sheep[CHINESE_TEXT]\",\"startDate\":\"3-21\",\"endDate\":\"4-19\",\"english\":\"Aries\"},\n" +
            "{\"id\":2,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"4-20\",\"endDate\":\"5-20\",\"english\":\"Taurus\"},\n" +
            "{\"id\":3,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"5-21\",\"endDate\":\"6-21\",\"english\":\"Gemini\"},\n" +
            "{\"id\":4,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"6-22\",\"endDate\":\"7-22\",\"english\":\"Cancer\"},\n" +
            "{\"id\":5,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"7-23\",\"endDate\":\"8-22\",\"english\":\"Leo\"},\n" +
            "{\"id\":6,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"8-23\",\"endDate\":\"9-22\",\"english\":\"Virgo\"},\n" +
            "{\"id\":7,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"9-23\",\"endDate\":\"10-23\",\"english\":\"Libra\"},\n" +
            "{\"id\":8,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"10-24\",\"endDate\":\"11-22\",\"english\":\"Scorpio\"},\n" +
            "{\"id\":9,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"11-23\",\"endDate\":\"12-21\",\"english\":\"Sagittarius\"},\n" +
            "{\"id\":10,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"12-22\",\"endDate\":\"1-19\",\"english\":\"Capricorn\"},\n" +
            "{\"id\":11,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"1-20\",\"endDate\":\"2-18\",\"english\":\"Aquarius\"},\n" +
            "{\"id\":12,\"name\":\"[CHINESE_TEXT]\",\"startDate\":\"2-19\",\"endDate\":\"3-20\",\"english\":\"Pisces\"}]";
    private boolean includeUnlimited = false;

    public ConstellationPicker(Activity activity) {
        super(activity);
    }

    public ConstellationPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    public void setIncludeUnlimited(boolean includeUnlimited) {
        this.includeUnlimited = includeUnlimited;
        setData(provideData());
    }

    @Override
    public void setDefaultValue(Object item) {
        if (item instanceof String) {
            setDefaultValueByName(item.toString());
        } else {
            super.setDefaultValue(item);
        }
    }

    public void setDefaultValueById(String id) {
        ConstellationEntity entity = new ConstellationEntity();
        entity.setId(id);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByName(String name) {
        ConstellationEntity entity = new ConstellationEntity();
        entity.setName(name);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByDate(DateEntity date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.toTimeInMillis());
        setDefaultValueByDate(calendar.getTime());
    }

    public void setDefaultValueByDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String name;
        switch (month) {
            case 1:
                name = day < 21 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 2:
                name = day < 20 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 3:
                name = day < 21 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]sheep[CHINESE_TEXT]";
                break;
            case 4:
                name = day < 21 ? "[CHINESE_TEXT]sheep[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 5:
                name = day < 22 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 6:
                name = day < 22 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 7:
                name = day < 23 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 8:
                name = day < 24 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 9:
                name = day < 24 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 10:
                name = day < 24 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 11:
                name = day < 23 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            case 12:
                name = day < 22 ? "[CHINESE_TEXT]" : "[CHINESE_TEXT]";
                break;
            default:
                name = "[CHINESE_TEXT]";
                break;
        }
        setDefaultValueByName(name);
    }

    public void setDefaultValueByEnglish(String english) {
        ConstellationEntity entity = new ConstellationEntity();
        entity.setEnglish(english);
        super.setDefaultValue(entity);
    }

    @Override
    protected List<?> provideData() {
        ArrayList<ConstellationEntity> data = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            for (int i = 0, n = jsonArray.length(); i < n; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ConstellationEntity entity = new ConstellationEntity();
                entity.setId(jsonObject.getString("id"));
                entity.setStartDate(jsonObject.getString("startDate"));
                entity.setEndDate(jsonObject.getString("endDate"));
                entity.setName(jsonObject.getString("name"));
                entity.setEnglish(jsonObject.getString("english"));
                if (!includeUnlimited && "0".equals(entity.getId())) {
                    continue;
                }
                data.add(entity);
            }
        } catch (JSONException e) {
            DialogLog.print(e);
        }
        return data;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\DateFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface DateFormatter {

    String formatYear(int year);

    String formatMonth(int month);

    String formatDay(int day);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\LinkageProvider.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

import androidx.annotation.NonNull;

import java.util.List;

public interface LinkageProvider {
    int INDEX_NO_FOUND = -1;

    boolean firstLevelVisible();

    boolean thirdLevelVisible();

    @NonNull
    List<?> provideFirstData();

    @NonNull
    List<?> linkageSecondData(int firstIndex);

    @NonNull
    List<?> linkageThirdData(int firstIndex, int secondIndex);

    int findFirstIndex(Object firstValue);

    int findSecondIndex(int firstIndex, Object secondValue);

    int findThirdIndex(int firstIndex, int secondIndex, Object thirdValue);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnCarPlatePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnCarPlatePickedListener {

    void onCarNumberPicked(String province, String letter);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDatePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDatePickedListener {

    void onDatePicked(int year, int month, int day);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDateSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDateSelectedListener {

    void onDateSelected(int year, int month, int day);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDatimePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDatimePickedListener {

    void onDatimePicked(int year, int month, int day, int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnDatimeSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnDatimeSelectedListener {

    void onDatimeSelected(int year, int month, int day, int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnLinkagePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnLinkagePickedListener {

    void onLinkagePicked(Object first, Object second, Object third);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnLinkageSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnLinkageSelectedListener {

    void onLinkageSelected(Object first, Object second, Object third);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnNumberPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnNumberPickedListener {

    void onNumberPicked(int position, Number item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnNumberSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnNumberSelectedListener {

    void onNumberSelected(int position, Number item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnOptionPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnOptionPickedListener {

    void onOptionPicked(int position, Object item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnOptionSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnOptionSelectedListener {

    void onOptionSelected(int position, Object item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimeMeridiemPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimeMeridiemPickedListener {

    void onTimePicked(int hour, int minute, int second, boolean isAnteMeridiem);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimeMeridiemSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimeMeridiemSelectedListener {

    void onTimeSelected(int hour, int minute, int second, boolean isAnteMeridiem);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimePickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimePickedListener {

    void onTimePicked(int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnTimeSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnTimeSelectedListener {

    void onTimeSelected(int hour, int minute, int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\OnYearPickedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface OnYearPickedListener {

    void onYearPicked(int year);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\contract\TimeFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract;

public interface TimeFormatter {

    String formatHour(int hour);

    String formatMinute(int minute);

    String formatSecond(int second);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\DatePicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnDatePickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.DateWheelLayout;

@SuppressWarnings("unused")
public class DatePicker extends ModalDialog {
    protected DateWheelLayout wheelLayout;
    private OnDatePickedListener onDatePickedListener;

    public DatePicker(@NonNull Activity activity) {
        super(activity);
    }

    public DatePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new DateWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onDatePickedListener != null) {
            int year = wheelLayout.getSelectedYear();
            int month = wheelLayout.getSelectedMonth();
            int day = wheelLayout.getSelectedDay();
            onDatePickedListener.onDatePicked(year, month, day);
        }
    }

    public void setOnDatePickedListener(OnDatePickedListener onDatePickedListener) {
        this.onDatePickedListener = onDatePickedListener;
    }

    public final DateWheelLayout getWheelLayout() {
        return wheelLayout;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\DatimePicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.TimeMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnDatimePickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.DatimeWheelLayout;

@SuppressWarnings({"unused", "WeakerAccess"})
public class DatimePicker extends ModalDialog {
    protected DatimeWheelLayout wheelLayout;
    private OnDatimePickedListener onDatimePickedListener;

    public DatimePicker(@NonNull Activity activity) {
        super(activity);
    }

    public DatimePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new DatimeWheelLayout(activity);
        wheelLayout.setDateMode(DateMode.YEAR_MONTH_DAY);
        wheelLayout.setTimeMode(TimeMode.HOUR_24_NO_SECOND);
        wheelLayout.setDateLabel("/", "/", "");
        wheelLayout.setTimeLabel(":", ":", "");
        wheelLayout.setCurtainEnabled(true);
        wheelLayout.setCurtainColor(ContextCompat.getColor(getContext(), R.color.wheel_select_bg));
        wheelLayout.setSelectedTextColor(ContextCompat.getColor(getContext(), R.color.wheel_select_text));
        wheelLayout.setTextColor(ContextCompat.getColor(getContext(), R.color.wheel_unselect_text));
        wheelLayout.setResetWhenLinkage(false, false);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onDatimePickedListener != null) {
            int year = wheelLayout.getSelectedYear();
            int month = wheelLayout.getSelectedMonth();
            int day = wheelLayout.getSelectedDay();
            int hour = wheelLayout.getSelectedHour();
            int minute = wheelLayout.getSelectedMinute();
            int second = wheelLayout.getSelectedSecond();
            onDatimePickedListener.onDatimePicked(year, month, day, hour, minute, second);
        }
    }

    public void setOnDatimePickedListener(OnDatimePickedListener onDatimePickedListener) {
        this.onDatimePickedListener = onDatimePickedListener;
    }

    public final DatimeWheelLayout getWheelLayout() {
        return wheelLayout;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\ConstellationEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class ConstellationEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String id;
    private String startDate;
    private String endDate;
    private String name;
    private String english;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return english;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConstellationEntity that = (ConstellationEntity) o;
        return Objects.equals(id, that.id) ||
                Objects.equals(startDate, that.startDate) ||
                Objects.equals(endDate, that.endDate) ||
                Objects.equals(name, that.name) ||
                Objects.equals(english, that.english);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startDate, endDate, name, english);
    }

    @NonNull
    @Override
    public String toString() {
        return "ConstellationEntity{" +
                "id='" + id + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", name='" + name + '\'' +
                ", english" + english + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\DateEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;

@SuppressWarnings({"unused"})
public class DateEntity implements Serializable {
    private int year;
    private int month;
    private int day;

    public static DateEntity target(int year, int month, int day) {
        DateEntity entity = new DateEntity();
        entity.setYear(year);
        entity.setMonth(month);
        entity.setDay(day);
        return entity;
    }

    public static DateEntity today() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return target(year, month, day);
    }

    public static DateEntity dayOnFuture(int day) {
        DateEntity entity = today();
        entity.setDay(entity.getDay() + day);
        return entity;
    }

    public static DateEntity monthOnFuture(int month) {
        DateEntity entity = today();
        entity.setMonth(entity.getMonth() + month);
        return entity;
    }

    public static DateEntity yearOnFuture(int year) {
        DateEntity entity = today();
        entity.setYear(entity.getYear() + year);
        return entity;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public long toTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DateEntity that = (DateEntity) o;
        return year == that.year &&
                month == that.month &&
                day == that.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    @NonNull
    @Override
    public String toString() {
        return year + "-" + month + "-" + day;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\DatimeEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;

@SuppressWarnings({"unused"})
public class DatimeEntity implements Serializable {
    private DateEntity date;
    private TimeEntity time;

    public static DatimeEntity now() {
        DatimeEntity entity = new DatimeEntity();
        entity.setDate(DateEntity.today());
        entity.setTime(TimeEntity.now());
        return entity;
    }

    public static DatimeEntity minuteOnFuture(int minute) {
        DatimeEntity entity = now();
        entity.setTime(TimeEntity.minuteOnFuture(minute));
        return entity;
    }

    public static DatimeEntity hourOnFuture(int hour) {
        DatimeEntity entity = now();
        entity.setTime(TimeEntity.hourOnFuture(hour));
        return entity;
    }

    public static DatimeEntity dayOnFuture(int day) {
        DatimeEntity entity = now();
        entity.setDate(DateEntity.dayOnFuture(day));
        return entity;
    }

    public static DatimeEntity monthOnFuture(int month) {
        DatimeEntity entity = now();
        entity.setDate(DateEntity.monthOnFuture(month));
        return entity;
    }

    public static DatimeEntity yearOnFuture(int year) {
        DatimeEntity entity = now();
        entity.setDate(DateEntity.yearOnFuture(year));
        return entity;
    }

    public DateEntity getDate() {
        return date;
    }

    public void setDate(DateEntity date) {
        this.date = date;
    }

    public TimeEntity getTime() {
        return time;
    }

    public void setTime(TimeEntity time) {
        this.time = time;
    }

    public long toTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
        calendar.set(Calendar.MINUTE, time.getMinute());
        calendar.set(Calendar.SECOND, time.getSecond());
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @NonNull
    @Override
    public String toString() {
        return date.toString() + " " + time.toString();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\EthnicEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class EthnicEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String code;
    private String name;
    private String spelling;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpelling() {
        return spelling;
    }

    public void setSpelling(String spelling) {
        this.spelling = spelling;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return spelling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EthnicEntity that = (EthnicEntity) o;
        return Objects.equals(code, that.code) ||
                Objects.equals(name, that.name) ||
                Objects.equals(spelling, that.spelling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, spelling);
    }

    @NonNull
    @Override
    public String toString() {
        return "EthnicEntity{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", spelling='" + spelling + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\PhoneCodeEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class PhoneCodeEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String code;
    private String name;
    private String english;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return english;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhoneCodeEntity that = (PhoneCodeEntity) o;
        return Objects.equals(code, that.code) ||
                Objects.equals(name, that.name) ||
                Objects.equals(english, that.english);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, english);
    }

    @NonNull
    @Override
    public String toString() {
        return "PhoneCodeEntity{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", english" + english + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\SexEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class SexEntity implements TextProvider, Serializable {
    private static final boolean IS_CHINESE;

    static {
        IS_CHINESE = Locale.getDefault().getDisplayLanguage().contains("[ph][ph]");
    }

    private String id;
    private String name;
    private String english;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    @Override
    public String provideText() {
        if (IS_CHINESE) {
            return name;
        }
        return english;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SexEntity that = (SexEntity) o;
        return Objects.equals(id, that.id) ||
                Objects.equals(name, that.name) ||
                Objects.equals(english, that.english);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, english);
    }

    @NonNull
    @Override
    public String toString() {
        return "SexEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", english" + english + '\'' +
                '}';
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\entity\TimeEntity.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;

@SuppressWarnings({"unused"})
public class TimeEntity implements Serializable {
    private int hour;
    private int minute;
    private int second;

    public static TimeEntity target(int hour, int minute, int second) {
        TimeEntity entity = new TimeEntity();
        entity.setHour(hour);
        entity.setMinute(minute);
        entity.setSecond(second);
        return entity;
    }

    public static TimeEntity now() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return target(hour, minute, second);
    }

    public static TimeEntity minuteOnFuture(int minute) {
        TimeEntity entity = now();
        entity.setMinute(entity.getMinute() + minute);
        return entity;
    }

    public static TimeEntity hourOnFuture(int hour) {
        TimeEntity entity = now();
        entity.setHour(entity.getHour() + hour);
        return entity;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public long toTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @NonNull
    @Override
    public String toString() {
        return hour + ":" + minute + ":" + second;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\EthnicPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.DialogLog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.EthnicSpec;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.EthnicEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class EthnicPicker extends OptionPicker {
    public static String JSON = "[{\"code\":\"01\",\"name\":\"Han\",\"spelling\":\"Han\"}," +
            "{\"code\":\"02\",\"name\":\"Mongol\",\"spelling\":\"Mongol\"}," +
            "{\"code\":\"03\",\"name\":\"Hui\",\"spelling\":\"Hui\"}," +
            "{\"code\":\"04\",\"name\":\"Zang\",\"spelling\":\"Zang\"}," +
            "{\"code\":\"05\",\"name\":\"Uygur\",\"spelling\":\"Uygur\"}," +
            "{\"code\":\"06\",\"name\":\"Miao\",\"spelling\":\"Miao\"}," +
            "{\"code\":\"07\",\"name\":\"Yi\",\"spelling\":\"Yi\"}," +
            "{\"code\":\"08\",\"name\":\"Zhuang\",\"spelling\":\"Zhuang\"}," +
            "{\"code\":\"09\",\"name\":\"Buyei\",\"spelling\":\"Buyei\"}," +
            "{\"code\":\"10\",\"name\":\"Chosen\",\"spelling\":\"Chosen\"}," +
            "{\"code\":\"11\",\"name\":\"Man\",\"spelling\":\"Man\"}," +
            "{\"code\":\"12\",\"name\":\"Dong\",\"spelling\":\"Dong\"}," +
            "{\"code\":\"13\",\"name\":\"Yao\",\"spelling\":\"Yao\"}," +
            "{\"code\":\"14\",\"name\":\"Bai\",\"spelling\":\"Bai\"}," +
            "{\"code\":\"15\",\"name\":\"Tujia\",\"spelling\":\"Tujia\"}," +
            "{\"code\":\"16\",\"name\":\"Hani\",\"spelling\":\"Hani\"}," +
            "{\"code\":\"17\",\"name\":\"Kazak\",\"spelling\":\"Kazak\"}," +
            "{\"code\":\"18\",\"name\":\"Dai\",\"spelling\":\"Dai\"}," +
            "{\"code\":\"19\",\"name\":\"Li\",\"spelling\":\"Li\"}," +
            "{\"code\":\"20\",\"name\":\"Lisu\",\"spelling\":\"Lisu\"}," +
            "{\"code\":\"21\",\"name\":\"Va\",\"spelling\":\"Va\"}," +
            "{\"code\":\"22\",\"name\":\"She\",\"spelling\":\"She\"}," +
            "{\"code\":\"23\",\"name\":\"Gaoshan\",\"spelling\":\"Gaoshan\"}," +
            "{\"code\":\"24\",\"name\":\"Lahu\",\"spelling\":\"Lahu\"}," +
            "{\"code\":\"25\",\"name\":\"Sui\",\"spelling\":\"Sui\"}," +
            "{\"code\":\"26\",\"name\":\"Dongxiang\",\"spelling\":\"Dongxiang\"}," +
            "{\"code\":\"27\",\"name\":\"Naxi\",\"spelling\":\"Naxi\"}," +
            "{\"code\":\"28\",\"name\":\"Jingpo\",\"spelling\":\"Jingpo\"}," +
            "{\"code\":\"29\",\"name\":\"Kirgiz\",\"spelling\":\"Kirgiz\"}," +
            "{\"code\":\"30\",\"name\":\"Tu\",\"spelling\":\"Tu\"}," +
            "{\"code\":\"31\",\"name\":\"Daur\",\"spelling\":\"Daur\"}," +
            "{\"code\":\"32\",\"name\":\"Mulao\",\"spelling\":\"Mulao\"}," +
            "{\"code\":\"33\",\"name\":\"Qiang\",\"spelling\":\"Qiang\"}," +
            "{\"code\":\"34\",\"name\":\"Blang\",\"spelling\":\"Blang\"}," +
            "{\"code\":\"35\",\"name\":\"Salar\",\"spelling\":\"Salar\"}," +
            "{\"code\":\"36\",\"name\":\"Maonan\",\"spelling\":\"Maonan\"}," +
            "{\"code\":\"37\",\"name\":\"Gelao\",\"spelling\":\"Gelao\"}," +
            "{\"code\":\"38\",\"name\":\"Xibe\",\"spelling\":\"Xibe\"}," +
            "{\"code\":\"39\",\"name\":\"Achang\",\"spelling\":\"Achang\"}," +
            "{\"code\":\"40\",\"name\":\"Pumi\",\"spelling\":\"Pumi\"}," +
            "{\"code\":\"41\",\"name\":\"Tajik\",\"spelling\":\"Tajik\"}," +
            "{\"code\":\"42\",\"name\":\"Nu\",\"spelling\":\"Nu\"}," +
            "{\"code\":\"43\",\"name\":\"Uzbek\",\"spelling\":\"Uzbek\"}," +
            "{\"code\":\"44\",\"name\":\"Russ\",\"spelling\":\"Russ\"}," +
            "{\"code\":\"45\",\"name\":\"Ewenki\",\"spelling\":\"Ewenki\"}," +
            "{\"code\":\"46\",\"name\":\"Deang\",\"spelling\":\"Deang\"}," +
            "{\"code\":\"47\",\"name\":\"Bonan\",\"spelling\":\"Bonan\"}," +
            "{\"code\":\"48\",\"name\":\"Yugur\",\"spelling\":\"Yugur\"}," +
            "{\"code\":\"49\",\"name\":\"Gin\",\"spelling\":\"Gin\"}," +
            "{\"code\":\"50\",\"name\":\"Tatar\",\"spelling\":\"Tatar\"}," +
            "{\"code\":\"51\",\"name\":\"Derung\",\"spelling\":\"Derung\"}," +
            "{\"code\":\"52\",\"name\":\"Oroqen\",\"spelling\":\"Oroqen\"}," +
            "{\"code\":\"53\",\"name\":\"Hezhen\",\"spelling\":\"Hezhen\"}," +
            "{\"code\":\"54\",\"name\":\"Monba\",\"spelling\":\"Monba\"}," +
            "{\"code\":\"55\",\"name\":\"Lhoba\",\"spelling\":\"Lhoba\"}," +
            "{\"code\":\"56\",\"name\":\"Jino\",\"spelling\":\"Jino\"}]";
    private int ethnicSpec = EthnicSpec.DEFAULT;

    public EthnicPicker(@NonNull Activity activity) {
        super(activity);
    }

    public EthnicPicker(@NonNull Activity activity, int themeResId) {
        super(activity, themeResId);
    }

    public void setEthnicSpec(@EthnicSpec int ethnicSpec) {
        this.ethnicSpec = ethnicSpec;
        setData(provideData());
    }

    @Override
    public void setDefaultValue(Object item) {
        if (item instanceof String) {
            setDefaultValueByName(item.toString());
        } else {
            super.setDefaultValue(item);
        }
    }

    public void setDefaultValueByCode(String code) {
        EthnicEntity entity = new EthnicEntity();
        entity.setCode(code);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByName(String name) {
        EthnicEntity entity = new EthnicEntity();
        entity.setName(name);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueBySpelling(String spelling) {
        EthnicEntity entity = new EthnicEntity();
        entity.setSpelling(spelling);
        super.setDefaultValue(entity);
    }

    @Override
    protected List<EthnicEntity> provideData() {
        ArrayList<EthnicEntity> data = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            for (int i = 0, n = jsonArray.length(); i < n; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                EthnicEntity entity = new EthnicEntity();
                entity.setCode(jsonObject.getString("code"));
                entity.setName(jsonObject.getString("name"));
                entity.setSpelling(jsonObject.getString("spelling"));
                data.add(entity);
            }
        } catch (JSONException e) {
            DialogLog.print(e);
        }
        switch (ethnicSpec) {
            case EthnicSpec.DEFAULT:
                EthnicEntity other = new EthnicEntity();
                other.setCode("97");
                other.setName("Other");
                other.setSpelling("Other");
                data.add(other);
                EthnicEntity foreign = new EthnicEntity();
                foreign.setCode("98");
                foreign.setName("Foreign Ancestry");
                foreign.setSpelling("Foreign");
                data.add(foreign);
                break;
            case EthnicSpec.SEVENTH_NATIONAL_CENSUS:
                EthnicEntity unrecognized = new EthnicEntity();
                unrecognized.setCode("97");
                unrecognized.setName("Undetermined EthnicityhumanPopulation");
                unrecognized.setSpelling("Unrecognized");
                data.add(unrecognized);
                EthnicEntity naturalization = new EthnicEntity();
                naturalization.setCode("98");
                naturalization.setName("Naturalized");
                naturalization.setSpelling("Naturalization");
                data.add(naturalization);
                break;
            default:
                break;
        }
        return data;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\impl\BirthdayFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

public class BirthdayFormatter extends SimpleDateFormatter {

    @Override
    public String formatYear(int year) {
        return super.formatYear(year) + "[ph]";
    }

    @Override
    public String formatMonth(int month) {
        return super.formatMonth(month) + "[ph]";
    }

    @Override
    public String formatDay(int day) {
        return super.formatDay(day) + "[ph]";
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\impl\CarPlateProvider.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.LinkageProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CarPlateProvider implements LinkageProvider {
    private static final String[] ABBREVIATIONS = {
            "Province1", "Province2", "Province3", "Province4", "Province5", "Province6", "Province7", "Province8", "Province9",
            "Province10", "Province11", "Province12", "Province13", "Province14", "Province15", "Province16", "Province17", "Province18",
            "Province19", "Province20", "Province21", "Province22", "Province23", "Province24", "Province25", "Province26", "Province27",
            "Province28", "Province29", "Province30", "Province31"};

    @Override
    public boolean firstLevelVisible() {
        return true;
    }

    @Override
    public boolean thirdLevelVisible() {
        return false;
    }

    @NonNull
    @Override
    public List<String> provideFirstData() {
        List<String> provinces = new ArrayList<>();
        Collections.addAll(provinces, ABBREVIATIONS);
        return provinces;
    }

    @NonNull
    @Override
    public List<String> linkageSecondData(int firstIndex) {
        List<String> letters = new ArrayList<>();
        if (firstIndex == INDEX_NO_FOUND) {
            firstIndex = 0;
        }
        String province = provideFirstData().get(firstIndex);
        switch (province) {
            case "Province1":
                for (char i = 'A'; i <= 'M'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.add("Y");
                break;
            case "Province2":
            case "Province3":
                for (char i = 'A'; i <= 'H'; i++) {
                    letters.add(String.valueOf(i));
                }
                break;
            case "Province4":
                for (char i = 'A'; i <= 'H'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.add("J");
                letters.add("R");
                letters.add("S");
                letters.add("T");
                break;
            case "Province5":
                for (char i = 'A'; i <= 'M'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("G");
                letters.remove("I");
                break;
            case "Province6":
            case "Province7":
                for (char i = 'A'; i <= 'M'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province8":
            case "Province9":
                for (char i = 'A'; i <= 'P'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province10":
            case "Province11":
                for (char i = 'A'; i <= 'K'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province12":
            case "Province13":
                for (char i = 'A'; i <= 'R'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province14":
                for (char i = 'A'; i <= 'D'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.add("R");
                break;
            case "Province15":
                for (char i = 'A'; i <= 'N'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province16":
                for (char i = 'A'; i <= 'L'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province17":
            case "Province18":
                for (char i = 'A'; i <= 'S'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province19":
                for (char i = 'A'; i <= 'V'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                letters.add("Y");
                break;
            case "Province20":
                for (char i = 'A'; i <= 'U'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province21":
                for (char i = 'A'; i <= 'N'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                letters.add("U");
                break;
            case "Province22":
                for (char i = 'A'; i <= 'Z'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province23":
                for (char i = 'A'; i <= 'P'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                letters.add("R");
                break;
            case "Province24":
            case "Province25":
                for (char i = 'A'; i <= 'E'; i++) {
                    letters.add(String.valueOf(i));
                }
                break;
            case "Province26":
                for (char i = 'A'; i <= 'D'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("D");
                letters.remove("E");
                break;
            case "Province27":
                for (char i = 'A'; i <= 'Z'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("G");
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province28":
            case "Province29":
                for (char i = 'A'; i <= 'J'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province30":

                letters.add("A-V");
                for (char i = 'A'; i <= 'S'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("B");
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province31":
                for (char i = 'A'; i <= 'K'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.add("V");
                break;
        }
        return letters;
    }

    @NonNull
    @Override
    public List<?> linkageThirdData(int firstIndex, int secondIndex) {
        return new ArrayList<>();
    }

    @Override
    public int findFirstIndex(Object firstValue) {
        if (firstValue == null) {
            return INDEX_NO_FOUND;
        }
        for (int i = 0, n = ABBREVIATIONS.length; i < n; i++) {
            String abbreviation = ABBREVIATIONS[i];
            if (abbreviation.equals(firstValue.toString())) {
                return i;
            }
        }
        return INDEX_NO_FOUND;
    }

    @Override
    public int findSecondIndex(int firstIndex, Object secondValue) {
        if (secondValue == null) {
            return INDEX_NO_FOUND;
        }
        List<String> letters = linkageSecondData(firstIndex);
        for (int i = 0, n = letters.size(); i < n; i++) {
            String letter = letters.get(i);
            if (letter.equals(secondValue.toString())) {
                return i;
            }
        }
        return INDEX_NO_FOUND;
    }

    @Override
    public int findThirdIndex(int firstIndex, int secondIndex, Object thirdValue) {
        return 0;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\impl\SimpleDateFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;

public class SimpleDateFormatter implements DateFormatter {

    @Override
    public String formatYear(int year) {
        if (year < 1000) {
            year += 1000;
        }
        return "" + year;
    }

    @Override
    public String formatMonth(int month) {
        return month < 10 ? "0" + month : "" + month;
    }

    @Override
    public String formatDay(int day) {
        return day < 10 ? "0" + day : "" + day;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\impl\SimpleTimeFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.TimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.TimeWheelLayout;

public class SimpleTimeFormatter implements TimeFormatter {
    private final TimeWheelLayout wheelLayout;

    public SimpleTimeFormatter(TimeWheelLayout wheelLayout) {
        this.wheelLayout = wheelLayout;
    }

    @Override
    public String formatHour(int hour) {
        if (wheelLayout.isHour12Mode()) {
            if (hour == 0) {
                hour = 24;
            }
            if (hour > 12) {
                hour = hour - 12;
            }
        }
        return hour < 10 ? "0" + hour : "" + hour;
    }

    @Override
    public String formatMinute(int minute) {
        return minute < 10 ? "0" + minute : "" + minute;
    }

    @Override
    public String formatSecond(int second) {
        return second < 10 ? "0" + second : "" + second;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\impl\SimpleWheelFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;

public class SimpleWheelFormatter implements WheelFormatter {

    @Override
    public String formatItem(@NonNull Object item) {
        return item.toString();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\impl\UnitDateFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;

public class UnitDateFormatter implements DateFormatter {

    @Override
    public String formatYear(int year) {
        return year + "[ph]";
    }

    @Override
    public String formatMonth(int month) {
        return month + "[ph]";
    }

    @Override
    public String formatDay(int day) {
        return day + "[ph]";
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\impl\UnitTimeFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.TimeFormatter;

public class UnitTimeFormatter implements TimeFormatter {

    @Override
    public String formatHour(int hour) {
        return hour + "[ph]";
    }

    @Override
    public String formatMinute(int minute) {
        return minute + "[ph]";
    }

    @Override
    public String formatSecond(int second) {
        return second + "[ph]";
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\LinkagePicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.LinkageProvider;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnLinkagePickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.LinkageWheelLayout;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

@SuppressWarnings({"WeakerAccess", "unused"})
public class LinkagePicker extends ModalDialog {
    protected LinkageWheelLayout wheelLayout;
    private OnLinkagePickedListener onLinkagePickedListener;

    public LinkagePicker(@NonNull Activity activity) {
        super(activity);
    }

    public LinkagePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new LinkageWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onLinkagePickedListener != null) {
            Object first = wheelLayout.getFirstWheelView().getCurrentItem();
            Object second = wheelLayout.getSecondWheelView().getCurrentItem();
            Object third = wheelLayout.getThirdWheelView().getCurrentItem();
            onLinkagePickedListener.onLinkagePicked(first, second, third);
        }
    }

    public void setData(@NonNull LinkageProvider data) {
        wheelLayout.setData(data);
    }

    public void setDefaultValue(Object first, Object second, Object third) {
        wheelLayout.setDefaultValue(first, second, third);
    }

    public void setOnLinkagePickedListener(OnLinkagePickedListener onLinkagePickedListener) {
        this.onLinkagePickedListener = onLinkagePickedListener;
    }

    public final LinkageWheelLayout getWheelLayout() {
        return wheelLayout;
    }

    public final WheelView getFirstWheelView() {
        return wheelLayout.getFirstWheelView();
    }

    public final WheelView getSecondWheelView() {
        return wheelLayout.getSecondWheelView();
    }

    public final WheelView getThirdWheelView() {
        return wheelLayout.getThirdWheelView();
    }

    public final TextView getFirstLabelView() {
        return wheelLayout.getFirstLabelView();
    }

    public final TextView getSecondLabelView() {
        return wheelLayout.getSecondLabelView();
    }

    public final TextView getThirdLabelView() {
        return wheelLayout.getThirdLabelView();
    }

    public final ProgressBar getLoadingView() {
        return wheelLayout.getLoadingView();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\NumberPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnNumberPickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.NumberWheelLayout;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

@SuppressWarnings("unused")
public class NumberPicker extends ModalDialog {
    protected NumberWheelLayout wheelLayout;
    private OnNumberPickedListener onNumberPickedListener;

    public NumberPicker(@NonNull Activity activity) {
        super(activity);
    }

    public NumberPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new NumberWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onNumberPickedListener != null) {
            int position = wheelLayout.getWheelView().getCurrentPosition();
            Number item = wheelLayout.getWheelView().getCurrentItem();
            onNumberPickedListener.onNumberPicked(position, item);
        }
    }

    public void setFormatter(WheelFormatter formatter) {
        wheelLayout.getWheelView().setFormatter(formatter);
    }

    public void setRange(int min, int max, int step) {
        wheelLayout.setRange(min, max, step);
    }

    public void setRange(float min, float max, float step) {
        wheelLayout.setRange(min, max, step);
    }

    public void setDefaultValue(Object item) {
        wheelLayout.setDefaultValue(item);
    }

    public void setDefaultPosition(int position) {
        wheelLayout.setDefaultPosition(position);
    }

    public final void setOnNumberPickedListener(OnNumberPickedListener onNumberPickedListener) {
        this.onNumberPickedListener = onNumberPickedListener;
    }

    public final NumberWheelLayout getWheelLayout() {
        return wheelLayout;
    }

    public final WheelView getWheelView() {
        return wheelLayout.getWheelView();
    }

    public final TextView getLabelView() {
        return wheelLayout.getLabelView();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\OptionPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnOptionPickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.OptionWheelLayout;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused"})
public class OptionPicker extends ModalDialog {
    protected OptionWheelLayout wheelLayout;
    protected int defaultPosition = -1;
    private OnOptionPickedListener onOptionPickedListener;
    private boolean initialized = false;
    private List<?> data;
    private Object defaultValue;

    public OptionPicker(@NonNull Activity activity) {
        super(activity);
    }

    public OptionPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new OptionWheelLayout(activity);
        wheelLayout.setCurtainEnabled(true);
        wheelLayout.setCurtainColor(ContextCompat.getColor(getContext(), R.color.wheel_select_bg));
        wheelLayout.setSelectedTextColor(ContextCompat.getColor(getContext(), R.color.wheel_select_text));
        wheelLayout.setTextColor(ContextCompat.getColor(getContext(), R.color.wheel_unselect_text));
        return wheelLayout;
    }

    @Override
    protected void initData() {
        super.initData();
        initialized = true;
        if (data == null || data.size() == 0) {
            data = provideData();
        }
        wheelLayout.setData(data);
        if (defaultValue != null) {
            wheelLayout.setDefaultValue(defaultValue);
        }
        if (defaultPosition != -1) {
            wheelLayout.setDefaultPosition(defaultPosition);
        }
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onOptionPickedListener != null) {
            int position = wheelLayout.getWheelView().getCurrentPosition();
            Object item = wheelLayout.getWheelView().getCurrentItem();
            onOptionPickedListener.onOptionPicked(position, item);
        }
    }

    protected List<?> provideData() {
        return null;
    }

    public final boolean isInitialized() {
        return initialized;
    }

    public void setData(Object... data) {
        setData(Arrays.asList(data));
    }

    public void setData(List<?> data) {
        this.data = data;
        if (initialized) {
            wheelLayout.setData(data);
        }
    }

    public void setDefaultValue(Object item) {
        this.defaultValue = item;
        if (initialized) {
            wheelLayout.setDefaultValue(item);
        }
    }

    public void setDefaultPosition(int position) {
        this.defaultPosition = position;
        if (initialized) {
            wheelLayout.setDefaultPosition(position);
        }
    }

    public void setOnOptionPickedListener(OnOptionPickedListener onOptionPickedListener) {
        this.onOptionPickedListener = onOptionPickedListener;
    }

    public final OptionWheelLayout getWheelLayout() {
        return wheelLayout;
    }

    public final WheelView getWheelView() {
        return wheelLayout.getWheelView();
    }

    public final TextView getLabelView() {
        return wheelLayout.getLabelView();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\PhoneCodePicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.DialogLog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.PhoneCodeEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class PhoneCodePicker extends OptionPicker {
    public static String JSON = "[{\"prefix\":\"1\",\"en\":\"USA\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1\",\"en\":\"PuertoRico\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1\",\"en\":\"Canada\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"7\",\"en\":\"Russia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"7\",\"en\":\"Kazeakhstan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"20\",\"en\":\"Egypt\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"27\",\"en\":\"South Africa\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"30\",\"en\":\"Greece\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"31\",\"en\":\"Netherlands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"32\",\"en\":\"Belgium\",\"cn\":\"[CHINESE_TEXT]Hour\"},\n" +
            "{\"prefix\":\"33\",\"en\":\"France\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"34\",\"en\":\"Spain\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"36\",\"en\":\"Hungary\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"40\",\"en\":\"Romania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"41\",\"en\":\"Switzerland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"43\",\"en\":\"Austria\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"United Kingdom\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"Jersey\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"Isle of Man\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"Guernsey\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"45\",\"en\":\"Denmark\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"46\",\"en\":\"Sweden\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"47\",\"en\":\"Norway\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"48\",\"en\":\"Poland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"51\",\"en\":\"Peru\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"52\",\"en\":\"Mexico\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"53\",\"en\":\"Cuba\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"54\",\"en\":\"Argentina\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"55\",\"en\":\"Brazill\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"56\",\"en\":\"Chile\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"57\",\"en\":\"Colombia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"58\",\"en\":\"Venezuela\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"60\",\"en\":\"Malaysia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"61\",\"en\":\"Australia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"62\",\"en\":\"Indonesia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"63\",\"en\":\"Philippines\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"64\",\"en\":\"NewZealand\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"65\",\"en\":\"Singapore\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"66\",\"en\":\"Thailand\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"81\",\"en\":\"Japan\",\"cn\":\"Day[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"82\",\"en\":\"Korea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"84\",\"en\":\"Vietnam\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"86\",\"en\":\"China\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"90\",\"en\":\"Turkey\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"91\",\"en\":\"Indea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"92\",\"en\":\"Pakistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"93\",\"en\":\"Italy\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"93\",\"en\":\"Afghanistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"94\",\"en\":\"SriLanka\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"94\",\"en\":\"Germany\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"95\",\"en\":\"Myanmar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"98\",\"en\":\"Iran\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"212\",\"en\":\"Morocco\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"213\",\"en\":\"Algera\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"216\",\"en\":\"Tunisia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"218\",\"en\":\"Libya\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"220\",\"en\":\"Gambia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"221\",\"en\":\"Senegal\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"222\",\"en\":\"Mauritania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"223\",\"en\":\"Mali\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"224\",\"en\":\"Guinea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"225\",\"en\":\"Cote divoire\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"226\",\"en\":\"Burkina Faso\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"227\",\"en\":\"Niger\",\"cn\":\"[CHINESE_TEXT]Day[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"228\",\"en\":\"Togo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"229\",\"en\":\"Benin\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"230\",\"en\":\"Mauritius\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"231\",\"en\":\"Liberia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"232\",\"en\":\"Sierra Leone\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"233\",\"en\":\"Ghana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"234\",\"en\":\"Nigeria\",\"cn\":\"[CHINESE_TEXT]Day[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"235\",\"en\":\"Chad\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"236\",\"en\":\"Central African Republic\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"237\",\"en\":\"Cameroon\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"238\",\"en\":\"Cape Verde\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"239\",\"en\":\"Sao Tome and Principe\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"240\",\"en\":\"Guinea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"241\",\"en\":\"Gabon\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"242\",\"en\":\"Republic of the Congo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"243\",\"en\":\"Democratic Republic of the Congo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"244\",\"en\":\"Angola\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"247\",\"en\":\"Ascension\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"248\",\"en\":\"Seychelles\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"249\",\"en\":\"Sudan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"250\",\"en\":\"Rwanda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"251\",\"en\":\"Ethiopia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"253\",\"en\":\"Djibouti\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"254\",\"en\":\"Kenya\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"255\",\"en\":\"Tanzania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"256\",\"en\":\"Uganda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"257\",\"en\":\"Burundi\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"258\",\"en\":\"Mozambique\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"260\",\"en\":\"Zambia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"261\",\"en\":\"Madagascar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"262\",\"en\":\"Reunion\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"262\",\"en\":\"Mayotte\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"263\",\"en\":\"Zimbabwe\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"264\",\"en\":\"Namibia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"265\",\"en\":\"Malawi\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"266\",\"en\":\"Lesotho\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"267\",\"en\":\"Botwana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"268\",\"en\":\"Swaziland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"269\",\"en\":\"Comoros\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"297\",\"en\":\"Aruba\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"298\",\"en\":\"Faroe Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"299\",\"en\":\"Greenland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"350\",\"en\":\"Gibraltar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"351\",\"en\":\"Portugal\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"352\",\"en\":\"Luxembourg\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"353\",\"en\":\"Ireland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"354\",\"en\":\"Iceland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"355\",\"en\":\"Albania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"356\",\"en\":\"Malta\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"357\",\"en\":\"Cyprus\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"358\",\"en\":\"Finland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"359\",\"en\":\"Bulgaria\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"370\",\"en\":\"Lithuania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"371\",\"en\":\"Latvia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"372\",\"en\":\"Estonia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"373\",\"en\":\"Moldova\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"374\",\"en\":\"Armenia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"375\",\"en\":\"Belarus\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"376\",\"en\":\"Andorra\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"377\",\"en\":\"Monaco\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"378\",\"en\":\"San Marino\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"380\",\"en\":\"Ukraine\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"381\",\"en\":\"Serbia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"382\",\"en\":\"Montenegro\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"383\",\"en\":\"Kosovo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"385\",\"en\":\"Croatia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"386\",\"en\":\"Slovenia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"387\",\"en\":\"Bosnia and Herzegovina\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"389\",\"en\":\"Macedonia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"420\",\"en\":\"Czech Republic\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"421\",\"en\":\"Slovakia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"423\",\"en\":\"Liechtenstein\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"501\",\"en\":\"Belize\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"502\",\"en\":\"Guatemala\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"503\",\"en\":\"EISalvador\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"504\",\"en\":\"Honduras\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"505\",\"en\":\"Nicaragua\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"506\",\"en\":\"Costa Rica\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"507\",\"en\":\"Panama\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"509\",\"en\":\"Haiti\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"590\",\"en\":\"Guadeloupe\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"591\",\"en\":\"Bolivia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"592\",\"en\":\"Guyana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"593\",\"en\":\"Ecuador\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"594\",\"en\":\"French Guiana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"595\",\"en\":\"Paraguay\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"596\",\"en\":\"Martinique\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"597\",\"en\":\"Suriname\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"598\",\"en\":\"Uruguay\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"599\",\"en\":\"Netherlands Antillse\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"670\",\"en\":\"Timor Leste\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"673\",\"en\":\"Brunei\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"675\",\"en\":\"Papua New Guinea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"676\",\"en\":\"Tonga\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"678\",\"en\":\"Vanuatu\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"679\",\"en\":\"Fiji\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"682\",\"en\":\"Cook Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"684\",\"en\":\"Samoa Eastern\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"685\",\"en\":\"Samoa Western\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"687\",\"en\":\"New Caledonia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"689\",\"en\":\"French Polynesia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"852\",\"en\":\"Hong Kong\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"853\",\"en\":\"Macao\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"855\",\"en\":\"Cambodia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"856\",\"en\":\"Laos\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"880\",\"en\":\"Bangladesh\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"886\",\"en\":\"Taiwan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"960\",\"en\":\"Maldives\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"961\",\"en\":\"Lebanon\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"962\",\"en\":\"Jordan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"963\",\"en\":\"Syria\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"964\",\"en\":\"Iraq\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"965\",\"en\":\"Kuwait\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"966\",\"en\":\"Saudi Arabia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"967\",\"en\":\"Yemen\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"968\",\"en\":\"Oman\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"970\",\"en\":\"Palestinian\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"971\",\"en\":\"United Arab Emirates\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"972\",\"en\":\"Israel\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"973\",\"en\":\"Bahrain\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"974\",\"en\":\"Qotar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"975\",\"en\":\"Bhutan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"976\",\"en\":\"Mongolia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"977\",\"en\":\"Nepal\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"992\",\"en\":\"Tajikistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"993\",\"en\":\"Turkmenistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"994\",\"en\":\"Azerbaijan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"995\",\"en\":\"Georgia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"996\",\"en\":\"Kyrgyzstan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"998\",\"en\":\"Uzbekistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1242\",\"en\":\"Bahamas\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1246\",\"en\":\"Barbados\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1264\",\"en\":\"Anguilla\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1268\",\"en\":\"Antigua and Barbuda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1340\",\"en\":\"Virgin Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1345\",\"en\":\"Cayman Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1441\",\"en\":\"Bermuda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1473\",\"en\":\"Grenada\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1649\",\"en\":\"Turks and Caicos Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1664\",\"en\":\"Montserrat\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1671\",\"en\":\"Guam\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1758\",\"en\":\"St.Lucia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1767\",\"en\":\"Dominica\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1784\",\"en\":\"St.Vincent\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1809\",\"en\":\"Dominican Republic\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1868\",\"en\":\"Trinidad and Tobago\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1869\",\"en\":\"St Kitts and Nevis\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1876\",\"en\":\"Jamaica\",\"cn\":\"[CHINESE_TEXT]\"}]";
    private boolean onlyChina = false;

    public PhoneCodePicker(@NonNull Activity activity) {
        super(activity);
    }

    public PhoneCodePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    public void setOnlyChina(boolean onlyChina) {
        this.onlyChina = onlyChina;
        setData(provideData());
    }

    @Override
    public void setDefaultValue(Object item) {
        if (item instanceof String) {
            setDefaultValueByName(item.toString());
        } else {
            super.setDefaultValue(item);
        }
    }

    public void setDefaultValueByCode(String code) {
        PhoneCodeEntity entity = new PhoneCodeEntity();
        entity.setCode(code);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByName(String name) {
        PhoneCodeEntity entity = new PhoneCodeEntity();
        entity.setName(name);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByEnglish(String english) {
        PhoneCodeEntity entity = new PhoneCodeEntity();
        entity.setEnglish(english);
        super.setDefaultValue(entity);
    }

    @Override
    protected List<?> provideData() {
        List<PhoneCodeEntity> data = new ArrayList<>();
        if (onlyChina) {
            PhoneCodeEntity china = new PhoneCodeEntity();
            china.setCode("+86");
            china.setName("[CHINESE_TEXT]+86");
            china.setEnglish("Chinese Mainland");
            data.add(china);
            PhoneCodeEntity hongKong = new PhoneCodeEntity();
            hongKong.setCode("+852");
            hongKong.setName("[CHINESE_TEXT]+852");
            hongKong.setEnglish("Hong Kong");
            data.add(hongKong);
            PhoneCodeEntity macao = new PhoneCodeEntity();
            macao.setCode("+853");
            macao.setName("[CHINESE_TEXT]+853");
            macao.setEnglish("Macao");
            data.add(macao);
            PhoneCodeEntity taiwan = new PhoneCodeEntity();
            taiwan.setCode("+886");
            taiwan.setName("[CHINESE_TEXT]+886");
            taiwan.setEnglish("Taiwan");
            data.add(taiwan);
        } else {
            try {
                JSONArray jsonArray = new JSONArray(JSON);
                for (int i = 0, n = jsonArray.length(); i < n; i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    PhoneCodeEntity entity = new PhoneCodeEntity();
                    entity.setCode("+" + jsonObject.getString("prefix"));
                    entity.setName(jsonObject.getString("cn"));
                    entity.setEnglish(jsonObject.getString("en"));
                    data.add(entity);
                }
            } catch (JSONException e) {
                DialogLog.print(e);
            }
        }
        return data;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\SexPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.DialogLog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.SexEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class SexPicker extends OptionPicker {
    public static String JSON = "[{\"id\":0,\"name\":\"[TEXT]\",\"english\":\"Secrecy\"},\n" +
            "{\"id\":1,\"name\":\"[TEXT]\",\"english\":\"Male\"},\n" +
            "{\"id\":2,\"name\":\"[TEXT]\",\"english\":\"Female\"}]";
    private boolean includeSecrecy;

    public SexPicker(Activity activity) {
        super(activity);
    }

    public SexPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    public void setIncludeSecrecy(boolean includeSecrecy) {
        this.includeSecrecy = includeSecrecy;
        setData(provideData());
    }

    @Override
    public void setDefaultValue(Object item) {
        if (item instanceof String) {
            setDefaultValueByName(item.toString());
        } else {
            super.setDefaultValue(item);
        }
    }

    public void setDefaultValueByName(String name) {
        SexEntity entity = new SexEntity();
        entity.setName(name);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByEnglish(String english) {
        SexEntity entity = new SexEntity();
        entity.setEnglish(english);
        super.setDefaultValue(entity);
    }

    @Override
    protected List<?> provideData() {
        ArrayList<SexEntity> data = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            for (int i = 0, n = jsonArray.length(); i < n; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SexEntity entity = new SexEntity();
                entity.setId(jsonObject.getString("id"));
                entity.setName(jsonObject.getString("name"));
                entity.setEnglish(jsonObject.getString("english"));
                if (!includeSecrecy && "0".equals(entity.getId())) {
                    continue;
                }
                data.add(entity);
            }
        } catch (JSONException e) {
            DialogLog.print(e);
        }
        return data;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\StrArrayPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class StrArrayPicker extends OptionPicker {

    @NonNull
    private final List<String> optionList;

    public StrArrayPicker(Activity activity, @NonNull String[] optionArray, int defaultPosition) {
        super(activity);
        this.optionList = Arrays.asList(optionArray);
        this.defaultPosition = defaultPosition;
    }

    @Override
    protected List<?> provideData() {
        return optionList;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\TimePicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnTimeMeridiemPickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnTimePickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.TimeWheelLayout;

@SuppressWarnings("unused")
public class TimePicker extends ModalDialog {
    protected TimeWheelLayout wheelLayout;
    private OnTimePickedListener onTimePickedListener;
    private OnTimeMeridiemPickedListener onTimeMeridiemPickedListener;

    public TimePicker(@NonNull Activity activity) {
        super(activity);
    }

    public TimePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new TimeWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        int hour = wheelLayout.getSelectedHour();
        int minute = wheelLayout.getSelectedMinute();
        int second = wheelLayout.getSelectedSecond();
        if (onTimePickedListener != null) {
            onTimePickedListener.onTimePicked(hour, minute, second);
        }
        if (onTimeMeridiemPickedListener != null) {
            onTimeMeridiemPickedListener.onTimePicked(hour, minute, second, wheelLayout.isAnteMeridiem());
        }
    }

    public void setOnTimePickedListener(OnTimePickedListener onTimePickedListener) {
        this.onTimePickedListener = onTimePickedListener;
    }

    public void setOnTimeMeridiemPickedListener(OnTimeMeridiemPickedListener onTimeMeridiemPickedListener) {
        this.onTimeMeridiemPickedListener = onTimeMeridiemPickedListener;
    }

    public final TimeWheelLayout getWheelLayout() {
        return wheelLayout;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\BaseWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.*;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.DialogLog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.OnWheelChangedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class BaseWheelLayout extends LinearLayout implements OnWheelChangedListener {
    private final List<WheelView> wheelViews = new ArrayList<>();
    private AttributeSet attrs;

    public BaseWheelLayout(Context context) {
        super(context);
        init(context, null);
        TypedArray a = context.obtainStyledAttributes(null, provideStyleableRes(),
                R.attr.WheelStyle, R.style.WheelDefault);
        onAttributeSet(context, a);
    }

    public BaseWheelLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, provideStyleableRes(),
                R.attr.WheelStyle, R.style.WheelDefault);
        onAttributeSet(context, a);
        a.recycle();
    }

    public BaseWheelLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, provideStyleableRes(),
                defStyleAttr, R.style.WheelDefault);
        onAttributeSet(context, a);
        a.recycle();
    }

    public BaseWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, provideStyleableRes(),
                defStyleAttr, defStyleRes);
        onAttributeSet(context, a);
        a.recycle();
    }

    private void init(Context context, AttributeSet attrs) {
        this.attrs = attrs;
        setOrientation(VERTICAL);
        inflate(context, provideLayoutRes(), this);
        onInit(context);
        wheelViews.addAll(provideWheelViews());
        for (WheelView wheelView : wheelViews) {
            wheelView.setOnWheelChangedListener(this);
        }
    }

    protected void onInit(@NonNull Context context) {

    }

    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {

    }

    @LayoutRes
    protected abstract int provideLayoutRes();

    @StyleableRes
    protected abstract int[] provideStyleableRes();

    protected abstract List<WheelView> provideWheelViews();

    public void setStyle(@StyleRes int style) {
        if (attrs == null) {
            DialogLog.print("Please use " + getClass().getSimpleName() + " in xml");
            return;
        }
        TypedArray a = getContext().obtainStyledAttributes(attrs, provideStyleableRes(), R.attr.WheelStyle, style);
        onAttributeSet(getContext(), a);
        a.recycle();
        requestLayout();
        invalidate();
    }

    @Override
    public void onWheelScrolled(WheelView view, int offset) {

    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {

    }

    @Override
    public void onWheelLoopFinished(WheelView view) {

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (WheelView wheelView : wheelViews) {
            wheelView.setEnabled(enabled);
        }
    }

    public void setVisibleItemCount(int visibleItemCount) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setVisibleItemCount(visibleItemCount);
        }
    }

    public void setItemSpace(@Px int space) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setItemSpace(space);
        }
    }

    public void setSameWidthEnabled(boolean sameWidthEnabled) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSameWidthEnabled(sameWidthEnabled);
        }
    }

    public void setDefaultItemPosition(int position) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setDefaultPosition(position);
        }
    }

    public void setCurtainEnabled(boolean hasCurtain) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainEnabled(hasCurtain);
        }
    }

    public void setCurtainColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainColor(color);
        }
    }

    public void setCurtainCorner(@CurtainCorner int corner) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainCorner(corner);
        }
    }

    public void setCurtainRadius(@Px float radius) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurtainRadius(radius);
        }
    }

    public void setAtmosphericEnabled(boolean hasAtmospheric) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setAtmosphericEnabled(hasAtmospheric);
        }
    }

    public void setCurvedEnabled(boolean curved) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurvedEnabled(curved);
        }
    }

    public void setCurvedMaxAngle(int curvedMaxAngle) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurvedMaxAngle(curvedMaxAngle);
        }
    }

    public void setCurvedIndicatorSpace(@Px int space) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCurvedIndicatorSpace(space);
        }
    }

    public void setCyclicEnabled(boolean cyclic) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setCyclicEnabled(cyclic);
        }
    }

    public void setIndicatorEnabled(boolean hasIndicator) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setIndicatorEnabled(hasIndicator);
        }
    }

    public void setIndicatorSize(@Px float size) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setIndicatorSize(size);
        }
    }

    public void setIndicatorColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setIndicatorColor(color);
        }
    }

    public void setMaxWidthText(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        for (WheelView wheelView : wheelViews) {
            wheelView.setMaxWidthText(text);
        }
    }

    public void setTextSize(@Px float textSize) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setTextSize(textSize);
        }
    }

    public void setSelectedTextSize(@Px float textSize) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSelectedTextSize(textSize);
        }
    }

    public void setTextColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setTextColor(color);
        }
    }

    public void setSelectedTextColor(@ColorInt int color) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSelectedTextColor(color);
        }
    }

    public void setSelectedTextBold(boolean bold) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setSelectedTextBold(bold);
        }
    }

    public void setTextAlign(@ItemTextAlign int align) {
        for (WheelView wheelView : wheelViews) {
            wheelView.setTextAlign(align);
        }
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\CarPlateWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.CarPlateProvider;

public class CarPlateWheelLayout extends LinkageWheelLayout {
    private CarPlateProvider provider;

    public CarPlateWheelLayout(Context context) {
        super(context);
    }

    public CarPlateWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarPlateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarPlateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        super.onInit(context);
        provider = new CarPlateProvider();
        setData(provider);
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        super.onAttributeSet(context, typedArray);
        setFirstVisible(provider.firstLevelVisible());
        setThirdVisible(provider.thirdLevelVisible());
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\DateWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnDateSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DateEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleDateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class DateWheelLayout extends BaseWheelLayout {
    private NumberWheelView yearWheelView;
    private NumberWheelView monthWheelView;
    private NumberWheelView dayWheelView;
    private TextView yearLabelView;
    private TextView monthLabelView;
    private TextView dayLabelView;
    private TextView spaceStartView;
    private TextView spaceEndView;
    private DateEntity startValue;
    private DateEntity endValue;
    private Integer selectedYear;
    private Integer selectedMonth;
    private Integer selectedDay;
    private OnDateSelectedListener onDateSelectedListener;
    private boolean resetWhenLinkage = true;

    public DateWheelLayout(Context context) {
        super(context);
    }

    public DateWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DateWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_date;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.DateWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(yearWheelView, monthWheelView, dayWheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        yearWheelView = findViewById(R.id.wheel_picker_date_year_wheel);
        monthWheelView = findViewById(R.id.wheel_picker_date_month_wheel);
        dayWheelView = findViewById(R.id.wheel_picker_date_day_wheel);
        yearLabelView = findViewById(R.id.wheel_picker_date_year_label);
        monthLabelView = findViewById(R.id.wheel_picker_date_month_label);
        dayLabelView = findViewById(R.id.wheel_picker_date_day_label);
        spaceStartView = findViewById(R.id.wheel_picker_date_start_view);
        spaceEndView = findViewById(R.id.wheel_picker_date_end_view);

        post(new Runnable() {
            @Override
            public void run() {
                yearLabelView.setHeight(monthWheelView.itemHeight);
                monthLabelView.setHeight(monthWheelView.itemHeight);
                dayLabelView.setHeight(monthWheelView.itemHeight);
                spaceStartView.setHeight(monthWheelView.itemHeight);
                spaceEndView.setHeight(monthWheelView.itemHeight);
            }
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.DateWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.DateWheelLayout_wheel_maxWidthText));
        setTextColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextSize(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.DateWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.DateWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.DateWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.DateWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.DateWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.DateWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.DateWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.DateWheelLayout_wheel_curvedMaxAngle, 90));
        setDateMode(typedArray.getInt(R.styleable.DateWheelLayout_wheel_dateMode, DateMode.YEAR_MONTH_DAY));
        String yearLabel = typedArray.getString(R.styleable.DateWheelLayout_wheel_yearLabel);
        String monthLabel = typedArray.getString(R.styleable.DateWheelLayout_wheel_monthLabel);
        String dayLabel = typedArray.getString(R.styleable.DateWheelLayout_wheel_dayLabel);
        setDateLabel(yearLabel, monthLabel, dayLabel);
        setDateFormatter(new SimpleDateFormatter());
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(DateEntity.today(), DateEntity.yearOnFuture(30), DateEntity.today());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_date_year_wheel) {
            selectedYear = yearWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedMonth = null;
                selectedDay = null;
            }
            changeMonth(selectedYear);
            dateSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_date_month_wheel) {
            selectedMonth = monthWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedDay = null;
            }
            changeDay(selectedYear, selectedMonth);
            dateSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_date_day_wheel) {
            selectedDay = dayWheelView.getItem(position);
            dateSelectedCallback();
        }
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        int id = view.getId();
        if (id == R.id.wheel_picker_date_year_wheel) {
            monthWheelView.setEnabled(state == ScrollState.IDLE);
            dayWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_date_month_wheel) {
            yearWheelView.setEnabled(state == ScrollState.IDLE);
            dayWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_date_day_wheel) {
            yearWheelView.setEnabled(state == ScrollState.IDLE);
            monthWheelView.setEnabled(state == ScrollState.IDLE);
        }
    }

    private void dateSelectedCallback() {
        if (onDateSelectedListener == null) {
            return;
        }
        dayWheelView.post(new Runnable() {
            @Override
            public void run() {
                onDateSelectedListener.onDateSelected(selectedYear, selectedMonth, selectedDay);
            }
        });
    }

    public void setDateMode(@DateMode int dateMode) {
        yearWheelView.setVisibility(View.VISIBLE);
        yearLabelView.setVisibility(View.VISIBLE);
        monthWheelView.setVisibility(View.VISIBLE);
        monthLabelView.setVisibility(View.VISIBLE);
        dayWheelView.setVisibility(View.VISIBLE);
        dayLabelView.setVisibility(View.VISIBLE);
        if (dateMode == DateMode.NONE) {
            yearWheelView.setVisibility(View.GONE);
            yearLabelView.setVisibility(View.GONE);
            monthWheelView.setVisibility(View.GONE);
            monthLabelView.setVisibility(View.GONE);
            dayWheelView.setVisibility(View.GONE);
            dayLabelView.setVisibility(View.GONE);
            return;
        }
        if (dateMode == DateMode.MONTH_DAY) {
            yearWheelView.setVisibility(View.GONE);
            yearLabelView.setVisibility(View.GONE);
            return;
        }
        if (dateMode == DateMode.YEAR_MONTH) {
            dayWheelView.setVisibility(View.GONE);
            dayLabelView.setVisibility(View.GONE);
        }
        if (dateMode == DateMode.YEAR) {
            yearLabelView.setVisibility(View.GONE);
            monthWheelView.setVisibility(View.GONE);
            monthLabelView.setVisibility(View.GONE);
            dayWheelView.setVisibility(View.GONE);
            dayLabelView.setVisibility(View.GONE);
        }
    }

    public void setRange(DateEntity startValue, DateEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(DateEntity startValue, DateEntity endValue, DateEntity defaultValue) {
        if (startValue == null) {
            startValue = DateEntity.today();
        }
        if (endValue == null) {
            endValue = DateEntity.yearOnFuture(30);
        }
        if (endValue.toTimeInMillis() < startValue.toTimeInMillis()) {
            throw new IllegalArgumentException("Ensure the start date is less than the end date");
        }
        this.startValue = startValue;
        this.endValue = endValue;
        if (defaultValue != null) {
            selectedYear = defaultValue.getYear();
            selectedMonth = defaultValue.getMonth();
            selectedDay = defaultValue.getDay();
        } else {
            selectedYear = null;
            selectedMonth = null;
            selectedDay = null;
        }
        changeYear();
    }

    public void setDefaultValue(DateEntity defaultValue) {
        setRange(startValue, endValue, defaultValue);
    }

    public void setDateFormatter(final DateFormatter dateFormatter) {
        if (dateFormatter == null) {
            return;
        }
        yearWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return dateFormatter.formatYear((Integer) value);
            }
        });
        monthWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return dateFormatter.formatMonth((Integer) value);
            }
        });
        dayWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return dateFormatter.formatDay((Integer) value);
            }
        });
    }

    public void setDateLabel(CharSequence year, CharSequence month, CharSequence day) {
        yearLabelView.setText(year);
        monthLabelView.setText(month);
        dayLabelView.setText(day);

    }

    public void setOnDateSelectedListener(OnDateSelectedListener onDateSelectedListener) {
        this.onDateSelectedListener = onDateSelectedListener;
    }

    public void setResetWhenLinkage(boolean resetWhenLinkage) {
        this.resetWhenLinkage = resetWhenLinkage;
    }

    public final DateEntity getStartValue() {
        return startValue;
    }

    public final DateEntity getEndValue() {
        return endValue;
    }

    public final NumberWheelView getYearWheelView() {
        return yearWheelView;
    }

    public final NumberWheelView getMonthWheelView() {
        return monthWheelView;
    }

    public final NumberWheelView getDayWheelView() {
        return dayWheelView;
    }

    public final TextView getYearLabelView() {
        return yearLabelView;
    }

    public final TextView getMonthLabelView() {
        return monthLabelView;
    }

    public final TextView getDayLabelView() {
        return dayLabelView;
    }

    public final TextView getSpaceStartView() {
        return spaceStartView;
    }

    public final TextView getSpaceEndView() {
        return spaceEndView;
    }

    public final int getSelectedYear() {
        return yearWheelView.getCurrentItem();
    }

    public final int getSelectedMonth() {
        return monthWheelView.getCurrentItem();
    }

    public final int getSelectedDay() {
        return dayWheelView.getCurrentItem();
    }

    private void changeYear() {
        final int min = Math.min(startValue.getYear(), endValue.getYear());
        final int max = Math.max(startValue.getYear(), endValue.getYear());
        if (selectedYear == null) {
            selectedYear = min;
        } else {
            selectedYear = Math.max(selectedYear, min);
            selectedYear = Math.min(selectedYear, max);
        }
        yearWheelView.setRange(min, max, 1);
        yearWheelView.setDefaultValue(selectedYear);
        changeMonth(selectedYear);
    }

    private void changeMonth(int year) {
        final int min, max;

        if (startValue.getYear() == endValue.getYear()) {
            min = Math.min(startValue.getMonth(), endValue.getMonth());
            max = Math.max(startValue.getMonth(), endValue.getMonth());
        } else if (year == startValue.getYear()) {
            min = startValue.getMonth();
            max = 12;
        } else if (year == endValue.getYear()) {
            min = 1;
            max = endValue.getMonth();
        } else {
            min = 1;
            max = 12;
        }
        if (selectedMonth == null) {
            selectedMonth = min;
        } else {
            selectedMonth = Math.max(selectedMonth, min);
            selectedMonth = Math.min(selectedMonth, max);
        }
        monthWheelView.setRange(min, max, 1);
        monthWheelView.setDefaultValue(selectedMonth);
        changeDay(year, selectedMonth);
    }

    private void changeDay(int year, int month) {
        final int min, max;

        if (year == startValue.getYear() && month == startValue.getMonth()
                && year == endValue.getYear() && month == endValue.getMonth()) {
            min = startValue.getDay();
            max = endValue.getDay();
        } else if (year == startValue.getYear() && month == startValue.getMonth()) {
            min = startValue.getDay();
            max = getTotalDaysInMonth(year, month);
        } else if (year == endValue.getYear() && month == endValue.getMonth()) {
            min = 1;
            max = endValue.getDay();
        } else {
            min = 1;
            max = getTotalDaysInMonth(year, month);
        }
        if (selectedDay == null) {
            selectedDay = min;
        } else {
            selectedDay = Math.max(selectedDay, min);
            selectedDay = Math.min(selectedDay, max);
        }
        dayWheelView.setRange(min, max, 1);
        dayWheelView.setDefaultValue(selectedDay);
    }

    private int getTotalDaysInMonth(int year, int month) {
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:

                return 31;
            case 4:
            case 6:
            case 9:
            case 11:

                return 30;
            case 2:

                if (year <= 0) {
                    return 29;
                }

                boolean isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
                if (isLeap) {
                    return 29;
                } else {
                    return 28;
                }
            default:
                return 30;
        }
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\DatimeWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.TimeMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnDatimeSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.TimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DatimeEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleDateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleTimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class DatimeWheelLayout extends BaseWheelLayout {
    private DateWheelLayout dateWheelLayout;
    private TimeWheelLayout timeWheelLayout;
    private DatimeEntity startValue;
    private DatimeEntity endValue;
    private OnDatimeSelectedListener onDatimeSelectedListener;

    public DatimeWheelLayout(Context context) {
        super(context);
    }

    public DatimeWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DatimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DatimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_datime;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.DatimeWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        List<WheelView> list = new ArrayList<>();
        list.addAll(dateWheelLayout.provideWheelViews());
        list.addAll(timeWheelLayout.provideWheelViews());
        return list;
    }

    @Override
    protected void onInit(@NonNull Context context) {
        dateWheelLayout = findViewById(R.id.wheel_picker_date_wheel);
        timeWheelLayout = findViewById(R.id.wheel_picker_time_wheel);

        setCurtainEnabled(true);
        getMonthLabelView().setTextColor(0xffffffff);
        getYearLabelView().setTextColor(0xffffffff);
        getDayLabelView().setTextColor(0xffffffff);
        getHourLabelView().setTextColor(0xffffffff);
        getMinuteLabelView().setTextColor(0xffffffff);
        getSecondLabelView().setTextColor(0xffffffff);

        post(() -> {
            View view_select_bg = findViewById(R.id.view_select_bg);
            ViewGroup.LayoutParams params = view_select_bg.getLayoutParams();
            params.height = dateWheelLayout.getYearWheelView().itemHeight;
            view_select_bg.setLayoutParams(params);
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.DatimeWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColor, 0xFF888888));
        setTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_itemSpace,
                (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.DatimeWheelLayout_wheel_curvedMaxAngle, 90));
        setDateMode(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_dateMode, DateMode.YEAR_MONTH_DAY));
        setTimeMode(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_timeMode, TimeMode.HOUR_24_NO_SECOND));
        String yearLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_yearLabel);
        String monthLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_monthLabel);
        String dayLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_dayLabel);
        setDateLabel(yearLabel, monthLabel, dayLabel);
        String hourLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_hourLabel);
        String minuteLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_minuteLabel);
        String secondLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_secondLabel);
        setTimeLabel(hourLabel, minuteLabel, secondLabel);
        setDateFormatter(new SimpleDateFormatter());
        setTimeFormatter(new SimpleTimeFormatter(timeWheelLayout));
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(DatimeEntity.now(), DatimeEntity.yearOnFuture(30), DatimeEntity.now());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        dateWheelLayout.onWheelSelected(view, position);
        timeWheelLayout.onWheelSelected(view, position);
        if (onDatimeSelectedListener == null) {
            return;
        }
        timeWheelLayout.post(new Runnable() {
            @Override
            public void run() {
                onDatimeSelectedListener.onDatimeSelected(dateWheelLayout.getSelectedYear(),
                        dateWheelLayout.getSelectedMonth(), dateWheelLayout.getSelectedDay(),
                        timeWheelLayout.getSelectedHour(), timeWheelLayout.getSelectedMinute(),
                        timeWheelLayout.getSelectedSecond());
            }
        });
    }

    @Override
    public void onWheelScrolled(WheelView view, int offset) {
        dateWheelLayout.onWheelScrolled(view, offset);
        timeWheelLayout.onWheelScrolled(view, offset);
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        dateWheelLayout.onWheelScrollStateChanged(view, state);
        timeWheelLayout.onWheelScrollStateChanged(view, state);
    }

    @Override
    public void onWheelLoopFinished(WheelView view) {
        dateWheelLayout.onWheelLoopFinished(view);
        timeWheelLayout.onWheelLoopFinished(view);
    }

    public void setDateMode(@DateMode int dateMode) {
        dateWheelLayout.setDateMode(dateMode);
    }

    public void setTimeMode(@TimeMode int timeMode) {
        timeWheelLayout.setTimeMode(timeMode);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue, DatimeEntity defaultValue) {
        if (startValue == null) {
            startValue = DatimeEntity.now();
        }
        if (endValue == null) {
            endValue = DatimeEntity.yearOnFuture(10);
        }
        if (defaultValue == null) {
            defaultValue = startValue;
        }
        dateWheelLayout.setRange(startValue.getDate(), endValue.getDate(), defaultValue.getDate());
        timeWheelLayout.setRange(null, null, defaultValue.getTime());
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public void setDefaultValue(DatimeEntity defaultValue) {
        if (defaultValue == null) {
            defaultValue = DatimeEntity.now();
        }
        dateWheelLayout.setDefaultValue(defaultValue.getDate());
        timeWheelLayout.setDefaultValue(defaultValue.getTime());
    }

    public void setDateFormatter(DateFormatter dateFormatter) {
        dateWheelLayout.setDateFormatter(dateFormatter);
    }

    public void setTimeFormatter(TimeFormatter timeFormatter) {
        timeWheelLayout.setTimeFormatter(timeFormatter);
    }

    public void setDateLabel(CharSequence year, CharSequence month, CharSequence day) {
        dateWheelLayout.setDateLabel(year, month, day);
    }

    public void setTimeLabel(CharSequence hour, CharSequence minute, CharSequence second) {
        timeWheelLayout.setTimeLabel(hour, minute, second);
    }

    public void setOnDatimeSelectedListener(OnDatimeSelectedListener onDatimeSelectedListener) {
        this.onDatimeSelectedListener = onDatimeSelectedListener;
    }

    public void setResetWhenLinkage(boolean dateResetWhenLinkage, boolean timeResetWhenLinkage) {
        dateWheelLayout.setResetWhenLinkage(dateResetWhenLinkage);
        timeWheelLayout.setResetWhenLinkage(timeResetWhenLinkage);
    }

    public final DatimeEntity getStartValue() {
        return startValue;
    }

    public final DatimeEntity getEndValue() {
        return endValue;
    }

    public final DateWheelLayout getDateWheelLayout() {
        return dateWheelLayout;
    }

    public final TimeWheelLayout getTimeWheelLayout() {
        return timeWheelLayout;
    }

    public final NumberWheelView getYearWheelView() {
        return dateWheelLayout.getYearWheelView();
    }

    public final NumberWheelView getMonthWheelView() {
        return dateWheelLayout.getMonthWheelView();
    }

    public final NumberWheelView getDayWheelView() {
        return dateWheelLayout.getDayWheelView();
    }

    public final NumberWheelView getHourWheelView() {
        return timeWheelLayout.getHourWheelView();
    }

    public final NumberWheelView getMinuteWheelView() {
        return timeWheelLayout.getMinuteWheelView();
    }

    public final NumberWheelView getSecondWheelView() {
        return timeWheelLayout.getSecondWheelView();
    }

    public final WheelView getMeridiemWheelView() {
        return timeWheelLayout.getMeridiemWheelView();
    }

    public final TextView getYearLabelView() {
        return dateWheelLayout.getYearLabelView();
    }

    public final TextView getMonthLabelView() {
        return dateWheelLayout.getMonthLabelView();
    }

    public final TextView getDayLabelView() {
        return dateWheelLayout.getDayLabelView();
    }

    public final TextView getHourLabelView() {
        return timeWheelLayout.getHourLabelView();
    }

    public final TextView getMinuteLabelView() {
        return timeWheelLayout.getMinuteLabelView();
    }

    public final TextView getSecondLabelView() {
        return timeWheelLayout.getSecondLabelView();
    }

    public final TextView getSpaceStartView() {
        return dateWheelLayout.getSpaceStartView();
    }

    public final TextView getSpaceEndView() {
        return dateWheelLayout.getSpaceEndView();
    }

    public final int getSelectedYear() {
        return dateWheelLayout.getSelectedYear();
    }

    public final int getSelectedMonth() {
        return dateWheelLayout.getSelectedMonth();
    }

    public final int getSelectedDay() {
        return dateWheelLayout.getSelectedDay();
    }

    public final int getSelectedHour() {
        return timeWheelLayout.getSelectedHour();
    }

    public final int getSelectedMinute() {
        return timeWheelLayout.getSelectedMinute();
    }

    public final int getSelectedSecond() {
        return timeWheelLayout.getSelectedSecond();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\LinkageWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.LinkageProvider;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnLinkageSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class LinkageWheelLayout extends BaseWheelLayout {
    private WheelView firstWheelView, secondWheelView, thirdWheelView;
    private TextView firstLabelView, secondLabelView, thirdLabelView;
    private ProgressBar loadingView;
    private Object firstValue, secondValue, thirdValue;
    private int firstIndex, secondIndex, thirdIndex;
    private LinkageProvider dataProvider;
    private OnLinkageSelectedListener onLinkageSelectedListener;

    public LinkageWheelLayout(Context context) {
        super(context);
    }

    public LinkageWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkageWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LinkageWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_linkage;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.LinkageWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(firstWheelView, secondWheelView, thirdWheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        firstWheelView = findViewById(R.id.wheel_picker_linkage_first_wheel);
        secondWheelView = findViewById(R.id.wheel_picker_linkage_second_wheel);
        thirdWheelView = findViewById(R.id.wheel_picker_linkage_third_wheel);
        firstLabelView = findViewById(R.id.wheel_picker_linkage_first_label);
        secondLabelView = findViewById(R.id.wheel_picker_linkage_second_label);
        thirdLabelView = findViewById(R.id.wheel_picker_linkage_third_label);
        loadingView = findViewById(R.id.wheel_picker_linkage_loading);
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.LinkageWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.LinkageWheelLayout_wheel_maxWidthText));
        setTextSize(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setTextColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.LinkageWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.LinkageWheelLayout_wheel_itemSpace,
                (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.LinkageWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.LinkageWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.LinkageWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.LinkageWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.LinkageWheelLayout_wheel_curvedMaxAngle, 90));
        setFirstVisible(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_firstVisible, true));
        setThirdVisible(typedArray.getBoolean(R.styleable.LinkageWheelLayout_wheel_thirdVisible, true));
        String firstLabel = typedArray.getString(R.styleable.LinkageWheelLayout_wheel_firstLabel);
        String secondLabel = typedArray.getString(R.styleable.LinkageWheelLayout_wheel_secondLabel);
        String thirdLabel = typedArray.getString(R.styleable.LinkageWheelLayout_wheel_thirdLabel);
        setLabel(firstLabel, secondLabel, thirdLabel);
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_linkage_first_wheel) {
            firstIndex = position;
            secondIndex = 0;
            thirdIndex = 0;
            changeSecondData();
            changeThirdData();
            selectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_linkage_second_wheel) {
            secondIndex = position;
            thirdIndex = 0;
            changeThirdData();
            selectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_linkage_third_wheel) {
            thirdIndex = position;
            selectedCallback();
        }
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        int id = view.getId();
        if (id == R.id.wheel_picker_linkage_first_wheel) {
            secondWheelView.setEnabled(state == ScrollState.IDLE);
            thirdWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_linkage_second_wheel) {
            firstWheelView.setEnabled(state == ScrollState.IDLE);
            thirdWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_linkage_third_wheel) {
            firstWheelView.setEnabled(state == ScrollState.IDLE);
            secondWheelView.setEnabled(state == ScrollState.IDLE);
        }
    }

    public void setData(@NonNull LinkageProvider provider) {
        setFirstVisible(provider.firstLevelVisible());
        setThirdVisible(provider.thirdLevelVisible());
        if (firstValue != null) {
            firstIndex = provider.findFirstIndex(firstValue);
        }
        if (secondValue != null) {
            secondIndex = provider.findSecondIndex(firstIndex, secondValue);
        }
        if (thirdValue != null) {
            thirdIndex = provider.findThirdIndex(firstIndex, secondIndex, thirdValue);
        }
        dataProvider = provider;
        changeFirstData();
        changeSecondData();
        changeThirdData();
    }

    public void setDefaultValue(Object first, Object second, Object third) {
        if (dataProvider != null) {
            firstIndex = dataProvider.findFirstIndex(first);
            secondIndex = dataProvider.findSecondIndex(firstIndex, second);
            thirdIndex = dataProvider.findThirdIndex(firstIndex, secondIndex, third);
            changeFirstData();
            changeSecondData();
            changeThirdData();
        } else {
            this.firstValue = first;
            this.secondValue = second;
            this.thirdValue = third;
        }
    }

    public void setFormatter(WheelFormatter first, WheelFormatter second, WheelFormatter third) {
        firstWheelView.setFormatter(first);
        secondWheelView.setFormatter(second);
        thirdWheelView.setFormatter(third);
    }

    public void setLabel(CharSequence first, CharSequence second, CharSequence third) {
        firstLabelView.setText(first);
        secondLabelView.setText(second);
        thirdLabelView.setText(third);
    }

    public void showLoading() {
        loadingView.setVisibility(VISIBLE);
    }

    public void hideLoading() {
        loadingView.setVisibility(GONE);
    }

    public void setOnLinkageSelectedListener(OnLinkageSelectedListener onLinkageSelectedListener) {
        this.onLinkageSelectedListener = onLinkageSelectedListener;
    }

    public void setFirstVisible(boolean visible) {
        if (visible) {
            firstWheelView.setVisibility(VISIBLE);
            firstLabelView.setVisibility(VISIBLE);
        } else {
            firstWheelView.setVisibility(GONE);
            firstLabelView.setVisibility(GONE);
        }
    }

    public void setThirdVisible(boolean visible) {
        if (visible) {
            thirdWheelView.setVisibility(VISIBLE);
            thirdLabelView.setVisibility(VISIBLE);
        } else {
            thirdWheelView.setVisibility(GONE);
            thirdLabelView.setVisibility(GONE);
        }
    }

    private void selectedCallback() {
        if (onLinkageSelectedListener == null) {
            return;
        }
        thirdWheelView.post(new Runnable() {
            @Override
            public void run() {
                Object first = firstWheelView.getCurrentItem();
                Object second = secondWheelView.getCurrentItem();
                Object third = thirdWheelView.getCurrentItem();
                onLinkageSelectedListener.onLinkageSelected(first, second, third);
            }
        });
    }

    private void changeFirstData() {
        firstWheelView.setData(dataProvider.provideFirstData());
        firstWheelView.setDefaultPosition(firstIndex);
    }

    private void changeSecondData() {
        secondWheelView.setData(dataProvider.linkageSecondData(firstIndex));
        secondWheelView.setDefaultPosition(secondIndex);
    }

    private void changeThirdData() {
        if (!dataProvider.thirdLevelVisible()) {
            return;
        }
        thirdWheelView.setData(dataProvider.linkageThirdData(firstIndex, secondIndex));
        thirdWheelView.setDefaultPosition(thirdIndex);
    }

    public final WheelView getFirstWheelView() {
        return firstWheelView;
    }

    public final WheelView getSecondWheelView() {
        return secondWheelView;
    }

    public final WheelView getThirdWheelView() {
        return thirdWheelView;
    }

    public final TextView getFirstLabelView() {
        return firstLabelView;
    }

    public final TextView getSecondLabelView() {
        return secondLabelView;
    }

    public final TextView getThirdLabelView() {
        return thirdLabelView;
    }

    public final ProgressBar getLoadingView() {
        return loadingView;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\NumberWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnNumberSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnOptionSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

public class NumberWheelLayout extends OptionWheelLayout {
    private OnNumberSelectedListener onNumberSelectedListener;

    public NumberWheelLayout(Context context) {
        super(context);
    }

    public NumberWheelLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberWheelLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NumberWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.NumberWheelLayout;
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.NumberWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.NumberWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_itemTextColor, 0xFF888888));
        setTextSize(typedArray.getDimension(R.styleable.NumberWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.NumberWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.NumberWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.NumberWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimensionPixelSize(R.styleable.NumberWheelLayout_wheel_indicatorSize, (int) (1 * density)));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.NumberWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.NumberWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.NumberWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.NumberWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.NumberWheelLayout_wheel_curvedMaxAngle, 90));
        getLabelView().setText(typedArray.getString(R.styleable.NumberWheelLayout_wheel_label));
        float minNumber = typedArray.getFloat(R.styleable.NumberWheelLayout_wheel_minNumber, 0);
        float maxNumber = typedArray.getFloat(R.styleable.NumberWheelLayout_wheel_maxNumber, 10);
        float stepNumber = typedArray.getFloat(R.styleable.NumberWheelLayout_wheel_stepNumber, 1);
        boolean isDecimal = typedArray.getBoolean(R.styleable.NumberWheelLayout_wheel_isDecimal, false);
        if (isDecimal) {
            setRange(minNumber, maxNumber, stepNumber);
        } else {
            setRange((int) minNumber, (int) maxNumber, (int) stepNumber);
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        if (onNumberSelectedListener != null) {
            Object item = getWheelView().getItem(position);
            onNumberSelectedListener.onNumberSelected(position, (Number) item);
        }
    }

    @Deprecated
    @Override
    public void setData(List<?> data) {
        throw new UnsupportedOperationException("Use setRange instead");
    }

    @Deprecated
    @Override
    public void setOnOptionSelectedListener(OnOptionSelectedListener onOptionSelectedListener) {
        throw new UnsupportedOperationException("Use setOnNumberSelectedListener instead");
    }

    public void setOnNumberSelectedListener(OnNumberSelectedListener onNumberSelectedListener) {
        this.onNumberSelectedListener = onNumberSelectedListener;
    }

    public void setRange(int min, int max, int step) {
        int minValue = Math.min(min, max);
        int maxValue = Math.max(min, max);

        int capacity = (maxValue - minValue) / step;
        List<Integer> data = new ArrayList<>(capacity);
        for (int i = minValue; i <= maxValue; i = i + step) {
            data.add(i);
        }
        super.setData(data);
    }

    public void setRange(float min, float max, float step) {
        float minValue = Math.min(min, max);
        float maxValue = Math.max(min, max);

        int capacity = (int) ((maxValue - minValue) / step);
        List<Float> data = new ArrayList<>(capacity);
        for (float i = minValue; i <= maxValue; i = i + step) {
            data.add(i);
        }
        super.setData(data);
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\OptionWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnOptionSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class OptionWheelLayout extends BaseWheelLayout {
    private WheelView wheelView;
    private TextView labelView;
    private OnOptionSelectedListener onOptionSelectedListener;

    public OptionWheelLayout(Context context) {
        super(context);
    }

    public OptionWheelLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OptionWheelLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OptionWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_option;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.OptionWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Collections.singletonList(wheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        wheelView = findViewById(R.id.wheel_picker_option_wheel);
        labelView = findViewById(R.id.wheel_picker_option_label);

        post(() -> {
            View view_select_bg = findViewById(R.id.view_select_bg);
            ViewGroup.LayoutParams params = view_select_bg.getLayoutParams();
            params.height = wheelView.itemHeight;
            view_select_bg.setLayoutParams(params);
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.OptionWheelLayout_wheel_maxWidthText));
        setTextColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextSize(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.OptionWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.OptionWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.OptionWheelLayout_wheel_curvedMaxAngle, 90));
        labelView.setText(typedArray.getString(R.styleable.OptionWheelLayout_wheel_label));
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        if (onOptionSelectedListener != null) {
            onOptionSelectedListener.onOptionSelected(position, wheelView.getItem(position));
        }
    }

    public void setData(List<?> data) {
        wheelView.setData(data);
    }

    public void setDefaultValue(Object value) {
        wheelView.setDefaultValue(value);
    }

    public void setDefaultPosition(int position) {
        wheelView.setDefaultPosition(position);
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener onOptionSelectedListener) {
        this.onOptionSelectedListener = onOptionSelectedListener;
    }

    public final WheelView getWheelView() {
        return wheelView;
    }

    public final TextView getLabelView() {
        return labelView;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\TimeWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.TimeMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnTimeMeridiemSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnTimeSelectedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.TimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.TimeEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleTimeFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class TimeWheelLayout extends BaseWheelLayout {
    private NumberWheelView hourWheelView;
    private NumberWheelView minuteWheelView;
    private NumberWheelView secondWheelView;
    private TextView hourLabelView;
    private TextView minuteLabelView;
    private TextView secondLabelView;
    private TextView spaceEndView;
    private WheelView meridiemWheelView;
    private TimeEntity startValue;
    private TimeEntity endValue;
    private Integer selectedHour;
    private Integer selectedMinute;
    private Integer selectedSecond;
    private boolean isAnteMeridiem;
    private int timeMode;
    private int hourStep = 1;
    private int minuteStep = 1;
    private int secondStep = 1;
    private OnTimeSelectedListener onTimeSelectedListener;
    private OnTimeMeridiemSelectedListener onTimeMeridiemSelectedListener;
    private boolean resetWhenLinkage = true;

    public TimeWheelLayout(Context context) {
        super(context);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_time;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.TimeWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(hourWheelView, minuteWheelView, secondWheelView, meridiemWheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        hourWheelView = findViewById(R.id.wheel_picker_time_hour_wheel);
        minuteWheelView = findViewById(R.id.wheel_picker_time_minute_wheel);
        secondWheelView = findViewById(R.id.wheel_picker_time_second_wheel);
        hourLabelView = findViewById(R.id.wheel_picker_time_hour_label);
        minuteLabelView = findViewById(R.id.wheel_picker_time_minute_label);
        secondLabelView = findViewById(R.id.wheel_picker_time_second_label);
        meridiemWheelView = findViewById(R.id.wheel_picker_time_meridiem_wheel);
        spaceEndView = findViewById(R.id.wheel_picker_time_end_view);

        post(new Runnable() {
            @Override
            public void run() {
                hourLabelView.setHeight(minuteWheelView.itemHeight);
                minuteLabelView.setHeight(minuteWheelView.itemHeight);
                secondLabelView.setHeight(minuteWheelView.itemHeight);
                spaceEndView.setHeight(minuteWheelView.itemHeight);
            }
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.TimeWheelLayout_wheel_maxWidthText));
        setTextColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_itemTextColor, 0xFF888888));
        setSelectedTextColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextSize(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.TimeWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.TimeWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_curtainColor, 0));
        setCurtainCorner(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.TimeWheelLayout_wheel_curvedMaxAngle, 90));
        setTimeMode(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_timeMode, TimeMode.HOUR_24_NO_SECOND));
        String hourLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_hourLabel);
        String minuteLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_minuteLabel);
        String secondLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_secondLabel);
        setTimeLabel(hourLabel, minuteLabel, secondLabel);
        setTimeFormatter(new SimpleTimeFormatter(this));
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(TimeEntity.target(0, 0, 0),
                    TimeEntity.target(23, 59, 59), TimeEntity.now());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_time_hour_wheel) {
            selectedHour = hourWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedMinute = null;
                selectedSecond = null;
            }
            changeMinute(selectedHour);
            timeSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_time_minute_wheel) {
            selectedMinute = minuteWheelView.getItem(position);
            if (resetWhenLinkage) {
                selectedSecond = null;
            }
            changeSecond();
            timeSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_time_second_wheel) {
            selectedSecond = secondWheelView.getItem(position);
            timeSelectedCallback();
        }
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        int id = view.getId();
        if (id == R.id.wheel_picker_time_hour_wheel) {
            minuteWheelView.setEnabled(state == ScrollState.IDLE);
            secondWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_time_minute_wheel) {
            hourWheelView.setEnabled(state == ScrollState.IDLE);
            secondWheelView.setEnabled(state == ScrollState.IDLE);
            return;
        }
        if (id == R.id.wheel_picker_time_second_wheel) {
            hourWheelView.setEnabled(state == ScrollState.IDLE);
            minuteWheelView.setEnabled(state == ScrollState.IDLE);
        }
    }

    private void timeSelectedCallback() {
        if (onTimeSelectedListener != null) {
            secondWheelView.post(new Runnable() {
                @Override
                public void run() {
                    onTimeSelectedListener.onTimeSelected(selectedHour, selectedMinute, selectedSecond);
                }
            });
        }
        if (onTimeMeridiemSelectedListener != null) {
            secondWheelView.post(new Runnable() {
                @Override
                public void run() {
                    onTimeMeridiemSelectedListener.onTimeSelected(selectedHour, selectedMinute, selectedSecond, isAnteMeridiem());
                }
            });
        }
    }

    public void setTimeMode(@TimeMode int timeMode) {
        this.timeMode = timeMode;
        hourWheelView.setVisibility(View.VISIBLE);
        hourLabelView.setVisibility(View.VISIBLE);
        minuteWheelView.setVisibility(View.VISIBLE);
        minuteLabelView.setVisibility(View.VISIBLE);
        secondWheelView.setVisibility(View.VISIBLE);
        secondLabelView.setVisibility(View.VISIBLE);
        meridiemWheelView.setVisibility(View.GONE);
        if (timeMode == TimeMode.NONE) {
            hourWheelView.setVisibility(View.GONE);
            hourLabelView.setVisibility(View.GONE);
            minuteWheelView.setVisibility(View.GONE);
            minuteLabelView.setVisibility(View.GONE);
            secondWheelView.setVisibility(View.GONE);
            secondLabelView.setVisibility(View.GONE);
            this.timeMode = timeMode;
            return;
        }
        if (timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_24_NO_SECOND) {
            secondWheelView.setVisibility(View.GONE);
            secondLabelView.setVisibility(View.GONE);
            minuteLabelView.setVisibility(View.GONE);
        }
        if (isHour12Mode()) {
            meridiemWheelView.setVisibility(View.VISIBLE);
            meridiemWheelView.setData(Arrays.asList("AM", "PM"));
        }
    }

    public boolean isHour12Mode() {
        return timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_12_HAS_SECOND;
    }

    public void setRange(TimeEntity startValue, TimeEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(TimeEntity startValue, TimeEntity endValue, TimeEntity defaultValue) {
        if (startValue == null) {
            startValue = TimeEntity.target(isHour12Mode() ? 1 : 0, 0, 0);
        }
        if (endValue == null) {
            endValue = TimeEntity.target(isHour12Mode() ? 12 : 23, 59, 59);
        }
        if (endValue.toTimeInMillis() < startValue.toTimeInMillis()) {
            throw new IllegalArgumentException("Ensure the start time is less than the time date");
        }
        this.startValue = startValue;
        this.endValue = endValue;
        if (defaultValue != null) {
            isAnteMeridiem = defaultValue.getHour() <= 12;
            defaultValue.setHour(wrapHour(defaultValue.getHour()));
            selectedHour = defaultValue.getHour();
            selectedMinute = defaultValue.getMinute();
            selectedSecond = defaultValue.getSecond();
        } else {
            selectedHour = null;
            selectedMinute = null;
            selectedSecond = null;
        }
        changeHour();
        changeAnteMeridiem();
    }

    public void setDefaultValue(@NonNull final TimeEntity defaultValue) {
        setRange(startValue, endValue, defaultValue);
    }

    public void setTimeFormatter(final TimeFormatter timeFormatter) {
        if (timeFormatter == null) {
            return;
        }
        hourWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatHour((Integer) value);
            }
        });
        minuteWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatMinute((Integer) value);
            }
        });
        secondWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatSecond((Integer) value);
            }
        });
    }

    public void setTimeLabel(CharSequence hour, CharSequence minute, CharSequence second) {
        hourLabelView.setText(hour);
        minuteLabelView.setText(minute);
        secondLabelView.setText(second);
    }

    public void setOnTimeSelectedListener(OnTimeSelectedListener onTimeSelectedListener) {
        this.onTimeSelectedListener = onTimeSelectedListener;
    }

    public void setOnTimeMeridiemSelectedListener(OnTimeMeridiemSelectedListener onTimeMeridiemSelectedListener) {
        this.onTimeMeridiemSelectedListener = onTimeMeridiemSelectedListener;
    }

    public void setResetWhenLinkage(boolean resetWhenLinkage) {
        this.resetWhenLinkage = resetWhenLinkage;
    }

    public void setTimeStep(int hourStep, int minuteStep, int secondStep) {
        this.hourStep = hourStep;
        this.minuteStep = minuteStep;
        this.secondStep = secondStep;
        if (isDataAlready()) {
            setRange(startValue, endValue, TimeEntity.target(selectedHour, selectedMinute, selectedSecond));
        }
    }

    public boolean isDataAlready() {
        return startValue != null && endValue != null;
    }

    public final TimeEntity getStartValue() {
        return startValue;
    }

    public final TimeEntity getEndValue() {
        return endValue;
    }

    public final NumberWheelView getHourWheelView() {
        return hourWheelView;
    }

    public final NumberWheelView getMinuteWheelView() {
        return minuteWheelView;
    }

    public final NumberWheelView getSecondWheelView() {
        return secondWheelView;
    }

    public final TextView getHourLabelView() {
        return hourLabelView;
    }

    public final TextView getMinuteLabelView() {
        return minuteLabelView;
    }

    public final TextView getSecondLabelView() {
        return secondLabelView;
    }

    public final WheelView getMeridiemWheelView() {
        return meridiemWheelView;
    }

    @Deprecated
    public final TextView getMeridiemLabelView() {
        throw new UnsupportedOperationException("Use getMeridiemWheelView instead");
    }

    public final int getSelectedHour() {
        int hour = hourWheelView.getCurrentItem();
        return wrapHour(hour);
    }

    private int wrapHour(int hour) {
        if (isHour12Mode() && hour > 12) {
            hour = hour - 12;
        }
        return hour;
    }

    public final int getSelectedMinute() {
        return minuteWheelView.getCurrentItem();
    }

    public final int getSelectedSecond() {
        if (timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_24_NO_SECOND) {
            return 0;
        }
        return secondWheelView.getCurrentItem();
    }

    public final boolean isAnteMeridiem() {
        return meridiemWheelView.getCurrentItem().toString().equalsIgnoreCase("AM");
    }

    private void changeHour() {
        int min = Math.min(startValue.getHour(), endValue.getHour());
        int max = Math.max(startValue.getHour(), endValue.getHour());
        int minHour = isHour12Mode() ? 1 : 0;
        int maxHour = isHour12Mode() ? 12 : 23;
        min = Math.max(minHour, min);
        max = Math.min(maxHour, max);
        if (selectedHour == null) {
            selectedHour = min;
        } else {
            selectedHour = Math.max(selectedHour, min);
            selectedHour = Math.min(selectedHour, max);
        }
        hourWheelView.setRange(min, max, hourStep);
        hourWheelView.setDefaultValue(selectedHour);
        changeMinute(selectedHour);
    }

    private void changeMinute(int hour) {
        final int min, max;

        if (hour == startValue.getHour() && hour == endValue.getHour()) {
            min = startValue.getMinute();
            max = endValue.getMinute();
        } else if (hour == startValue.getHour()) {
            min = startValue.getMinute();
            max = 59;
        } else if (hour == endValue.getHour()) {
            min = 0;
            max = endValue.getMinute();
        } else {
            min = 0;
            max = 59;
        }
        if (selectedMinute == null) {
            selectedMinute = min;
        } else {
            selectedMinute = Math.max(selectedMinute, min);
            selectedMinute = Math.min(selectedMinute, max);
        }
        minuteWheelView.setRange(min, max, minuteStep);
        minuteWheelView.setDefaultValue(selectedMinute);
        changeSecond();
    }

    private void changeSecond() {
        if (selectedSecond == null) {
            selectedSecond = 0;
        }
        secondWheelView.setRange(0, 59, secondStep);
        secondWheelView.setDefaultValue(selectedSecond);
    }

    private void changeAnteMeridiem() {
        meridiemWheelView.setDefaultValue(isAnteMeridiem ? "AM" : "PM");
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\widget\YearWheelLayout.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.DateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DatimeEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.impl.SimpleDateFormatter;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.NumberWheelView;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class YearWheelLayout extends BaseWheelLayout {
    private DateWheelLayout dateWheelLayout;
    private DatimeEntity startValue;
    private DatimeEntity endValue;

    public YearWheelLayout(Context context) {
        super(context);
    }

    public YearWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public YearWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public YearWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_year;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.DatimeWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        List<WheelView> list = new ArrayList<>();
        list.addAll(dateWheelLayout.provideWheelViews());
        return list;
    }

    @Override
    protected void onInit(@NonNull Context context) {
        dateWheelLayout = findViewById(R.id.wheel_picker_date_wheel);

        setCurtainEnabled(true);
        getMonthLabelView().setBackgroundColor(0x1A2B79D7);
        getYearLabelView().setBackgroundColor(0x1A2B79D7);
        getDayLabelView().setBackgroundColor(0x1A2B79D7);

        post(() -> {
            View view_select_bg = findViewById(R.id.view_select_bg);
            ViewGroup.LayoutParams params = view_select_bg.getLayoutParams();
            params.height = dateWheelLayout.getYearWheelView().itemHeight;
            view_select_bg.setLayoutParams(params);
        });
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibleItemCount(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.DatimeWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_itemTextColor, 0xFF888888));
        setTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSize, 15 * scaledDensity));
        setSelectedTextSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_itemTextSizeSelected, 15 * scaledDensity));
        setSelectedTextBold(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_itemTextBoldSelected, false));
        setTextAlign(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_itemSpace,
                (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_indicatorColor, 0xFFC9C9C9));
        setIndicatorSize(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.DatimeWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.DatimeWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setCurtainCorner(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_curtainCorner, CurtainCorner.NONE));
        setCurtainRadius(typedArray.getDimension(R.styleable.DatimeWheelLayout_wheel_curtainRadius, 0));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.DatimeWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.DatimeWheelLayout_wheel_curvedMaxAngle, 90));
        setDateMode(typedArray.getInt(R.styleable.DatimeWheelLayout_wheel_dateMode, DateMode.YEAR));
        String yearLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_yearLabel);
        String monthLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_monthLabel);
        String dayLabel = typedArray.getString(R.styleable.DatimeWheelLayout_wheel_dayLabel);
        setDateLabel(yearLabel, monthLabel, dayLabel);
        setDateFormatter(new SimpleDateFormatter());
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && startValue == null && endValue == null) {
            setRange(DatimeEntity.now(), DatimeEntity.yearOnFuture(30), DatimeEntity.now());
        }
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        dateWheelLayout.onWheelSelected(view, position);
    }

    @Override
    public void onWheelScrolled(WheelView view, int offset) {
        dateWheelLayout.onWheelScrolled(view, offset);
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, @ScrollState int state) {
        dateWheelLayout.onWheelScrollStateChanged(view, state);
    }

    @Override
    public void onWheelLoopFinished(WheelView view) {
        dateWheelLayout.onWheelLoopFinished(view);
    }

    public void setDateMode(@DateMode int dateMode) {
        dateWheelLayout.setDateMode(dateMode);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue) {
        setRange(startValue, endValue, null);
    }

    public void setRange(DatimeEntity startValue, DatimeEntity endValue, DatimeEntity defaultValue) {
        if (startValue == null) {
            startValue = DatimeEntity.now();
        }
        if (endValue == null) {
            endValue = DatimeEntity.yearOnFuture(10);
        }
        if (defaultValue == null) {
            defaultValue = startValue;
        }
        dateWheelLayout.setRange(startValue.getDate(), endValue.getDate(), defaultValue.getDate());
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public void setDefaultValue(DatimeEntity defaultValue) {
        if (defaultValue == null) {
            defaultValue = DatimeEntity.now();
        }
        dateWheelLayout.setDefaultValue(defaultValue.getDate());
    }

    public void setDateFormatter(DateFormatter dateFormatter) {
        dateWheelLayout.setDateFormatter(dateFormatter);
    }

    public void setDateLabel(CharSequence year, CharSequence month, CharSequence day) {
        dateWheelLayout.setDateLabel(year, month, day);
    }

    public void setResetWhenLinkage(boolean dateResetWhenLinkage, boolean timeResetWhenLinkage) {
        dateWheelLayout.setResetWhenLinkage(dateResetWhenLinkage);
    }

    public final DatimeEntity getStartValue() {
        return startValue;
    }

    public final DatimeEntity getEndValue() {
        return endValue;
    }

    public final DateWheelLayout getDateWheelLayout() {
        return dateWheelLayout;
    }

    public final NumberWheelView getYearWheelView() {
        return dateWheelLayout.getYearWheelView();
    }

    public final NumberWheelView getMonthWheelView() {
        return dateWheelLayout.getMonthWheelView();
    }

    public final NumberWheelView getDayWheelView() {
        return dateWheelLayout.getDayWheelView();
    }

    public final TextView getYearLabelView() {
        return dateWheelLayout.getYearLabelView();
    }

    public final TextView getMonthLabelView() {
        return dateWheelLayout.getMonthLabelView();
    }

    public final TextView getDayLabelView() {
        return dateWheelLayout.getDayLabelView();
    }

    public final TextView getSpaceStartView() {
        return dateWheelLayout.getSpaceStartView();
    }

    public final TextView getSpaceEndView() {
        return dateWheelLayout.getSpaceEndView();
    }

    public final int getSelectedYear() {
        return dateWheelLayout.getSelectedYear();
    }

    public final int getSelectedMonth() {
        return dateWheelLayout.getSelectedMonth();
    }

    public final int getSelectedDay() {
        return dateWheelLayout.getSelectedDay();
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelpicker\YearPicker.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.dialog.ModalDialog;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.contract.OnYearPickedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DateEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.DatimeEntity;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.widget.YearWheelLayout;

import java.util.Calendar;

@SuppressWarnings({"unused", "WeakerAccess"})
public class YearPicker extends ModalDialog {
    protected YearWheelLayout wheelLayout;
    private OnYearPickedListener onYearPickedListener;

    public YearPicker(@NonNull Activity activity, @Nullable Integer year) {
        super(activity);

        int nowYear = Calendar.getInstance().get(Calendar.YEAR);
        DatimeEntity startTimeEntity = new DatimeEntity();
        startTimeEntity.setDate(DateEntity.target(nowYear - 1000, 1, 1));
        DatimeEntity defaultEntity = new DatimeEntity();
        defaultEntity.setDate(DateEntity.target(year == null ? nowYear : year, 1, 1));
        wheelLayout.setRange(startTimeEntity, DatimeEntity.now(), defaultEntity);

        wheelLayout.setCurtainEnabled(true);
        wheelLayout.setCurtainColor(ContextCompat.getColor(getContext(), R.color.wheel_select_bg));
        wheelLayout.setSelectedTextColor(ContextCompat.getColor(getContext(), R.color.wheel_select_text));
        wheelLayout.setTextColor(ContextCompat.getColor(getContext(), R.color.wheel_unselect_text));

        wheelLayout.setResetWhenLinkage(false, false);

    }

    @NonNull
    @Override
    protected View createBodyView() {
        wheelLayout = new YearWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onYearPickedListener != null) {
            onYearPickedListener.onYearPicked(wheelLayout.getSelectedYear());
        }
    }

    public YearPicker setOnYearPickedListener(OnYearPickedListener onYearPickedListener) {
        this.onYearPickedListener = onYearPickedListener;
        return this;
    }

    public final YearWheelLayout getWheelLayout() {
        return wheelLayout;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\annotation\CurtainCorner.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface CurtainCorner {
    int NONE = 0;
    int ALL = 1;
    int TOP = 2;
    int BOTTOM = 3;
    int LEFT = 4;
    int RIGHT = 5;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\annotation\ItemTextAlign.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface ItemTextAlign {
    int CENTER = 0;
    int LEFT = 1;
    int RIGHT = 2;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\annotation\ScrollState.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface ScrollState {
    int IDLE = 0;
    int DRAGGING = 1;
    int SCROLLING = 2;
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\contract\OnWheelChangedListener.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget.WheelView;

public interface OnWheelChangedListener {

    void onWheelScrolled(WheelView view, int offset);

    void onWheelSelected(WheelView view, int position);

    void onWheelScrollStateChanged(WheelView view, @ScrollState int state);

    void onWheelLoopFinished(WheelView view);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\contract\TextProvider.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract;

public interface TextProvider {

    String provideText();

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\contract\WheelFormatter.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract;

import androidx.annotation.NonNull;

public interface WheelFormatter {

    String formatItem(@NonNull Object item);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\widget\NumberWheelView.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget;

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

public class NumberWheelView extends WheelView {

    public NumberWheelView(Context context) {
        super(context);
    }

    public NumberWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected List<?> generatePreviewData() {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 10; i = i + 1) {
            data.add(i);
        }
        return data;
    }

    @Deprecated
    @Override
    public void setData(List<?> data) {
        if (isInEditMode()) {
            super.setData(generatePreviewData());
        } else {
            throw new UnsupportedOperationException("Use setRange instead");
        }
    }

    public void setRange(int min, int max, int step) {
        int minValue = Math.min(min, max);
        int maxValue = Math.max(min, max);

        int capacity = (maxValue - minValue) / step;
        List<Integer> data = new ArrayList<>(capacity);
        for (int i = minValue; i <= maxValue; i = i + step) {
            data.add(i);
        }
        super.setData(data);
    }

    public void setRange(float min, float max, float step) {
        float minValue = Math.min(min, max);
        float maxValue = Math.max(min, max);

        int capacity = (int) ((maxValue - minValue) / step);
        List<Float> data = new ArrayList<>(capacity);
        for (float i = minValue; i <= maxValue; i = i + step) {
            data.add(i);
        }
        super.setData(data);
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\wheelview\widget\WheelView.java =====

package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.*;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ItemTextAlign;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.annotation.ScrollState;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.OnWheelChangedListener;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.TextProvider;
import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelview.contract.WheelFormatter;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused"})
public class WheelView extends View implements Runnable {
    @Deprecated
    public static final int SCROLL_STATE_IDLE = ScrollState.IDLE;
    @Deprecated
    public static final int SCROLL_STATE_DRAGGING = ScrollState.DRAGGING;
    @Deprecated
    public static final int SCROLL_STATE_SCROLLING = ScrollState.SCROLLING;
    private final Handler handler = new Handler();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.LINEAR_TEXT_FLAG);
    private final Scroller scroller;
    private final Rect rectDrawn = new Rect();
    private final Rect rectIndicatorHead = new Rect();
    private final Rect rectIndicatorFoot = new Rect();
    private final Rect rectCurrentItem = new Rect();
    private final Camera camera = new Camera();
    private final Matrix matrixRotate = new Matrix();
    private final Matrix matrixDepth = new Matrix();
    private final int minimumVelocity;
    private final int maximumVelocity;
    private final int touchSlop;
    private final AttributeSet attrs;
    public int itemHeight, halfItemHeight;
    protected List<?> data = new ArrayList<>();
    protected WheelFormatter formatter;
    protected Object defaultItem;
    protected int visibleItemCount;
    protected int defaultItemPosition;
    protected int currentPosition;
    protected String maxWidthText;
    protected int textColor, selectedTextColor;
    protected float textSize, selectedTextSize;
    protected boolean selectedTextBold;
    protected float indicatorSize;
    protected int indicatorColor;
    protected int curtainColor;
    protected int curtainCorner;
    protected float curtainRadius;
    protected int itemSpace;
    protected int textAlign;
    protected boolean sameWidthEnabled;
    protected boolean indicatorEnabled;
    protected boolean curtainEnabled;
    protected boolean atmosphericEnabled;
    protected boolean cyclicEnabled;
    protected boolean curvedEnabled;
    protected int curvedMaxAngle = 90;
    protected int curvedIndicatorSpace;
    private VelocityTracker tracker;
    private OnWheelChangedListener onWheelChangedListener;
    private int lastScrollPosition;
    private int drawnItemCount;
    private int halfDrawnItemCount;
    private int textMaxWidth, textMaxHeight;
    private int halfWheelHeight;
    private int minFlingYCoordinate, maxFlingYCoordinate;
    private int wheelCenterXCoordinate, wheelCenterYCoordinate;
    private int drawnCenterXCoordinate, drawnCenterYCoordinate;
    private int scrollOffsetYCoordinate;
    private int lastPointYCoordinate;
    private int downPointYCoordinate;
    private boolean isClick;
    private boolean isForceFinishScroll;

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.WheelStyle);
    }

    public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.attrs = attrs;
        initAttrs(context, attrs, defStyleAttr, R.style.WheelDefault);
        initTextPaint();
        updateVisibleItemCount();
        scroller = new Scroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
        touchSlop = configuration.getScaledTouchSlop();
        if (isInEditMode()) {
            setData(generatePreviewData());
        }
    }

    private void initTextPaint() {
        paint.setColor(textColor);
        paint.setTextSize(textSize);
        paint.setFakeBoldText(false);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setStyle(@StyleRes int style) {
        if (attrs == null) {
            throw new RuntimeException("Please use " + getClass().getSimpleName() + " in xml");
        }
        initAttrs(getContext(), attrs, R.attr.WheelStyle, style);
        initTextPaint();
        requestLayout();
        invalidate();
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) {
            float density = context.getResources().getDisplayMetrics().density;
            float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
            visibleItemCount = 5;
            defaultItemPosition = 0;
            sameWidthEnabled = false;
            maxWidthText = "";
            textAlign = ItemTextAlign.CENTER;
            textColor = 0xFF888888;
            selectedTextColor = 0xFF000000;
            textSize = 15 * scaledDensity;
            selectedTextSize = textSize;
            selectedTextBold = false;
            itemSpace = (int) (20 * density);
            cyclicEnabled = false;
            indicatorEnabled = true;
            indicatorColor = 0xFFC9C9C9;
            indicatorSize = 1 * density;
            curvedIndicatorSpace = (int) (1 * density);
            curtainEnabled = false;
            curtainColor = 0xFFFFFFFF;
            curtainCorner = CurtainCorner.NONE;
            curtainRadius = 0;
            atmosphericEnabled = false;
            curvedEnabled = false;
            curvedMaxAngle = 90;
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WheelView,
                defStyleAttr, defStyleRes);
        onAttributeSet(context, typedArray);
        typedArray.recycle();
    }

    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        visibleItemCount = typedArray.getInt(R.styleable.WheelView_wheel_visibleItemCount, 5);
        sameWidthEnabled = typedArray.getBoolean(R.styleable.WheelView_wheel_sameWidthEnabled, false);
        maxWidthText = typedArray.getString(R.styleable.WheelView_wheel_maxWidthText);
        textColor = typedArray.getColor(R.styleable.WheelView_wheel_itemTextColor, 0xFF888888);
        selectedTextColor = typedArray.getColor(R.styleable.WheelView_wheel_itemTextColorSelected, 0xFF000000);
        textSize = typedArray.getDimension(R.styleable.WheelView_wheel_itemTextSize, 15 * scaledDensity);
        selectedTextSize = typedArray.getDimension(R.styleable.WheelView_wheel_itemTextSizeSelected, textSize);
        selectedTextBold = typedArray.getBoolean(R.styleable.WheelView_wheel_itemTextBoldSelected, false);
        textAlign = typedArray.getInt(R.styleable.WheelView_wheel_itemTextAlign, ItemTextAlign.CENTER);
        itemSpace = typedArray.getDimensionPixelSize(R.styleable.WheelView_wheel_itemSpace, (int) (20 * density));
        cyclicEnabled = typedArray.getBoolean(R.styleable.WheelView_wheel_cyclicEnabled, false);
        indicatorEnabled = typedArray.getBoolean(R.styleable.WheelView_wheel_indicatorEnabled, true);
        indicatorColor = typedArray.getColor(R.styleable.WheelView_wheel_indicatorColor, 0xFFC9C9C9);
        indicatorSize = typedArray.getDimension(R.styleable.WheelView_wheel_indicatorSize, 1 * density);
        curvedIndicatorSpace = typedArray.getDimensionPixelSize(R.styleable.WheelView_wheel_curvedIndicatorSpace, (int) (1 * density));
        curtainEnabled = typedArray.getBoolean(R.styleable.WheelView_wheel_curtainEnabled, false);
        curtainColor = typedArray.getColor(R.styleable.WheelView_wheel_curtainColor, 0xFFFFFFFF);
        curtainCorner = typedArray.getInt(R.styleable.WheelView_wheel_curtainCorner, CurtainCorner.NONE);
        curtainRadius = typedArray.getDimension(R.styleable.WheelView_wheel_curtainRadius, 0);
        atmosphericEnabled = typedArray.getBoolean(R.styleable.WheelView_wheel_atmosphericEnabled, false);
        curvedEnabled = typedArray.getBoolean(R.styleable.WheelView_wheel_curvedEnabled, false);
        curvedMaxAngle = typedArray.getInteger(R.styleable.WheelView_wheel_curvedMaxAngle, 90);
    }

    protected List<?> generatePreviewData() {
        List<String> data = new ArrayList<>();
        data.add("[CHINESE_TEXT]human");
        data.add("[CHINESE_TEXT]");
        data.add("[CHINESE_TEXT]");
        data.add("[CHINESE_TEXT]");
        data.add("[CHINESE_TEXT]human[CHINESE_TEXT]");
        data.add("[CHINESE_TEXT]");
        return data;
    }

    private void updateVisibleItemCount() {
        final int minCount = 2;
        if (visibleItemCount < minCount) {
            throw new ArithmeticException("Visible item count can not be less than " + minCount);
        }

        int evenNumberFlag = 2;
        if (visibleItemCount % evenNumberFlag == 0) {
            visibleItemCount += 1;
        }
        drawnItemCount = visibleItemCount + 2;
        halfDrawnItemCount = drawnItemCount / 2;
    }

    private void computeTextWidthAndHeight() {
        textMaxWidth = textMaxHeight = 0;
        if (sameWidthEnabled) {
            textMaxWidth = (int) paint.measureText(formatItem(0));
        } else if (!TextUtils.isEmpty(maxWidthText)) {
            textMaxWidth = (int) paint.measureText(maxWidthText);
        } else {

            int itemCount = getItemCount();
            for (int i = 0; i < itemCount; ++i) {
                int width = (int) paint.measureText(formatItem(i));
                textMaxWidth = Math.max(textMaxWidth, width);
            }
        }
        Paint.FontMetrics metrics = paint.getFontMetrics();
        textMaxHeight = (int) (metrics.bottom - metrics.top);
    }

    public int getItemCount() {
        return data.size();
    }

    public <T> T getItem(int position) {
        final int size = data.size();
        if (size == 0) {
            return null;
        }
        int index = (position + size) % size;
        if (index >= 0 && index <= size - 1) {

            return (T) data.get(index);
        }
        return null;
    }

    public int getPosition(Object item) {
        if (item == null) {
            return 0;
        }
        return data.indexOf(item);
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public <T> T getCurrentItem() {
        return getItem(currentPosition);
    }

    public int getVisibleItemCount() {
        return visibleItemCount;
    }

    public void setVisibleItemCount(@IntRange(from = 2) int count) {
        visibleItemCount = count;
        updateVisibleItemCount();
        requestLayout();
    }

    public boolean isCyclicEnabled() {
        return cyclicEnabled;
    }

    public void setCyclicEnabled(boolean isCyclic) {
        this.cyclicEnabled = isCyclic;
        computeFlingLimitYCoordinate();
        invalidate();
    }

    public void setOnWheelChangedListener(OnWheelChangedListener listener) {
        onWheelChangedListener = listener;
    }

    public void setFormatter(WheelFormatter formatter) {
        this.formatter = formatter;
    }

    public List<?> getData() {
        return data;
    }

    public void setData(List<?> newData) {
        if (newData == null) {
            newData = new ArrayList<>();
        }
        data = newData;
        notifyDataSetChanged(0, false);
    }

    public void setDefaultValue(Object value) {
        if (value == null) {
            return;
        }
        boolean found = false;
        int position = 0;
        for (Object item : data) {
            if (item.equals(value)) {
                found = true;
                break;
            }
            if (formatter != null && formatter.formatItem(item).equals(formatter.formatItem(value))) {
                found = true;
                break;
            }
            if (item instanceof TextProvider) {
                String text = ((TextProvider) item).provideText();
                if (text.equals(value.toString())) {
                    found = true;
                    break;
                }
            }
            if (item.toString().equals(value.toString())) {
                found = true;
                break;
            }
            position++;
        }
        if (!found) {
            position = 0;
        }
        setDefaultPosition(position);
    }

    public void setDefaultPosition(int position) {
        notifyDataSetChanged(position, false);
    }

    public boolean isSameWidthEnabled() {
        return sameWidthEnabled;
    }

    public void setSameWidthEnabled(boolean sameWidthEnabled) {
        this.sameWidthEnabled = sameWidthEnabled;
        computeTextWidthAndHeight();
        requestLayout();
        invalidate();
    }

    public String getMaxWidthText() {
        return maxWidthText;
    }

    public void setMaxWidthText(String text) {
        if (null == text) {
            throw new NullPointerException("Maximum width text can not be null!");
        }
        maxWidthText = text;
        computeTextWidthAndHeight();
        requestLayout();
        invalidate();
    }

    @ColorInt
    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(@ColorInt int color) {
        textColor = color;
        invalidate();
    }

    @ColorInt
    public int getSelectedTextColor() {
        return selectedTextColor;
    }

    public void setSelectedTextColor(@ColorInt int color) {
        selectedTextColor = color;
        computeCurrentItemRect();
        invalidate();
    }

    @Px
    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(@Px float size) {
        textSize = size;
        computeTextWidthAndHeight();
        requestLayout();
        invalidate();
    }

    @Px
    public float getSelectedTextSize() {
        return selectedTextSize;
    }

    public void setSelectedTextSize(@Px float size) {
        selectedTextSize = size;
        computeTextWidthAndHeight();
        requestLayout();
        invalidate();
    }

    public boolean getSelectedTextBold() {
        return selectedTextBold;
    }

    public void setSelectedTextBold(boolean bold) {
        this.selectedTextBold = bold;
        computeTextWidthAndHeight();
        requestLayout();
        invalidate();
    }

    @Px
    public int getItemSpace() {
        return itemSpace;
    }

    public void setItemSpace(@Px int space) {
        itemSpace = space;
        requestLayout();
        invalidate();
    }

    public boolean isIndicatorEnabled() {
        return indicatorEnabled;
    }

    public void setIndicatorEnabled(boolean indicatorEnabled) {
        this.indicatorEnabled = indicatorEnabled;
        computeIndicatorRect();
        invalidate();
    }

    @Px
    public float getIndicatorSize() {
        return indicatorSize;
    }

    public void setIndicatorSize(@Px float size) {
        indicatorSize = size;
        computeIndicatorRect();
        invalidate();
    }

    @ColorInt
    public int getIndicatorColor() {
        return indicatorColor;
    }

    public void setIndicatorColor(@ColorInt int color) {
        indicatorColor = color;
        invalidate();
    }

    @Px
    public int getCurvedIndicatorSpace() {
        return curvedIndicatorSpace;
    }

    public void setCurvedIndicatorSpace(@Px int space) {
        curvedIndicatorSpace = space;
        computeIndicatorRect();
        invalidate();
    }

    public boolean isCurtainEnabled() {
        return curtainEnabled;
    }

    public void setCurtainEnabled(boolean curtainEnabled) {
        this.curtainEnabled = curtainEnabled;
        if (curtainEnabled) {
            indicatorEnabled = false;
        }
        computeCurrentItemRect();
        invalidate();
    }

    @ColorInt
    public int getCurtainColor() {
        return curtainColor;
    }

    public void setCurtainColor(@ColorInt int color) {
        curtainColor = color;
        invalidate();
    }

    @CurtainCorner
    public int getCurtainCorner() {
        return curtainCorner;
    }

    public void setCurtainCorner(@CurtainCorner int curtainCorner) {
        this.curtainCorner = curtainCorner;
        invalidate();
    }

    @Px
    public float getCurtainRadius() {
        return curtainRadius;
    }

    public void setCurtainRadius(@Px float curtainRadius) {
        this.curtainRadius = curtainRadius;
        invalidate();
    }

    public boolean isAtmosphericEnabled() {
        return atmosphericEnabled;
    }

    public void setAtmosphericEnabled(boolean atmosphericEnabled) {
        this.atmosphericEnabled = atmosphericEnabled;
        invalidate();
    }

    public boolean isCurvedEnabled() {
        return curvedEnabled;
    }

    public void setCurvedEnabled(boolean isCurved) {
        this.curvedEnabled = isCurved;
        requestLayout();
        invalidate();
    }

    public int getCurvedMaxAngle() {
        return curvedMaxAngle;
    }

    public void setCurvedMaxAngle(int curvedMaxAngle) {
        this.curvedMaxAngle = curvedMaxAngle;
        requestLayout();
        invalidate();
    }

    @ItemTextAlign
    public int getTextAlign() {
        return textAlign;
    }

    public void setTextAlign(@ItemTextAlign int align) {
        textAlign = align;
        updatePaintTextAlign();
        computeDrawnCenterCoordinate();
        invalidate();
    }

    private void updatePaintTextAlign() {
        switch (textAlign) {
            case ItemTextAlign.LEFT:
                paint.setTextAlign(Paint.Align.LEFT);
                break;
            case ItemTextAlign.RIGHT:
                paint.setTextAlign(Paint.Align.RIGHT);
                break;
            case ItemTextAlign.CENTER:
            default:
                paint.setTextAlign(Paint.Align.CENTER);
                break;
        }
    }

    public Typeface getTypeface() {
        return paint.getTypeface();
    }

    public void setTypeface(Typeface typeface) {
        if (typeface == null) {
            return;
        }
        paint.setTypeface(typeface);
        computeTextWidthAndHeight();
        requestLayout();
        invalidate();
    }

    private void notifyDataSetChanged(int position, boolean smooth) {
        position = Math.min(position, getItemCount() - 1);
        position = Math.max(position, 0);
        if (smooth) {
            smoothScrollTo(position);
        } else {
            scrollTo(position);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        final int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        final int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        int resultWidth = textMaxWidth;
        int resultHeight = textMaxHeight * visibleItemCount + itemSpace * (visibleItemCount - 1);

        if (curvedEnabled) {
            resultHeight = (int) (2 * resultHeight / Math.PI);
        }

        resultWidth += getPaddingLeft() + getPaddingRight();
        resultHeight += getPaddingTop() + getPaddingBottom();

        resultWidth = measureSize(modeWidth, sizeWidth, resultWidth);
        resultHeight = measureSize(modeHeight, sizeHeight, resultHeight);
        setMeasuredDimension(resultWidth, resultHeight);
    }

    private int measureSize(int mode, int sizeExpect, int sizeActual) {
        int realSize;
        if (mode == MeasureSpec.EXACTLY) {
            realSize = sizeExpect;
        } else {
            realSize = sizeActual;
            if (mode == MeasureSpec.AT_MOST) {
                realSize = Math.min(realSize, sizeExpect);
            }
        }
        return realSize;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {

        rectDrawn.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());

        wheelCenterXCoordinate = rectDrawn.centerX();
        wheelCenterYCoordinate = rectDrawn.centerY();

        computeDrawnCenterCoordinate();
        halfWheelHeight = rectDrawn.height() / 2;
        itemHeight = rectDrawn.height() / visibleItemCount;
        halfItemHeight = itemHeight / 2;

        computeFlingLimitYCoordinate();

        computeIndicatorRect();

        computeCurrentItemRect();
    }

    private void computeDrawnCenterCoordinate() {
        switch (textAlign) {
            case ItemTextAlign.LEFT:
                drawnCenterXCoordinate = rectDrawn.left;
                break;
            case ItemTextAlign.RIGHT:
                drawnCenterXCoordinate = rectDrawn.right;
                break;
            case ItemTextAlign.CENTER:
            default:
                drawnCenterXCoordinate = wheelCenterXCoordinate;
                break;
        }
        drawnCenterYCoordinate = (int) (wheelCenterYCoordinate -
                ((paint.ascent() + paint.descent()) / 2));
    }

    private void computeFlingLimitYCoordinate() {
        int currentItemOffset = defaultItemPosition * itemHeight;
        minFlingYCoordinate = cyclicEnabled ? Integer.MIN_VALUE
                : -itemHeight * (getItemCount() - 1) + currentItemOffset;
        maxFlingYCoordinate = cyclicEnabled ? Integer.MAX_VALUE : currentItemOffset;
    }

    private void computeIndicatorRect() {
        if (!indicatorEnabled) {
            return;
        }
        int indicatorSpace = curvedEnabled ? curvedIndicatorSpace : 0;
        int halfIndicatorSize = (int) (indicatorSize / 2f);
        int indicatorHeadCenterYCoordinate = wheelCenterYCoordinate + halfItemHeight + indicatorSpace;
        int indicatorFootCenterYCoordinate = wheelCenterYCoordinate - halfItemHeight - indicatorSpace;
        rectIndicatorHead.set(rectDrawn.left, indicatorHeadCenterYCoordinate - halfIndicatorSize,
                rectDrawn.right, indicatorHeadCenterYCoordinate + halfIndicatorSize);
        rectIndicatorFoot.set(rectDrawn.left, indicatorFootCenterYCoordinate - halfIndicatorSize,
                rectDrawn.right, indicatorFootCenterYCoordinate + halfIndicatorSize);
    }

    private void computeCurrentItemRect() {
        if (!curtainEnabled && selectedTextColor == -1) {
            return;
        }
        rectCurrentItem.set(rectDrawn.left, wheelCenterYCoordinate - halfItemHeight,
                rectDrawn.right, wheelCenterYCoordinate + halfItemHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != onWheelChangedListener) {
            onWheelChangedListener.onWheelScrolled(this, scrollOffsetYCoordinate);
        }
        if (itemHeight - halfDrawnItemCount <= 0) {
            return;
        }
        drawAllItem(canvas);
        drawCurtain(canvas);
        drawIndicator(canvas);
    }

    private void drawAllItem(Canvas canvas) {
        int drawnDataStartPos = -1 * scrollOffsetYCoordinate / itemHeight - halfDrawnItemCount;
        for (int drawnDataPosition = drawnDataStartPos + defaultItemPosition,
             drawnOffsetPos = -1 * halfDrawnItemCount;
             drawnDataPosition < drawnDataStartPos + defaultItemPosition + drawnItemCount;
             drawnDataPosition++, drawnOffsetPos++) {

            initTextPaint();
            boolean isCenterItem = drawnDataPosition == drawnDataStartPos + defaultItemPosition + drawnItemCount / 2;

            int drawnItemCenterYCoordinate = drawnCenterYCoordinate + (drawnOffsetPos * itemHeight)
                    + scrollOffsetYCoordinate % itemHeight;
            int centerYCoordinateAbs = Math.abs(drawnCenterYCoordinate - drawnItemCenterYCoordinate);

            float ratio = (drawnCenterYCoordinate - centerYCoordinateAbs - rectDrawn.top) * 1f /
                    (drawnCenterYCoordinate - rectDrawn.top);
            float degree = computeDegree(drawnItemCenterYCoordinate, ratio);
            float distanceToCenter = computeYCoordinateAtAngle(degree);

            if (curvedEnabled) {
                int transXCoordinate = wheelCenterXCoordinate;
                switch (textAlign) {
                    case ItemTextAlign.LEFT:
                        transXCoordinate = rectDrawn.left;
                        break;
                    case ItemTextAlign.RIGHT:
                        transXCoordinate = rectDrawn.right;
                        break;
                    case ItemTextAlign.CENTER:
                    default:
                        break;
                }
                float transYCoordinate = wheelCenterYCoordinate - distanceToCenter;

                camera.save();
                camera.rotateX(degree);
                camera.getMatrix(matrixRotate);
                camera.restore();
                matrixRotate.preTranslate(-transXCoordinate, -transYCoordinate);
                matrixRotate.postTranslate(transXCoordinate, transYCoordinate);

                camera.save();
                camera.translate(0, 0, computeDepth(degree));
                camera.getMatrix(matrixDepth);
                camera.restore();
                matrixDepth.preTranslate(-transXCoordinate, -transYCoordinate);
                matrixDepth.postTranslate(transXCoordinate, transYCoordinate);
                matrixRotate.postConcat(matrixDepth);
            }

            computeAndSetAtmospheric(centerYCoordinateAbs);

            float drawCenterYCoordinate = curvedEnabled ? drawnCenterYCoordinate - distanceToCenter
                    : drawnItemCenterYCoordinate;
            drawItemRect(canvas, drawnDataPosition, isCenterItem, drawCenterYCoordinate);
        }
    }

    private void drawItemRect(Canvas canvas, int dataPosition, boolean isCenterItem, float drawCenterYCoordinate) {

        if (selectedTextColor == -1) {
            canvas.save();
            canvas.clipRect(rectDrawn);
            if (curvedEnabled) {
                canvas.concat(matrixRotate);
            }
            drawItemText(canvas, dataPosition, drawCenterYCoordinate);
            canvas.restore();
            return;
        }

        if (textSize == selectedTextSize) {
            canvas.save();
            if (curvedEnabled) {
                canvas.concat(matrixRotate);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutRect(rectCurrentItem);
            } else {
                canvas.clipRect(rectCurrentItem, Region.Op.DIFFERENCE);
            }
            drawItemText(canvas, dataPosition, drawCenterYCoordinate);
            canvas.restore();
            paint.setColor(selectedTextColor);
            canvas.save();
            if (curvedEnabled) {
                canvas.concat(matrixRotate);
            }
            canvas.clipRect(rectCurrentItem);
            drawItemText(canvas, dataPosition, drawCenterYCoordinate);
            canvas.restore();
            return;
        }

        if (!isCenterItem) {
            canvas.save();
            if (curvedEnabled) {
                canvas.concat(matrixRotate);
            }
            drawItemText(canvas, dataPosition, drawCenterYCoordinate);
            canvas.restore();
            return;
        }

        paint.setColor(selectedTextColor);
        paint.setTextSize(selectedTextSize);
        paint.setFakeBoldText(selectedTextBold);
        canvas.save();
        if (curvedEnabled) {
            canvas.concat(matrixRotate);
        }
        drawItemText(canvas, dataPosition, drawCenterYCoordinate);
        canvas.restore();
    }

    private void drawItemText(Canvas canvas, int dataPosition, float drawCenterYCoordinate) {
        boolean hasCut = false;
        String ellipsis = "...";
        int measuredWidth = getMeasuredWidth();
        float ellipsisWidth = paint.measureText(ellipsis);
        String data = obtainItemText(dataPosition);
        while (paint.measureText(data) + ellipsisWidth - measuredWidth > 0) {

            int length = data.length();
            if (length > 1) {
                data = data.substring(0, length - 1);
                hasCut = true;
            }
        }
        if (hasCut) {
            data = data + ellipsis;
        }
        canvas.drawText(data, drawnCenterXCoordinate, drawCenterYCoordinate, paint);
    }

    private float computeDegree(int drawnItemCenterYCoordinate, float ratio) {

        int unit = 0;
        if (drawnItemCenterYCoordinate > drawnCenterYCoordinate) {
            unit = 1;
        } else if (drawnItemCenterYCoordinate < drawnCenterYCoordinate) {
            unit = -1;
        }
        return clamp((-(1 - ratio) * curvedMaxAngle * unit), -curvedMaxAngle, curvedMaxAngle);
    }

    private float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private String obtainItemText(int drawnDataPosition) {
        String data = "";
        final int itemCount = getItemCount();
        if (cyclicEnabled) {
            if (itemCount != 0) {
                int actualPosition = drawnDataPosition % itemCount;
                actualPosition = actualPosition < 0 ? (actualPosition + itemCount) : actualPosition;
                data = formatItem(actualPosition);
            }
        } else {
            if (isPositionInRange(drawnDataPosition, itemCount)) {
                data = formatItem(drawnDataPosition);
            }
        }
        return data;
    }

    public String formatItem(int position) {
        return formatItem(getItem(position));
    }

    public String formatItem(Object item) {
        if (item == null) {
            return "";
        }
        if (item instanceof TextProvider) {
            return ((TextProvider) item).provideText();
        }
        if (formatter != null) {
            return formatter.formatItem(item);
        }
        return item.toString();
    }

    private void computeAndSetAtmospheric(int abs) {
        if (atmosphericEnabled) {
            int alpha = (int) ((drawnCenterYCoordinate - abs) * 1.0F / drawnCenterYCoordinate * 255);
            alpha = Math.max(alpha, 0);
            paint.setAlpha(alpha);
        }
    }

    private void drawCurtain(Canvas canvas) {

        if (!curtainEnabled) {
            return;
        }
        int alpha = Color.alpha(curtainColor);
        int red = Color.red(curtainColor);
        int green = Color.green(curtainColor);
        int blue = Color.blue(curtainColor);
        paint.setColor(0);
        paint.setStyle(Paint.Style.FILL);
        if (curtainRadius > 0) {
            Path path = new Path();
            float[] radii;
            switch (curtainCorner) {
                case CurtainCorner.ALL:
                    radii = new float[]{
                            curtainRadius, curtainRadius, curtainRadius, curtainRadius,
                            curtainRadius, curtainRadius, curtainRadius, curtainRadius
                    };
                    break;
                case CurtainCorner.TOP:
                    radii = new float[]{
                            curtainRadius, curtainRadius, curtainRadius, curtainRadius, 0, 0, 0, 0
                    };
                    break;
                case CurtainCorner.BOTTOM:
                    radii = new float[]{
                            0, 0, 0, 0, curtainRadius, curtainRadius, curtainRadius, curtainRadius
                    };
                    break;
                case CurtainCorner.LEFT:
                    radii = new float[]{
                            curtainRadius, curtainRadius, 0, 0, 0, 0, curtainRadius, curtainRadius
                    };
                    break;
                case CurtainCorner.RIGHT:
                    radii = new float[]{
                            0, 0, curtainRadius, curtainRadius, curtainRadius, curtainRadius, 0, 0
                    };
                    break;
                default:
                    radii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
                    break;
            }
            path.addRoundRect(new RectF(rectCurrentItem), radii, Path.Direction.CCW);
            canvas.drawPath(path, paint);
            return;
        }
        canvas.drawRect(rectCurrentItem, paint);
    }

    private void drawIndicator(Canvas canvas) {

        if (!indicatorEnabled) {
            return;
        }
        paint.setColor(indicatorColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rectIndicatorHead, paint);
        canvas.drawRect(rectIndicatorFoot, paint);
    }

    private boolean isPositionInRange(int position, int itemCount) {
        return position >= 0 && position < itemCount;
    }

    private float computeYCoordinateAtAngle(float degree) {

        return sinDegree(degree) / sinDegree(curvedMaxAngle) * halfWheelHeight;
    }

    private float sinDegree(float degree) {
        return (float) Math.sin(Math.toRadians(degree));
    }

    private int computeDepth(float degree) {
        return (int) (halfWheelHeight - Math.cos(Math.toRadians(degree)) * halfWheelHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleActionDown(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    handleActionMove(event);
                    break;
                case MotionEvent.ACTION_UP:
                    handleActionUp(event);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    handleActionCancel(event);
                    break;
                default:
                    break;
            }
        }
        if (isClick) {

            performClick();
        }
        return true;
    }

    private void handleActionDown(MotionEvent event) {
        if (null != getParent()) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        obtainOrClearTracker();
        tracker.addMovement(event);
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
            isForceFinishScroll = true;
        }
        downPointYCoordinate = lastPointYCoordinate = (int) event.getY();
    }

    private void handleActionMove(MotionEvent event) {
        int endPoint = computeDistanceToEndPoint(scroller.getFinalY() % itemHeight);
        if (Math.abs(downPointYCoordinate - event.getY()) < touchSlop && endPoint > 0) {
            isClick = true;
            return;
        }
        isClick = false;
        if (null != tracker) {
            tracker.addMovement(event);
        }
        if (null != onWheelChangedListener) {
            onWheelChangedListener.onWheelScrollStateChanged(this, ScrollState.DRAGGING);
        }

        float move = event.getY() - lastPointYCoordinate;
        if (Math.abs(move) < 1) {
            return;
        }
        scrollOffsetYCoordinate += move;
        lastPointYCoordinate = (int) event.getY();
        invalidate();
    }

    private void handleActionUp(MotionEvent event) {
        if (null != getParent()) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        if (isClick) {
            return;
        }
        int yVelocity = 0;
        if (null != tracker) {
            tracker.addMovement(event);
            tracker.computeCurrentVelocity(1000, maximumVelocity);
            yVelocity = (int) tracker.getYVelocity();
        }

        isForceFinishScroll = false;
        if (Math.abs(yVelocity) > minimumVelocity) {
            scroller.fling(0, scrollOffsetYCoordinate, 0, yVelocity, 0,
                    0, minFlingYCoordinate, maxFlingYCoordinate);
            int endPoint = computeDistanceToEndPoint(scroller.getFinalY() % itemHeight);
            scroller.setFinalY(scroller.getFinalY() + endPoint);
        } else {
            int endPoint = computeDistanceToEndPoint(scrollOffsetYCoordinate % itemHeight);
            scroller.startScroll(0, scrollOffsetYCoordinate, 0, endPoint);
        }

        if (!cyclicEnabled) {
            if (scroller.getFinalY() > maxFlingYCoordinate) {
                scroller.setFinalY(maxFlingYCoordinate);
            } else if (scroller.getFinalY() < minFlingYCoordinate) {
                scroller.setFinalY(minFlingYCoordinate);
            }
        }
        handler.post(this);
        cancelTracker();
    }

    private void handleActionCancel(MotionEvent event) {
        if (null != getParent()) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        cancelTracker();
    }

    private void obtainOrClearTracker() {
        if (null == tracker) {
            tracker = VelocityTracker.obtain();
        } else {
            tracker.clear();
        }
    }

    private void cancelTracker() {
        if (null != tracker) {
            tracker.recycle();
            tracker = null;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int computeDistanceToEndPoint(int remainder) {
        if (Math.abs(remainder) > halfItemHeight) {
            if (scrollOffsetYCoordinate < 0) {
                return -itemHeight - remainder;
            } else {
                return itemHeight - remainder;
            }
        } else {
            return -1 * remainder;
        }
    }

    @Override
    public void run() {
        if (itemHeight == 0) {
            return;
        }
        int itemCount = getItemCount();
        if (itemCount == 0) {
            if (null != onWheelChangedListener) {
                onWheelChangedListener.onWheelScrollStateChanged(this, ScrollState.IDLE);
            }
            return;
        }
        if (scroller.isFinished() && !isForceFinishScroll) {
            int position = computePosition(itemCount);
            position = position < 0 ? position + itemCount : position;
            currentPosition = position;
            if (null != onWheelChangedListener) {
                onWheelChangedListener.onWheelSelected(this, position);
                onWheelChangedListener.onWheelScrollStateChanged(this, ScrollState.IDLE);
            }
            postInvalidate();
            return;
        }

        if (scroller.computeScrollOffset()) {
            if (null != onWheelChangedListener) {
                onWheelChangedListener.onWheelScrollStateChanged(this, ScrollState.SCROLLING);
            }
            scrollOffsetYCoordinate = scroller.getCurrY();
            int position = computePosition(itemCount);
            if (lastScrollPosition != position) {
                if (position == 0 && lastScrollPosition == itemCount - 1) {
                    if (null != onWheelChangedListener) {
                        onWheelChangedListener.onWheelLoopFinished(this);
                    }
                }
                lastScrollPosition = position;
            }
            postInvalidate();
            handler.postDelayed(this, 20);
        }
    }

    private int computePosition(int itemCount) {
        return (-1 * scrollOffsetYCoordinate / itemHeight + defaultItemPosition) % itemCount;
    }

    public final void smoothScrollTo(final int position) {
        if (isInEditMode() || !isAttachedToWindow()) {
            scrollTo(position);
            return;
        }
        int differencesLines = currentPosition - position;
        int newScrollOffsetYCoordinate = scrollOffsetYCoordinate + (differencesLines * itemHeight);
        ValueAnimator animator = ValueAnimator.ofInt(scrollOffsetYCoordinate, newScrollOffsetYCoordinate);
        animator.setDuration(100);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scrollOffsetYCoordinate = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scrollTo(position);
            }
        });
        if (isAttachedToWindow()) {
            animator.start();
        }
    }

    public void scrollTo(int position) {
        scrollOffsetYCoordinate = 0;
        defaultItem = getItem(position);
        defaultItemPosition = position;
        currentPosition = position;
        computeFlingLimitYCoordinate();
        updatePaintTextAlign();
        computeTextWidthAndHeight();
        requestLayout();
        invalidate();
    }

}