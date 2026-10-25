package dev.srlordm.slmvoice.audio.play

import java.util.concurrent.FutureTask
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object AudioPreparation {
    private val lock = Any()
    private val executor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        ThreadFactory { task ->
            Thread(task, "SLM Voice Audio Preparation").apply { isDaemon = true }
        }
    )

    private var latestTask: FutureTask<Unit>? = null
    private var closed = false

    fun submitLatest(action: () -> Unit) {
        synchronized(lock) {
            check(!closed) { "Audio preparation executor is shut down" }
            cancelLatestLocked()
            val task = FutureTask(Callable { action() })
            latestTask = task
            executor.execute(task)
        }
    }

    fun cancelLatest() {
        synchronized(lock) { cancelLatestLocked() }
    }

    fun executeCleanup(action: () -> Unit) {
        synchronized(lock) {
            if (closed) action()
            else executor.execute(action)
        }
    }

    fun shutdown() {
        synchronized(lock) {
            if (closed) return
            closed = true
            cancelLatestLocked()
            executor.shutdown()
        }
    }

    private fun cancelLatestLocked() {
        latestTask?.let {
            it.cancel(true)
            executor.remove(it)
        }
        latestTask = null
    }
}
