package dev.srlordm.slmvoice.entrypoint

import dev.srlordm.slmvoice.LOGGER
import dev.srlordm.slmvoice.Soundboard
import dev.srlordm.slmvoice.audio.data.AudioConfiguration
import dev.srlordm.slmvoice.audio.data.SoundId
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.audio.play.ArrayAudioProvider
import dev.srlordm.slmvoice.audio.play.AudioPreparation
import dev.srlordm.slmvoice.audio.play.AudioProvider
import dev.srlordm.slmvoice.audio.play.AudioScheduler
import dev.srlordm.slmvoice.config.AudioConfig
import dev.srlordm.slmvoice.config.AudioProviderType
import dev.srlordm.slmvoice.util.read
import dev.srlordm.slmvoice.util.client
import net.minecraft.text.Text
import javax.sound.sampled.AudioFormat
import java.util.concurrent.CancellationException

interface SoundboardEntrypoint {
    val format: AudioFormat
    val connected: Boolean
    val frameSize get() = format.sampleRate.toInt() / 50

    val scheduler: AudioScheduler
    val muted: Boolean

    fun schedule(entry: SoundEntry, local: Boolean) {
        val requestId = scheduler.beginRequest(entry.id)
        val providerType = Soundboard.config.provider
        val audioFormat = format
        val configuration = AudioConfig[entry] ?: AudioConfiguration.DEFAULT

        try {
            AudioPreparation.submitLatest {
                try {
                    val provider = providerType.create(entry, audioFormat, configuration)
                    scheduler.completeRequest(requestId, provider, local)
                } catch (cancelled: CancellationException) {
                    scheduler.failRequest(requestId)
                } catch (error: Throwable) {
                    if (scheduler.failRequest(requestId)) {
                        LOGGER.error("Failed to load audio '${entry.id}'", error)
                        client.execute {
                            if (scheduler.isCurrentRequest(requestId)) {
                                client.player?.sendMessage(
                                    Text.literal("SLM Voice: Could not play '${entry.id.file}'. The file may use an unsupported MP3 encoding or metadata."),
                                    false
                                )
                            }
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            if (scheduler.failRequest(requestId)) {
                LOGGER.error("Failed to queue audio '${entry.id}' for preparation", error)
                client.player?.sendMessage(
                    Text.literal("SLM Voice: Could not prepare '${entry.id.file}'."),
                    false
                )
            }
        }
    }

    fun schedule(provider: AudioProvider, local: Boolean) =
        scheduler.schedule(provider, local)

    fun scheduleArray(
        data: ShortArray,
        local: Boolean,
        configuration: AudioConfiguration,
        soundId: SoundId? = null,
    ) {
        val requestId = scheduler.beginRequest(soundId)
        runCatching {
            ArrayAudioProvider(data, format, configuration)
        }.onSuccess {
            scheduler.completeRequest(requestId, it, local)
        }.onFailure {
            scheduler.failRequest(requestId)
            LOGGER.error("Failed to load edited audio buffer", it)
            client.player?.sendMessage(Text.literal("SLM Voice: Could not play this audio segment."), false)
        }
    }
}
