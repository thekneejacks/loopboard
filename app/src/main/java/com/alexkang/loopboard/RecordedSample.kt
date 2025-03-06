package com.alexkang.loopboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.alexkang.loopboard.LoopboardApplication.Companion.getApplication
import com.alexkang.loopboard.Recorder.RecorderCallback
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Random
import java.util.concurrent.ExecutorService

class RecordedSample private constructor(
    val name: String,
    context: Context,
    isCapturingAudio: Boolean
) {
    private lateinit var bytes: ByteArray
    private var audioTrack: AudioTrack? = null
    private val reRecorder: Recorder
    var volume: Int = 100
        private set
    var pitch: Int
        private set
    var length: Int = 2
        private set
    private val modulatorExecutor: ExecutorService?

    @JvmField
    @set:Synchronized
    var modulatorSpeed: Int = 1

    @JvmField
    @set:Synchronized
    var modulatorIntensity: Int = 0
    var isLooping: Boolean = false
        private set
    private var isHighOctave = false
    var isModulatingRandom: Boolean = false
        private set
    var isModulatingSine: Boolean = false
        private set
    var isModulatingSaw: Boolean = false
        private set
    var isReRecording: Boolean = false
        private set
    var r: Random = Random()


    init {
        this.pitch = Utils.SAMPLE_RATE_HZ
        this.modulatorExecutor = getApplication(context).executorService
        this.reRecorder = Recorder(context, isCapturingAudio)
    }

    fun isHighOctave(): Boolean {
        return isHighOctave
    }


    @Synchronized
    fun play(isLooped: Boolean) {
        // Stop any ongoing playback.
        audioTrack!!.stop()
        audioTrack!!.reloadStaticData()

        // Set looping, if needed.
        if (isLooped) {
            // The actual amount of frames in a PCM file is half of the raw byte size.
            Log.d("debug length", length.toString())
            audioTrack!!.setLoopPoints(0, bytes.size / this.length, -1)
        } else {
            audioTrack!!.setLoopPoints(0, 0, 0)
        }

        // Play the sample and update the loop status.
        audioTrack!!.play()
        isLooping = isLooped
    }

    @Synchronized
    fun stop() {
        if (audioTrack!!.state == AudioTrack.STATE_INITIALIZED) {
            audioTrack!!.pause()
        }
        isLooping = false
    }

    @Synchronized
    fun adjustVolume(targetVolume: Int) {
        this.volume = targetVolume
        val finalvolume = (targetVolume / 100.0).toFloat()
        audioTrack!!.setVolume(finalvolume)
    }

    @Synchronized
    fun adjustPitch(i: Int) {
        this.pitch = i
        if (this.isHighOctave) audioTrack!!.setPlaybackRate(i * 2)
        else audioTrack!!.setPlaybackRate(i)
    }

    @Synchronized
    fun adjustPlayLength(i: Int) {
        this.length = i
        if (isLooping) play(true)
    }

    @Synchronized
    fun setHighOctave(x: Boolean) {
        this.isHighOctave = x
        val i = this.pitch
        if (x) audioTrack!!.setPlaybackRate(i * 2)
        else audioTrack!!.setPlaybackRate(i)
    }

    @Synchronized
    fun startRandomMod() {
        if (this.isModulatingRandom) {
            //no two random modulators should run simultaneously
            return
        }
        this.isModulatingRandom = true
        modulatorExecutor!!.submit(object : Runnable {
            var intervalModifier: Int = 0
            var rangeModifier: Int = 0
            var min: Int = 0
            var max: Int = 0
            var rand: Int = 0

            override fun run() {
                while (isModulatingRandom) {
                    intervalModifier = modulatorSpeed
                    rangeModifier = modulatorIntensity
                    min = rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    max =
                        Utils.SAMPLE_RATE_HZ_TIMES_TWO - rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    rand = r.nextInt((max - min) + min) + Math.round((min / 2).toFloat())
                    adjustPitch(rand)
                    try {
                        Thread.sleep((2000 / modulatorSpeed).toLong())
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        })
    }

    @Synchronized
    fun startSineMod() {
        if (this.isModulatingSine) {
            //no two sine modulators should run simultaneously
            return
        }
        this.isModulatingSine = true
        modulatorExecutor!!.submit(object : Runnable {
            var climbing: Boolean = true
            var intervalModifier: Int = 0
            var rangeModifier: Int = 0
            var min: Int = 0
            var max: Int = 0
            var i: Int = 0

            override fun run() {
                while (isModulatingSine) {
                    intervalModifier = modulatorSpeed
                    rangeModifier = modulatorIntensity
                    min = rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    max =
                        Utils.SAMPLE_RATE_HZ_TIMES_TWO - rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    i = pitch

                    if (climbing) {
                        if (i >= max - 10) {
                            climbing = false
                        } else {
                            adjustPitch(i + (10 * intervalModifier))
                            //adjustPitch(i + 100);
                        }
                    } else {
                        if (i <= min + 10) {
                            climbing = true
                        } else {
                            adjustPitch(i - (10 * intervalModifier))
                            //adjustPitch(i - 100);
                        }
                    }
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        })
    }

    @Synchronized
    fun startSawMod() {
        if (this.isModulatingSaw) {
            //no two saw modulators should run simultaneously
            return
        }
        this.isModulatingSaw = true
        modulatorExecutor!!.submit(object : Runnable {
            var intervalModifier: Int = 0
            var rangeModifier: Int = 0
            var min: Int = 0
            var max: Int = 0
            var i: Int = 0

            override fun run() {
                while (isModulatingSaw) {
                    intervalModifier = modulatorSpeed
                    rangeModifier = modulatorIntensity
                    min = rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    max =
                        Utils.SAMPLE_RATE_HZ_TIMES_TWO - (rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT)
                    i = pitch
                    if (i >= max - 10) {
                        adjustPitch(min)
                    } else {
                        adjustPitch(i + 10 * intervalModifier)
                    }
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        })
    }

    @Synchronized
    fun stopRandomMod() {
        this.isModulatingRandom = false
    }

    @Synchronized
    fun stopSineMod() {
        this.isModulatingSine = false
    }

    @Synchronized
    fun stopSawMod() {
        this.isModulatingSaw = false
    }

    @Synchronized
    fun setIsCapturingAudio(t: Boolean) {
        reRecorder.isCapturingAudio = t
    }

    @Synchronized
    fun setAudioPlaybackCaptureConfiguration(context: Context) {
        reRecorder.setAudioPlaybackCaptureConfiguration(context)
    }

    @Synchronized
    fun shutdown() {
        //Stop all modulations
        this.stopRandomMod()
        this.stopSineMod()
        this.stopSawMod()
        this.stopReRecording()
        audioTrack!!.release()
        reRecorder.shutdown()
    }

    /** Update a recorded sample and save it to disk.  */
    @Synchronized
    fun save(context: Context, bytes: ByteArray) {
        this.stop()
        loadNewSample(bytes)
        Utils.saveRecording(context, name, bytes)
    }


    /** Updates the recorded sample. Overwrites any previous recording in this sample.  */
    private fun loadNewSample(bytes: ByteArray) {
        this.bytes = bytes
        this.audioTrack =
            AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Utils.SAMPLE_RATE_HZ)
                    .build(),
                bytes.size,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

        // Write the audio bytes to the audioTrack to play back.
        audioTrack!!.write(bytes, 0, bytes.size)
    }

    //I think each sample should have its own recorder. what could go wrong
    @Synchronized
    fun startReRecording(context: Context) {
        if (this.isReRecording) return
        this.isReRecording = true

        reRecorder.startRecording { recordedBytes: ByteArray ->
            this.save(
                context,
                recordedBytes
            )
        }
    }

    @Synchronized
    fun stopReRecording() {
        reRecorder.stopRecording()
        this.isReRecording = false
    }

    companion object {
        private const val TAG = "RecordedSample"

        /**
         * Open a PCM [File] and initialize it to play back. This is the correct way to obtain a
         * [RecordedSample] object.
         *
         * @return A sample object ready to be played, or null if an error occurred.
         */
        fun openSavedSample(
            context: Context,
            fileName: String,
            isCapturingAudio: Boolean
        ): RecordedSample? {
            try {
                // Read the file into bytes.
                val input = context.openFileInput(fileName)
                val output = ByteArray(input.available())
                val bytesRead = input.read(output)
                input.close()

                // Make sure we actually read all the bytes.
                if (bytesRead == output.size) {
                    val recordedSample = RecordedSample(fileName, context, isCapturingAudio)
                    recordedSample.loadNewSample(output)
                    return recordedSample
                }
            } catch (e: FileNotFoundException) {
                Log.e(
                    TAG, String.format(
                        "refreshRecordings: Unable to open sample %s", fileName
                    )
                )
            } catch (e: IOException) {
                Log.e(
                    TAG, String.format(
                        "refreshRecordings: Error while reading sample %s", fileName
                    )
                )
            }
            return null
        }
    }
}
