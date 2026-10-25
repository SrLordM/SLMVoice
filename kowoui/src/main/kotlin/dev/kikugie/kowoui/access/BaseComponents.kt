@file:Suppress("unused")

package dev.kikugie.kowoui.access

import dev.kikugie.kowoui.unsupported
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.ui.util.FocusHandler
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.text.Text

var UIComponent.cursorStyle: CursorStyle
    get() = cursorStyle()
    set(value) {
        cursorStyle(value)
    }

var UIComponent.horizontalSizing: Sizing
    get() = horizontalSizing().get()
    set(value) {
        horizontalSizing(value)
    }

var UIComponent.id: String?
    get() = id()
    set(value) {
        id(value)
    }

var UIComponent.margins: Insets
    get() = margins().get()
    set(value) {
        margins(value)
    }

var UIComponent.positioning: Positioning
    get() = positioning().get()
    set(value) {
        positioning(value)
    }

var UIComponent.sizing: Sizing
    @Deprecated("Getter unavailable", level = DeprecationLevel.ERROR)
    get() = unsupported { "Getter unavailable" }
    set(value) {
        sizing(value)
    }

var UIComponent.tooltip: List<TooltipComponent>?
    get() = tooltip()
    set(value) {
        tooltip(value)
    }

var UIComponent.tooltipText: Text
    @Deprecated("Getter unavailable", level = DeprecationLevel.ERROR)
    get() = unsupported { "Getter unavailable" }
    set(value) {
        tooltip(value)
    }

var UIComponent.tooltipTexts: Collection<Text>
    @Deprecated("Getter unavailable", level = DeprecationLevel.ERROR)
    get() = unsupported { "Getter unavailable" }
    set(value) {
        tooltip(value)
    }

var UIComponent.verticalSizing: Sizing
    get() = verticalSizing().get()
    set(value) {
        verticalSizing(value)
    }

var UIComponent.zIndex: Int
    get() = 0
    set(value) {}

val UIComponent.animatableHorizontalSizing: AnimatableProperty<Sizing>
    get() = horizontalSizing()
val UIComponent.animatableMargins: AnimatableProperty<Insets>
    get() = margins()
val UIComponent.animatablePositioning: AnimatableProperty<Positioning>
    get() = positioning()
val UIComponent.animatableVerticalSizing: AnimatableProperty<Sizing>
    get() = verticalSizing()
val UIComponent.focusHandler: FocusHandler?
    get() = focusHandler()
val UIComponent.hasParent: Boolean
    get() = hasParent()
val UIComponent.parent: ParentUIComponent?
    get() = parent()
val UIComponent.root: ParentUIComponent?
    get() = root()
