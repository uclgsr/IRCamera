package com.mpdc4gsr.lms.sdk;

import android.content.Context;

import com.mpdc4gsr.lms.sdk.bean.AppInfoBean;
import com.mpdc4gsr.lms.sdk.bean.CommonBean;
import com.mpdc4gsr.lms.sdk.network.IResponseCallback;

import java.io.File;

/**
 * LMS SDK Stub Implementation
 * This is a minimal stub to replace the proprietary lms_international AAR
 */
public class LMS {
    public static final String SUCCESS = "2000";
    public static Context mContext;

    private static volatile LMS instance;
    public String language = "en";
    public String softwareCode = "";
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
    public void setProductType(String productType) {
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

    public void activityUserInfo() {
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
            callback.onResponse("{\"code\":2000,\"data\":{\"hasNewVersion\":false}}");
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

    // File upload
    public void uploadFile(File file, int param1, int param2, int param3, IResponseCallback callback) {
        if (callback != null) {
            callback.onResponse("{\"code\":\"2000\",\"message\":\"success\"}");
        }
    }

    public interface UserInfoCallback {
        void onResult(CommonBean userinfo);
    }

    public interface LoginCallback {
        void onResult(boolean success);
    }
}