package dev.kikugie.kowoui.dynamic

import io.wispforest.owo.ui.container.WrappingParentUIComponent
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing.content

class WrapperContainer<T : UIComponent>(child: T) : WrappingParentUIComponent<T>(content(), content(), child) {
    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        super.draw(context, mouseX, mouseY, partialTicks, delta)
        drawChildren(context, mouseX, mouseY, partialTicks, delta, children())
    }
}
