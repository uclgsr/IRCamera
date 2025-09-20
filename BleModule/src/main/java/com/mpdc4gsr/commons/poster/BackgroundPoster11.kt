package com.mpdc4gsr.commons.poster

import java.util.Objects
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import kotlin.concurrent.Volatile

internal class BackgroundPoster(private val executorService: ExecutorService) : Runnable, Poster {
    private val queue: Queue<Runnable?>

    @Volatile
    private var executorRunning = false

    init {
        queue = ConcurrentLinkedQueue<Runnable?>()
    }

    override fun enqueue(runnable: Runnable) {
        Objects.requireNonNull<Runnable?>(runnable, "runnable is null, cannot be enqueued")
        synchronized(this) {
            queue.add(runnable)
            if (!executorRunning) {
                executorRunning = true
                executorService.execute(this)
            }
        }
    }

    override fun clear() {
        synchronized(this) {
            queue.clear()
        }
    }

    override fun run() {
        try {
            while (true) {
                var runnable = queue.poll()
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll()
                        if (runnable == null) {
                            executorRunning = false
                            return
                        }
                    }
                }
                runnable!!.run()
            }
        } finally {
            executorRunning = false
        }
    }
}
