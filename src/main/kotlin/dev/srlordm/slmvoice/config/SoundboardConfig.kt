package dev.srlordm.slmvoice.config

import dev.srlordm.slmvoice.LOGGER
import dev.srlordm.slmvoice.audio.data.SoundId
import dev.srlordm.slmvoice.config.AudioProviderType.STREAM
import dev.srlordm.slmvoice.config.CobaltAPIVersion.V7
import dev.srlordm.slmvoice.util.runOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.fabricmc.loader.api.FabricLoader
import org.lwjgl.glfw.GLFW
import kotlin.io.path.*

@Serializable
@OptIn(ExperimentalSerializationApi::class)
class SoundboardConfig(
    @JvmField @SerialName("dark_mode")
    var dark: Boolean = false,
    @JvmField @SerialName("board_columns")
    var columns: Int = 3,
    @JvmField @SerialName("audio_provider")
    var provider: AudioProviderType = STREAM,
    @JvmField @SerialName("audio_volume")
    var volume: Double = 1.0,
    @JvmField @SerialName("cobalt_api_endpoint")
    var cobaltEndpoint: String = "https://api.cobalt.tools",
    @JvmField @SerialName("cobalt_api_version")
    var cobaltVersion: CobaltAPIVersion = V7,
    @JvmField @SerialName("cobalt_api_token")
    var cobaltToken: String = "",
    @JvmField @SerialName("favourites")
    val favourites: MutableList<SoundId> = mutableListOf(),
    @JvmField @SerialName("sound_keybinds")
    val keybinds: MutableMap<String, Int> = mutableMapOf(),
    @JvmField @SerialName("sound_keybind_combos")
    val keybindCombos: MutableMap<String, List<Int>> = mutableMapOf(),
) {
    @Transient private var persistenceBlocked = false
    @Transient private var latestSaveVersion = 0L
    @Transient private val saveStateLock = Any()
    @Transient private val saveMutex = Mutex()

    fun keybind(sound: String): List<Int>? =
        keybindCombos[sound]?.takeIf { it.isNotEmpty() } ?: keybinds[sound]?.let(::listOf)

    fun keybindText(sound: String): String =
        keybind(sound)?.joinToString(" + ") { keyName(it) } ?: "None"

    fun setKeybind(sound: String, keys: List<Int>) {
        val normalized = keys.distinct().filter { it >= GLFW.GLFW_KEY_SPACE }
        if (normalized.isEmpty()) removeKeybind(sound)
        else {
            keybindCombos[sound] = normalized
            keybinds.remove(sound)
            save()
        }
    }

    fun setKeybindReplacing(sound: String, keys: List<Int>, conflicts: Collection<String>) {
        conflicts.asSequence().filter { it != sound }.forEach {
            keybindCombos.remove(it)
            keybinds.remove(it)
        }
        setKeybind(sound, keys)
    }

    fun removeKeybind(sound: String) {
        keybindCombos.remove(sound)
        keybinds.remove(sound)
        save()
    }

    fun save() {
        if (persistenceBlocked) return
        val version: Long
        val contents: String
        try {
            synchronized(saveStateLock) {
                version = ++latestSaveVersion
                val snapshot = SoundboardConfig(
                    dark, columns, provider, volume, cobaltEndpoint, cobaltVersion, cobaltToken,
                    favourites.toMutableList(), keybinds.toMutableMap(), keybindCombos.mapValues { it.value.toList() }.toMutableMap()
                )
                contents = json.encodeToString(snapshot)
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to serialize config $file", e)
            return
        }
        runOn(Dispatchers.IO) {
            saveMutex.withLock {
                if (persistenceBlocked || synchronized(saveStateLock) { version != latestSaveVersion }) return@withLock
                try {
                    writeConfigAtomically(file, contents)
                } catch (e: Exception) {
                    LOGGER.error("Failed to save config $file", e)
                }
            }
        }
    }

    companion object Loader {
        private val configDir = FabricLoader.getInstance().configDir
        @JvmStatic val file = configDir.resolve("slmvoice.json")
        @JvmStatic val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }

        fun load(): SoundboardConfig {
            if (file.exists()) try {
                return file.inputStream().use(json::decodeFromStream)
            } catch (e: Exception) {
                val backup = backupInvalidConfig(file)
                LOGGER.error("Failed to read config $file${backup?.let { "; backup saved to $it" } ?: "; preserving the original file and disabling writes"}", e)
                return SoundboardConfig().also {
                    if (backup == null) it.persistenceBlocked = true else it.save()
                }
            }
            return SoundboardConfig().apply(SoundboardConfig::save)
        }

        fun keyName(key: Int): String = when (key) {
            GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "CTRL"
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "SHIFT"
            GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "ALT"
            GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> "SUPER"
            else -> GLFW.glfwGetKeyName(key, 0)?.uppercase() ?: key.toString()
        }
    }
}
