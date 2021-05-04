package dev.brookmg.exorecord.lib

import android.app.Application

/**
 * It's wise to pass the application context here
 */
class ExoRecord(private val application: Application) : IExoRecord{

    val exoRecordProcessor: ExoRecordProcessor by lazy { ExoRecordProcessor(applicationContext = application) }

    override suspend fun startRecording() = exoRecordProcessor.startRecording()

    override suspend fun stopRecording(saveAsAAC: Boolean): IExoRecord.Record = exoRecordProcessor.stopRecording(saveAsAAC)

}