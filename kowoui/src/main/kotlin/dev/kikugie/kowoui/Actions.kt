@file:Suppress("unused")

package dev.kikugie.kowoui

import io.wispforest.owo.ui.component.*
import io.wispforest.owo.ui.container.CollapsibleContainer
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.UIComponent.FocusSource
import io.wispforest.owo.ui.event.*
import net.minecraft.client.gui.Click
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput

fun <T : SlimSliderComponent> T.onChange(action: (Double) -> Unit) =
    this.also { onChanged().subscribe(SlimSliderComponent.OnChanged(action)) }

fun <T : ColorPickerComponent> T.onChange(action: (Color) -> Unit) =
    this.also { onChanged().subscribe(ColorPickerComponent.OnChanged(action)) }

fun <T : SmallCheckboxComponent> T.onChange(action: (Boolean) -> Unit) =
    this.also { onChanged().subscribe(SmallCheckboxComponent.OnChanged(action)) }

fun <T : SliderComponent> T.onChange(action: (Double) -> Unit) =
    this.also { onChanged().subscribe(SliderComponent.OnChanged(action)) }

fun <T : TextAreaComponent> T.onChange(action: (String) -> Unit) =
    this.also { onChanged().subscribe(TextAreaComponent.OnChanged(action)) }

fun <T : TextBoxComponent> T.onChange(action: (String) -> Unit) =
    this.also { onChanged().subscribe(TextBoxComponent.OnChanged(action)) }

fun <T : UIComponent> T.onCharType(action: (Char, Int) -> Boolean) =
    this.also { charTyped().subscribe(CharTyped { input: CharInput ->
        action(input.codepoint().toChar(), input.modifiers())
    }) }

fun <T : UIComponent> T.onFocusGain(action: (FocusSource) -> Unit) =
    this.also { focusGained().subscribe(FocusGained(action)) }

fun <T : UIComponent> T.onFocusLose(action: () -> Unit) =
    this.also { focusLost().subscribe(FocusLost(action)) }

fun <T : UIComponent> T.onKeyPress(action: (Int, Int, Int) -> Boolean) =
    this.also { keyPress().subscribe(KeyPress { input: KeyInput ->
        action(input.key(), input.scancode(), input.modifiers())
    }) }

fun <T : UIComponent> T.onMouseDown(action: (Double, Double, Int) -> Boolean) =
    this.also { mouseDown().subscribe(MouseDown { click: Click, _ ->
        action(click.x(), click.y(), click.button())
    }) }

fun <T : UIComponent> T.onMouseDrag(action: (Double, Double, Double, Double, Int) -> Boolean) =
    this.also { mouseDrag().subscribe(MouseDrag { click: Click, deltaX, deltaY ->
        action(click.x(), click.y(), deltaX, deltaY, click.button())
    }) }

fun <T : UIComponent> T.onMouseEnter(action: () -> Unit) =
    this.also { mouseEnter().subscribe(MouseEnter(action)) }

fun <T : UIComponent> T.onMouseLeave(action: () -> Unit) =
    this.also { mouseLeave().subscribe(MouseLeave(action)) }

fun <T : UIComponent> T.onMouseScroll(action: (Double, Double, Double) -> Boolean) =
    this.also { mouseScroll().subscribe(MouseScroll(action)) }

fun <T : UIComponent> T.onMouseUp(action: (Double, Double, Int) -> Boolean) =
    this.also { mouseUp().subscribe(MouseUp { click: Click ->
        action(click.x(), click.y(), click.button())
    }) }

fun <T : SliderComponent> T.onSlideEnd(action: () -> Unit) =
    this.also { slideEnd().subscribe(SliderComponent.OnSlideEnd(action)) }

fun <T : SlimSliderComponent> T.onSlideEnd(action: () -> Unit) =
    this.also { onSlideEnd().subscribe(SlimSliderComponent.OnSlideEnd(action)) }

fun <T : CollapsibleContainer> T.onToggle(action: (Boolean) -> Unit) =
    this.also { onToggled().subscribe(CollapsibleContainer.OnToggled(action)) }
