package dev.kikugie.kowoui.dynamic

import dev.kikugie.kowoui.access.sizing
import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing.fixed

class FixedSpacerComponent(pixels: Int) : BaseUIComponent() {
    init {
        sizing = fixed(pixels)
    }

    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
    }
}
