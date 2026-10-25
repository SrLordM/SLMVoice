package dev.srlordm.slmvoice.audio.play

import dev.srlordm.slmvoice.LOGGER
import dev.srlordm.slmvoice.audio.data.SoundId
import dev.srlordm.slmvoice.entrypoint.SoundboardEntrypoint
import java.util.concurrent.atomic.AtomicReference

class AudioScheduler(val entry: SoundboardEntrypoint) {
    private sealed interface State {
        val requestId: Long
        val soundId: SoundId?

        data class Idle(override val requestId: Long) : State {
            override val soundId: SoundId? = null
        }
        data class Loading(override val requestId: Long, override val soundId: SoundId?) : State
        data class Ready(
            override val requestId: Long,
            override val soundId: SoundId?,
            val provider: AudioProvider,
            val local: Boolean,
        ) : State
        data class Playing(override val requestId: Long, override val soundId: SoundId?) : State
    }

    private class ActivePlayback(
        val requestId: Long,
        val provider: AudioProvider,
        val local: Boolean,
    ) {
        private var closed = false

        @Synchronized
        fun next(samples: Int): ShortArray? = if (closed) null else provider.next(samples)

        @Synchronized
        fun close() {
            if (closed) return
            closed = true
            provider.close()
        }
    }

    private val state = AtomicReference<State>(State.Idle(0L))
    private val active = AtomicReference<ActivePlayback?>(null)

    @Volatile
    var local: Boolean = false
        private set

    val playing: Boolean
        get() = state.get() !is State.Idle

    val playingSoundId: SoundId?
        get() = (state.get() as? State.Playing)?.soundId

    fun beginRequest(soundId: SoundId? = null): Long {
        while (true) {
            val current = state.get()
            val loading = State.Loading(current.requestId + 1, soundId)
            if (!state.compareAndSet(current, loading)) continue
            if (current is State.Ready) closeProviderLater(current.provider)
            stopActivePlayback()
            AudioPreparation.cancelLatest()
            return loading.requestId
        }
    }

    fun completeRequest(requestId: Long, provider: AudioProvider, local: Boolean): Boolean {
        while (true) {
            val current = state.get()
            if (current !is State.Loading || current.requestId != requestId) {
                provider.close()
                return false
            }
            if (state.compareAndSet(current, State.Ready(requestId, current.soundId, provider, local))) return true
        }
    }

    fun failRequest(requestId: Long): Boolean {
        while (true) {
            val current = state.get()
            if (current !is State.Loading || current.requestId != requestId) return false
            if (state.compareAndSet(current, State.Idle(requestId))) return true
        }
    }

    fun isCurrentRequest(requestId: Long): Boolean = state.get().requestId == requestId

    fun schedule(provider: AudioProvider, local: Boolean) {
        val requestId = beginRequest()
        completeRequest(requestId, provider, local)
    }

    fun next(): ShortArray? {
        while (true) {
            when (val current = state.get()) {
                is State.Idle -> {
                    closeActive()
                    return null
                }
                is State.Loading -> return null
                is State.Ready -> {
                    val playing = State.Playing(current.requestId, current.soundId)
                    if (!state.compareAndSet(current, playing)) continue
                    val playback = ActivePlayback(current.requestId, current.provider, current.local)
                    if (state.get() != playing || !installPlayback(playback)) {
                        closePlaybackLater(playback)
                        return null
                    }
                    local = current.local
                    if (state.get() != playing || active.get() !== playback) {
                        if (active.compareAndSet(playback, null)) closePlaybackLater(playback)
                        local = false
                        return null
                    }
                    return nextActive(playback)
                }
                is State.Playing -> return nextActive()
            }
        }
    }

    fun reset() {
        while (true) {
            val current = state.get()
            val idle = State.Idle(current.requestId + 1)
            if (!state.compareAndSet(current, idle)) continue
            if (current is State.Ready) closeProviderLater(current.provider)
            active.getAndSet(null)?.let { playback ->
                closePlaybackLater(playback)
            }
            break
        }
        AudioPreparation.cancelLatest()
        local = false
    }

    fun shutdown() {
        while (true) {
            val current = state.get()
            if (state.compareAndSet(current, State.Idle(current.requestId + 1))) {
                if (current is State.Ready) closeProviderLater(current.provider)
                break
            }
        }
        active.getAndSet(null)?.let(::closePlaybackLater)
        AudioPreparation.cancelLatest()
        local = false
    }

    private fun nextActive(playback: ActivePlayback? = active.get()): ShortArray? {
        playback ?: return null
        val data = runCatching { playback.next(entry.frameSize) }
            .onFailure { LOGGER.error("Failed while streaming scheduled audio", it) }
            .getOrNull()

        if (data != null && active.get() === playback && state.get() !is State.Idle) return data

        if (active.compareAndSet(playback, null)) {
            closePlaybackLater(playback)
            local = false
        }
        while (true) {
            val current = state.get()
            if (current !is State.Playing || current.requestId != playback.requestId) break
            if (state.compareAndSet(current, State.Idle(current.requestId))) break
        }
        return null
    }

    private fun closeActive() {
        active.getAndSet(null)?.let { playback ->
            closePlaybackLater(playback)
        }
        local = false
    }

    private fun stopActivePlayback() {
        active.getAndSet(null)?.let { playback ->
            closePlaybackLater(playback)
        }
        local = false
    }

    private fun closePlaybackLater(playback: ActivePlayback) {
        AudioPreparation.executeCleanup { runCatching { playback.close() } }
    }

    private fun closeProviderLater(provider: AudioProvider) {
        AudioPreparation.executeCleanup { runCatching { provider.close() } }
    }

    private fun installPlayback(playback: ActivePlayback): Boolean {
        while (true) {
            val previous = active.get()
            if (previous != null && previous.requestId > playback.requestId) return false
            if (!active.compareAndSet(previous, playback)) continue
            previous?.let(::closePlaybackLater)
            return true
        }
    }
}
