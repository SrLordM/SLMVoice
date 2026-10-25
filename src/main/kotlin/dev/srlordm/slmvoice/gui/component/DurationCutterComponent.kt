package dev.srlordm.slmvoice.gui.component

import dev.srlordm.slmvoice.util.Property
import dev.srlordm.slmvoice.util.drawLinePrecise
import dev.srlordm.slmvoice.util.idOf
import dev.srlordm.slmvoice.gui.SlmVoiceTheme
import io.wispforest.owo.ui.base.BaseParentUIComponent
import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.*
import net.minecraft.client.gui.Click
import net.minecraft.client.gl.RenderPipelines
import kotlin.time.Duration

// 0..1
typealias Scalar = Double

class DurationCutterComponent(
    private val full: Duration,
    private val from: Property<Duration>,
    private val until: Property<Duration>,
) : BaseParentUIComponent(Sizing.fill(), Sizing.fill()) {

    private var selected: Slider? = null
    private val min: Slider = MinSlider()
    private val max: Slider = MaxSlider()
    private val both = listOf(min, max)
    private val Duration.progress get() = this / full
    var thickness: Double = 1.0

    override fun applySizing() {
        super.applySizing()
        each { inflate(space); update() }
    }

    override fun children(): List<UIComponent> = both

    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        super.draw(context, mouseX, mouseY, partialTicks, delta)
        each { draw(context, mouseX, mouseY, partialTicks, delta) }
    }

    override fun layout(space: Size) = each { inflate(space) }

    override fun removeChild(child: UIComponent): ParentUIComponent {
        throw UnsupportedOperationException("This UIComponent can't abandon its children")
    }

    fun update() = each(Slider::update)

    private inline fun each(action: Slider.() -> Unit) {
        min.action()
        max.action()
    }

    private inner class MaxSlider : Slider(until) {
        override fun isValid(mouseX: Double): Boolean =
            (mouseX - 2) > min.x() && mouseX < (container.x() + container.width())

        override fun moveTo(dest: Double) {
            pos = dest.coerceAtLeast(container.width() * min.pos + 2) / container.width()
        }
    }

    private inner class MinSlider : Slider(from) {
        override fun isValid(mouseX: Double): Boolean =
            mouseX > container.x() && (mouseX + 2) < max.x()

        override fun moveTo(dest: Double) {
            pos = dest.coerceAtMost(container.width() * max.pos - 2) / container.width()
        }
    }

    @Suppress("LeakingThis")
    private abstract inner class Slider(private val delegate: Property<Duration>) : BaseUIComponent() {
        var invalidDrag = false
        val container get() = this@DurationCutterComponent
        var pos: Scalar
            get() = delegate().progress
            set(value) = value.coerceIn(0.0, 1.0).run {
                delegate.set(full * this)
                update(this)
            }

        init {
            sizing(Sizing.fixed(3), Sizing.fill())
            cursorStyle(CursorStyle.MOVE)
        }

        abstract fun isValid(mouseX: Double): Boolean

        abstract fun moveTo(dest: Double)

        override fun canFocus(source: UIComponent.FocusSource?): Boolean = true

        override fun determineHorizontalContentSize(sizing: Sizing?): Int = 3

        override fun onMouseDrag(click: Click, deltaX: Double, deltaY: Double): Boolean {
            super.onMouseDrag(click, deltaX, deltaY)
            if (invalidDrag || !isValid(click.x() - deltaX + x())) invalidDrag = true
            else {
                selected = this
                move(deltaX)
            }
            return true
        }

        override fun onMouseUp(click: Click): Boolean =
            super.onMouseUp(click).also { invalidDrag = false; selected = null }

        override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
            val hovered = mouseY in y()..y() + height() && mouseX in x()..x() + width() && (selected == null || selected == this && !invalidDrag)
            val color = if (hovered) SlmVoiceTheme.PURPLE_LIGHT else SlmVoiceTheme.TEXT_SECONDARY
            val u = if (hovered) 8F else 0F
            context.drawLinePrecise(x() + 1.5, y() + 7.0, x() + 1.5, y() + height() - 7.0, thickness, Color.ofArgb(color))
            context.drawTexture(RenderPipelines.GUI_TEXTURED, POINTER_TEXTURE, x() - 2, y(), u, 0F, 8, 8, 8, 8, TEXTURE_SIZE, TEXTURE_SIZE)
            context.drawTexture(RenderPipelines.GUI_TEXTURED, POINTER_TEXTURE, x() - 2, y() + height() - 8,  u, 8F, 8, 8,8, 8, TEXTURE_SIZE, TEXTURE_SIZE)
        }

        fun move(delta: Double) = moveTo(container.width() * pos + delta)

        fun update(dest: Scalar = delegate().progress) {
            updateX(container.x() + (container.width() * dest).toInt().coerceIn(1, container.width() - 2) - 1)
        }
    }

    companion object {
        val POINTER_TEXTURE = idOf("textures/gui/pointer.png")
        const val TEXTURE_SIZE = 16
    }
}
