// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\network' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


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