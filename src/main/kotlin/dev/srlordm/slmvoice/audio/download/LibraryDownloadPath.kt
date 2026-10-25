package dev.srlordm.slmvoice.audio.download

import dev.srlordm.slmvoice.audio.BASE_DIR
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path

internal fun resolveLibraryDownloadPath(input: String): Path? {
    val relative = runCatching { Path.of(input) }.getOrNull() ?: return null
    if (input.isBlank() || relative.isAbsolute || relative.root != null || input.contains(':') ||
        relative.any { it.toString() == ".." }
    ) return null

    val root = runCatching { BASE_DIR.toAbsolutePath().normalize().toRealPath() }.getOrNull() ?: return null
    val destination = root.resolve(relative).normalize()
    if (!destination.startsWith(root) || destination == root) return null
    return validateLibraryDownloadPath(destination)
}

internal fun validateLibraryDownloadPath(path: Path): Path? {
    val rootPath = runCatching { BASE_DIR.toAbsolutePath().normalize() }.getOrNull() ?: return null
    val realRoot = runCatching { rootPath.toRealPath() }.getOrNull() ?: return null
    val destination = path.toAbsolutePath().normalize()
    if ((!destination.startsWith(rootPath) && !destination.startsWith(realRoot)) || destination == rootPath || destination == realRoot) return null

    var existing = destination
    while (!Files.exists(existing, NOFOLLOW_LINKS)) {
        existing = existing.parent ?: return null
    }
    val realExisting = runCatching { existing.toRealPath() }.getOrNull() ?: return null
    if (!realExisting.startsWith(realRoot)) return null

    if (Files.exists(destination, NOFOLLOW_LINKS)) {
        val realDestination = runCatching { destination.toRealPath() }.getOrNull() ?: return null
        if (!realDestination.startsWith(realRoot) || !Files.isRegularFile(realDestination)) return null
    }
    return destination
}
