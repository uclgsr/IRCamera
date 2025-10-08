// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\common\task' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


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