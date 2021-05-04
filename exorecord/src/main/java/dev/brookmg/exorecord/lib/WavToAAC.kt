package dev.brookmg.exorecord.lib

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.math.roundToInt


class WavToAAC {

    companion object {
        const val COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm"
        const val CODEC_TIMEOUT_IN_MS = 1000
        const val LOGTAG = "WavToAAC"
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun convertWavToAAC(context: Context, fileToConvert: String, samplingRate: Int,
                        audioBitRate: Int, channelCount: Int, progress: (Int) -> Unit = {} ) : String {
        val filePath: String = context.filesDir.absolutePath + File.separator + fileToConvert
        val aacFilePath: String = context.filesDir.absolutePath.toString() + "/" + fileToConvert.split('.')[0] + ".aac"

        val inputFile = File(filePath)
        val fis = FileInputStream(inputFile)

        val outputFile = File(aacFilePath)
        if (outputFile.exists()) outputFile.delete()

        val mux = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        var outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, samplingRate, channelCount)
        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate)

        val codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE)
        codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val codecInputBuffers: Array<ByteBuffer> = codec.inputBuffers // Note: Array of buffers
        val codecOutputBuffers: Array<ByteBuffer> = codec.outputBuffers

        val outBuffInfo = MediaCodec.BufferInfo()
        val tempBuffer = ByteArray(samplingRate)
        var hasMoreData = true
        var presentationTimeUs = 0.0
        var audioTrackIdx = 0
        var totalBytesRead = 0
        var percentComplete = 0

        while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
            var inputBufIndex = 0

            while (inputBufIndex != -1 && hasMoreData) {
                inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS.toLong())
                if (inputBufIndex >= 0) {
                    val dstBuf: ByteBuffer = codecInputBuffers[inputBufIndex]
                    dstBuf.clear()
                    val bytesRead: Int = fis.read(tempBuffer, 0, dstBuf.limit())
                    if (bytesRead == -1) { // -1 implies EOS
                        hasMoreData = false
                        codec.queueInputBuffer(inputBufIndex, 0, 0, presentationTimeUs.toLong(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        totalBytesRead += bytesRead
                        dstBuf.put(tempBuffer, 0, bytesRead)
                        codec.queueInputBuffer(inputBufIndex, 0, bytesRead, presentationTimeUs.toLong(), 0)
                        presentationTimeUs = (1000000L * (totalBytesRead / 2) / samplingRate).toDouble()
                    }
                }
            }

            var outputBufIndex = 0
            while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS.toLong())

                if (outputBufIndex >= 0) {
                    val encodedData: ByteBuffer = codecOutputBuffers[outputBufIndex]
                    encodedData.position(outBuffInfo.offset)
                    encodedData.limit(outBuffInfo.offset + outBuffInfo.size)
                    if (outBuffInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && outBuffInfo.size != 0) {
                        codec.releaseOutputBuffer(outputBufIndex, false)
                    } else {
                        mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo)
                        codec.releaseOutputBuffer(outputBufIndex, false)
                    }
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    outputFormat = codec.outputFormat
                    audioTrackIdx = mux.addTrack(outputFormat)
                    mux.start()
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // NO OP
                } else {
                    Log.e(LOGTAG, "Unknown return code from dequeueOutputBuffer - $outputBufIndex")
                }
            }

            percentComplete = (totalBytesRead.toFloat() / inputFile.length() * 100.0).roundToInt()
            progress.invoke(percentComplete)

        }

        fis.close()
        mux.stop()
        mux.release()
        Log.v(LOGTAG, "Compression done ...")
        return aacFilePath
    }

}