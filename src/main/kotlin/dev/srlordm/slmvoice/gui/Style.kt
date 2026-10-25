package dev.srlordm.slmvoice.gui

import dev.srlordm.slmvoice.CONFIG
import io.wispforest.owo.ui.core.Surface

object SlmVoiceTheme {
    val BACKGROUND = 0xFF0D0B14.toInt()
    val SCREEN_BACKGROUND = 0xF50D0B14.toInt()
    val PANEL = 0xFF15111F.toInt()
    val CARD = 0xFF20192D.toInt()
    val PURPLE = 0xFF8B5CF6.toInt()
    val SELECTED = 0xFF6D28D9.toInt()
    val PURPLE_LIGHT = 0xFFC4B5FD.toInt()
    val TEXT_PRIMARY = 0xFFF5F3FF.toInt()
    val TEXT_SECONDARY = 0xFFA8A1B8.toInt()
    val SEPARATOR = 0xFF332B42.toInt()
    val CARD_HOVER = 0xFF2A2338.toInt()
    val BUTTON_DISABLED = 0xFF1A1623.toInt()
    val SCRIM = 0x99000000.toInt()
    val SCROLLBAR = SELECTED
}

val CONFIG_PANEL: Surface
    get() = if (CONFIG.dark) {
        Surface.flat(SlmVoiceTheme.BACKGROUND).and(Surface.outline(SlmVoiceTheme.SEPARATOR))
    } else Surface.PANEL
