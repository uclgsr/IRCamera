package com.mpdc4gsr.commons.poster

import java.util.Objects
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService

internal class AsyncPoster(private val executorService: ExecutorService) : Runnable, Poster {
    private val queue: Queue<Runnable?>

    init {
        queue = ConcurrentLinkedQueue<Runnable?>()
    }

    override fun enqueue(runnable: Runnable) {
        Objects.requireNonNull<Runnable?>(runnable, "runnable is null, cannot be enqueued")
        queue.add(runnable)
        executorService.execute(this)
    }

    override fun clear() {
        synchronized(this) {
            queue.clear()
        }
    }

    override fun run() {
        val runnable = queue.poll()
        if (runnable != null) {
            runnable.run()
        }
    }
}
