// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\gzuliyujiang\dialog' directory and its subdirectories.
// Total files: 8 | Generated on: 2025-10-08 01:42:39


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