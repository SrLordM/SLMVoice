package dev.srlordm.slmvoice.gui.component

import dev.srlordm.slmvoice.gui.SlmVoiceTheme
import dev.srlordm.slmvoice.util.client
import dev.srlordm.slmvoice.util.drawScrollingText
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import dev.srlordm.slmvoice.audio.data.SoundId
import net.minecraft.text.Text as McText
import java.util.function.Consumer

class ScrollingButtonComponent(private val fullMessage: McText, onPress: Consumer<ButtonComponent>?) : ButtonComponent(fullMessage, Consumer { component ->
    val scrolling = component as? ScrollingButtonComponent
    if (scrolling?.primaryPressHandler != null) scrolling.handlePrimaryPress()
    else onPress?.accept(component)
}) {
    private var clippedMessage: McText = fullMessage
    private var lastMouseX = 0
    var entryId: SoundId? = null
    var alignLeft = false
    var contentPadding = 8
    var rightText: McText? = null
    var rightTextColor = SlmVoiceTheme.TEXT_SECONDARY
    var rightInset = 0
    var leftMarker: McText? = null
    var markerColor = SlmVoiceTheme.TEXT_PRIMARY
    var leftBoxWidth = 0
    var subText: McText? = null
    var subTextColor = SlmVoiceTheme.TEXT_SECONDARY
    var folderIcon = false
    var primaryPressHandler: ((Double) -> Unit)? = null

    override fun draw(context: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        lastMouseX = mouseX
        val textRenderer = client.textRenderer
        val rightWidth = rightText?.let { textRenderer.getWidth(it) + contentPadding } ?: 0
        val markerWidth = if (leftBoxWidth > 0) leftBoxWidth + 8 else leftMarker?.let { textRenderer.getWidth(it) + 6 } ?: 0
        val textWidth = textRenderer.getWidth(fullMessage)
        val textStart = x() + contentPadding + markerWidth
        val textEnd = x() + width() - contentPadding - rightWidth - rightInset
        val contentWidth = (textEnd - textStart).coerceAtLeast(8)
        val overflowing = textWidth > contentWidth
        val hovered = isInBoundingBox(mouseX.toDouble(), mouseY.toDouble())
        val color = if (active()) SlmVoiceTheme.TEXT_PRIMARY else SlmVoiceTheme.TEXT_SECONDARY
        val hasSubText = subText != null
        val y = if (hasSubText) y() + 6 else y() + (height() - 8) / 2

        renderer().draw(context, this, delta)

        if (folderIcon) PixelIcons.drawFolder(context, this.x() + 6, this.y() + (height() - 9) / 2)

        leftMarker?.let {
            if (leftBoxWidth > 0) {
                val boxX = x() + contentPadding
                val boxY = y() + (height() - 18) / 2
                val localX = mouseX - x()
                val playHovered = hovered && localX >= contentPadding && localX <= contentPadding + leftBoxWidth
                val playing = it.string == "||"
                val iconColor = if (playing || playHovered) SlmVoiceTheme.TEXT_PRIMARY else markerColor

                if (playing || playHovered) {
                    context.fill(boxX, boxY, boxX + leftBoxWidth, boxY + 18, SlmVoiceTheme.BACKGROUND)
                    context.fill(boxX + 1, boxY + 1, boxX + leftBoxWidth - 1, boxY + 17, if (playing) SlmVoiceTheme.SELECTED else SlmVoiceTheme.CARD)
                    context.fill(boxX, boxY, boxX + leftBoxWidth, boxY + 1, SlmVoiceTheme.PURPLE)
                    context.fill(boxX, boxY + 17, boxX + leftBoxWidth, boxY + 18, SlmVoiceTheme.PURPLE)
                    context.fill(boxX, boxY, boxX + 1, boxY + 18, SlmVoiceTheme.PURPLE)
                    context.fill(boxX + leftBoxWidth - 1, boxY, boxX + leftBoxWidth, boxY + 18, SlmVoiceTheme.PURPLE)
                }

                context.drawText(textRenderer, it, boxX + leftBoxWidth / 2 - textRenderer.getWidth(it) / 2, boxY + 5, iconColor, textShadow())
            } else {
                context.drawText(textRenderer, it, x() + contentPadding, y, markerColor, textShadow())
            }
        }

        if (!overflowing) {
            val x = if (alignLeft) textStart else x() + width() / 2 - textWidth / 2
            context.drawText(textRenderer, fullMessage, x, y, color, textShadow())
        } else if (hovered) {
            context.drawScrollingText(
                textRenderer,
                fullMessage,
                if (alignLeft) textStart + textWidth / 2 else x() + width() / 2,
                textStart,
                y(),
                textEnd,
                y() + height(),
                color,
                textShadow()
            )
        } else {
            val text = clipped(contentWidth)
            val clippedWidth = textRenderer.getWidth(text)
            val x = if (alignLeft) textStart else x() + width() / 2 - clippedWidth / 2
            context.drawText(textRenderer, text, x, y, color, textShadow())
        }

        subText?.let {
            context.drawText(textRenderer, it, textStart, y + 10, subTextColor, textShadow())
        }

        rightText?.let {
            context.drawText(textRenderer, it, x() + width() - contentPadding - rightInset - textRenderer.getWidth(it), y, rightTextColor, textShadow())
        }

        drawTooltip(context, mouseX, mouseY, partialTicks, delta)
    }

    private fun handlePrimaryPress() {
        val localX = (lastMouseX - x()).toDouble().coerceIn(0.0, width().toDouble())
        primaryPressHandler?.invoke(localX)
    }

    private fun clipped(maxWidth: Int): McText {
        if (clippedMessage !== fullMessage && client.textRenderer.getWidth(clippedMessage) <= maxWidth) return clippedMessage

        val raw = fullMessage.string
        val suffix = "..."
        var end = raw.length
        while (end > 0 && client.textRenderer.getWidth(raw.take(end) + suffix) > maxWidth) end--
        clippedMessage = (raw.take(end).trimEnd() + suffix).let(McText::of)
        return clippedMessage
    }
}
