// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\activity' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


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