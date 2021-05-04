package dev.brookmg.exorecord

import android.app.Application
import dev.brookmg.exorecord.lib.ExoRecord

class App : Application() {

    companion object {
        lateinit var instance: App
        val exoRecordInstance: ExoRecord by lazy { ExoRecord(instance) }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}