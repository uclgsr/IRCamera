// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms' directory and its subdirectories.
// Total files: 26 | Generated on: 2025-10-08 01:42:38


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\activity\LmsUpdateDialog.java =====

package com.mpdc4gsr.libunified.app.lms.activity;

import android.app.Dialog;
import android.content.Context;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class LmsUpdateDialog extends Dialog {
    public LmsUpdateDialog(Context context) {
        super(context);
    }

    public void setTitle(String title) {
    }

    public void setMessage(String message) {
    }

    public void setPositiveButton(String text, android.content.DialogInterface.OnClickListener listener) {
    }

    public void setNegativeButton(String text, android.content.DialogInterface.OnClickListener listener) {
    }

    public static class Build {
        public static final Build INSTANCE = new Build();

        private String contentStr = "";
        private int upgradeFlag = 0;
        private Function0<Unit> sureEvent = null;
        private Function0<Unit> cancelEvent = null;

        public Build setContentStr(String content) {
            this.contentStr = content;
            return this;
        }

        public Build setUpgradeFlag(int flag) {
            this.upgradeFlag = flag;
            return this;
        }

        public Build setSureEvent(Function0<Unit> event) {
            this.sureEvent = event;
            return this;
        }

        public Build setCancelEvent(Function0<Unit> event) {
            this.cancelEvent = event;
            return this;
        }

        public void setProgressNum(float progress) {
            // Stub implementation for progress updates
        }

        public void dismiss() {
            // Stub implementation for dismissing dialog
        }

        public LmsUpdateDialog build(Context context) {
            return new LmsUpdateDialog(context);
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\bean\AppInfoBean.java =====

package com.mpdc4gsr.libunified.app.lms.bean;

import java.util.List;

public class AppInfoBean {
    public boolean hasNewVersion = false;
    public String versionName = "";
    public String versionCode = "";
    public String downloadUrl = "";
    public String description = "";
    public boolean forceUpdate = false;

    // Additional fields needed by AppVersionUtils
    public String downloadPackageUrl = "";
    public String forcedUpgradeFlag = "0";
    public String versionNo = "";
    public List<UpdateDescription> softConfigOtherTypeVOList = null;

    public AppInfoBean() {
    }

    public int getVersionCode() {
        try {
            return Integer.parseInt(versionCode);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static class UpdateDescription {
        public int descType = 0;
        public String textDescription = "";

        public UpdateDescription() {
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\bean\CommonBean.java =====

package com.mpdc4gsr.libunified.app.lms.bean;

public class CommonBean {
    public String username = "";
    public String email = "";
    public String userId = "";
    public String avatar = "";
    public String code = "2000";
    public String data = "";

    public CommonBean() {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\bean\FeedBackBean.java =====

package com.mpdc4gsr.libunified.app.lms.bean;

import java.io.Serializable;

public class FeedBackBean implements Serializable {
    public String feedback = "";
    public String contact = "";
    public String logPath = "";
    public String sn = "";
    public String lastConnectSn = "";

    public FeedBackBean() {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\Config.java =====

package com.mpdc4gsr.libunified.app.lms;

public class Config {
    public static final String KEY_PRIVACY_AGREEMENT = "privacy_agreement";

    // Broadcast actions for login/logout events
    public static final String ACTION_BROADCAST_LOGIN = "com.mpdc4gsr.ACTION_BROADCAST_LOGIN";
    public static final String ACTION_BROADCAST_LOGOFF = "com.mpdc4gsr.ACTION_BROADCAST_LOGOFF";
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\feedback\activity\FeedbackActivity.java =====

package com.mpdc4gsr.libunified.app.lms.feedback.activity;

import android.app.Activity;
import android.os.Bundle;

public class FeedbackActivity extends Activity {
    public static final String FEEDBACKBEAN = "feedbackBean";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Stub implementation - do nothing
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\LMS.java =====

package com.mpdc4gsr.libunified.app.lms;

import android.content.Context;

import com.mpdc4gsr.libunified.app.lms.bean.AppInfoBean;
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean;
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class LMS {
    public static final String SUCCESS = "2000";
    public static Context mContext;

    private static volatile LMS instance;
    public String language = "en";
    public String softwareCode = "";
    private String productType = "";
    private Context context;
    private boolean isLogin = false;
    private String token = "";
    private String loginName = "";
    private AppInfoBean updateAppInfoBean;

    public static LMS getInstance() {
        if (instance == null) {
            synchronized (LMS.class) {
                if (instance == null) {
                    instance = new LMS();
                }
            }
        }
        return instance;
    }

    public LMS init(Context context) {
        if (context != null) {
            this.context = context.getApplicationContext();
            LMS.mContext = this.context;
        }
        return this;
    }

    // Stub methods for initialization configuration
    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public void setLoginType(String loginType) {
    }

    public void setSoftwareCode(String softwareCode) {
    }

    public void setEnabledLog(boolean enabled) {
    }

    public void setPrivacyPolicy(String url) {
    }

    public void setServicesAgreement(String url) {
    }

    public void initXutils() {
    }

    public void setWxAppId(String appId) {
    }

    public void setBuglyAppId(String buglyAppId) {
    }

    public void setAppKey(String appKey) {
    }

    public void setAppSecret(String appSecret) {
    }

    public void setAuthSecret(String authSecret) {
    }

    // Authentication methods
    public boolean isLogin() {
        return isLogin;
    }

    public String getToken() {
        return token;
    }

    public String getLoginName() {
        return loginName;
    }

    public void activityLogin() {
    }

    public void activityLogin(Object param) {
    }

    public void activityLogin(Object param, LoginCallback callback) {
        if (callback != null) {
            callback.onResult(true); // Stub: always success
        }
    }

    public void activityLogin(Object param1, Object param2, boolean param3, Object param4, Object param5) {
    }

    public void activityUserInfo(@NotNull Context requireContext) {
    }

    public void activityEnv() {
    }

    public void syncUserInfo() {
    }

    // User info methods
    public void getUserInfo(UserInfoCallback callback) {
        if (callback != null) {
            // Return empty user info
            callback.onResult(new CommonBean());
        }
    }

    // App update methods
    public void checkAppUpdate(IResponseCallback callback) {
        if (callback != null) {
            // Return no update available
            callback.onResponse("{\"code\":\"2000\",\"data\":{\"hasNewVersion\":false}}");
        }
    }

    public AppInfoBean getUpdateAppInfoBean() {
        if (updateAppInfoBean == null) {
            updateAppInfoBean = new AppInfoBean();
        }
        return updateAppInfoBean;
    }

    // Statement/policy methods
    public void getStatement(String type, IResponseCallback callback) {
        if (callback != null) {
            callback.onResponse("{\"code\":\"2000\",\"data\":{\"url\":\"\"}}");
        }
    }

    // Device binding
    public void bindDevice(String sn, String randomNum, String param3, String param4, IResponseCallback callback) {
        if (callback != null) {
            callback.onResponse("{\"code\":\"2000\",\"message\":\"success\"}");
        }
    }

    // File upload methods - multiple signatures for compatibility
    public void uploadFile(File file, int param1, int param2, int param3, IResponseCallback callback) {
        uploadFileInternal(file, callback);
    }

    public void uploadFile(File file, IResponseCallback callback) {
        uploadFileInternal(file, callback);
    }

    private void uploadFileInternal(File file, IResponseCallback callback) {
        if (callback != null) {
            if (file == null || !file.exists()) {
                callback.onFail(new Exception("File not found or invalid"));
                return;
            }

            // Simulate file upload with more realistic response including file data
            String fileName = file.getName();
            String fileId = "file_" + System.currentTimeMillis() + "_" + fileName.hashCode();
            String response = String.format(
                    "{\"code\":\"2000\",\"message\":\"success\",\"data\":{\"fileSecret\":\"%s\",\"url\":\"https://example.com/uploads/%s\"}}",
                    fileId,
                    fileName
            );
            callback.onResponse(response);
        }
    }

    public interface UserInfoCallback {
        void onResult(CommonBean userinfo);
    }

    public interface LoginCallback {
        void onResult(boolean success);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\network\HttpProxy.java =====

package com.mpdc4gsr.libunified.app.lms.network;

public class HttpProxy {
    private static volatile HttpProxy instance;

    public static HttpProxy getInstance() {
        if (instance == null) {
            synchronized (HttpProxy.class) {
                if (instance == null) {
                    instance = new HttpProxy();
                }
            }
        }
        return instance;
    }

    public static HttpProxy getInstant() {
        return getInstance();
    }

    public void post(String url, Object params, IResponseCallback callback) {
        if (callback != null) {
            callback.onResponse("{\"code\":\"2000\",\"message\":\"success\"}");
        }
    }

    public void post(String url, boolean param, Object params, IResponseCallback callback) {
        post(url, params, callback);
    }

    // For backward compatibility
    public static class Companion {
        // Static reference for Kotlin import compatibility
        public static final HttpProxy instant = getInstance();

        public static HttpProxy getInstant() {
            return HttpProxy.getInstance();
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\network\IResponseCallback.java =====

package com.mpdc4gsr.libunified.app.lms.network;

public interface IResponseCallback {
    void onResponse(String response);

    default void onFail(Exception e) {
    }

    default void onFail(String failMsg, String errorCode) {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\network\ResponseBean.java =====

package com.mpdc4gsr.libunified.app.lms.network;

import com.google.gson.Gson;
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean;

public class ResponseBean {
    public String code = "2000";
    public String message = "success";
    public Object data = null;

    public ResponseBean() {
    }

    public static CommonBean convertCommonBean(String response, Object defaultData) {
        CommonBean bean = new CommonBean();
        if (response != null && !response.isEmpty()) {
            try {
                // Properly parse JSON response using Gson
                ResponseBean responseBean = new Gson().fromJson(response, ResponseBean.class);
                if (responseBean != null) {
                    bean.code = responseBean.code;
                    bean.data = responseBean.data != null ? responseBean.data.toString() : "";
                } else {
                    // If parsing fails, treat as error
                    bean.code = "error";
                }
                if (defaultData != null) {
                    bean.data = defaultData.toString();
                }
            } catch (Exception e) {
                bean.code = "error";
            }
        } else {
            // Empty or null response should be treated as error
            bean.code = "error";
        }
        return bean;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\package-info.java =====

package com.mpdc4gsr.libunified.app.lms;


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\UrlConstants.java =====

package com.mpdc4gsr.libunified.app.lms;

public class UrlConstants {
    public static volatile String BASE_URL = "https://example.com/";

    public static synchronized void setBaseUrl(String url, boolean useHttps) {
        // The useHttps parameter is ignored in this stub implementation
        BASE_URL = url;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\utils\ConstantUtils.java =====

package com.mpdc4gsr.libunified.app.lms.utils;

public class ConstantUtils {
    public static final String LOGIN_TS001_TYPE = "TS001";
    public static final String LOGIN_TC001_TYPE = "TC001";
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\utils\DateUtils.java =====

package com.mpdc4gsr.libunified.app.lms.utils;

public class DateUtils {
    public static String formatDate(long timestamp) {
        return "";
    }

    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\utils\LanguageUtils.java =====

package com.mpdc4gsr.libunified.app.lms.utils;

import android.content.Context;

public class LanguageUtils {
    public static String getLanguageId(Context context) {
        return "en";
    }

    public static String getCurrentLanguage() {
        return "en";
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\utils\NetworkUtils.java =====

package com.mpdc4gsr.libunified.app.lms.utils;

import android.content.Context;

public class NetworkUtils {
    public static boolean isNetworkAvailable() {
        return true;
    }

    public static boolean isWifiConnected() {
        return false;
    }

    public static boolean isConnected(Context context) {
        return true;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\utils\SPUtils.java =====

package com.mpdc4gsr.libunified.app.lms.utils;

import android.content.Context;

public class SPUtils {
    private static volatile SPUtils instance;

    public static SPUtils getInstance(Context context) {
        // The context parameter is unused in this stub implementation
        if (instance == null) {
            synchronized (SPUtils.class) {
                if (instance == null) {
                    instance = new SPUtils();
                }
            }
        }
        return instance;
    }

    public void put(String key, Object value) {
    }

    public String getString(String key) {
        return "";
    }

    public String getString(String key, String defaultValue) {
        return defaultValue;
    }

    public boolean getBoolean(String key) {
        return false;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return defaultValue;
    }

    public int getInt(String key) {
        return 0;
    }

    public int getInt(String key, int defaultValue) {
        return defaultValue;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\utils\StringUtils.java =====

package com.mpdc4gsr.libunified.app.lms.utils;

import android.content.Context;

public class StringUtils {
    public static String getResString(Context context, int resId) {
        return "";
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\utils\TLog.java =====

package com.mpdc4gsr.libunified.app.lms.utils;

public class TLog {
    public static void d(String tag, String message) {
    }

    public static void i(String tag, String message) {
    }

    public static void w(String tag, String message) {
    }

    public static void e(String tag, String message) {
    }

    public static void e(String tag, String message, Throwable throwable) {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\weiget\LmsLoadDialog.java =====

package com.mpdc4gsr.libunified.app.lms.weiget;

import android.app.Dialog;
import android.content.Context;

public class LmsLoadDialog extends Dialog {
    public LmsLoadDialog(Context context) {
        super(context);
    }

    public void setMessage(String message) {
    }

    public void setCancelable(boolean cancelable) {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\weiget\LmsLoadView.java =====

package com.mpdc4gsr.libunified.app.lms.weiget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class LmsLoadView extends View {
    private static final int DEFAULT_DOT_COUNT = 3;
    private static final int DEFAULT_DOT_RADIUS = 5;
    private static final int DEFAULT_DOT_SPACING = 6;
    private static final float DEFAULT_MOVE_RATE = 0.75f;
    private static final int DEFAULT_DOT_COLOR = 0xFFFFFFFF; // White

    private Paint dotPaint;
    private int dotCount = DEFAULT_DOT_COUNT;
    private float dotRadius = DEFAULT_DOT_RADIUS;
    private float dotSpacing = DEFAULT_DOT_SPACING;
    private float moveRate = DEFAULT_MOVE_RATE;
    private int dotColor = DEFAULT_DOT_COLOR;

    private ObjectAnimator animator;
    private float animationProgress = 0f;

    public LmsLoadView(Context context) {
        this(context, null);
    }

    public LmsLoadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LmsLoadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs);
        initPaint();
        initAnimation();
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        if (attrs != null) {
            // Convert dp to pixels for default values
            float density = context.getResources().getDisplayMetrics().density;
            dotRadius = DEFAULT_DOT_RADIUS * density;
            dotSpacing = DEFAULT_DOT_SPACING * density;

            // If we had styleable attributes, we would parse them here
            // For now, using defaults and hardcoded values
        }
    }

    private void initPaint() {
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(dotColor);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    private void initAnimation() {
        animator = ObjectAnimator.ofFloat(this, "animationProgress", 0f, 1f);
        animator.setDuration(1500);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float totalWidth = (dotCount * dotRadius * 2) + ((dotCount - 1) * dotSpacing);
        float totalHeight = dotRadius * 2;

        int width = (int) totalWidth;
        int height = (int) totalHeight;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerY = getHeight() / 2f;
        float startX = dotRadius;

        for (int i = 0; i < dotCount; i++) {
            float centerX = startX + (i * (dotRadius * 2 + dotSpacing));

            // Calculate alpha based on animation progress
            float phase = (animationProgress + (i * 0.2f)) % 1f;
            int alpha = (int) (255 * (0.3f + 0.7f * Math.sin(phase * Math.PI * 2)));

            dotPaint.setAlpha(Math.max(50, Math.min(255, alpha)));
            canvas.drawCircle(centerX, centerY, dotRadius, dotPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    public void startAnimation() {
        if (animator != null && !animator.isStarted() && isAttachedToWindow()) {
            animator.start();
        }
    }

    public void stopAnimation() {
        if (animator != null && animator.isStarted()) {
            animator.cancel();
        }
    }

    // Getters for animation progress
    public float getAnimationProgress() {
        return animationProgress;
    }

    // Setter for ObjectAnimator
    public void setAnimationProgress(float progress) {
        this.animationProgress = progress;
        invalidate();
    }

    // Public setters for customization
    public void setDotColor(int color) {
        this.dotColor = color;
        dotPaint.setColor(color);
        invalidate();
    }

    public void setDotRadius(float radius) {
        this.dotRadius = radius;
        requestLayout();
    }

    public void setDotSpacing(float spacing) {
        this.dotSpacing = spacing;
        requestLayout();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\weiget\TToast.java =====

package com.mpdc4gsr.libunified.app.lms.weiget;

import android.content.Context;

import com.mpdc4gsr.libunified.app.compose.components.ComposeToastHelper;

public class TToast {
    private static final long SHORT_DURATION = 2000L;
    private static final long LONG_DURATION = 3500L;

    public static void shortToast(Context context, String message) {
        if (context != null && message != null && !message.isEmpty()) {
            ComposeToastHelper.INSTANCE.show(context, message, SHORT_DURATION);
        }
    }

    public static void shortToast(Context context, int resId) {
        if (context != null) {
            ComposeToastHelper.INSTANCE.show(context, resId, SHORT_DURATION);
        }
    }

    public static void longToast(Context context, String message) {
        if (context != null && message != null && !message.isEmpty()) {
            ComposeToastHelper.INSTANCE.show(context, message, LONG_DURATION);
        }
    }

    public static void longToast(Context context, int resId) {
        if (context != null) {
            ComposeToastHelper.INSTANCE.show(context, resId, LONG_DURATION);
        }
    }

    public static void show(Context context, String message) {
        shortToast(context, message);
    }

    public static void show(Context context, int resId) {
        shortToast(context, resId);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\common\Callback.java =====

package com.mpdc4gsr.libunified.app.lms.xutils.common;

public interface Callback<T> {
    void onSuccess(T result);

    void onError(Throwable ex, boolean isOnCallback);

    void onCancelled(CancelledException cex);

    void onFinished();

    // Add CommonCallback interface
    interface CommonCallback<T> extends Callback<T> {
        // Default implementation for stub
        default void onSuccess(T result) {
        }

        default void onError(Throwable ex, boolean isOnCallback) {
        }

        default void onCancelled(CancelledException cex) {
        }

        default void onFinished() {
        }
    }

    // Add ProgressCallback interface
    interface ProgressCallback<T> extends Callback<T> {
        void onWaiting();

        void onStarted();

        void onLoading(long total, long current, boolean isDownloading);
    }

    class CancelledException extends Exception {
        public CancelledException(String message) {
            super(message);
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\common\task\PriorityExecutor.java =====

package com.mpdc4gsr.libunified.app.lms.xutils.common.task;

public class PriorityExecutor {
    private int corePoolSize;
    private boolean allowCoreThreadTimeOut;

    public PriorityExecutor() {
        this.corePoolSize = 1;
        this.allowCoreThreadTimeOut = false;
    }

    public PriorityExecutor(int corePoolSize, boolean allowCoreThreadTimeOut) {
        this.corePoolSize = corePoolSize;
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public static void execute(Runnable task) {
        // Execute immediately - stub implementation
        if (task != null) {
            task.run();
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\http\RequestParams.java =====

package com.mpdc4gsr.libunified.app.lms.xutils.http;

import com.mpdc4gsr.libunified.app.lms.xutils.common.task.PriorityExecutor;

public class RequestParams {
    public String uri;
    public boolean isAsJsonContent = false;
    private String saveFilePath;
    private String cacheDirName;
    private boolean autoResume = false;
    private PriorityExecutor executor;

    public RequestParams() {
    }

    public RequestParams(String url) {
        this.uri = url;
    }

    public void addParameter(String key, Object value) {
    }

    public void addBodyParameter(String key, Object value) {
    }

    public void addHeader(String key, String value) {
    }

    public void setSaveFilePath(String path) {
        this.saveFilePath = path;
    }

    public void setCacheDirName(String name) {
        this.cacheDirName = name;
    }

    public void setAutoResume(boolean autoResume) {
        this.autoResume = autoResume;
    }

    public void setExecutor(PriorityExecutor executor) {
        this.executor = executor;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\x.java =====

package com.mpdc4gsr.libunified.app.lms.xutils;

import com.mpdc4gsr.libunified.app.lms.xutils.common.Callback;
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams;

import java.io.File;

public class x {
    private static HttpManager httpManager = new HttpManager();

    public static HttpManager http() {
        return httpManager;
    }

    public static class HttpManager {
        public void post(RequestParams params, Callback.CommonCallback<String> callback) {
            // Stub implementation - do nothing
        }

        public void get(RequestParams params, Callback.ProgressCallback<File> callback) {
            // Stub implementation - do nothing for now
            // In a real implementation, this would handle HTTP GET requests with progress callbacks
        }
    }
}