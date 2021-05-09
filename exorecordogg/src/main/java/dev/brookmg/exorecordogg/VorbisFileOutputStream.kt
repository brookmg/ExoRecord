package dev.brookmg.exorecordogg

import android.util.Log
import java.io.IOException

/**
 * Converts incoming PCM Audio Data into OGG data into a file. This will be implemented using the
 * open source BSD-licensed stuff from Xiph.org.
 *
 * NOTE: This implementation has a limitation of MAX_STREAMS concurrent output streams.
 * When i wrote this, it was set to 8. Check in vorbis-fileoutputstream.c to see what it is set to.
 *
 */
internal class VorbisFileOutputStream : AudioOutputStream {
    // The index into native memory where the ogg stream info is stored.
    private val oggStreamIdx: Int
    private lateinit var info: VorbisInfo

    companion object {
        const val VORBIS_BLOCK_SIZE = 1024

        init {
            System.loadLibrary("ogg")
            System.loadLibrary("vorbis")
            System.loadLibrary("vorbis-stream")
        }
    }

    constructor(fileName: String, s: VorbisInfo) {
        info = s
        oggStreamIdx = create(fileName, s)
        Log.e("OGG", "OggStreamIDx = $oggStreamIdx")
    }

    constructor(fileName: String) {
        oggStreamIdx = create(fileName, VorbisInfo())
    }

    @Throws(IOException::class)
    override fun close() {
        closeStreamIdx(oggStreamIdx)
        Log.e("OGG", "Close = $oggStreamIdx")
    }

    /**
     * Write PCM data to ogg. This assumes that you pass your streams in interleaved.
     * @param buffer the pcm buffer short array
     * @param offset the start offset in the data.
     * @param length the number of bytes to write.
     * @throws IOException if an I/O error occurs. In particular,
     * an `IOException` is thrown if the output
     * stream is closed.
     */
    @Throws(IOException::class)
    override fun write(buffer: ShortArray, offset: Int, length: Int) {
        writeStreamIdx(oggStreamIdx, buffer, offset, length)
        Log.e(
            "OGG",
            "Writing = { " + oggStreamIdx + ", " + buffer.size + ", " + offset + ", " + length + " }"
        )
    }

    @Throws(IOException::class)
    private external fun writeStreamIdx(idx: Int, pcmdata: ShortArray, offset: Int, size: Int): Int

    @Throws(IOException::class)
    private external fun closeStreamIdx(idx: Int)

    @Throws(IOException::class)
    private external fun create(path: String, s: VorbisInfo?): Int

    override fun getSampleRate(): Int = info.sampleRate
}