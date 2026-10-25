package dev.srlordm.slmvoice.gui.component

import dev.srlordm.slmvoice.util.client
import dev.srlordm.slmvoice.util.drawScrollingText
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import net.minecraft.text.Text

class ScrollingLabelComponent(text: Text = Text.empty()) : LabelComponent(text) {
    private var center: (Int) -> Int = { x() + width() / 2 }
    private var clippedText: Text = text
    var contentPadding = 0
    var scrollOnlyOnHover = true
    var allowHoverScroll = true

    fun center(func:  (Int) -> Int) {
        center = func
    }

    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        val startX = x() + contentPadding
        val endX = x() + width() - contentPadding
        val maxWidth = (endX - startX).coerceAtLeast(8)
        val textWidth = textRenderer.getWidth(text())
        val overflowing = textWidth > maxWidth
        val hovered = isInBoundingBox(mouseX.toDouble(), mouseY.toDouble())

        if (overflowing && (!allowHoverScroll || scrollOnlyOnHover && !hovered)) {
            val clipped = clipped(maxWidth)
            val clippedWidth = textRenderer.getWidth(clipped)
            context.enableScissor(startX, y(), endX, y() + height())
            context.drawText(
                textRenderer,
                clipped,
                startX + maxWidth / 2 - clippedWidth / 2,
                y() + (height() - 8) / 2,
                color().get().argb(),
                shadow()
            )
            context.disableScissor()
            return
        }

        context.drawScrollingText(
            textRenderer,
            text(),
            center(textRenderer.getWidth(text())),
            startX,
            y(),
            endX,
            y() + height(),
            color().get().argb(),
            shadow()
        )
    }

    private fun clipped(maxWidth: Int): Text {
        if (clippedText !== text() && textRenderer.getWidth(clippedText) <= maxWidth) return clippedText

        val raw = text().string
        val suffix = "..."
        var end = raw.length
        while (end > 0 && textRenderer.getWidth(raw.take(end) + suffix) > maxWidth) end--
        clippedText = Text.of(raw.take(end).trimEnd() + suffix)
        return clippedText
    }
}

class CenteredEllipsisLabelComponent(text: Text = Text.empty()) : LabelComponent(text) {
    var contentPadding = 0
    private var clippedText: Text = text

    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        val startX = x() + contentPadding
        val endX = x() + width() - contentPadding
        val maxWidth = (endX - startX).coerceAtLeast(8)
        val rendered = if (textRenderer.getWidth(text()) <= maxWidth) text() else clipped(maxWidth)
        val renderedWidth = textRenderer.getWidth(rendered)
        val drawX = startX + (maxWidth - renderedWidth) / 2
        val drawY = y() + (height() - 8) / 2

        context.enableScissor(startX, y(), endX, y() + height())
        context.drawText(textRenderer, rendered, drawX, drawY, color().get().argb(), shadow())
        context.disableScissor()
    }

    private fun clipped(maxWidth: Int): Text {
        if (clippedText.string.endsWith("...") && textRenderer.getWidth(clippedText) <= maxWidth) return clippedText

        val raw = text().string
        val suffix = "..."
        var end = raw.length
        while (end > 0 && textRenderer.getWidth(raw.take(end) + suffix) > maxWidth) end--
        clippedText = Text.of(raw.take(end).trimEnd() + suffix)
        return clippedText
    }
}
