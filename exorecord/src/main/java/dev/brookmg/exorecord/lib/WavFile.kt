package dev.brookmg.exorecord.lib

import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.Format.NO_VALUE
import dev.brookmg.exorecord.lib.Util.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class WavFile(
    private val applicationContext: Context, private val fileName: String,
) {

    private var currentSampleRateHZ = NO_VALUE
    private var currentBytePerFrame = NO_VALUE
    private var currentChannelCount = NO_VALUE

    private val fileOutputStream = DataOutputStream(
        FileOutputStream(
            applicationContext.filesDir.absolutePath + File.separator + fileName
        )
    )
    
    private var audioArray: MutableList<Byte> = mutableListOf()

    fun appendBytes(byteArray: ByteArray) {
        byteArray.forEach { audioArray.add(it) }
        if (audioArray.size > 10) {
            fileOutputStream.write(audioArray.toByteArray())
            audioArray.clear()
        }
    }
    
    fun writeHeaders(sampleRateHz: Int, bytePerFrame: Int, channelCount: Int) {
        currentSampleRateHZ = sampleRateHz
        currentBytePerFrame = bytePerFrame
        currentChannelCount = channelCount

        // write the wav file per the wav file format
        fileOutputStream.write("RIFF".toByteArray()) // 00 - 04 - RIFF
        fileOutputStream.writeInt(Integer.reverseBytes(audioArray.size + 44 - 8)) // 04 - 08 - how big is the rest of this file?
        fileOutputStream.write("WAVE".toByteArray()) // 08 - 12 - WAVE
        fileOutputStream.write("fmt ".toByteArray()) // 12 - 16 - fmt

        fileOutputStream.writeInt(Integer.reverseBytes(16)) // 16 - 20 - size of this chunk
        fileOutputStream.write(1.toShort().toByteArray(2), 0, 2) // 20 - 22 - what is the audio format? 1 for PCM = Pulse Code Modulation
        fileOutputStream.write(2.toShort().toByteArray(2), 0, 2) // 22 - 24 - mono or stereo? 1 or 2?  (or 5 or ???)

        // THE CHAOS
        fileOutputStream.writeInt(Integer.reverseBytes(sampleRateHz / 2)) // 24 - 28 - samples per second (numbers per second)
        fileOutputStream.writeInt(Integer.reverseBytes(((sampleRateHz / 2) * bytePerFrame * channelCount))) // 28 - 32 - bytes per second
        fileOutputStream.write((bytePerFrame * channelCount).toShort().toByteArray(2), 0, 2) // 32 - 34 - # of bytes in one sample, for all channels
        fileOutputStream.write((bytePerFrame * 8).toShort().toByteArray(2), 0, 2) // 34 - 36 // - how many bits in a sample(number)?  usually 16 or 24

        fileOutputStream.write("data".toByteArray()) // 36 40 - data
        fileOutputStream.writeInt(Integer.reverseBytes(audioArray.size))
    }

    suspend fun save(saveAsAAC: Boolean = false) : String? = withContext(Dispatchers.IO){
        try {

            // Change the wav content size
            fileOutputStream.write(audioArray.toByteArray())
            fileOutputStream.close()

            val randomAccessFile = RandomAccessFile(applicationContext.filesDir.absolutePath + File.separator + fileName, "rw")
            randomAccessFile.seek(40)
            randomAccessFile.write(Integer.reverseBytes(fileOutputStream.size() - 40).toByteArray(4), 0, 4)
            randomAccessFile.seek(4)
            randomAccessFile.write(Integer.reverseBytes(fileOutputStream.size() - 8).toByteArray(4), 0, 4)

            audioArray.clear()

            if (saveAsAAC && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return@withContext WavToAAC().convertWavToAAC(applicationContext, fileName,
                        samplingRate = currentSampleRateHZ,
                        audioBitRate = (currentSampleRateHZ) * currentBytePerFrame * currentChannelCount,
                        channelCount = currentChannelCount) { progress ->
                    Log.v("WaveToAAC" , "Working @ $progress%")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }


}