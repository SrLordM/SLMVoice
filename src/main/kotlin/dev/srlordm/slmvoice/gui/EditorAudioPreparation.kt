package dev.srlordm.slmvoice.gui

import dev.srlordm.slmvoice.LOGGER
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.util.client
import dev.srlordm.slmvoice.util.read
import java.util.concurrent.CancellationException
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import kotlin.math.min

data class PreparedEditorAudio(
    val samples: ShortArray,
    val waveform: DoubleArray,
)

object EditorAudioPreparation {
    private const val WAVEFORM_BINS = 2048

    private val lock = Any()
    private val executor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(1),
        ThreadFactory { task ->
            Thread(task, "SLM Voice Editor Audio Preparation").apply { isDaemon = true }
        }
    )

    private var latestTask: FutureTask<Unit>? = null
    private var closed = false

    fun submitLatest(
        entry: SoundEntry,
        format: AudioFormat,
        onProgress: (String) -> Unit,
        onComplete: (Result<PreparedEditorAudio>) -> Unit,
    ) {
        synchronized(lock) {
            check(!closed) { "Editor audio preparation executor is shut down" }
            cancelLatestLocked()

            val task = FutureTask(Callable {
                val startedAt = System.nanoTime()
                var stage = "reading and decoding"
                var sampleCount: Int? = null
                var decodeMillis = 0L
                var waveformMillis = 0L
                LOGGER.info("Preparing Audio Settings data for '{}'", entry.id)
                dispatch { onProgress("Reading and decoding audio...") }

                val result = try {
                    val decodeStartedAt = System.nanoTime()
                    val samples = entry.read(format)
                    sampleCount = samples.size
                    decodeMillis = elapsedMillis(decodeStartedAt)
                    check(samples.isNotEmpty()) { "Audio contains no samples" }
                    stage = "waveform preparation"
                    dispatch { onProgress("Preparing waveform...") }
                    val waveformStartedAt = System.nanoTime()
                    val waveform = samples.toWaveform()
                    waveformMillis = elapsedMillis(waveformStartedAt)
                    Result.success(PreparedEditorAudio(samples, waveform))
                } catch (error: Throwable) {
                    if (error is CancellationException || error is InterruptedException || Thread.currentThread().isInterrupted) {
                        LOGGER.debug("Audio Settings preparation was cancelled for '{}'", entry.id)
                    } else {
                        LOGGER.error(
                            "Failed during {} for Audio Settings '{}'; samples={}, elapsed={}ms",
                            stage,
                            entry.id,
                            sampleCount ?: "unknown",
                            elapsedMillis(startedAt),
                            error,
                        )
                    }
                    Result.failure(error)
                }

                if (result.isSuccess) {
                    LOGGER.info(
                        "Prepared Audio Settings for '{}' ({} samples) in {}ms; decode={}ms, waveform={}ms",
                        entry.id,
                        result.getOrThrow().samples.size,
                        elapsedMillis(startedAt),
                        decodeMillis,
                        waveformMillis,
                    )
                }
                dispatch { onComplete(result) }
            })
            latestTask = task
            executor.execute(task)
        }
    }

    fun cancelLatest() {
        synchronized(lock) { cancelLatestLocked() }
    }

    fun shutdown() {
        synchronized(lock) {
            if (closed) return
            closed = true
            cancelLatestLocked()
            executor.shutdownNow()
        }
    }

    private fun cancelLatestLocked() {
        latestTask?.let {
            it.cancel(true)
            executor.remove(it)
        }
        latestTask = null
    }

    private fun dispatch(action: () -> Unit) {
        try {
            client.execute(action)
        } catch (error: Throwable) {
            LOGGER.error("Failed to dispatch Audio Settings update to the Minecraft client thread", error)
        }
    }

    private fun elapsedMillis(startedAt: Long) =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

    private fun ShortArray.toWaveform(): DoubleArray {
        val bins = min(size, WAVEFORM_BINS)
        val sampleCount = size.toLong()
        val binCount = bins.toLong()
        return DoubleArray(bins) { bin ->
            val start = (bin.toLong() * sampleCount / binCount).toInt()
            val end = ((bin + 1L) * sampleCount / binCount).toInt().coerceAtLeast(start + 1)
            var sum = 0L
            for (index in start until end) {
                if ((index and 0x3FFF) == 0 && Thread.currentThread().isInterrupted) {
                    throw InterruptedException("Waveform preparation was cancelled")
                }
                sum += this[index]
            }
            sum.toDouble() / (end - start)
        }
    }
}
