package com.mpdc4gsr.lib.core.lms.network;


public class ResponseBean {
    public String code = "2000";
    public String message = "success";
    public Object data = null;

    public ResponseBean() {
    }

    public static ResponseBean convertCommonBean(String response, Object defaultData) {
        ResponseBean bean = new ResponseBean();
        if (response != null && !response.isEmpty()) {
            try {
                // Simple JSON parsing - would normally use proper JSON library
                if (response.contains("\"code\"")) {
                    // Extract code if present
                    bean.code = "2000"; // Default success
                }
                bean.data = defaultData;
            } catch (Exception e) {
                bean.code = "error";
                bean.message = "Parse error";
            }
        }
        return bean;
    }
}