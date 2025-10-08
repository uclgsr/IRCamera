// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\bean' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


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