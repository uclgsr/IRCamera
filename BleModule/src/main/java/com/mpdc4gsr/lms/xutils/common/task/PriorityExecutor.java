package com.mpdc4gsr.lms.xutils.common.task;


public class PriorityExecutor {
    public static void execute(Runnable task) {
        // Execute immediately - stub implementation
        if (task != null) {
            task.run();
        }
    }
}