package dev.srlordm.slmvoice.config

import dev.srlordm.slmvoice.audio.data.AudioConfiguration
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.audio.play.ArrayAudioProvider
import dev.srlordm.slmvoice.audio.play.AudioProvider
import dev.srlordm.slmvoice.audio.play.StreamAudioProvider
import dev.srlordm.slmvoice.util.convert
import dev.srlordm.slmvoice.util.read
import javax.sound.sampled.AudioFormat
import kotlin.io.path.extension

enum class AudioProviderType {
    STREAM {
        override fun create(
            entry: SoundEntry,
            format: AudioFormat,
            configuration: AudioConfiguration
        ): AudioProvider {
            val file = entry.id.path(file = true)
            return if (file?.extension?.equals("mp3", ignoreCase = true) == true) {
                ArrayAudioProvider(entry.read(format), format, configuration)
            } else {
                StreamAudioProvider(entry.supplier().convert(format), configuration)
            }
        }
    },
    ARRAY {
        override fun create(
            entry: SoundEntry,
            format: AudioFormat,
            configuration: AudioConfiguration
        ) = ArrayAudioProvider(entry.read(format), format, configuration)
    };

    abstract fun create(
        entry: SoundEntry,
        format: AudioFormat,
        configuration: AudioConfiguration = AudioConfig[entry] ?: AudioConfiguration.DEFAULT
    ): AudioProvider
}
