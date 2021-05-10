package dev.brookmg.exorecord

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsExtractorFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import dev.brookmg.exorecord.lib.ExoRecord
import dev.brookmg.exorecord.lib.IExoRecord
import dev.brookmg.exorecordogg.ExoRecordOgg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    companion object {
        const val FORMAT_MP3 = "mp3"
        const val FORMAT_MP4 = "mp4"
        const val FORMAT_M3U = "m3u"
        const val FORMAT_M3U8 = "m3u8"
    }

    private lateinit var startRadioButton: Button
    private lateinit var stopRadioButton: Button

    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button

    private val exoRecordListener = object: ExoRecord.ExoRecordListener {
        override fun onStartRecording(recordFileName: String) {
            startRecordingButton.isGone = true
            stopRecordingButton.isGone = false
        }

        override fun onStopRecording(record: IExoRecord.Record) {
            startRecordingButton.isGone = false
            stopRecordingButton.isGone = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startRecordingButton = findViewById(R.id.button_rec)
        stopRecordingButton = findViewById(R.id.button_rec_stop)

        val streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one?s=1619514803&e=1619529203&h=f378f4ca18759ebfa5fd1d674c794cfc"

        val mainMediaSource: MediaSource
        val uri = Uri.parse(streamUrl)
        val lastPath = uri.lastPathSegment

        val bandwidthMeter = DefaultBandwidthMeter.Builder(applicationContext).build()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory()
        val httpDataSourceFactory = DefaultHttpDataSourceFactory(
            "-- Audio Test --",
            bandwidthMeter,
            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
            true
        )

        val dataSourceFactory = DefaultDataSourceFactory(this, bandwidthMeter, httpDataSourceFactory)

        if (lastPath == null || lastPath.isEmpty()) return
        mainMediaSource = if (lastPath.contains(FORMAT_M3U8) ||
            lastPath.contains(FORMAT_M3U)) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .setExtractorFactory(HlsExtractorFactory.DEFAULT)
                .createMediaSource(MediaItem.fromUri(uri))
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory, DefaultExtractorsFactory()).createMediaSource(
                MediaItem.fromUri(uri)
            )
        }

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context, enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean, enableOffload: Boolean
            ): AudioSink {
                return DefaultAudioSink(
                    AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES,
                    DefaultAudioSink.DefaultAudioProcessorChain(App.exoRecordInstance.exoRecordProcessor),
                    enableFloatOutput, enableAudioTrackPlaybackParams, enableOffload
                )
            }
        }

        val trackSelector = DefaultTrackSelector(applicationContext, trackSelectionFactory)
        val exoPlayer = SimpleExoPlayer.Builder(applicationContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .build()

        exoPlayer.setMediaSource(mainMediaSource)
        exoPlayer.prepare()
        App.exoRecordInstance.addExoRecordListener("ዋና", exoRecordListener)

        findViewById<Button>(R.id.button).setOnClickListener { exoPlayer.playWhenReady = true }
        findViewById<Button>(R.id.button2).setOnClickListener { exoPlayer.stop() }

        startRecordingButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                App.exoRecordInstance.startRecording()
            }
        }

        stopRecordingButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val wavFilePath = App.exoRecordInstance.stopRecording()
                Log.e("WavFile", wavFilePath.toString())

                val progressBarTextView = findViewById<TextView>(R.id.converting_progress_bar_text)
                val progressBar = findViewById<ProgressBar>(R.id.converting_progress_bar)

                progressBarTextView.text = "Converting to OGG"

                withContext(Dispatchers.IO) {
                    ExoRecordOgg.convertFile(
                        applicationContext = App.instance,
                        fileName = wavFilePath.filePath,
                        sampleRate = wavFilePath.sampleBitRate,
                        channels = wavFilePath.channelCount,
                        quality = 1f,
                    ) { progressBar.progress = it.roundToInt() }
                }

                progressBarTextView.text = "Conversion Done"
            }
        }
    }

    override fun onStop() {
        super.onStop()
        App.exoRecordInstance.removeExoRecordListener("ዋና")
    }
}