package com.mpdc4gsr.libunified.app.lms.network;

import com.blankj.utilcode.util.GsonUtils;
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
                // Properly parse JSON response using GsonUtils
                ResponseBean responseBean = GsonUtils.fromJson(response, ResponseBean.class);
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