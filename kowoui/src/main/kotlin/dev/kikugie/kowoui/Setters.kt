package dev.kikugie.kowoui

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.GridLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.core.UIComponent

inline fun <T : UIComponent> FlowLayout.child(build: () -> T): T =
    build().also { this@child.child(it) }

inline fun <T : UIComponent> StackLayout.child(build: () -> T): T =
    build().also { this@child.child(it) }

inline fun <T : UIComponent> FlowLayout.child(index: Int, build: () -> T): T =
    build().also { this@child.child(index, it) }

inline fun <T : UIComponent> StackLayout.child(index: Int, build: () -> T): T =
    build().also { this@child.child(index, it) }

inline fun <T : UIComponent> GridLayout.child(row: Int, column: Int, build: () -> T): T =
    build().also { this@child.child(it, row, column) }

// Different name because of type inference
fun <T : UIComponent> GridLayout.setChild(row: Int, column: Int, child: T): T =
    child.also { this@setChild.child(it, row, column) }

fun FlowLayout.children(vararg UIComponents: UIComponent): FlowLayout =
    children(UIComponents.toList())

fun StackLayout.children(vararg UIComponents: UIComponent): StackLayout =
    children(UIComponents.toList())

fun FlowLayout.children(index: Int, vararg UIComponents: UIComponent): FlowLayout =
    children(index, UIComponents.toList())

fun StackLayout.children(index: Int, vararg UIComponents: UIComponent): StackLayout =
    children(index, UIComponents.toList())