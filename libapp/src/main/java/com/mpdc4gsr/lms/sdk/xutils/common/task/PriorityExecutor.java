package com.mpdc4gsr.lms.sdk.xutils.common.task;

/**
 * Priority Executor stub for LMS SDK
 */
public class PriorityExecutor {
    public static void execute(Runnable task) {
        // Execute immediately - stub implementation
        if (task != null) {
            task.run();
        }
    }
}