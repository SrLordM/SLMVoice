package dev.srlordm.slmvoice.util

import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.ParentUIComponent
import net.minecraft.client.font.TextRenderer
import net.minecraft.text.Text
import net.minecraft.util.Util
import net.minecraft.util.math.MathHelper
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun <T : ParentUIComponent> T.all(): Sequence<UIComponent> = sequence {
    for (it in children()) {
        if (it is ParentUIComponent) for (it1 in it.all())
            yield(it1)
        yield(it)
    }
    yield(this@all)
}

fun OwoUIGraphics.drawLinePrecise(x1: Double, y1: Double, x2: Double, y2: Double, thickness: Double, color: Color) {
    drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), thickness, color)
}

fun OwoUIGraphics.drawScrollingText(
    textRenderer: TextRenderer,
    text: Text,
    centerX: Int,
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    color: Int,
    textShadow: Boolean
) {
    val textWidth = textRenderer.getWidth(text)
    val j = (startY + endY - 9) / 2 + 1
    val buttonWidth = endX - startX
    if (textWidth <= buttonWidth)
        drawText(textRenderer, text, centerX - textWidth / 2, j, color, textShadow)
    else {
        val l = textWidth - buttonWidth
        val d = Util.getMeasuringTimeMs().toDouble() / 1000.0
        val e = max(l.toDouble() * 0.5, 3.0)
        val f = sin((Math.PI / 2) * cos((Math.PI * 2) * d / e)) / 2.0 + 0.5
        val g = MathHelper.lerp(f, 0.0, l.toDouble())
        enableScissor(startX, startY, endX, endY)
        drawText(textRenderer, text, startX - g.toInt(), j, color, textShadow)
        disableScissor()
    }
}
