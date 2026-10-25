package dev.srlordm.slmvoice.gui.widget

import dev.kikugie.kowoui.*
import dev.kikugie.kowoui.access.*
import dev.kikugie.kowoui.dynamic.dynamicButton
import dev.kikugie.kowoui.dynamic.dynamicLabel
import dev.kikugie.kowoui.experimental.plusAssign
import dev.srlordm.slmvoice.ModKeyBinds
import dev.srlordm.slmvoice.Soundboard
import dev.srlordm.slmvoice.audio.data.AudioConfiguration.Companion.DEFAULT
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.audio.registry.SoundRegistry
import dev.srlordm.slmvoice.config.AudioConfig
import dev.srlordm.slmvoice.entrypoint.SoundboardEntrypoint
import dev.srlordm.slmvoice.gui.EditorAudioPreparation
import dev.srlordm.slmvoice.gui.PreparedEditorAudio
import dev.srlordm.slmvoice.gui.SlmVoiceTheme
import dev.srlordm.slmvoice.gui.component.*
import dev.srlordm.slmvoice.gui.component.TimeInputComponent.Companion.asString
import dev.srlordm.slmvoice.gui.screen.SoundBrowser
import dev.srlordm.slmvoice.util.currentScreen
import dev.srlordm.slmvoice.util.duration
import io.wispforest.owo.ui.component.ButtonComponent.Renderer
import io.wispforest.owo.ui.component.SlimSliderComponent.Axis.HORIZONTAL
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.ui.core.Sizing.*
import net.minecraft.text.Text
import kotlin.time.Duration

