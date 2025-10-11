package com.mpdc4gsr.component.shared.app.lms.bean;

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

