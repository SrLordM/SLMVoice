package dev.srlordm.slmvoice.audio.registry

import dev.kikugie.kowoui.translation
import dev.srlordm.slmvoice.CONFIG
import dev.srlordm.slmvoice.MOD_ID
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.audio.data.SoundGroup
import dev.srlordm.slmvoice.audio.data.SoundId

object SoundRegistry {
    lateinit var favourites: SoundGroup
        private set

    val initialized get() = FileAudioHolder.initialized
    val groups get() = FileAudioHolder.groups + ResourceAudioHolder.groups
    val entries get() = FileAudioHolder.entries + ResourceAudioHolder.entries

    operator fun get(id: SoundId) = FileAudioHolder[id] ?: ResourceAudioHolder[id]

    fun update() {
        FileAudioHolder.update()
        favourites = favourites()
    }

    fun refresh() {
        FileAudioHolder.refresh()
        favourites = favourites()
    }

    private fun favourites(): SoundGroup {
        val entries = CONFIG.favourites.mapNotNull(::get)
        return SoundGroup(
            SoundId(MOD_ID, "!favourites"),
            entries.associateBy(SoundEntry::id),
            "slmvoice.favourites".translation()
        )
    }
}
