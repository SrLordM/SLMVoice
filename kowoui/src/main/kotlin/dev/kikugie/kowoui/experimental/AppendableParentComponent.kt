package dev.kikugie.kowoui.experimental

import io.wispforest.owo.ui.core.UIComponent

interface AppendableParentComponent {
    fun <C : UIComponent> addChild(UIComponent: C)
    fun <C : UIComponent> addChildren(UIComponents: Iterable<C>)
}