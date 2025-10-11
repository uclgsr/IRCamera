package com.mpdc4gsr.component.shared.app.lms;

import android.content.Context;

import com.mpdc4gsr.component.shared.app.lms.bean.AppInfoBean;
import com.mpdc4gsr.component.shared.app.lms.bean.CommonBean;
import com.mpdc4gsr.component.shared.app.lms.network.IResponseCallback;

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

