package dev.srlordm.slmvoice.gui.component

import dev.srlordm.slmvoice.util.drawLinePrecise
import dev.srlordm.slmvoice.util.volumeScale
import dev.srlordm.slmvoice.gui.SlmVoiceTheme
import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing
import kotlin.math.absoluteValue
import kotlin.properties.Delegates.observable

class WaveformComponent(
    private val data: DoubleArray,
    scalar: Double = 1.0
) : BaseUIComponent() {
    var thickness: Double = 1.0
    var color: Color = Color.ofArgb(SlmVoiceTheme.PURPLE)
    var mult: Double by observable(scalar) {_, _, _ ->
        update()
    }
    private var lastHeight: Int = 0
    private var lines: IntArray = intArrayOf()

    init {
        sizing(Sizing.fill())
    }

    override fun applySizing() {
        super.applySizing()
        if (lines.size != width() || lastHeight != height())
            update()
    }

    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        if (lines.isEmpty()) return
        val local = margins().get()
        var drawX = x() + local.right + .5
        val drawY = y() + local.top + .0
        for (i in 0..<lines.lastIndex) context.drawLinePrecise(
            drawX,
            drawY + lines[i],
            ++drawX,
            drawY + lines[i + 1],
            thickness,
            color
        )
    }

    private fun update() {
        if (data.isEmpty() || width() <= 0 || height() <= 0) {
            lines = intArrayOf()
            lastHeight = height()
            return
        }

        val compressed = compress(data, width())
        val max = compressed.maxBy { it.absoluteValue }.absoluteValue
        lines = compressed.map {
            if (max == 0.0) height() / 2
            else point((it * mult.volumeScale).coerceIn(-max, max), max)
        }.toIntArray()
        lastHeight = height()
    }

    private fun compress(array: DoubleArray, length: Int): DoubleArray {
        val window = array.size / length.toDouble()
        return DoubleArray(length) {
            val start = (it * window).toInt()
            val end = ((it + 1) * window).toInt().coerceAtMost(data.size)
            var sum = 0.0
            for (index in start until end) sum += array[index]
            if (end == start) array[start.coerceAtMost(array.lastIndex)] else sum / (end - start)
        }
    }

    private fun point(it: Double, max: Double): Int {
        val halfHeight = height() / 2F
        val lineHeight = it / max * halfHeight
        return (halfHeight - lineHeight).toInt()
    }
}
