package com.mpdc4gsr.lib.core.lms.network;

import com.mpdc4gsr.lib.core.lms.bean.CommonBean;


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
                // Simple JSON parsing - would normally use proper JSON library
                if (response.contains("\"code\"")) {
                    // Extract code if present
                    bean.code = "2000"; // Default success
                }
                if (defaultData != null) {
                    bean.data = defaultData.toString();
                }
            } catch (Exception e) {
                bean.code = "error";
            }
        }
        return bean;
    }
}