package dev.brookmg.exorecord.lib

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.audio.AudioProcessor
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("unused")
class ExoRecordProcessor internal constructor(private val applicationContext: Context) : AudioProcessor, IExoRecord {

    private var sampleRateHz: Int = 0
    private var channelCount: Int = 0
    private var bytePerFrame: Int = 0

    @C.Encoding private var encoding: Int = 0
    private var isActive: Boolean = false

    private var processBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer? = null

    private var inputEnded: Boolean = false
    private var wavFile: WavFile? = null
    private var fileName: String = ""

    init {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        channelCount = Format.NO_VALUE
        sampleRateHz = Format.NO_VALUE
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, inputAudioFormat.encoding))
        }

        this.sampleRateHz = inputAudioFormat.sampleRate
        this.channelCount = inputAudioFormat.channelCount
        this.encoding = inputAudioFormat.encoding
        this.bytePerFrame = inputAudioFormat.bytesPerFrame

        isActive = true

        return inputAudioFormat
    }

    override fun isActive() = isActive

    private fun recordBuffer(inputBuffer: ByteBuffer) {
        val byteArray = ByteArray(inputBuffer.remaining())
        inputBuffer.get(byteArray)
        if (isActive) wavFile?.appendBytes(byteArray)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        var position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val frameCount = (limit - position) / (2 * channelCount)
        val outputSize = frameCount * channelCount * 2

        recordBuffer(inputBuffer)

        if (processBuffer.capacity() < outputSize) {
            processBuffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder())
        } else {
            processBuffer.clear()
        }

        while (position < limit) {
            var summedUp = 0
            for (channelIndex in 0 until channelCount) {
                val current = inputBuffer.getShort(position + 2 * channelIndex)
                processBuffer.putShort(current)
                summedUp += current
            }
            position += channelCount * 2
        }

        inputBuffer.position(limit)

        processBuffer.flip()
        outputBuffer = this.processBuffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val outputBuffer = this.outputBuffer
        this.outputBuffer = AudioProcessor.EMPTY_BUFFER
        return outputBuffer ?: ByteBuffer.allocate(0)
    }

    override fun isEnded(): Boolean = inputEnded && processBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        CoroutineScope(Dispatchers.IO).launch {
            stopRecording()
            withContext(Dispatchers.Main) {
                flush()
                processBuffer = AudioProcessor.EMPTY_BUFFER
                sampleRateHz = Format.NO_VALUE
                channelCount = Format.NO_VALUE
                encoding = Format.NO_VALUE
            }
        }
    }

    override suspend fun startRecording() : String {
        stopRecording()
        fileName = "radio-${System.nanoTime()}.wav"
        wavFile = WavFile(applicationContext = applicationContext, fileName)
        wavFile?.writeHeaders(sampleRateHz, bytePerFrame, channelCount)
        isActive = true
        return fileName
    }

    override suspend fun stopRecording(): IExoRecord.Record {
        isActive = false
        val wav = wavFile?.save()
        wavFile = null
        return IExoRecord.Record(fileName, sampleRateHz, bytePerFrame, channelCount)
    }

}