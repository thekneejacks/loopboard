package com.alexkang.loopboard

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.alexkang.loopboard.LoopboardApplication.Companion.getApplication
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import kotlin.concurrent.Volatile


internal class Recorder(context: Context, isCapturingAudio: Boolean) {
    private val recordExecutor: ExecutorService?
    private var audioRecord: AudioRecord? = null
    private var audioPlaybackCaptureConfiguration: AudioPlaybackCaptureConfiguration?

    @Volatile
    private var isRecording = false

    @JvmField
    @get:Synchronized
    @set:Synchronized
    @Volatile
    var isCapturingAudio: Boolean = false

    fun interface RecorderCallback {
        fun onAudioRecorded(recordedBytes: ByteArray)
    }

    init {
        this.audioPlaybackCaptureConfiguration =
            getApplication(context).audioPlaybackCaptureConfiguration
        this.recordExecutor = getApplication(context).executorService
        this.isCapturingAudio = isCapturingAudio
    }

    @Synchronized
    fun setAudioPlaybackCaptureConfiguration(context: Context) {
        this.audioPlaybackCaptureConfiguration =
            getApplication(context).audioPlaybackCaptureConfiguration
    }

    @Synchronized
    fun startRecording(recorderCallback: RecorderCallback) {
        if (isRecording) {
            Log.d(TAG, "startRecording called while another recording is in progress")
            return
        }

        refresh()

        isRecording = true
        recordExecutor!!.submit {
            try {
                audioRecord!!.startRecording()
            } catch (e: IllegalStateException) {
                isRecording = false
                Log.e(
                    TAG,
                    "startRecording failed because the AudioRecord was uninitialized"
                )
                return@submit
            }
            val output = ByteArrayOutputStream()
            val buffer =
                ByteArray(Utils.MIN_BUFFER_SIZE)

            // Remove a small first chunk of the recording to avoid the sound of the user tapping
            // the button.
            audioRecord!!.read(
                ByteArray(AUDIO_CUTOFF_LENGTH),
                0,
                AUDIO_CUTOFF_LENGTH
            )


            // Keep recording until stopRecording() is invoked.
            while (isRecording) {
                audioRecord!!.read(buffer, 0, Utils.MIN_BUFFER_SIZE)
                output.write(buffer, 0, Utils.MIN_BUFFER_SIZE)
            }
            try {
                output.flush()
                val recordedBytes = output.toByteArray()
                output.close()

                // Discard this recording if it was too short.
                if (recordedBytes.size < MIN_RECORDING_SIZE) {
                    return@submit
                }

                recorderCallback.onAudioRecorded(recordedBytes)
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Error while ending a recording"
                )
            }
        }
    }

    @Synchronized
    fun stopRecording() {
        if (!isRecording) {
            Log.d(TAG, "stopRecording called even though no recordings are in progress")
            shutdown() //anyway
            return
        }

        // Mark ourselves as not recording so the ongoing recording knows to stop.
        isRecording = false
        // to facilitate multiple recorders, release audioRecord every time we stop recording
        shutdown()
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun refresh() {
        shutdown()
        audioRecord =
            if (!isCapturingAudio || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || audioPlaybackCaptureConfiguration == null) {
                //Microphone mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(Utils.MIN_BUFFER_SIZE)
                        .build()
                } else {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        Utils.SAMPLE_RATE_HZ,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        Utils.MIN_BUFFER_SIZE
                    )
                }
            } else { // Audio capture mode
                AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration!!)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(Utils.MIN_BUFFER_SIZE)
                    .build()
            }

        //recordExecutor = Executors.newSingleThreadExecutor();
    }

    @Synchronized
    fun shutdown() {
        if (audioRecord != null) {
            audioRecord!!.release()
        }
        //if (recordExecutor != null) {
        //    recordExecutor.shutdown();
        //}
    }

    companion object {
        private const val AUDIO_CUTOFF_LENGTH = (Utils.SAMPLE_RATE_HZ / 3.675).toInt()
        private const val MIN_RECORDING_SIZE = 8000
        private const val TAG = "Recorder"
        private val audioFormat: AudioFormat = Utils.AUDIO_FORMAT
    }
}
