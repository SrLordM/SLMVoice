package dev.kikugie.kowoui.experimental

import io.wispforest.owo.ui.core.UIComponent

interface MutableParentComponent<T> where T : Position {
    fun <C : UIComponent> addChild(position: T, UIComponent: C)
    fun <C : UIComponent> addChildren(position: T, UIComponents: Iterable<C>)
}