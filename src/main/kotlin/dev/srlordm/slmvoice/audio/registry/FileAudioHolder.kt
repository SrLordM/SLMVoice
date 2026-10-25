package dev.srlordm.slmvoice.audio.registry

import dev.kikugie.kowoui.text
import dev.kikugie.kowoui.translation
import dev.srlordm.slmvoice.MOD_ID
import dev.srlordm.slmvoice.audio.*
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.audio.data.SoundGroup
import dev.srlordm.slmvoice.audio.data.SoundId
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.minecraft.text.Text
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.io.path.PathWalkOption.BREADTH_FIRST

@OptIn(ExperimentalPathApi::class)
object FileAudioHolder {
    private const val MISSING_PROBE_INTERVAL_MS = 500L

    private val fileAccessCache: Object2LongMap<Path> = Object2LongOpenHashMap()
    private var lastMissingProbe = 0L
    private var loaded = false
    val initialized get() = loaded
    var sounds: GroupMap = emptyMap()
        private set

    val groups get() = update().let { sounds.values.asSequence() }
    val entries get() = update().let { sounds.values.asSequence().flatMap { it.entries.values.asSequence() } }

    operator fun get(id: SoundId): SoundEntry? {
        val path = runCatching {
            SUPPORTED_FORMATS.asSequence()
                .map { BASE_DIR.resolve(id.path.removePrefix("/") + ".$it") }
                .firstOrNull { it.exists() }
        }
            .getOrNull() ?: return null
        val props = path.withExtension("properties")
        val cached = cache(path) && (props.notExists() || cache(props))
        if (!cached) update()
        return sounds[id.parent()][id]
    }

    fun update() {
        val now = System.currentTimeMillis()
        pruneMissingFiles(now)
        if (!loaded) refresh()
    }

    fun refresh() {
        val titles: MutableMap<SoundId, Text?> = mutableMapOf()
        val buffer: MutableMap<SoundId, MutableList<SoundEntry>> = mutableMapOf()
        BASE_DIR.walk(BREADTH_FIRST).filter { it.extension.isAudioFormat() }.forEach { path ->
            val id = BASE_DIR.relativize(path).invariantSeparatorsPathString.let {
                if ('/' !in it) "/$it" else it
            }.let { SoundId(MOD_ID, it.removeAudioExtension()) }
            val parentId = id.parent()
            val props = path.withExtension("properties")
            val entry = sounds[parentId][id]?.takeIf { cache(path) && (props.notExists() || cache(props)) }
                ?: entry(id, path, props)
            buffer.getOrPut(parentId, ::mutableListOf) += entry
            titles.computeIfAbsent(parentId) { title(path.parent) }
        }
        sounds = buffer.mapValues { (id, list) -> SoundGroup(id, list.associateBy(SoundEntry::id), titles[id]) }
        loaded = true
    }

    private fun pruneMissingFiles(now: Long) {
        if (!loaded || now - lastMissingProbe < MISSING_PROBE_INTERVAL_MS) return
        lastMissingProbe = now

        sounds = sounds.mapNotNull { (id, group) ->
            val entries = group.entries.filterKeys { it.path(file = true) != null }
            if (entries.isEmpty()) null else id to SoundGroup(id, entries, group.title)
        }.toMap()
    }

    private fun title(path: Path): Text? {
        val props = path.resolve(".properties")
        val title = runCatching { PropertiesReader.read(props)["title"]?.text() }.getOrNull()
        return when {
            title != null -> title
            path == BASE_DIR -> "slmvoice.title".translation()
            else -> null
        }
    }

    private fun entry(id: SoundId, path: Path, props: Path): SoundEntry {
        val title = runCatching { PropertiesReader.read(props)["title"]?.text() }.getOrNull()
        return SoundEntry(id, path::inputStream, null, title)
    }

    private fun cache(path: Path) =
        if (path.exists() && (path.isRegularFile() || path.isDirectory())) path.getLastModifiedTime().toMillis()
            .let { fileAccessCache.put(path, it) == it }
        else {
            fileAccessCache.removeLong(path)
            false
        }

    private fun Path.withExtension(ext: String): Path =
        resolveSibling("${fileName.nameWithoutExtension}.$ext")
}
