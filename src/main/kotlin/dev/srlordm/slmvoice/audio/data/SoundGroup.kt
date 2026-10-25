package dev.srlordm.slmvoice.audio.data

import dev.kikugie.kowoui.fallbackTranslation
import dev.srlordm.slmvoice.MOD_ID
import dev.srlordm.slmvoice.audio.prefix
import net.minecraft.text.Text

data class SoundGroup(
    val id: SoundId,
    val entries: Map<SoundId, SoundEntry>,
    private val text: Text? = null,
) {
    val title: Text by lazy {
        text ?: id.run {
            val key = "$MOD_ID.dir.$namespace${directory.replace('/', '.').prefix(".")}"
            val fallback = str.removePrefix("$MOD_ID:")
            key.fallbackTranslation(fallback)
        }
    }

    fun isEmpty() = entries.isEmpty()
    operator fun get(id: SoundId) = entries[id]
}