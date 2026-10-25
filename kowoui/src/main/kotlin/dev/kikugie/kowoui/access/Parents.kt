@file:Suppress("unused")

package dev.kikugie.kowoui.access

import dev.kikugie.kowoui.util.CombinedAlignment
import io.wispforest.owo.ui.core.*

var ParentUIComponent.allowOverflow: Boolean
    get() = allowOverflow()
    set(value) {
        allowOverflow(value)
    }

var ParentUIComponent.padding: Insets
    get() = padding().get()
    set(value) {
        padding(value)
    }

var ParentUIComponent.surface: Surface
    get() = surface()
    set(value) {
        surface(value)
    }

var ParentUIComponent.alignment: CombinedAlignment
    get() = CombinedAlignment.of(horizontalAlignment, verticalAlignment)
    set(value) {
        horizontalAlignment = value.horizontal
        verticalAlignment = value.vertical
    }

var ParentUIComponent.horizontalAlignment: HorizontalAlignment
    get() = horizontalAlignment()
    set(value) {
        horizontalAlignment(value)
    }

var ParentUIComponent.verticalAlignment: VerticalAlignment
    get() = verticalAlignment()
    set(value) {
        verticalAlignment(value)
    }

val ParentUIComponent.animatablePadding: AnimatableProperty<Insets>
    get() = padding()
val ParentUIComponent.children: List<UIComponent>
    get() = children()