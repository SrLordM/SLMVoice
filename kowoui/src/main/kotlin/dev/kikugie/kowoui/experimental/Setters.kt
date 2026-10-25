@file:Suppress("IMPLICIT_CAST_TO_ANY")

package dev.kikugie.kowoui.experimental

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.GridLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.ParentUIComponent

operator fun <P : ParentUIComponent, T : UIComponent> P.plusAssign(UIComponent: T) = when (this) {
    is AppendableParentComponent -> addChild(UIComponent)
    is FlowLayout -> child(UIComponent)
    is StackLayout -> child(UIComponent)
    else -> error("Parent must implement AppendableParentComponent")
}.let { }

operator fun <P : ParentUIComponent, T : UIComponent, I : Iterable<T>> P.plusAssign(UIComponents: I) = when (this) {
    is AppendableParentComponent -> addChildren(UIComponents)
    is FlowLayout -> children(UIComponents.toList())
    is StackLayout -> children(UIComponents.toList())
    else -> error("Parent must implement AppendableParentComponent")
}.let { }

operator fun <P : ParentUIComponent, T : UIComponent> P.plus(UIComponent: T): P = when (this) {
    is AppendableParentComponent -> addChild(UIComponent)
    is FlowLayout -> child(UIComponent)
    is StackLayout -> child(UIComponent)
    else -> error("Parent must implement AppendableParentComponent")
}.let { this }

operator fun <P : ParentUIComponent, T : UIComponent, I : Iterable<T>> P.plus(UIComponents: I) = when (this) {
    is AppendableParentComponent -> addChildren(UIComponents)
    is FlowLayout -> children(UIComponents.toList())
    is StackLayout -> children(UIComponents.toList())
    else -> error("Parent must implement AppendableParentComponent")
}.let { this }

fun FlowLayout.at(index: Int) = FlowLayoutSetter(index, this)
fun StackLayout.at(index: Int) = StackLayoutSetter(index, this)
fun GridLayout.at(row: Int, column: Int) = GridLayoutSetter(row, column, this)

class FlowLayoutSetter(private val index: Int, private val layout: FlowLayout) {
    operator fun <T : UIComponent> plusAssign(UIComponent: T) {
        layout.child(index, UIComponent)
    }
}

class StackLayoutSetter(private val index: Int, private val layout: StackLayout) {
    operator fun <T : UIComponent> plusAssign(UIComponent: T) {
        layout.child(index, UIComponent)
    }
}

class GridLayoutSetter(private val row: Int, private val column: Int, private val layout: GridLayout) {
    operator fun <T : UIComponent> plusAssign(UIComponent: T) {
        layout.child(UIComponent, row, column)
    }
}