package dev.srlordm.slmvoice.gui.component

import dev.srlordm.slmvoice.gui.SlmVoiceTheme
import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing

object PixelIcons {
    fun drawLock(context: OwoUIGraphics, x: Int, y: Int, locked: Boolean) {
        val body = if (locked) SlmVoiceTheme.PURPLE else SlmVoiceTheme.TEXT_SECONDARY
        val shackle = if (locked) SlmVoiceTheme.PURPLE_LIGHT else SlmVoiceTheme.TEXT_SECONDARY
        if (locked) {
            context.fill(x + 2, y + 1, x + 8, y + 3, shackle)
            context.fill(x + 1, y + 3, x + 3, y + 6, shackle)
            context.fill(x + 7, y + 3, x + 9, y + 6, shackle)
        } else {
            context.fill(x + 5, y + 1, x + 9, y + 3, shackle)
            context.fill(x + 8, y + 3, x + 10, y + 6, shackle)
        }
        context.fill(x, y + 5, x + 10, y + 12, body)
        context.fill(x + 1, y + 6, x + 9, y + 11, SlmVoiceTheme.SELECTED)
        context.fill(x + 4, y + 7, x + 6, y + 9, SlmVoiceTheme.TEXT_PRIMARY)
        context.fill(x + 4, y + 8, x + 6, y + 10, SlmVoiceTheme.TEXT_PRIMARY)
    }

    fun drawFolder(context: OwoUIGraphics, x: Int, y: Int) {
        context.fill(x + 1, y, x + 6, y + 2, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x, y + 2, x + 10, y + 9, SlmVoiceTheme.SELECTED)
        context.fill(x + 1, y + 3, x + 9, y + 8, SlmVoiceTheme.PURPLE)
    }

    fun drawMicrophone(context: OwoUIGraphics, x: Int, y: Int) {
        context.fill(x + 4, y, x + 8, y + 1, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 3, y + 1, x + 9, y + 7, SlmVoiceTheme.PURPLE)
        context.fill(x + 4, y + 2, x + 5, y + 6, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 7, y + 2, x + 8, y + 6, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 1, y + 4, x + 3, y + 7, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 9, y + 4, x + 11, y + 7, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 2, y + 7, x + 4, y + 9, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 8, y + 7, x + 10, y + 9, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 3, y + 8, x + 9, y + 10, SlmVoiceTheme.PURPLE)
        context.fill(x + 5, y + 10, x + 7, y + 13, SlmVoiceTheme.PURPLE_LIGHT)
        context.fill(x + 3, y + 13, x + 9, y + 14, SlmVoiceTheme.PURPLE)
    }
}

class PixelMicrophoneComponent : BaseUIComponent() {
    init {
        sizing(Sizing.fixed(12), Sizing.fixed(16))
    }

    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        PixelIcons.drawMicrophone(context, x(), y() + 1)
    }
}
