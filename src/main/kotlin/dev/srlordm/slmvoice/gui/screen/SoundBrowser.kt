package dev.srlordm.slmvoice.gui.screen

import dev.kikugie.kowoui.*
import dev.kikugie.kowoui.access.*
import dev.kikugie.kowoui.dynamic.wrap
import dev.kikugie.kowoui.experimental.plus
import dev.kikugie.kowoui.experimental.plusAssign
import dev.srlordm.slmvoice.Soundboard
import dev.srlordm.slmvoice.LOGGER
import dev.srlordm.slmvoice.audio.BASE_DIR
import dev.srlordm.slmvoice.audio.data.SoundEntry
import dev.srlordm.slmvoice.audio.data.SoundId
import dev.srlordm.slmvoice.audio.hasAudioExtension
import dev.srlordm.slmvoice.audio.registry.SoundRegistry
import dev.srlordm.slmvoice.config.AudioConfig
import dev.srlordm.slmvoice.config.SoundboardConfig
import dev.srlordm.slmvoice.entrypoint.SoundboardAccess
import dev.srlordm.slmvoice.gui.SlmVoiceTheme
import dev.srlordm.slmvoice.gui.component.PixelMicrophoneComponent
import dev.srlordm.slmvoice.gui.component.PixelIcons
import dev.srlordm.slmvoice.gui.component.ScrollingButtonComponent
import dev.srlordm.slmvoice.mixin.owo_ui.ScrollContainerAccessor
import dev.srlordm.slmvoice.gui.widget.SoundSettingsWidget
import dev.srlordm.slmvoice.util.client
import dev.srlordm.slmvoice.util.ctrlDown
import dev.srlordm.slmvoice.util.navigate
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Insets.*
import io.wispforest.owo.ui.core.Positioning.relative
import io.wispforest.owo.ui.core.Sizing.content
import io.wispforest.owo.ui.core.Sizing.expand
import io.wispforest.owo.ui.core.Sizing.fill
import io.wispforest.owo.ui.core.Sizing.fixed
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.UIComponent
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class SoundBrowser : ModScreen() {
    private sealed interface CategorySelection {
        data object All : CategorySelection
        data object Favorites : CategorySelection
        data object Shortcuts : CategorySelection
        data class Folder(val id: SoundId) : CategorySelection
    }

    companion object : ScreenManager(SoundBrowser::class) {
        private const val FILE_TOOLTIP = "slmvoice.browser.tooltip.file"
        private const val OPEN_DIRECTORY_TOOLTIP = "slmvoice.browser.tooltip.open_directory"
        private const val IMPORT_TOOLTIP = "slmvoice.browser.tooltip.import"
        private const val VOLUME_TOOLTIP = "slmvoice.browser.tooltip.volume"
        private const val BOUND_TOOLTIP = "slmvoice.browser.tooltip.bound"
        private const val MEMORY_ENABLED_TOOLTIP = "slmvoice.browser.tooltip.memory_enabled"
        private const val MEMORY_DISABLED_TOOLTIP = "slmvoice.browser.tooltip.memory_disabled"

        private data class BrowserSessionState(
            val query: String,
            val category: String,
            val audioScroll: Double
        )

        private var browserMemoryEnabled = true
        private var rememberedBrowserState: BrowserSessionState? = null

        private fun categoryKey(category: CategorySelection) = when (category) {
            CategorySelection.All -> "all"
            CategorySelection.Favorites -> "favorites"
            CategorySelection.Shortcuts -> "shortcuts"
            is CategorySelection.Folder -> "folder:${category.id.str}"
        }

        private fun categoryFromKey(key: String?) = when {
            key == null || key == "all" -> CategorySelection.All
            key == "favorites" -> CategorySelection.Favorites
            key == "shortcuts" -> CategorySelection.Shortcuts
            key.startsWith("folder:") -> CategorySelection.Folder(SoundId(key.removePrefix("folder:")))
            else -> CategorySelection.All
        }

        internal fun clearSessionMemory() {
            browserMemoryEnabled = true
            rememberedBrowserState = null
        }

        private val PANEL = SlmVoiceTheme.PANEL
        private val PANEL_ALT = SlmVoiceTheme.CARD
        private val PANEL_DARK = SlmVoiceTheme.BACKGROUND
        private val BORDER = SlmVoiceTheme.SEPARATOR
        private val TEXT_MUTED = SlmVoiceTheme.TEXT_SECONDARY
        private val ACCENT = SlmVoiceTheme.PURPLE
        private val ACCENT_HOVER = SlmVoiceTheme.PURPLE_LIGHT
        private val ACTION = SlmVoiceTheme.SELECTED
        private val ACTION_HOVER = SlmVoiceTheme.PURPLE
        private val ROW = SlmVoiceTheme.CARD
        private val ROW_HOVER = SlmVoiceTheme.CARD_HOVER
        private val ROW_SELECTED = SlmVoiceTheme.SELECTED
        private val SECONDARY = SlmVoiceTheme.CARD
        private val SECONDARY_HOVER = SlmVoiceTheme.SEPARATOR
        private val STAR = SlmVoiceTheme.PURPLE_LIGHT
        private const val AUDIO_LIST_RIGHT_INSET = 10
        private const val AUDIO_GRID_GAP = 6
        private const val MIN_AUDIO_CARD_WIDTH = 200
        private const val AUDIO_GRID_CARD_PERCENT = 49
        private const val COMPACT_LAYOUT_MIN_WIDTH = 540
        private const val COMPACT_INITIAL_SCREEN_WIDTH = 628
        private const val COMPACT_CATEGORY_WIDTH = 118
        private const val CATEGORY_WIDTH = 150
    }

    internal var settings: SoundSettingsWidget? = null
    private var settingsOverlay: UIComponent? = null
    private var query = rememberedBrowserState?.takeIf { browserMemoryEnabled }?.query.orEmpty()
    private var selectedCategory: CategorySelection = categoryFromKey(
        rememberedBrowserState?.takeIf { browserMemoryEnabled }?.category
    )
    private var restoredAudioScroll = rememberedBrowserState?.takeIf { browserMemoryEnabled }?.audioScroll
    private var selectedEntry: SoundEntry? = null
    private var bindingTarget: SoundEntry? = null
    private val bindingKeys = mutableListOf<Int>()
    private val ignoredBindingKeys = mutableSetOf<Int>()
    private val audioRows = mutableMapOf<SoundId, ScrollingButtonComponent>()
    private var displayedPlayingSoundId: SoundId? = null
    private var audioGridColumns = 1
    private var compactLayout = false
    private var detailsToggleButton: ButtonComponent? = null
    private var memoryToggleButton: ButtonComponent? = null
    private var bindConflictOverlay: UIComponent? = null
    private var conflictTarget: SoundEntry? = null
    private var conflictKeys: List<Int> = emptyList()
    private var conflictIds: List<String> = emptyList()
    private var conflictWaiting = false
    private val conflictCaptureKeys = mutableListOf<Int>()
    private val ignoredHeldKeys = mutableSetOf<Int>()
    private var compactDetailsOverlay: UIComponent? = null
    private var ignoreOpeningAccent = true
    private lateinit var headerLayout: FlowLayout
    private lateinit var browserLayout: FlowLayout
    private lateinit var sidebarPanel: FlowLayout
    private lateinit var categoryList: FlowLayout
    private lateinit var categoryScroll: ScrollContainer<FlowLayout>
    private lateinit var audioList: FlowLayout
    private lateinit var audioScroll: ScrollContainer<FlowLayout>
    private lateinit var audioTitle: LabelComponent
    private lateinit var detailsPanelComponent: FlowLayout
    private lateinit var detailsHeader: FlowLayout
    private lateinit var selectedDetails: FlowLayout
    private data class ScrollPositions(val categories: Double?, val audio: Double?)
    private var scrollMutationDepth = 0
    private var scrollMutationPositions: ScrollPositions? = null
    private var pendingScrollRestore: ScrollPositions? = null

    override fun tick() {
        super.tick()
        refreshResponsiveLayout()
        refreshAudioGridLayout()
        pendingScrollRestore?.let {
            restoreScrollPositions(it)
            pendingScrollRestore = null
        }
        refreshPlaybackIndicators()
    }

    fun closeSettings() {
        preserveScrollPositions {
            settings?.update()
            settings?.dispose()
            settingsOverlay?.let(root::removeChild)
            settingsOverlay = null
            settings = null
        }
    }

    internal fun isCurrentSettings(widget: SoundSettingsWidget): Boolean =
        settings === widget && settingsOverlay != null

    fun beginBinding(entry: SoundEntry) {
        bindingTarget = entry
        bindingKeys.clear()
        ignoredBindingKeys.clear()
        (GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_LAST)
            .filter { InputUtil.isKeyPressed(client.window, it) }
            .forEach(ignoredBindingKeys::add)
        selectedEntry = entry
        rebuildDetails()
    }

    fun isBinding(entry: SoundEntry) = bindingTarget?.id == entry.id

    fun removeBinding(entry: SoundEntry) {
        Soundboard.config.removeKeybind(entry.id.str)
        bindingKeys.clear()
        ignoredBindingKeys.clear()
        bindingTarget = null
        selectedEntry = entry
        rebuild()
    }

    fun createFavourites(container: FlowLayout? = null) {
        rebuild()
    }

    override fun close() {
        rememberBrowserState()
        bindingTarget = null
        bindingKeys.clear()
        ignoredBindingKeys.clear()
        closeBindConflict(false)
        settings?.update()
        settings?.dispose()
        settings = null
        closeCompactDetails()
        super.close()
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (bindConflictOverlay != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) closeBindConflict()
            else if (conflictWaiting) addConflictCaptureKeys(input.key())
            return true
        }
        bindingTarget?.let { entry ->
            when (val key = input.key()) {
                GLFW.GLFW_KEY_ESCAPE -> {
                    cancelBinding()
                }
                else -> {
                    if (key !in ignoredBindingKeys) addBindingKeys(key)
                    selectedEntry = entry
                    rebuildDetails()
                }
            }
            return true
        }
        if (compactDetailsOverlay != null && input.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeCompactDetails()
            return true
        }
        return super.keyPressed(input)
    }

    override fun keyReleased(input: KeyInput): Boolean {
        if (bindConflictOverlay != null) {
            if (conflictWaiting) {
                val releasedKey = input.key()
                if (!ignoredHeldKeys.remove(releasedKey) && conflictCaptureKeys.isNotEmpty() &&
                    conflictCaptureKeys.none { InputUtil.isKeyPressed(client.window, it) }
                ) {
                    val candidate = conflictCaptureKeys.toList()
                    conflictCaptureKeys.clear()
                    ignoredHeldKeys.clear()
                    completeConflictAttempt(candidate)
                }
            }
            return true
        }
        bindingTarget?.let { entry ->
            if (ignoredBindingKeys.remove(input.key())) return true
            if (bindingKeys.isNotEmpty() && bindingKeys.none { InputUtil.isKeyPressed(client.window, it) }) {
                finishBinding(entry)
            }
            return true
        }
        return super.keyReleased(input)
    }

    override fun charTyped(input: CharInput): Boolean {
        if (bindConflictOverlay != null) return true
        val chr = input.codepoint().toChar()
        if (ignoreOpeningAccent && chr in setOf(
                '~', '`', '^', '\'', '"', '´',
                'á', 'à', 'ã', 'â', 'ä', 'é', 'è', 'ê', 'ë', 'í', 'ì', 'î', 'ï',
                'ó', 'ò', 'õ', 'ô', 'ö', 'ú', 'ù', 'û', 'ü',
                'Á', 'À', 'Ã', 'Â', 'Ä', 'É', 'È', 'Ê', 'Ë', 'Í', 'Ì', 'Î', 'Ï',
                'Ó', 'Ò', 'Õ', 'Ô', 'Ö', 'Ú', 'Ù', 'Û', 'Ü'
            )
        ) {
            ignoreOpeningAccent = false
            return true
        }
        ignoreOpeningAccent = false
        return super.charTyped(input)
    }

    override fun build(UIComponent: StackLayout) {
        if (SoundRegistry.initialized) SoundRegistry.update()
        else SoundRegistry.refresh()
        if (selectedCategory is CategorySelection.Folder &&
            SoundRegistry.groups.none { it.id == (selectedCategory as CategorySelection.Folder).id }
        ) {
            selectedCategory = CategorySelection.All
        }
        super.build(UIComponent)
        restoredAudioScroll?.let { pendingScrollRestore = ScrollPositions(null, it) }
        restoredAudioScroll = null
    }

    override fun setup(container: FlowLayout) = container.apply {
        surface = Surface.flat(SlmVoiceTheme.SCREEN_BACKGROUND).and(Surface.outline(SlmVoiceTheme.SEPARATOR))
        padding = of(6)
    }

    override fun adapterPadding() = both(24, 16)

    override fun FlowLayout.configure() {
        compactLayout = client.window.scaledWidth < COMPACT_INITIAL_SCREEN_WIDTH
        horizontalSizing = fill()
        verticalSizing = fill()
        gap = 6
        this += headerBar()
        browserLayout = horizontalFlow {
            id = "browser-layout"
            horizontalSizing = fill()
            verticalSizing = expand()
            gap = 6
            sidebarPanel = sidebar()
            this += sidebarPanel
            this += centerList()
            detailsPanel().let { if (!compactLayout) this += it }
        }
        this += browserLayout
        rebuild()
    }

    private fun headerBar() = horizontalFlow {
        horizontalSizing = fill()
        verticalSizing = fixed(34)
        gap = 8
        padding = horizontal(12)
        verticalAlignment = io.wispforest.owo.ui.core.VerticalAlignment.CENTER
        surface = Surface.flat(PANEL).and(Surface.outline(BORDER))

        this += horizontalFlow {
            horizontalSizing = expand()
            verticalSizing = fill()
            gap = 4
            verticalAlignment = io.wispforest.owo.ui.core.VerticalAlignment.CENTER
            this += label(">".text()) {
                horizontalSizing = fixed(8)
                color = Color.ofArgb(ACCENT)
            }
            this += PixelMicrophoneComponent()
            this += label("SLM Voice".text()) {
                horizontalSizing = expand()
                color = Color.ofArgb(SlmVoiceTheme.TEXT_PRIMARY)
                shadow = true
            }
        }
        this += headerButton("Open folder") { openSelectedFolder() }.apply {
            horizontalSizing = fixed(68)
            verticalSizing = fixed(20)
            tooltipText = OPEN_DIRECTORY_TOOLTIP.translation()
        }
        this += headerButton("Refresh") { refreshSounds() }.apply {
            horizontalSizing = fixed(58)
            verticalSizing = fixed(20)
            tooltipText = Text.literal("Rescan the sound library")
        }
        this += headerButton("Import") { importSounds(BASE_DIR) }.apply {
            horizontalSizing = fixed(52)
            verticalSizing = fixed(20)
            tooltipText = IMPORT_TOOLTIP.translation()
        }
        if (compactLayout) {
            detailsToggleButton = headerButton("Details") { openCompactDetails() }.apply {
                horizontalSizing = fixed(48)
                verticalSizing = fixed(20)
            }
            this += detailsToggleButton!!
        }
        this += secondaryButton("X") { close() }.apply {
            horizontalSizing = fixed(24)
            verticalSizing = fixed(20)
            tooltipText = Text.literal("Close sound browser")
        }
    }.also { headerLayout = it }

    private fun sidebar() = verticalFlow {
        id = "category-panel"
        horizontalSizing = fixed(if (compactLayout) COMPACT_CATEGORY_WIDTH else CATEGORY_WIDTH)
        verticalSizing = fill()
        gap = 5
        padding = of(6)
        surface = Surface.flat(PANEL_DARK).and(Surface.outline(BORDER))

        this += horizontalFlow {
            horizontalSizing = fill()
            verticalSizing = fixed(20)
            gap = 4
            verticalAlignment = io.wispforest.owo.ui.core.VerticalAlignment.CENTER
            this += textBox {
                id = "search"
                horizontalSizing = fill()
                verticalSizing = fill()
                maxLength = Short.MAX_VALUE.toInt()
                drawBackground = true
                placeholder = "Search audio...".text()
                text = query
                onChange {
                    val next = it.trim()
                    if (query == next) return@onChange
                    query = next
                    rebuild()
                }
            }.wrap {
                horizontalSizing = expand()
                verticalSizing = fill()
                surface = Surface.flat(SlmVoiceTheme.BACKGROUND).and(Surface.outline(BORDER))
                padding = of(1)
            }
            this += memoryToggleButton()
        }.wrap {
            horizontalSizing = fill()
        }

        this += label("CATEGORIES".text()) {
            horizontalSizing = fill()
            color = Color.ofArgb(TEXT_MUTED)
            margins = top(4)
        }

        categoryList = verticalFlow {
            id = "category-list"
            horizontalSizing = fill()
            gap = 4
            padding = right(8)
        }
        categoryScroll = verticalScroll(categoryList) {
            id = "category-scroll"
            horizontalSizing = fill()
            verticalSizing = expand()
            scrollbar = ScrollContainer.Scrollbar.flat(Color.ofArgb(SlmVoiceTheme.SCROLLBAR))
            scrollbarThickness = 5
            surface = Surface.BLANK
        }
        this += categoryScroll
    }

    private fun centerList() = verticalFlow {
        id = "audio-panel"
        horizontalSizing = expand()
        verticalSizing = fill()
        gap = 6
        padding = of(8)
        surface = Surface.flat(PANEL).and(Surface.outline(BORDER))

        audioTitle = label("AUDIOS".text()) {
            id = "audio-title"
            horizontalSizing = fill()
            color = Color.ofArgb(TEXT_MUTED)
        }
        this += audioTitle

        audioList = verticalFlow {
            id = "audio-list"
            horizontalSizing = fill()
            padding = right(AUDIO_LIST_RIGHT_INSET)
        }
        audioScroll = verticalScroll(audioList) {
            id = "audio-scroll"
            horizontalSizing = fill()
            verticalSizing = expand()
            scrollbar = ScrollContainer.Scrollbar.flat(Color.ofArgb(SlmVoiceTheme.SCROLLBAR))
            scrollbarThickness = 6
            surface = Surface.BLANK
        }
        this += audioScroll
    }

    private fun detailsPanel() = verticalFlow {
        id = "details-panel"
        horizontalSizing = fixed(228)
        verticalSizing = fill()
        gap = 6
        padding = of(12)
        surface = Surface.flat(PANEL_DARK).and(Surface.outline(BORDER))
        detailsHeader = horizontalFlow {
            id = "details-header"
            horizontalSizing = fill()
            verticalSizing = fixed(22)
            verticalAlignment = io.wispforest.owo.ui.core.VerticalAlignment.CENTER
        }
        this += detailsHeader
        selectedDetails = verticalFlow {
            id = "selected-details-content"
            horizontalSizing = fill()
            verticalSizing = content()
            gap = 7
        }
        this += verticalScroll(selectedDetails) {
            id = "details-scroll"
            horizontalSizing = fill()
            verticalSizing = expand()
            scrollbar = ScrollContainer.Scrollbar.flat(Color.ofArgb(SlmVoiceTheme.SCROLLBAR))
            scrollbarThickness = 5
            surface = Surface.BLANK
        }
    }.also {
        detailsPanelComponent = it
    }

    private fun rebuild(): Unit = preserveScrollPositions {
        SoundRegistry.update()
        rebuildCategories()
        rebuildAudioList()
        rebuildDetails()
    }

    private fun rebuildCategories(): Unit = preserveScrollPositions {
        if (!::categoryList.isInitialized) return@preserveScrollPositions
        categoryList.children().toList().forEach(categoryList::removeChild)

        val entries = SoundRegistry.entries.toList()
        val all = filteredEntries(entries).size
        val shortcuts = entries.filter { Soundboard.config.keybind(it.id.str) != null }
        categoryList.child(categoryButton("All sounds", all, CategorySelection.All))
        categoryList.child(categoryButton("Favorites", filteredEntries(SoundRegistry.favourites.entries.values).size, CategorySelection.Favorites))
        categoryList.child(categoryButton("Shortcuts", filteredEntries(shortcuts).size, CategorySelection.Shortcuts))
        val groups = SoundRegistry.groups.filter { filteredEntries(it.entries.values).isNotEmpty() }.toList()
        if (groups.isNotEmpty()) categoryList.child(separator())
        groups.forEach { group ->
            val count = filteredEntries(group.entries.values).size
            categoryList.child(categoryButton(group.title.string, count, CategorySelection.Folder(group.id)))
        }
    }

    private fun rebuildAudioList(): Unit = preserveScrollPositions {
        if (!::audioList.isInitialized || !::audioTitle.isInitialized) return@preserveScrollPositions
        val entries = currentEntries()

        audioTitle.text = "AUDIOS (${entries.size})".text()
        audioList.children().toList().forEach(audioList::removeChild)
        audioRows.clear()
        displayedPlayingSoundId = SoundboardAccess.active?.scheduler?.playingSoundId

        if (selectedEntry?.id !in entries.asSequence().map { it.id }.toSet()) {
            selectedEntry = entries.firstOrNull()
        }

        val columns = audioGridColumnCount()
        audioGridColumns = columns
        entries.chunked(columns).forEach { rowEntries ->
            if (columns == 1) {
                audioList.child(audioRow(rowEntries.first()))
            } else {
                audioList.child(horizontalFlow {
                    horizontalSizing = fill()
                    verticalSizing = fixed(40)
                    gap = AUDIO_GRID_GAP
                    rowEntries.forEach { entry ->
                        this += audioRow(entry).apply { horizontalSizing = fill(AUDIO_GRID_CARD_PERCENT) }
                    }
                    if (rowEntries.size == 1) {
                        this += verticalFlow {
                            horizontalSizing = fill(AUDIO_GRID_CARD_PERCENT)
                            verticalSizing = fill()
                        }
                    }
                })
            }
        }
    }

    private fun audioGridColumnCount(): Int {
        if (!::audioList.isInitialized) return 1
        val availableWidth = (audioList.width() - AUDIO_LIST_RIGHT_INSET).coerceAtLeast(0)
        val twoColumnMinimum = (MIN_AUDIO_CARD_WIDTH * 100 + AUDIO_GRID_CARD_PERCENT - 1) / AUDIO_GRID_CARD_PERCENT
        return if (availableWidth >= twoColumnMinimum) 2 else 1
    }

    private fun refreshAudioGridLayout() {
        if (!::audioList.isInitialized) return
        val columns = audioGridColumnCount()
        if (columns == audioGridColumns) return
        rebuildAudioList()
    }

    private fun refreshResponsiveLayout() {
        if (!::browserLayout.isInitialized) return
        val width = browserLayout.width()
        if (width <= 0) return
        val useCompact = width < COMPACT_LAYOUT_MIN_WIDTH
        if (useCompact == compactLayout) return

        closeCompactDetails()
        compactLayout = useCompact
        if (compactLayout) {
            browserLayout.removeChild(detailsPanelComponent)
            sidebarPanel.horizontalSizing = fixed(COMPACT_CATEGORY_WIDTH)
            val button = headerButton("Details") { openCompactDetails() }.apply {
                horizontalSizing = fixed(48)
                verticalSizing = fixed(20)
            }
            detailsToggleButton = button
            headerLayout.child(headerLayout.children().size - 1, button)
        } else {
            detailsToggleButton?.let(headerLayout::removeChild)
            detailsToggleButton = null
            sidebarPanel.horizontalSizing = fixed(CATEGORY_WIDTH)
            detailsPanelComponent.verticalSizing = fill()
            browserLayout.child(detailsPanelComponent)
        }
        rebuildDetails()
        rebuildAudioList()
    }

    private fun openCompactDetails() {
        if (!compactLayout) return
        if (compactDetailsOverlay?.parent() == null) compactDetailsOverlay = null
        if (compactDetailsOverlay != null) return
        detailsPanelComponent.verticalSizing = fill()
        compactDetailsOverlay = overlay(detailsPanelComponent) {
            closeOnClick = false
            surface = Surface.flat(SlmVoiceTheme.SCRIM)
            sizing = fill()
            positioning = relative(50, 50)
            zIndex = 100
            onMouseDown { _, _, _ -> true }
        }
        root + compactDetailsOverlay!!
    }

    private fun closeCompactDetails() {
        val detailsOverlay = compactDetailsOverlay ?: return
        root.removeChild(detailsOverlay)
        compactDetailsOverlay = null
    }

    private fun rebuildDetails(): Unit = preserveScrollPositions {
        if (!::selectedDetails.isInitialized) return@preserveScrollPositions
        detailsHeader.children().toList().forEach(detailsHeader::removeChild)
        selectedDetails.children().toList().forEach(selectedDetails::removeChild)
        val entry = selectedEntry

        detailsHeader.child(label("SELECTED AUDIO".text()) {
            horizontalSizing = expand()
            color = Color.ofArgb(TEXT_MUTED)
        })
        if (compactLayout) {
            detailsHeader.child(secondaryButton("Back") { closeCompactDetails() }.apply {
                horizontalSizing = fixed(42)
                verticalSizing = fixed(20)
            })
        }

        if (entry == null) {
            selectedDetails.child(label("Select a sound".text()) {
                horizontalSizing = fill()
                color = Color.ofArgb(SlmVoiceTheme.TEXT_PRIMARY)
            })
            return@preserveScrollPositions
        }

        val bind = Soundboard.config.keybind(entry.id.str)
        val key = if (bindingTarget?.id == entry.id && bindingKeys.isNotEmpty()) {
            bindingKeys.joinToString(" + ") { SoundboardConfig.keyName(it) }
        } else Soundboard.config.keybindText(entry.id.str)
        val favorite = entry.id in Soundboard.config.favourites
        selectedDetails.child(label(entry.title) {
            horizontalSizing = fill()
            color = Color.ofArgb(SlmVoiceTheme.TEXT_PRIMARY)
            shadow = true
            tooltipText = entry.title
        })
        val fileName = entry.id.path(file = true)?.fileName?.toString().orEmpty().ifBlank { entry.id.file }
        selectedDetails.child(verticalFlow {
            horizontalSizing = fill()
            verticalSizing = content()
            gap = 3
            padding = of(6)
            surface = Surface.flat(PANEL).and(Surface.outline(BORDER))
            this += detailRow("Category", categoryName(entry))
            this += detailRow("Shortcut", key)
            this += detailRow("Favorite", if (favorite) "Yes" else "No")
            if (fileName.isNotBlank()) this += detailRow("File", fileName)
        })
        selectedDetails.child(separator())
        selectedDetails.child(actionButton("Play in Voice Chat") {
            play(entry)
        }.apply {
            horizontalSizing = fill()
            verticalSizing = fixed(26)
            margins = top(4)
        })
        selectedDetails.child(horizontalFlow {
            horizontalSizing = fill()
            gap = 4
            this += secondaryButton("Stop audio") {
                SoundboardAccess.forEach { scheduler.reset() }
                refreshPlaybackIndicators()
                rebuildDetails()
                rebuildAudioList()
            }.apply {
                horizontalSizing = fill(48)
                verticalSizing = fixed(20)
                tooltipText = Text.literal("Stop the current audio")
            }
            this += secondaryButton(if (favorite) "Unfavorite" else "Favorite") {
                if (favorite) Soundboard.config.favourites -= entry.id
                else Soundboard.config.favourites += entry.id
                Soundboard.config.save()
                rebuild()
            }.apply {
                horizontalSizing = fill(48)
                verticalSizing = fixed(20)
                tooltipText = Text.literal(if (favorite) "Remove from favorites" else "Add to favorites")
            }
        })
        val isCapturingBinding = bindingTarget?.id == entry.id
        selectedDetails.child(horizontalFlow {
            horizontalSizing = fill()
            gap = 4
            this += (if (isCapturingBinding) actionButton("Waiting...") {} else secondaryButton("Set shortcut") {
                beginBinding(entry)
            }).apply {
                horizontalSizing = expand()
                verticalSizing = fixed(20)
                tooltipText = Text.literal(if (isCapturingBinding) "Press a key combination; Esc cancels" else "Set a shortcut for this audio")
            }
            this += secondaryButton("Clear") {
                removeBinding(entry)
            }.apply {
                horizontalSizing = fixed(68)
                verticalSizing = fixed(20)
                active = bind != null && !isCapturingBinding
                tooltipText = Text.literal("Remove this audio's shortcut")
            }
        })
        selectedDetails.child(secondaryButton("Edit audio") {
            settings(entry)
        }.apply {
            horizontalSizing = fill()
            verticalSizing = fixed(20)
        })
    }

    private fun categoryButton(name: String, count: Int, category: CategorySelection) = ScrollingButtonComponent("$name ($count)".text(), null).apply {
        horizontalSizing = fill()
        verticalSizing = fixed(26)
        alignLeft = true
        contentPadding = 20
        folderIcon = category is CategorySelection.Folder
        renderer = categoryRenderer(category == selectedCategory)
        onPress {
            selectedCategory = category
            rebuild()
        }
    }

    private fun audioRow(entry: SoundEntry) = ScrollingButtonComponent(entry.title, null).apply {
        val bind = Soundboard.config.keybind(entry.id.str)
        val favorite = entry.id in Soundboard.config.favourites
        horizontalSizing = fill()
        verticalSizing = fixed(if (bind != null) 34 else 30)
        margins = bottom(5)
        alignLeft = true
        contentPadding = 12
        leftBoxWidth = 30
        leftMarker = playbackMarker(entry.id == displayedPlayingSoundId)
        entryId = entry.id
        markerColor = SlmVoiceTheme.TEXT_PRIMARY
        subText = bind?.let { "[${Soundboard.config.keybindText(entry.id.str)}]".text() }
        rightText = if (favorite) "\u2605".text() else "\u2606".text()
        rightTextColor = if (favorite) STAR else TEXT_MUTED
        rightInset = 20
        renderer = rowRenderer(entry)
        updateTooltip(entry)
        onMouseScroll { _, _, amount ->
            if (!ctrlDown) return@onMouseScroll false
            adjustVolume(entry, amount)
            updateTooltip(entry)
            true
        }
        primaryPressHandler = { localX ->
            selectedEntry = entry
            val playLeft = contentPadding.toDouble()
            val playRight = playLeft + leftBoxWidth
            val starText = rightText
            val starWidth = starText?.let(client.textRenderer::getWidth) ?: 0
            val starLeft = width() - contentPadding - rightInset - starWidth
            val starRight = starLeft + starWidth
            when {
                localX >= playLeft && localX < playRight -> {
                    play(entry)
                }
                starText != null && localX >= starLeft && localX <= starRight -> {
                    toggleFavorite(entry)
                    val nowFavorite = entry.id in Soundboard.config.favourites
                    rightText = if (nowFavorite) "\u2605".text() else "\u2606".text()
                    rightTextColor = if (nowFavorite) STAR else TEXT_MUTED
                    if (selectedCategory == CategorySelection.Favorites) rebuildAudioList()
                }
                ctrlDown -> settings(entry)
            }
            rebuildDetails()
        }
        audioRows[entry.id] = this
    }

    private fun actionButton(text: String, action: () -> Unit) = button(text.text()) {
        horizontalSizing = fill()
        verticalSizing = fixed(20)
        renderer = ButtonComponent.Renderer { context, button, _ ->
            val fill = if (button.isHovered) ACTION_HOVER else ACTION
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), SlmVoiceTheme.BACKGROUND)
            context.fill(button.x() + 1, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, fill)
            context.fill(button.x() + 1, button.y(), button.x() + button.width() - 1, button.y() + 1, ACCENT_HOVER)
        }
        onPress { action() }
    }

    private fun headerButton(text: String, action: () -> Unit) = button(text.text()) {
        horizontalSizing = fill()
        verticalSizing = fixed(20)
        renderer = ButtonComponent.Renderer { context, button, _ ->
            val fill = if (button.isHovered) ROW_HOVER else ROW
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill)
            if (button.isHovered) {
                context.fill(button.x(), button.y() + button.height() - 1, button.x() + button.width(), button.y() + button.height(), ACCENT)
            }
        }
        onPress { action() }
    }

    private fun secondaryButton(text: String, action: () -> Unit) = button(text.text()) {
        horizontalSizing = fill()
        verticalSizing = fixed(20)
        renderer = ButtonComponent.Renderer { context, button, _ ->
            val fill = if (button.isHovered) SECONDARY_HOVER else SECONDARY
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), SlmVoiceTheme.BACKGROUND)
            context.fill(button.x() + 1, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, fill)
        }
        onPress { action() }
    }

    private fun memoryToggleButton() = button(Text.empty()) {
        horizontalSizing = fixed(18)
        verticalSizing = fixed(18)
        tooltipText = memoryTooltip()
        renderer = ButtonComponent.Renderer { context, button, _ ->
            val left = button.x()
            val top = button.y()
            val right = left + button.width()
            val bottom = top + button.height()
            val bg = if (button.isHovered) SECONDARY_HOVER else SECONDARY
            context.fill(left, top, right, bottom, SlmVoiceTheme.BACKGROUND)
            context.fill(left + 1, top + 1, right - 1, bottom - 1, bg)

            PixelIcons.drawLock(context, left + (button.width() - 10) / 2, top + (button.height() - 12) / 2, browserMemoryEnabled)
        }
        onPress { toggleBrowserMemory() }
    }.also { memoryToggleButton = it }

    private fun memoryTooltip() =
        (if (browserMemoryEnabled) MEMORY_ENABLED_TOOLTIP else MEMORY_DISABLED_TOOLTIP).translation()

    private fun toggleBrowserMemory() {
        browserMemoryEnabled = !browserMemoryEnabled
        if (browserMemoryEnabled) rememberBrowserState()
        else rememberedBrowserState = null
        memoryToggleButton?.tooltipText = memoryTooltip()
    }

    private fun rememberBrowserState() {
        if (!browserMemoryEnabled) return
        val audioOffset = if (::audioScroll.isInitialized) audioScroll.scrollOffset() else restoredAudioScroll ?: 0.0
        rememberedBrowserState = BrowserSessionState(query, categoryKey(selectedCategory), audioOffset)
    }

    private fun finishBinding(entry: SoundEntry) {
        val keys = bindingKeys.distinct()
        val conflicts = conflictingBinds(entry, keys)
        bindingKeys.clear()
        ignoredBindingKeys.clear()
        bindingTarget = null
        selectedEntry = entry
        if (conflicts.isEmpty()) {
            Soundboard.config.setKeybind(entry.id.str, keys)
            rebuild()
        } else {
            showBindConflict(entry, keys, conflicts)
        }
    }

    private fun cancelBinding() {
        bindingKeys.clear()
        ignoredBindingKeys.clear()
        bindingTarget = null
        rebuildDetails()
    }

    private fun conflictingBinds(entry: SoundEntry, keys: List<Int>): List<String> {
        val combo = keys.toSet()
        val config = Soundboard.config
        return (config.keybindCombos.keys + config.keybinds.keys).distinct().filter { sound ->
            sound != entry.id.str && config.keybind(sound)?.toSet() == combo
        }
    }

    private fun showBindConflict(entry: SoundEntry, keys: List<Int>, conflicts: List<String>) {
        conflictTarget = entry
        conflictKeys = keys.toList()
        conflictIds = conflicts.toList()
        conflictWaiting = false
        conflictCaptureKeys.clear()
        ignoredHeldKeys.clear()
        refreshBindConflictOverlay()
    }

    private fun refreshBindConflictOverlay() {
        bindConflictOverlay?.let(root::removeChild)
        val dialogWidth = minOf(360, (client.window.scaledWidth - 24).coerceAtLeast(180))
        val horizontalActions = dialogWidth >= 300
        val actionWidth = ((dialogWidth - 24 - 12) / 3).coerceAtLeast(1)
        val halfActionWidth = ((dialogWidth - 24 - 6) / 2).coerceAtLeast(1)
        val maxNameLength = ((dialogWidth - 44) / 6).coerceIn(18, 50)
        val names = conflictIds.map { id ->
            val name = SoundRegistry[SoundId(id)]?.title?.string ?: SoundId(id).file.substringBeforeLast('.')
            if (name.length > maxNameLength) "${name.take(maxNameLength - 3)}..." else name
        }
        bindConflictOverlay = overlay(verticalFlow {
            horizontalSizing = fixed(dialogWidth)
            verticalSizing = content()
            gap = 8
            padding = of(12)
            surface = Surface.flat(PANEL).and(Surface.outline(BORDER))
            this += horizontalFlow {
                horizontalSizing = fill()
                verticalSizing = fixed(18)
                verticalAlignment = io.wispforest.owo.ui.core.VerticalAlignment.CENTER
                this += label("Shortcut conflict".text()) {
                    horizontalSizing = expand()
                    color = Color.ofArgb(SlmVoiceTheme.TEXT_PRIMARY)
                    shadow = true
                }
                this += secondaryButton("X") { closeBindConflict() }.apply {
                    horizontalSizing = fixed(18)
                    verticalSizing = fixed(18)
                }
            }
            val description = if (dialogWidth < 300) {
                "This shortcut is already assigned\nto another audio."
            } else "This shortcut is already assigned to another audio."
            this += label(description.text()) {
                horizontalSizing = fill()
                color = Color.ofArgb(TEXT_MUTED)
            }
            this += verticalFlow {
                horizontalSizing = fill()
                gap = 4
                padding = of(8)
                surface = Surface.flat(PANEL_ALT)
                this += label("SHORTCUT".text()) {
                    horizontalSizing = fill()
                    color = Color.ofArgb(TEXT_MUTED)
                }
                this += label(conflictKeys.joinToString(" + ") { SoundboardConfig.keyName(it) }.text()) {
                    horizontalSizing = fill()
                    color = Color.ofArgb(SlmVoiceTheme.TEXT_PRIMARY)
                    shadow = true
                }
                this += label("CURRENTLY USED BY".text()) {
                    horizontalSizing = fill()
                    color = Color.ofArgb(TEXT_MUTED)
                    margins = top(3)
                }
                names.forEach { name ->
                    this += label(name.text()) {
                        horizontalSizing = fill()
                        color = Color.ofArgb(SlmVoiceTheme.TEXT_PRIMARY)
                        shadow = true
                    }
                }
            }
            this += label("Would you like to replace it or choose another shortcut?".text()) {
                horizontalSizing = fill()
                color = Color.ofArgb(TEXT_MUTED)
            }
            if (conflictWaiting) {
                this += label("Press a new shortcut...".text()) {
                    horizontalSizing = fill()
                    color = Color.ofArgb(ACCENT_HOVER)
                }
            }
            this += horizontalFlow {
                horizontalSizing = fill()
                gap = 6
                verticalSizing = fixed(if (horizontalActions) 20 else if (conflictWaiting) 72 else 46)
                if (horizontalActions) {
                    this += secondaryButton(if (conflictWaiting) "Waiting..." else "Try another") {
                        if (!conflictWaiting) startConflictCapture()
                    }.apply { horizontalSizing = fixed(actionWidth) }
                    this += actionButton("Replace") {
                        replaceConflictingBind()
                    }.apply { horizontalSizing = fixed(actionWidth) }
                    this += secondaryButton("Cancel") { closeBindConflict() }
                        .apply { horizontalSizing = fixed(actionWidth) }
                } else {
                    this += horizontalFlow {
                        horizontalSizing = fill()
                        verticalSizing = fixed(20)
                        gap = 6
                        this += secondaryButton(if (conflictWaiting) "Waiting..." else "Try another") {
                            if (!conflictWaiting) startConflictCapture()
                        }.apply { horizontalSizing = fixed(halfActionWidth) }
                        this += actionButton("Replace") {
                            replaceConflictingBind()
                        }.apply { horizontalSizing = fixed(halfActionWidth) }
                    }
                    this += secondaryButton("Cancel") { closeBindConflict() }.apply { horizontalSizing = fill() }
                }
            }
        }) {
            closeOnClick = false
            surface = Surface.flat(SlmVoiceTheme.SCRIM)
            sizing = fill()
            positioning = relative(50, 50)
            zIndex = 110
            onMouseDown { _, _, _ -> true }
        }
        root + bindConflictOverlay!!
    }

    private fun closeBindConflict(refreshDetails: Boolean = true) {
        bindConflictOverlay?.let(root::removeChild)
        bindConflictOverlay = null
        conflictTarget = null
        conflictKeys = emptyList()
        conflictIds = emptyList()
        conflictWaiting = false
        conflictCaptureKeys.clear()
        ignoredHeldKeys.clear()
        if (refreshDetails) rebuildDetails()
    }

    private fun startConflictCapture() {
        conflictWaiting = true
        conflictCaptureKeys.clear()
        ignoredHeldKeys.clear()
        (GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_LAST)
            .filter { InputUtil.isKeyPressed(client.window, it) }
            .forEach(ignoredHeldKeys::add)
        refreshBindConflictOverlay()
    }

    private fun addConflictCaptureKeys(primary: Int) {
        if (primary < GLFW.GLFW_KEY_SPACE || primary in ignoredHeldKeys) return
        if (primary !in conflictCaptureKeys) conflictCaptureKeys += primary
        (GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_LAST)
            .filter { it !in ignoredHeldKeys && InputUtil.isKeyPressed(client.window, it) }
            .forEach { if (it !in conflictCaptureKeys) conflictCaptureKeys += it }
    }

    private fun completeConflictAttempt(keys: List<Int>) {
        if (keys.isEmpty()) return
        val entry = conflictTarget ?: return
        val conflicts = conflictingBinds(entry, keys)
        conflictWaiting = false
        if (conflicts.isEmpty()) {
            Soundboard.config.setKeybind(entry.id.str, keys)
            selectedEntry = entry
            closeBindConflict(false)
            rebuild()
        } else {
            conflictKeys = keys
            conflictIds = conflicts
            refreshBindConflictOverlay()
        }
    }

    private fun replaceConflictingBind() {
        val entry = conflictTarget ?: return
        Soundboard.config.setKeybindReplacing(entry.id.str, conflictKeys, conflictIds)
        selectedEntry = entry
        closeBindConflict(false)
        rebuild()
    }

    private fun detailRow(name: String, value: String) = horizontalFlow {
        horizontalSizing = fill()
        verticalSizing = fixed(16)
        this += label("$name:".text()) {
            horizontalSizing = fixed(70)
            color = Color.ofArgb(TEXT_MUTED)
        }
        this += label(value.text()) {
            horizontalSizing = fill()
            color = Color.ofArgb(SlmVoiceTheme.TEXT_PRIMARY)
            tooltipText = Text.literal(value)
        }
    }

    private fun openSelectedFolder() {
        val baseDirectory = BASE_DIR.toAbsolutePath().normalize()
        val directory = when (val category = selectedCategory) {
            is CategorySelection.Folder -> {
                if (SoundRegistry.groups.none { it.id == category.id }) baseDirectory
                else category.id.path(file = false)?.toAbsolutePath()?.normalize() ?: baseDirectory
            }
            CategorySelection.All,
            CategorySelection.Favorites,
            CategorySelection.Shortcuts -> baseDirectory
        }

        runCatching {
            require(Files.isDirectory(directory)) { "Selected sound folder is unavailable" }
            val realBase = baseDirectory.toRealPath()
            val realDirectory = directory.toRealPath()
            require(realDirectory.startsWith(realBase)) { "Selected sound folder is outside the SLM Voice library" }
            realDirectory.navigate()
        }.onFailure { error ->
            LOGGER.error("Could not open the selected SLM Voice folder", error)
            client.player?.sendMessage(Text.literal("SLM Voice: Could not open this folder."), false)
        }
    }

    private fun separator() = horizontalFlow {
        horizontalSizing = fill()
        verticalSizing = fixed(1)
        margins = vertical(4)
        surface = Surface.flat(BORDER)
    }

    private fun categoryRenderer(selected: Boolean) = ButtonComponent.Renderer { context, button, _ ->
        val fill = when {
            selected -> ROW_SELECTED
            button.isHovered -> ROW_HOVER
            else -> ROW
        }
        context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill)
        if (selected) {
            context.fill(button.x(), button.y(), button.x() + 4, button.y() + button.height(), ACCENT)
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + 1, ACCENT)
            context.fill(button.x(), button.y() + button.height() - 1, button.x() + button.width(), button.y() + button.height(), ACCENT)
        }
    }

    private fun rowRenderer(entry: SoundEntry) = ButtonComponent.Renderer { context, button, _ ->
        val selected = entry.id == selectedEntry?.id
        val fill = when {
            selected -> ROW_SELECTED
            button.isHovered -> ROW_HOVER
            else -> ROW
        }
        context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), SlmVoiceTheme.BACKGROUND)
        context.fill(button.x() + 1, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, fill)
        if (selected) {
            context.fill(button.x() + 1, button.y() + 1, button.x() + 6, button.y() + button.height() - 1, ACCENT)
            context.fill(button.x() + 1, button.y(), button.x() + button.width() - 1, button.y() + 1, ACCENT)
            context.fill(button.x() + 1, button.y() + button.height() - 1, button.x() + button.width() - 1, button.y() + button.height(), ACCENT)
            context.fill(button.x() + button.width() - 2, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, ACCENT_HOVER)
        } else if (button.isHovered) {
            context.fill(button.x() + 1, button.y() + 1, button.x() + 4, button.y() + button.height() - 1, ACCENT)
        }
    }

    private fun currentEntries(): List<SoundEntry> = when (val category = selectedCategory) {
        CategorySelection.All -> filteredEntries(SoundRegistry.entries.toList())
        CategorySelection.Favorites -> filteredEntries(SoundRegistry.favourites.entries.values)
        CategorySelection.Shortcuts -> filteredEntries(SoundRegistry.entries.filter { Soundboard.config.keybind(it.id.str) != null }.toList())
        is CategorySelection.Folder -> filteredEntries(SoundRegistry.groups.firstOrNull { it.id == category.id }?.entries?.values.orEmpty())
    }.sortedBy { it.title.string.lowercase() }

    private fun categoryName(entry: SoundEntry): String {
        val selected = selectedCategory
        if (selected is CategorySelection.Folder) {
            SoundRegistry.groups.firstOrNull { it.id == selected.id }?.let { return it.title.string }
            return selected.id.file.ifEmpty { selected.id.directory }.ifEmpty { "All sounds" }
        }

        SoundRegistry.groups.firstOrNull { group -> entry.id in group.entries.keys }?.let {
            return it.title.string
        }

        return entry.group.file.ifEmpty { entry.group.directory }.ifEmpty { "All sounds" }
    }

    private fun filteredEntries(entries: Collection<SoundEntry>): List<SoundEntry> {
        if (query.isBlank()) return entries.toList()
        val normalized = query.lowercase()
        return entries.filter {
            it.title.string.lowercase().contains(normalized) ||
                it.id.file.lowercase().contains(normalized) ||
                it.id.path.lowercase().contains(normalized)
        }
    }

    private fun ScrollingButtonComponent.updateTooltip(entry: SoundEntry) {
        val volume = ((AudioConfig[entry]?.volume ?: entry.settings?.volume ?: 1.0) * 100).toInt()
        val bind = Soundboard.config.keybind(entry.id.str)
        tooltipText = FILE_TOOLTIP.translation().copy()
            .append("\n").append(VOLUME_TOOLTIP.translation(volume.toString()))
            .apply {
                if (bind != null) append("\n").append(BOUND_TOOLTIP.translation(Soundboard.config.keybindText(entry.id.str)))
            }
    }

    private fun adjustVolume(entry: SoundEntry, scroll: Double) {
        val current = AudioConfig[entry] ?: entry.settings?.clone() ?: dev.srlordm.slmvoice.audio.data.AudioConfiguration.DEFAULT.clone()
        current.volume = (current.volume + scroll * 0.05).coerceIn(0.0, 1.0)
        AudioConfig[entry] = current
    }

    private fun toggleFavorite(entry: SoundEntry) {
        if (entry.id in Soundboard.config.favourites) Soundboard.config.favourites -= entry.id
        else Soundboard.config.favourites += entry.id
        Soundboard.config.save()
    }

    private fun play(entry: SoundEntry) {
        SoundboardAccess.active?.schedule(entry, false)
        refreshPlaybackIndicators()
    }

    private fun refreshPlaybackIndicators() {
        val playingSoundId = SoundboardAccess.active?.scheduler?.playingSoundId
        if (displayedPlayingSoundId == playingSoundId) return
        displayedPlayingSoundId = playingSoundId
        audioRows.forEach { (id, row) -> row.leftMarker = playbackMarker(id == playingSoundId) }
    }

    private fun playbackMarker(playing: Boolean) = if (playing) "||".text() else "\u25B6".text()

    private fun addBindingKeys(primary: Int) {
        if (primary < GLFW.GLFW_KEY_SPACE) return
        addBindingKey(primary)
        (GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_LAST)
            .filter { it !in ignoredBindingKeys && InputUtil.isKeyPressed(client.window, it) }
            .forEach(::addBindingKey)
    }

    private fun addBindingKey(key: Int) {
        if (key >= GLFW.GLFW_KEY_SPACE && key !in bindingKeys) bindingKeys += key
    }

    private fun refreshSounds() {
        SoundRegistry.refresh()
        rebuild()
    }

    private fun importSounds(location: Path) {
        val selected = TinyFileDialogs.tinyfd_openFileDialog(
            "Import sounds",
            null,
            null,
            "Audio files (*.mp3, *.wav)",
            true
        ) ?: return

        Files.createDirectories(location)
        selected.split('|').asSequence()
            .map(Path::of)
            .filter { Files.isRegularFile(it) && it.fileName.toString().hasAudioExtension() }
            .forEach { source ->
                Files.copy(source, uniqueDestination(location, source.fileName.toString()), StandardCopyOption.COPY_ATTRIBUTES)
            }
        SoundRegistry.refresh()
        rebuild()
    }

    private fun uniqueDestination(location: Path, fileName: String): Path {
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var candidate = location.resolve(fileName)
        var index = 1
        while (Files.exists(candidate)) {
            candidate = location.resolve("$base-$index$ext")
            index++
        }
        return candidate
    }

    private fun settings(entry: SoundEntry): Unit = preserveScrollPositions {
        settings?.update()
        settings?.dispose()
        settingsOverlay?.let(root::removeChild)
        settings = SoundSettingsWidget(entry, SoundboardAccess.delegates.first())
        val availableHeight = (client.window.scaledHeight - 32).coerceAtLeast(1)
        val viewportHeight = availableHeight.coerceAtMost(440)
        settings!!.margins = top(((viewportHeight - 400) / 2).coerceAtLeast(0))
        val settingsScroll = verticalScroll(settings!!) {
            horizontalSizing = fixed(420)
            verticalSizing = fixed(viewportHeight)
            scrollbar = ScrollContainer.Scrollbar.flat(Color.ofArgb(SlmVoiceTheme.SCROLLBAR))
            scrollbarThickness = 5
            surface = Surface.BLANK
        }
        settingsOverlay = overlay(settingsScroll) {
            closeOnClick = false
            surface = Surface.flat(SlmVoiceTheme.SCRIM)
            sizing = fill()
            positioning = relative(50, 50)
            zIndex = 100
            onMouseDown { _, _, _ -> true }
        }
        root + settingsOverlay!!
    }

    private fun preserveScrollPositions(action: () -> Unit) {
        val outermost = scrollMutationDepth == 0
        if (outermost) {
            scrollMutationPositions = pendingScrollRestore ?: captureScrollPositions()
        }
        scrollMutationDepth++
        try {
            action()
        } finally {
            scrollMutationDepth--
            if (outermost) {
                val positions = scrollMutationPositions
                scrollMutationPositions = null
                if (positions != null) {
                    restoreScrollPositions(positions)
                    pendingScrollRestore = positions.takeIf(::scrollRestoreWasClampedToTop)
                }
            }
        }
    }

    private fun captureScrollPositions() = ScrollPositions(
        categories = if (::categoryScroll.isInitialized) categoryScroll.scrollOffset() else null,
        audio = if (::audioScroll.isInitialized) audioScroll.scrollOffset() else null
    )

    private fun restoreScrollPositions(positions: ScrollPositions) {
        positions.categories?.let { if (::categoryScroll.isInitialized) categoryScroll.restoreScrollOffset(it) }
        positions.audio?.let { if (::audioScroll.isInitialized) audioScroll.restoreScrollOffset(it) }
    }

    private fun scrollRestoreWasClampedToTop(positions: ScrollPositions): Boolean {
        val categoriesLost = positions.categories?.let {
            it > 0.5 && ::categoryScroll.isInitialized && categoryScroll.scrollOffset() < 0.5
        } == true
        val audioLost = positions.audio?.let {
            it > 0.5 && ::audioScroll.isInitialized && audioScroll.scrollOffset() < 0.5
        } == true
        return categoriesLost || audioLost
    }

    private fun ScrollContainer<*>.scrollOffset() = (this as ScrollContainerAccessor).getScrollOffset()

    private fun ScrollContainer<*>.restoreScrollOffset(offset: Double) {
        val accessor = this as ScrollContainerAccessor
        accessor.invokeScrollBy(offset - accessor.getScrollOffset(), true, false)
    }
}
