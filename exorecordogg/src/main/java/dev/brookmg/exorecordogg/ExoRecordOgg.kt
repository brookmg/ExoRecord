package dev.brookmg.exorecordogg

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class ExoRecordOgg {

    companion object {

        fun convertFile(
            applicationContext: Application, fileName: String,
            sampleRate: Int, channels: Int, quality: Float,
            progress: suspend (Float) -> Unit = {}
        ) : Record {
            val coroutineScope = CoroutineScope(Dispatchers.Main)

            val oggFilePath: String = applicationContext.filesDir.absolutePath.toString() + "/" + fileName.split('.')[0] + ".ogg"
            val vorbis = VorbisFileOutputStream(
                oggFilePath,
                VorbisInfo().apply {
                    this.channels = channels
                    this.sampleRate = sampleRate
                    this.quality = quality
                })

            var inputWav: FileInputStream? = null

            try {
                inputWav = FileInputStream(applicationContext.filesDir.absolutePath + File.separator + fileName)
                inputWav.skip(44)   // Skip the wav file header
                val fileTotalSize = inputWav.channel.size()
                var currentlyRead = 0

                val buffer = ByteArray(1024)
                var length: Int
                while (inputWav.read(buffer) > 0) {
                    currentlyRead += 1024
                    val shortArray = ShortArray(buffer.size / 2) {
                        (buffer[it * 2].toUByte().toInt() +
                                (buffer[(it * 2) + 1].toInt() shl 8)).toShort()
                    }
                    length = shortArray.size
                    vorbis.write(shortArray, 0, length)

                    coroutineScope.launch {
                        progress.invoke(
                            ((currentlyRead.toFloat()) / fileTotalSize.toFloat()) * 100f
                        )
                    }
                }
            } finally {
                inputWav?.close()
                vorbis.close()
                coroutineScope.launch { progress.invoke(100f) }
            }

            return Record(
                applicationContext.filesDir.absolutePath.toString() + "/" + fileName,
                sampleRate,
                quality,
                channels,
                oggFilePath
            )
        }

        fun convertFromStream(
            applicationContext: Application, fileName: String,
            inputWav: InputStream, totalSize: Int,
            sampleRate: Int, channels: Int, quality: Float,
            progress: suspend (Float) -> Unit = {}
        ) : Record {
            val coroutineScope = CoroutineScope(Dispatchers.Main)

            val oggFilePath: String = applicationContext.filesDir.absolutePath.toString() + "/" + fileName + ".ogg"
            val vorbis = VorbisFileOutputStream(
                oggFilePath,
                VorbisInfo().apply {
                    this.channels = channels
                    this.sampleRate = sampleRate
                    this.quality = quality
                })

            try {
                inputWav.skip(44)   // Skip the wav file header
                var currentlyRead = 0

                val buffer = ByteArray(1024)
                var length: Int
                while (inputWav.read(buffer) > 0) {
                    currentlyRead += 1024
                    val shortArray = ShortArray(buffer.size / 2) {
                        (buffer[it * 2].toUByte().toInt() +
                                (buffer[(it * 2) + 1].toInt() shl 8)).toShort()
                    }
                    length = shortArray.size
                    vorbis.write(shortArray, 0, length)

                    coroutineScope.launch {
                        progress.invoke(
                            ((currentlyRead.toFloat()) / totalSize.toFloat()) * 100f
                        )
                    }
                }
            } finally {
                inputWav.close()
                vorbis.close()
                coroutineScope.launch { progress.invoke(100f) }
            }

            return Record(
                fileName,
                sampleRate,
                quality,
                channels,
                oggFilePath
            )
        }
    }

}