package com.mpdc4gsr.component.shared.app.lms.xutils.common.task;

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

