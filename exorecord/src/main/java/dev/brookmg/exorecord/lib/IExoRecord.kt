package dev.brookmg.exorecord.lib

interface IExoRecord {

    data class Record(val filePath: String, val sampleBitRate: Int, val bytePerFrame: Int,
                      val channelCount: Int, val aacFilePath: String? = null)

    suspend fun startRecording()
    suspend fun stopRecording(saveAsAAC: Boolean = false) : Record

}