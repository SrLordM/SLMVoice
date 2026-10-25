@file:Suppress("UnstableApiUsage")

package dev.kikugie.kowoui

import io.wispforest.owo.ui.component.*
import io.wispforest.owo.ui.component.SlimSliderComponent.Axis
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@JvmOverloads inline fun stack(build: StackLayout.() -> Unit = {}): StackLayout =
    UIContainers.stack(Sizing.content(), Sizing.content()).apply(build)

@JvmOverloads inline fun grid(rows: Int, columns: Int, build: GridLayout.() -> Unit = {}): GridLayout =
    UIContainers.grid(Sizing.content(), Sizing.content(), rows, columns).apply(build)

@JvmOverloads inline fun horizontalFlow(build: FlowLayout.() -> Unit = {}): FlowLayout =
    UIContainers.horizontalFlow(Sizing.content(), Sizing.content()).apply(build)

@JvmOverloads inline fun verticalFlow(build: FlowLayout.() -> Unit = {}): FlowLayout =
    UIContainers.verticalFlow(Sizing.content(), Sizing.content()).apply(build)

@JvmOverloads inline fun ltrTextFlow(build: FlowLayout.() -> Unit = {}): FlowLayout =
    UIContainers.ltrTextFlow(Sizing.content(), Sizing.content()).apply(build)

@JvmOverloads inline fun <T : UIComponent> horizontalScroll(child: T, build: ScrollContainer<T>.() -> Unit = {}): ScrollContainer<T> =
    UIContainers.horizontalScroll(Sizing.content(), Sizing.content(), child).apply(build)

@JvmOverloads inline fun <T : UIComponent> verticalScroll(child: T, build: ScrollContainer<T>.() -> Unit = {}): ScrollContainer<T> =
    UIContainers.verticalScroll(Sizing.content(), Sizing.content(), child).apply(build)

@JvmOverloads inline fun <T : UIComponent> draggable(child: T, build: DraggableContainer<T>.() -> Unit = {}): DraggableContainer<T> =
    UIContainers.draggable(Sizing.content(), Sizing.content(), child).apply(build)

@JvmOverloads inline fun collapsible(title: Text, expanded: Boolean, build: CollapsibleContainer.() -> Unit = {}): CollapsibleContainer =
    UIContainers.collapsible(Sizing.content(), Sizing.content(), title, expanded).apply(build)

@JvmOverloads inline fun <T : UIComponent> overlay(child: T, build: OverlayContainer<T>.() -> Unit = {}): OverlayContainer<T> =
    UIContainers.overlay(child).apply(build)

@JvmOverloads inline fun button(build: ButtonComponent.() -> Unit = {}): ButtonComponent =
    UIComponents.button("".text()) {}.apply(build)

@JvmOverloads inline fun button(text: Text, build: ButtonComponent.() -> Unit = {}): ButtonComponent =
    UIComponents.button(text) {}.apply(build)

@JvmOverloads inline fun textBox(build: TextBoxComponent.() -> Unit = {}): TextBoxComponent =
    UIComponents.textBox(Sizing.content()).apply(build)

@JvmOverloads inline fun textArea(build: TextAreaComponent.() -> Unit = {}): TextAreaComponent =
    UIComponents.textArea(Sizing.content(), Sizing.content()).apply(build)

@JvmOverloads inline fun <T : Entity> entity(entity: T, build: EntityComponent<T>.() -> Unit = {}): EntityComponent<T> =
    UIComponents.entity(Sizing.content(), entity).apply(build)

@JvmOverloads inline fun <T : Entity> entity(type: EntityType<T>, nbt: NbtCompound?, build: EntityComponent<T>.() -> Unit = {}): EntityComponent<T> =
    UIComponents.entity(Sizing.content(), type, nbt).apply(build)

@JvmOverloads inline fun item(stack: ItemStack, build: ItemComponent.() -> Unit = {}): ItemComponent =
    UIComponents.item(stack).apply(build)

@JvmOverloads inline fun block(state: BlockState, build: BlockComponent.() -> Unit = {}): BlockComponent =
    UIComponents.block(state).apply(build)

@JvmOverloads inline fun block(state: BlockState, blockEntity: BlockEntity, build: BlockComponent.() -> Unit = {}): BlockComponent =
    UIComponents.block(state, blockEntity).apply(build)

@JvmOverloads inline fun block(state: BlockState, nbt: NbtCompound?, build: BlockComponent.() -> Unit = {}): BlockComponent =
    UIComponents.block(state, nbt).apply(build)

@JvmOverloads inline fun label(build: LabelComponent.() -> Unit = {}): LabelComponent =
    UIComponents.label("".text()).apply(build)

@JvmOverloads inline fun label(text: Text, build: LabelComponent.() -> Unit = {}): LabelComponent =
    UIComponents.label(text).apply(build)

@JvmOverloads inline fun checkbox(text: Text, build: CheckboxComponent.() -> Unit = {}): CheckboxComponent =
    UIComponents.checkbox(text).apply(build)

@JvmOverloads inline fun smallCheckbox(text: Text, build: SmallCheckboxComponent.() -> Unit = {}): SmallCheckboxComponent =
    UIComponents.smallCheckbox(text).apply(build)

@JvmOverloads inline fun slider(build: SliderComponent.() -> Unit = {}): SliderComponent =
    UIComponents.slider(Sizing.content()).apply(build)

@JvmOverloads inline fun slimSlider(axis: Axis, build: SlimSliderComponent.() -> Unit = {}): SlimSliderComponent =
    UIComponents.slimSlider(axis).apply(build)

@JvmOverloads inline fun discreteSlider(min: Double, max: Double, build: DiscreteSliderComponent.() -> Unit = {}): DiscreteSliderComponent =
    UIComponents.discreteSlider(Sizing.content(), min, max).apply(build)

@JvmOverloads inline fun discreteSlider(range: ClosedFloatingPointRange<Double>, build: DiscreteSliderComponent.() -> Unit = {}): DiscreteSliderComponent =
    UIComponents.discreteSlider(Sizing.content(), range.start, range.endInclusive).apply(build)

@JvmOverloads inline fun sprite(id: SpriteIdentifier, build: SpriteComponent.() -> Unit = {}): SpriteComponent =
    UIComponents.sprite(id).apply(build)

@JvmOverloads inline fun sprite(sprite: Sprite, build: SpriteComponent.() -> Unit = {}): SpriteComponent =
    UIComponents.sprite(sprite).apply(build)

@JvmOverloads inline fun texture(
    texture: Identifier,
    u: Int,
    v: Int,
    regionWidth: Int,
    regionHeight: Int,
    textureWidth: Int = 256,
    textureHeight: Int = 256,
    build: TextureComponent.() -> Unit = {},
): TextureComponent =
    UIComponents.texture(texture, u, v, regionWidth, regionHeight, textureWidth, textureHeight).apply(build)

@JvmOverloads inline fun box(build: BoxComponent.() -> Unit = {}): BoxComponent =
    UIComponents.box(Sizing.content(), Sizing.content()).apply(build)

@JvmOverloads inline fun dropdown(build: DropdownComponent.() -> Unit = {}): DropdownComponent =
    UIComponents.dropdown(Sizing.content()).apply(build)

@JvmOverloads inline fun spacer(percent: Int = 100, build: SpacerComponent.() -> Unit = {}): SpacerComponent =
    UIComponents.spacer(percent).apply(build)
