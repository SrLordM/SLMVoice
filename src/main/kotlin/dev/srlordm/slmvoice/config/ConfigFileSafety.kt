package dev.srlordm.slmvoice.config

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.Instant

internal fun backupInvalidConfig(file: Path): Path? = runCatching {
    if (!Files.exists(file)) return null
    val backup = file.resolveSibling("${file.fileName}.invalid-${Instant.now().toEpochMilli()}.bak")
    Files.copy(file, backup)
    backup
}.getOrNull()

internal fun writeConfigAtomically(file: Path, contents: String) {
    Files.createDirectories(file.toAbsolutePath().parent)
    val temporary = Files.createTempFile(file.toAbsolutePath().parent, "${file.fileName}.", ".tmp")
    try {
        Files.writeString(temporary, contents, Charsets.UTF_8, WRITE)
        try {
            Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, file, REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(temporary)
    }
}
