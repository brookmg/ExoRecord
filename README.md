<p align="center">
	<img src="https://github.com/brookmg/ExoRecord/blob/master/exorecord.svg?raw=true" alt="ExoPlayer" /><br>
	<h1 align="center"> ExoRecord </h1>
	<p align="center">
		<a href="https://jitpack.io/#brookmg/ExoRecord"><img src="https://jitpack.io/v/brookmg/exorecord.svg" alt="Current Version" /></a>
		<a href="https://circleci.com/gh/brookmg/ExoRecord/tree/master"><img src="https://circleci.com/gh/brookmg/ExoRecord/tree/master.svg?style=svg" alt="CircleCI" /></a>
		<a href="https://app.codacy.com/app/brookmg/ExoRecord?utm_source=github.com&utm_medium=referral&utm_content=brookmg/ExoRecord&utm_campaign=Badge_Grade_Dashboard"><img src="https://api.codacy.com/project/badge/Grade/9a865b7dc8124bed9d1476e6ed331a2a" alt="Codacy Badge" /></a></p><h4 align="center"> An Android library to record audio stream played by Exoplayer </h4></p>

#### Works on top of [ExoPlayer](https://exoplayer.dev)

Because this library uses coroutine, it currently supports only kotlin. Make sure your app is 
implementing the latest coroutine library:

 ```gradle 
     dependencies {
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:[latest-version]'
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:[latest-version]'
     }
 ```

follow the following steps to add the exorecord dependency to your app:
* make sure to add jitpack to your repositories

```gradle 
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```

* implement this library

```gradle 
    dependencies {
        implementation 'com.github.brookmg.exorecord:exorecord:[latest-version]'
    }
```

Samples
-------
* More expressive sample application is located in the `app` module

* Initialising the library
```kotlin
    // Preferably in the Application call
    val exoRecordInstance: ExoRecord by lazy { ExoRecord(instance) }
```

After initiation, we need to connect exoplayer and exorecord to be able to record 
the steaming feed that's going to be played. 

```kotlin
    // ... Define other components necessary for ExoPlayer ( Bandwidth meter, Data source, Track selection ... ) 

    val renderersFactory = object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
            context: Context, enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean, enableOffload: Boolean
        ): AudioSink {
            return DefaultAudioSink(
                AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES,
                // We attach the AudioProcessor from exoRecord here
                DefaultAudioSink.DefaultAudioProcessorChain(exoRecordInstance.exoRecordProcessor),
                enableFloatOutput, enableAudioTrackPlaybackParams, enableOffload
            )
        }
    }

    // Create the ExoPlayer instance to be used for playing stream urls
    val exoPlayer = SimpleExoPlayer.Builder(applicationContext, renderersFactory)
                .setTrackSelector(trackSelector)
                .setBandwidthMeter(bandwidthMeter)
                .build()    

    exoPlayer.setMediaSource(mediaSource)
    exoPlayer.prepare()

    exoPlayer.playWhenReady = true  // Start playing 
```

* Add state change listeners
```kotlin
    private val exoRecordListener = object: ExoRecord.ExoRecordListener {
        override fun onStartRecording(recordFileName: String) {
            // Recording wav started on file `recordFileName`
        }

        override fun onStopRecording(record: IExoRecord.Record) {
            // Recording finished. Details like sampleRate and channel count are 
            // located in the `record` variable
        }
    }

    // Add the listener to the exoRecord instance created
    exoRecordInstance.addExoRecordListener("MainListener", exoRecordListener)

    // Be sure to remove listener on lifecycle change to avoid memory leaks
    exoRecordInstance.removeExoRecordListener("MainListener")   
```

* Simply starting or stopping recording from your activity or fragment like:
```kotlin
    // Starting the recording
    CoroutineScope(Dispatchers.Main).launch {
        App.exoRecordInstance.startRecording()
    }

    // Stopping the recording
    CoroutineScope(Dispatchers.Main).launch {
        val wavFilePath = App.exoRecordInstance.stopRecording()
    }
```

## Audio conversion

Even though storing audio recording in a raw wave format might conserve the true quality and bitrate of the original stream, 
it's not truly efficient for normal consumption. ExoRecord provides optional modules for converting these wave files into other
encoding.

### ExoRecordOGG
This module can convert any wave file into ogg file format. 

* To add this library implement 

```gradle 
    dependencies {
        implementation 'com.github.brookmg.exorecord:exorecord:[latest-version]'
        implementation 'com.github.brookmg.exorecord:exorecordogg:[latest-version]'
    }
```

* Simply convert the wave file 
```kotlin
    CoroutineScope(Dispatchers.IO).launch {
        val converted = ExoRecordOgg.convertFile(
            applicationContext = instance, // The application instance
            fileName = "recording-0.wav", // File name ... This file should be present in the `/data/data/[app-package-name]/files` directory
            sampleRate = 44_100, // 44.1Hz
            channelCount = 2,  // Stereo
            quality = 1f
        ) { progress -> 
            Log.v("conversion", "Ogg file conversion at $progress%")
        }     
    }
```

## Features in this lib:
- [x] Recording audio from exoplayer
- [x] Conversion to OGG

#### make sure you have enabled java8 in your project
 
```gradle
    android {
        ...
        
        compileOptions {
            sourceCompatibility = '1.8'
            targetCompatibility = '1.8'
        }
    }
```

## License

```
Copyright (C) 2021 Brook Mezgebu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
