package dev.srlordm.slmvoice.audio.data

import dev.kikugie.kowoui.fallbackTranslation
import dev.srlordm.slmvoice.MOD_ID
import dev.srlordm.slmvoice.audio.prefix
import net.minecraft.text.Text
import java.io.InputStream

data class SoundEntry(
    val id: SoundId,
    val supplier: () -> InputStream,
    val settings: AudioConfiguration? = null,
    private val text: Text? = null,
) {
    val group: SoundId by lazy { id.parent() }
    val title: Text by lazy {
        text ?: id.run {
            // example:slmvoice/sound.wav -> slmvoice.file.example.sound
            "$MOD_ID.file.$namespace${id.path.replace('/', '.').prefix(".")}".fallbackTranslation(file)
        }
    }
}
