package dev.srlordm.slmvoice.audio.download

import net.minecraft.text.Text

sealed interface CobaltResponse

class StreamResponse(val url: String) : CobaltResponse
class ErrorResponse(val cause: Text) : CobaltResponse