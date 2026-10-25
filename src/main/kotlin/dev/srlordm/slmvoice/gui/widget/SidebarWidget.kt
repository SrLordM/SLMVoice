package dev.srlordm.slmvoice.gui.widget

import dev.kikugie.kowoui.access.renderer
import dev.kikugie.kowoui.access.sizing
import dev.kikugie.kowoui.access.tooltipText
import dev.kikugie.kowoui.button
import dev.kikugie.kowoui.experimental.plusAssign
import dev.kikugie.kowoui.text
import dev.kikugie.kowoui.translation
import dev.srlordm.slmvoice.audio.BASE_DIR
import dev.srlordm.slmvoice.gui.SlmVoiceTheme
import dev.srlordm.slmvoice.gui.screen.DownloadScreen
import dev.srlordm.slmvoice.gui.screen.SoundBrowser
import dev.srlordm.slmvoice.util.currentScreen
import dev.srlordm.slmvoice.util.idOf
import dev.srlordm.slmvoice.util.navigate
import io.wispforest.owo.ui.component.ButtonComponent.Renderer
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Sizing.content
import io.wispforest.owo.ui.core.Sizing.fixed
import net.minecraft.client.gui.screen.Screen
import kotlin.reflect.full.createInstance

class SidebarWidget(private val current: Screen) : FlowLayout(fixed(16), content(), Algorithm.VERTICAL) {
    companion object {
        val WIDGETS = idOf("textures/gui/widgets.png")
    }

    init {
        gap(2)
        this += tab<SoundBrowser>("browser", 0)
        this += tab<DownloadScreen>("downloader", 1)
        this += button {
            id = "slmvoice-folder"
            sizing = fixed(16)
            renderer = Renderer { context, button, _ ->
                val bg = if (button.isHovered) SlmVoiceTheme.CARD_HOVER else SlmVoiceTheme.CARD
                context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), bg)
                val x = button.x() + 3
                val y = button.y() + 5
                context.fill(x, y, x + 5, y + 2, SlmVoiceTheme.PURPLE_LIGHT)
                context.fill(x, y + 2, x + 10, y + 9, SlmVoiceTheme.SELECTED)
                context.fill(x + 1, y + 4, x + 9, y + 8, SlmVoiceTheme.PURPLE)
            }
            tooltipText = "slmvoice.folder".translation()
            onPress { BASE_DIR.navigate() }
        }
    }

    private inline fun <reified T : Screen> tab(name: String, index: Int) = button {
        id = name
        sizing = fixed(16)
        renderer = Renderer.texture(WIDGETS, index * 16, 0, 256, 256)
        active = current !is T
        tooltipText = "slmvoice.$name".translation()
        onPress { currentScreen = T::class.createInstance() }
    }
}
