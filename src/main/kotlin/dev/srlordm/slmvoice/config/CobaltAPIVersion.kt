package dev.srlordm.slmvoice.config

import dev.srlordm.slmvoice.audio.download.CobaltAPIV10
import dev.srlordm.slmvoice.audio.download.CobaltAPIV7
import kotlinx.coroutines.Job
import java.net.URI
import java.nio.file.Path

enum class CobaltAPIVersion {
    V7 {
        override fun download(url: URI, dest: Path) = CobaltAPIV7.download(url, dest)
    },
    V10 {
        override fun download(url: URI, dest: Path) = CobaltAPIV10.download(url, dest)
    };

    abstract fun download(url: URI, dest: Path): Job
}