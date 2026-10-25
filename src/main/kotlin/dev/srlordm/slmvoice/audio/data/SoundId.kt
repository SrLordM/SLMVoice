package dev.srlordm.slmvoice.audio.data

import dev.srlordm.slmvoice.MOD_ID
import dev.srlordm.slmvoice.audio.BASE_DIR
import dev.srlordm.slmvoice.audio.SUPPORTED_FORMATS
import kotlinx.serialization.Serializable
import net.minecraft.util.Identifier
import kotlin.io.path.exists

/**
 * ```
 * [namespace:][directory]/(filename)
 * ```
 *
 * @property str Primitive representation of the id
 */
@JvmInline
@Serializable
value class SoundId(val str: String) {
    val namespace get() = str.substringBefore(':', "")
    val directory get() = str.substringAfter(':').substringBeforeLast('/')
    val path get() = str.substringAfter(':')
    val file get() = str.substringAfterLast('/', "")

    constructor(namespace: String, path: String) : this("${ if (namespace.isEmpty() || namespace == MOD_ID) "" else "$namespace:" }$path")
    constructor(id: Identifier) : this(id.toString())

    fun parent() = SoundId(namespace, directory.ifEmpty { "/" })
    fun path(file: Boolean) = when {
        namespace.isNotEmpty() && namespace != MOD_ID -> null
        file -> SUPPORTED_FORMATS.asSequence()
            .map { BASE_DIR.resolve("${path.removePrefix("/")}.$it") }
            .firstOrNull { it.exists() }
        directory.isEmpty() -> BASE_DIR
        else -> BASE_DIR.resolve(directory)
    }

    override fun toString() = str
}
