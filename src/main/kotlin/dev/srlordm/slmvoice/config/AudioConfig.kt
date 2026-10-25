package dev.srlordm.slmvoice.config

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import dev.srlordm.slmvoice.LOGGER
import dev.srlordm.slmvoice.audio.data.AudioConfiguration
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.audio.data.SoundId
import dev.srlordm.slmvoice.util.runOn
import kotlinx.coroutines.Dispatchers
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.reader

private typealias ConfigEntries = MutableMap<SoundId, AudioConfiguration>

object AudioConfig {
    private val configurations: ConfigEntries = mutableMapOf()
    private val configDir = FabricLoader.getInstance().configDir
    private val file: Path = configDir.resolve("slmvoice_tracks.json")
    private var persistenceBlocked = false
    // Uses gson because kotlinx serialization can't handle plain maps
    private val json = GsonBuilder()
        .setPrettyPrinting()
        .setLenient()
        .create()

    init {
        load()
    }

    operator fun get(entry: SoundEntry): AudioConfiguration? = get(entry.id)
    operator fun get(id: SoundId): AudioConfiguration? = configurations[id]
    operator fun set(entry: SoundEntry, configuration: AudioConfiguration) = set(entry.id, configuration)
    operator fun set(id: SoundId, configuration: AudioConfiguration) {
        configurations[id] = configuration
        runOn(Dispatchers.IO) { save() }
    }
    operator fun minusAssign(entry: SoundEntry) = minusAssign(entry.id)
    operator fun minusAssign(id: SoundId) {
        configurations.remove(id)
        runOn(Dispatchers.IO) { save() }
    }

    fun load() {
        if (!file.exists()) return
        try {
            require(file.isReadable()) { "Configuration file is not readable" }
            val loaded = file.reader().use {
                val token: TypeToken<MutableMap<String, AudioConfiguration?>> = TypeToken.getParameterized(Map::class.java, String::class.java, AudioConfiguration::class.java) as TypeToken<MutableMap<String, AudioConfiguration?>>
                json.fromJson<MutableMap<String, AudioConfiguration?>>(it, token)
                    ?: throw JsonParseException("Configuration is empty")
            }
            require(loaded.values.none { it == null }) { "Configuration contains null audio entries" }
            val conf = loaded.mapValues { requireNotNull(it.value) }.mapKeys { e -> SoundId(e.key) }
            configurations.clear()
            configurations.putAll(conf)
        } catch (e: Exception) {
            val backup = backupInvalidConfig(file)
            persistenceBlocked = backup == null
            LOGGER.error("Failed to read audio configuration $file; defaults will be used${backup?.let { ", backup saved to $it" } ?: "; preserving the original file and disabling writes"}", e)
        }
    }

    fun save() {
        if (persistenceBlocked) return
        try {
            val contents = json.toJson(configurations.mapKeys { it.key.str })
            synchronized(this) { writeConfigAtomically(file, contents) }
        } catch (e: Exception) {
            LOGGER.error("Failed to save audio configuration $file", e)
        }
    }
}
