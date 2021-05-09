package dev.brookmg.exorecord.lib

interface IExoRecord {

    data class Record(val filePath: String, val sampleBitRate: Int, val bitRate: Int,
                      val channelCount: Int)

    suspend fun startRecording() : String
    suspend fun stopRecording() : Record

}