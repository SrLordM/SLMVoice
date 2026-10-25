package dev.kikugie.kowoui.dynamic

import dev.kikugie.kowoui.cached
import io.wispforest.owo.ui.component.ButtonComponent
import net.minecraft.text.Text as McText

class DynamicButtonComponent(initial: McText) : ButtonComponent(initial, {}) {
    private var provider: (() -> McText)? = null
    private var cache: McText by cached(initial) {
        message = it
    }

    fun text(provider: () -> McText) = apply {
        this.provider = provider
    }

    override fun getMessage(): McText {
        provider?.invoke()?.let { cache = it }
        return super.getMessage()
    }
}
