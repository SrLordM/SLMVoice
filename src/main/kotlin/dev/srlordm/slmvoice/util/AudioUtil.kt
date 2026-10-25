package dev.srlordm.slmvoice.util

import dev.srlordm.slmvoice.audio.data.SoundEntry
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.CancellationException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.io.path.extension
import kotlin.math.E
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val Double.volumeScale get() =
    (exp(this) - 1) / (E - 1)

fun AudioFormat.duration(length: Int): Duration =
    duration(length.toLong())

fun AudioFormat.duration(length: Long): Duration =
    (length / sampleRate.toDouble()).seconds

fun AudioFormat.offset(duration: Duration): Int =
    (sampleRate * (duration.inWholeMicroseconds / 1_000_000.0)).toInt()

val AudioInputStream.duration: Duration
    get() = format.duration(frameLength)

fun InputStream.convert(format: AudioFormat) =
    convert(openAudioStream(this), format)

private fun openAudioStream(input: InputStream): AudioInputStream {
    val buffered = BufferedInputStream(input)
    buffered.mark(16 * 1024 * 1024)
    val skippedId3 = buffered.skipId3v2Tag()
    if (!skippedId3) buffered.reset()

    return try {
        AudioSystem.getAudioInputStream(buffered)
    } catch (e: UnsupportedAudioFileException) {
        if (skippedId3) throw e
        buffered.reset()
        if (!buffered.skipId3v2Tag()) throw e
        AudioSystem.getAudioInputStream(buffered)
    }
}

private fun BufferedInputStream.skipId3v2Tag(): Boolean {
    val header = ByteArray(10)
    if (read(header) != header.size) return false
    if (header[0].toInt().toChar() != 'I' || header[1].toInt().toChar() != 'D' || header[2].toInt().toChar() != '3') {
        reset()
        return false
    }

    val size = ((header[6].toInt() and 0x7F) shl 21) or
        ((header[7].toInt() and 0x7F) shl 14) or
        ((header[8].toInt() and 0x7F) shl 7) or
        (header[9].toInt() and 0x7F)
    val footer = if ((header[5].toInt() and 0x10) != 0) 10 else 0
    var remaining = (size + footer).toLong()
    while (remaining > 0) {
        checkInterrupted()
        val skipped = skip(remaining)
        if (skipped <= 0) return false
        remaining -= skipped
    }
    return true
}

fun SoundEntry.read(format: AudioFormat): ShortArray {
    val file = id.path(file = true)
    return supplier().use {
        if (file?.extension?.equals("mp3", ignoreCase = true) == true) it.decodeMp3(format)
        else it.convert(format).use { stream -> bytesToShorts(stream.readAllBytesInterruptibly()) }
    }
}

fun InputStream.decodeMp3(targetFormat: AudioFormat): ShortArray {
    val source = ShortArrayBuilder()
    BufferedInputStream(this).use { input ->
        val bitstream = Bitstream(input)
        val decoder = Decoder()
        try {
            while (true) {
                checkInterrupted()
                val header = bitstream.readFrame() ?: break
                val buffer = decoder.decodeFrame(header, bitstream) as SampleBuffer
                source.appendMixedMono(buffer)
                bitstream.closeFrame()
            }
        } finally {
            runCatching { bitstream.close() }
        }
    }
    return resampleMono(source.toShortArray(), source.sampleRate, targetFormat.sampleRate)
}

private class ShortArrayBuilder {
    private var data = ShortArray(8192)
    private var size = 0
    var sampleRate: Float = 0f
        private set

    fun appendMixedMono(buffer: SampleBuffer) {
        if (sampleRate <= 0f) sampleRate = buffer.sampleFrequency.toFloat()
        val samples = buffer.buffer
        val channels = buffer.channelCount.coerceAtLeast(1)
        var i = 0
        while (i < buffer.bufferLength) {
            if ((i and 0x1FFF) == 0) checkInterrupted()
            var mixed = 0
            repeat(channels) { channel ->
                if (i + channel < buffer.bufferLength) mixed += samples[i + channel].toInt()
            }
            append((mixed / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            i += channels
        }
    }

    private fun append(value: Short) {
        if (size >= data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun toShortArray() = data.copyOf(size)
}

private fun resampleMono(source: ShortArray, sourceRate: Float, targetRate: Float): ShortArray {
    if (source.isEmpty() || sourceRate <= 0f) return source
    if (sourceRate == targetRate) return source

    val targetLength = (source.size * targetRate / sourceRate).roundToInt().coerceAtLeast(1)
    return ShortArray(targetLength) { index ->
        if ((index and 0x3FFF) == 0) checkInterrupted()
        val position = index * sourceRate / targetRate
        val left = position.toInt().coerceIn(0, source.lastIndex)
        val right = (left + 1).coerceAtMost(source.lastIndex)
        val fraction = position - left
        (source[left] + (source[right] - source[left]) * fraction)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }
}

private fun InputStream.readAllBytesInterruptibly(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
        checkInterrupted()
        val count = read(buffer)
        if (count < 0) break
        if (count == 0) continue
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun checkInterrupted() {
    if (Thread.currentThread().isInterrupted) throw CancellationException("Audio preparation was cancelled")
}

fun convert(stream: AudioInputStream, targetFormat: AudioFormat): AudioInputStream {
    val originalFormat = stream.format
    val intermediateFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        originalFormat.sampleRate,
        16,
        originalFormat.channels,
        originalFormat.channels * 2,
        originalFormat.sampleRate,
        false
    )
    return stream
        .let { AudioSystem.getAudioInputStream(intermediateFormat, it) }
        .let { AudioSystem.getAudioInputStream(targetFormat, it) }
}

fun bytesToShorts(array: ByteArray, length: Int = array.size / 2) = ShortArray(length) {
    val byte0 = array.getOrElse(it * 2) { 0 }.toInt() and 255
    val byte1 = array.getOrElse(it * 2 + 1) { 0 }.toInt() and 255
    (byte1 shl 8 or byte0).toShort()
}
