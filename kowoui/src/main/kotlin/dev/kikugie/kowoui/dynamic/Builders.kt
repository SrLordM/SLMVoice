package dev.kikugie.kowoui.dynamic

import io.wispforest.owo.ui.core.UIComponent
import net.minecraft.text.Text as McText

inline infix fun <T : UIComponent> T.wrap(build: WrapperContainer<T>.() -> Unit) =
    WrapperContainer(this).apply(build)

@JvmOverloads inline fun fixedSpacer(pixels: Int, build: FixedSpacerComponent.() -> Unit = {}) =
    FixedSpacerComponent(pixels).apply(build)

@JvmOverloads inline fun dynamicButton(text: McText = McText.empty(), build: DynamicButtonComponent.() -> Unit = {}) =
    DynamicButtonComponent(text).apply(build)

@JvmOverloads inline fun dynamicLabel(text: McText = McText.empty(), build: DynamicLabelComponent.() -> Unit = {}) =
    DynamicLabelComponent(text).apply(build)

@JvmOverloads inline fun coloredTextBox(build: ColoredTextComponent.() -> Unit = {}) =
    ColoredTextComponent().apply(build)