class SoundSettingsWidget(
    private val entry: SoundEntry,
    private val access: SoundboardEntrypoint,
) : FlowLayout(content(), content(), Algorithm.VERTICAL) {
    companion object {
        private const val CLOSE = "X"
        private const val CLOSE_TOOLTIP = "slmvoice.browser.tooltip.close"
        private const val PLAY_TOOLTIP = "slmvoice.browser.tooltip.play"
        private const val BIND_SET_TOOLTIP = "slmvoice.browser.tooltip.bind_set"
        private const val BIND_CLEAR_TOOLTIP = "slmvoice.browser.tooltip.bind_clear"
        private val PANEL = SlmVoiceTheme.BACKGROUND
        private val CARD = SlmVoiceTheme.CARD
        private val BORDER = SlmVoiceTheme.SEPARATOR
        private val ACCENT = SlmVoiceTheme.PURPLE
        private val STAR = SlmVoiceTheme.PURPLE_LIGHT
        private val TEXT = SlmVoiceTheme.TEXT_PRIMARY
        private val ACTION = SlmVoiceTheme.SELECTED
        private val ACTION_HOVER = SlmVoiceTheme.PURPLE
        private val BUTTON_IDLE = SlmVoiceTheme.CARD
        private val BUTTON_HOVER = SlmVoiceTheme.CARD_HOVER
        private val BUTTON_DISABLED = SlmVoiceTheme.BUTTON_DISABLED
        private val FAVOURITE_TOOLTIP: (Boolean) -> Text = {
            if (it) "slmvoice.browser.tooltip.unfavourite".translation()
            else "slmvoice.browser.tooltip.favourite".translation()
        }
    }

    private var audioData: ShortArray? = null
    private var duration: Duration? = null
    private var loadingState: UIComponent? = null
    private var disposed = false
    private var favourite = entry.id in Soundboard.config.favourites
    private val settings = (AudioConfig[entry] ?: entry.settings ?: DEFAULT).clone()

    init {
        create()
        prepareAudio()
    }

    fun update() {
        val duration = duration ?: return
        if (settings.isDefault(duration) || settings == entry.settings) AudioConfig -= entry
        else AudioConfig[entry] = settings
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        EditorAudioPreparation.cancelLatest()
        audioData = null
    }

    private fun prepareAudio() {
        EditorAudioPreparation.submitLatest(
            entry,
            access.format,
            onProgress = { message ->
                if (isCurrent()) showLoadState(message)
            },
            onComplete = { result ->
                if (!isCurrent()) return@submitLatest
                result.onSuccess(::showPreparedAudio).onFailure {
                    showLoadError()
                }
            }
        )
    }

    private fun isCurrent() =
        !disposed && (currentScreen as? SoundBrowser)?.isCurrentSettings(this) == true

    private fun showPreparedAudio(prepared: PreparedEditorAudio) {
        audioData = prepared.samples
        val duration = access.format.duration(prepared.samples.size)
        this.duration = duration
        settings.start = settings.start.coerceIn(Duration.ZERO, duration)
        settings.end = when (settings.end) {
            Duration.INFINITE -> duration
            else -> settings.end.coerceIn(settings.start, duration)
        }
        loadingState?.let { removeChild(it) }
        loadingState = null
        createControls(prepared.waveform, duration)
    }

    private fun showLoadError() {
        showLoadState("Could not load audio")
    }

    private fun showLoadState(message: String) {
        loadingState?.let { removeChild(it) }
        loadingState = horizontalFlow {
            id = "audio-loading-state"
            horizontalSizing = fill()
            verticalSizing = fixed(34)
            horizontalAlignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            surface = Surface.flat(CARD)

            this += label(message.text()) {
                horizontalSizing = content()
                verticalSizing = content()
                horizontalTextAlignment = HorizontalAlignment.CENTER
                color = Color.ofArgb(SlmVoiceTheme.TEXT_SECONDARY)
            }
        }
        this += loadingState!!
    }

    private fun create() = with(this as FlowLayout) {
        id = "settings-container"
        horizontalSizing = fixed(420)
        verticalSizing = content()
        gap = 6
        padding = Insets.of(8)
        surface = Surface.flat(PANEL).and(Surface.outline(BORDER))
        this += horizontalFlow {
            id = "settings-title-bar"
            horizontalSizing = fill()
            verticalSizing = fixed(26)
            verticalAlignment = VerticalAlignment.CENTER
            padding = Insets.horizontal(8)
            surface = Surface.flat(SlmVoiceTheme.PANEL).and(Surface.outline(BORDER))

            this += label("AUDIO SETTINGS".text()) {
                horizontalSizing = expand()
                color = Color.ofArgb(TEXT)
                horizontalTextAlignment = HorizontalAlignment.CENTER
                shadow = true
            }
            this += button(CLOSE.text()) {
                id = "close"
                horizontalSizing = fixed(22)
                verticalSizing = fixed(20)
                renderer = Renderer.flat(BUTTON_IDLE, BUTTON_HOVER, BUTTON_DISABLED)
                tooltipText = CLOSE_TOOLTIP.translation()
                onPress { (currentScreen as? SoundBrowser)?.closeSettings() }
            }
        }

        this += CenteredEllipsisLabelComponent(entry.id.file.ifEmpty { entry.title.string }.text()).apply {
            id = "file-name"
            horizontalSizing = fill()
            verticalSizing = fixed(24)
            surface = Surface.flat(CARD)
            padding = Insets.of(8)
            contentPadding = 8
            horizontalTextAlignment = HorizontalAlignment.CENTER
        }

        showLoadState("Loading audio...")
    }

    private fun createControls(waveformData: DoubleArray, duration: Duration) = with(this as FlowLayout) {
        val waveform = WaveformComponent(waveformData, settings.volume)
        val cutter = DurationCutterComponent(duration, settings::start, settings::end)

        this += horizontalFlow {
            id = "volume-row"
            horizontalSizing = fill()
            verticalSizing = fixed(28)
            gap = 8
            verticalAlignment = VerticalAlignment.CENTER
            padding = Insets.horizontal(8)

            this += label("Volume".text()) {
                horizontalSizing = fixed(58)
                color = Color.ofArgb(TEXT)
            }
            this += slimSlider(HORIZONTAL) {
                id = "volume"
                horizontalSizing = expand()
                value = settings.volume.coerceIn(0.0, 1.0)
                onChange {
                    settings.volume = it
                    waveform.mult = it
                }
            }
            this += dynamicLabel {
                id = "volume-label"
                horizontalSizing = fixed(42)
                horizontalTextAlignment = HorizontalAlignment.RIGHT
                color = Color.ofArgb(ACCENT)
                text { "${(settings.volume * 100).toInt()}%".text() }
            }
        }

        this += verticalFlow {
            id = "cut-section"
            horizontalSizing = fill()
            gap = 6
            padding = Insets.of(8)
            surface = Surface.flat(CARD)

            this += label("AUDIO SEGMENT".text()) {
                horizontalSizing = fill()
                color = Color.ofArgb(SlmVoiceTheme.TEXT_SECONDARY)
            }
            this += stack {
                id = "waveform-cutter"
                horizontalSizing = fill()
                verticalSizing = fixed(78)
                surface = Surface.flat(SlmVoiceTheme.PANEL).and(Surface.outline(BORDER))
                padding = Insets.of(1)
                children(waveform, cutter)
            }
            this += horizontalFlow {
                id = "duration-controls"
                horizontalSizing = fill()
                gap = 8
                verticalAlignment = VerticalAlignment.CENTER

                this += verticalFlow {
                    horizontalSizing = fixed(84)
                    gap = 3
                    this += label("Start".text()) {
                        horizontalSizing = fill()
                        horizontalTextAlignment = HorizontalAlignment.CENTER
                        color = Color.ofArgb(SlmVoiceTheme.TEXT_SECONDARY)
                    }
                    this += TimeInputComponent(duration, settings::start).apply {
                        id = "start"
                        horizontalSizing = fill()
                        validate { it <= settings.end }
                        onDurationChange { cutter.update() }
                    }
                }
                this += dynamicLabel{
                    id = "duration"
                    horizontalSizing = expand()
                    horizontalTextAlignment = HorizontalAlignment.CENTER
                    verticalTextAlignment = VerticalAlignment.CENTER
                    color = Color.ofArgb(SlmVoiceTheme.TEXT_SECONDARY)
                    text { "Segment: ${(settings.end - settings.start).asString}s".text() }
                }
                this += verticalFlow {
                    horizontalSizing = fixed(84)
                    gap = 3
                    this += label("End".text()) {
                        horizontalSizing = fill()
                        horizontalTextAlignment = HorizontalAlignment.CENTER
                        color = Color.ofArgb(SlmVoiceTheme.TEXT_SECONDARY)
                    }
                    this += TimeInputComponent(duration, settings::end).apply {
                        id = "end"
                        horizontalSizing = fill()
                        validate { it >= settings.start }
                        onDurationChange { cutter.update() }
                    }
                }
            }
        }

        this += horizontalFlow {
            id = "bind-row"
            horizontalSizing = fill()
            verticalSizing = fixed(38)
            gap = 8
            padding = Insets.of(8)
            verticalAlignment = VerticalAlignment.CENTER
            surface = Surface.flat(CARD)

            this += label("Bind".text()) {
                horizontalSizing = fixed(50)
                color = Color.ofArgb(TEXT)
            }
            this += dynamicLabel {
                id = "bind"
                horizontalTextAlignment = HorizontalAlignment.CENTER
                verticalTextAlignment = VerticalAlignment.CENTER
                horizontalSizing = expand()
                verticalSizing = fixed(22)
                surface = Surface.flat(SlmVoiceTheme.BACKGROUND).and(Surface.outline(BORDER))
                text {
                    val browser = currentScreen as? SoundBrowser
                    if (browser?.isBinding(entry) == true) "Press a key...".text()
                    else {
                        Soundboard.config.keybindText(entry.id.str).text()
                    }
                }
            }
            this += button("Change".text()) {
                id = "bind-set"
                horizontalSizing = fixed(58)
                verticalSizing = fixed(22)
                renderer = Renderer.flat(BUTTON_IDLE, BUTTON_HOVER, BUTTON_DISABLED)
                tooltipText = BIND_SET_TOOLTIP.translation()
                onPress { (currentScreen as? SoundBrowser)?.beginBinding(entry) }
            }
            this += button("Clear".text()) {
                id = "bind-clear"
                horizontalSizing = fixed(48)
                verticalSizing = fixed(22)
                renderer = Renderer.flat(BUTTON_IDLE, BUTTON_HOVER, BUTTON_DISABLED)
                tooltipText = BIND_CLEAR_TOOLTIP.translation()
                onPress { (currentScreen as? SoundBrowser)?.removeBinding(entry) }
            }
        }

        this += horizontalFlow {
            id = "favorite-row"
            horizontalSizing = fill()
            verticalSizing = fixed(34)
            gap = 8
            padding = Insets.of(8)
            verticalAlignment = VerticalAlignment.CENTER
            surface = Surface.flat(CARD)

            this += dynamicLabel {
                id = "favorite-star"
                horizontalSizing = fixed(12)
                verticalSizing = fill()
                verticalTextAlignment = VerticalAlignment.CENTER
                color = Color.ofArgb(STAR)
                text { if (favourite) "\u2605".text() else "\u2606".text() }
            }
            this += label("Favorite".text()) {
                horizontalSizing = expand()
                color = Color.ofArgb(TEXT)
            }
            this += dynamicButton {
                id = "favourite"
                horizontalSizing = fixed(64)
                verticalSizing = fixed(22)
                renderer = Renderer.flat(BUTTON_IDLE, BUTTON_HOVER, BUTTON_DISABLED)
                tooltipText = FAVOURITE_TOOLTIP(favourite)
                text { if (favourite) "YES".text() else "NO".text() }
                onPress {
                    favourite = !favourite
                    tooltipText = FAVOURITE_TOOLTIP(favourite)

                    if (!favourite) Soundboard.config.favourites -= entry.id
                    else Soundboard.config.favourites += entry.id

                    Soundboard.config.save()
                    SoundRegistry.update()
                    (currentScreen as? SoundBrowser)?.createFavourites()
                }
            }
        }

        this += horizontalFlow {
            id = "footer-actions"
            horizontalSizing = fill()
            verticalSizing = fixed(34)
            gap = 8
            this += dynamicButton {
                id = "play"
                horizontalSizing = expand()
                verticalSizing = fill()
                renderer = Renderer.flat(BUTTON_IDLE, BUTTON_HOVER, BUTTON_DISABLED)
                tooltipText = PLAY_TOOLTIP.translation(ModKeyBinds["browser"]!!.boundKeyLocalizedText.string)
                text { if (access.scheduler.playing) "||  Stop".text() else "\u25B6  Test".text() }
                onPress {
                    if (access.scheduler.playing) access.scheduler.reset()
                    else {
                        AudioConfig.save()
                        audioData?.let { access.scheduleArray(it, true, settings, entry.id) }
                    }
                }
            }
            this += button("Save".text()) {
                id = "save"
                horizontalSizing = expand()
                verticalSizing = fill()
                renderer = Renderer { context, button, _ ->
                    val fill = if (button.isHovered) ACTION_HOVER else ACTION
                    context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), SlmVoiceTheme.BACKGROUND)
                    context.fill(button.x() + 1, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, fill)
                    context.fill(button.x() + 1, button.y(), button.x() + button.width() - 1, button.y() + 1, SlmVoiceTheme.PURPLE_LIGHT)
                }
                onPress {
                    update()
                    (currentScreen as? SoundBrowser)?.closeSettings()
                }
            }
        }
    }
}
