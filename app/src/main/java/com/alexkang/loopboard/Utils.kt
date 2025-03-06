package com.alexkang.loopboard

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import java.io.IOException

internal object Utils {
    const val MAX_SAMPLES: Int = 24
    const val SAMPLE_RATE_HZ: Int = 48000
    const val SAMPLE_RATE_HZ_TIMES_TWO: Int = SAMPLE_RATE_HZ * 2
    const val SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT: Int = SAMPLE_RATE_HZ / 8

    val MIN_BUFFER_SIZE: Int = AudioRecord.getMinBufferSize(
        SAMPLE_RATE_HZ,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    val AUDIO_FORMAT: AudioFormat = AudioFormat.Builder()
        .setSampleRate(SAMPLE_RATE_HZ)
        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .build()

    private const val TAG = "Utils"
    /**
     * Saves an audio recording under the given name.
     *
     * @return whether or not the file was successfully saved
     */
    fun saveRecording(context: Context, name: String, recordedBytes: ByteArray?): Boolean {
        try {
            val output = context.openFileOutput(name, Context.MODE_PRIVATE)
            output.write(recordedBytes)
            output.close()
            return true
        } catch (e: IOException) {
            Log.e(TAG, String.format("Failed to save recording %s", name))
        }
        return false
    }

    interface ACTION {
        companion object {
            const val STARTFOREGROUND_ACTION: String =
                "com.alexkang.loopboard.MediaProjectionService.action.startforeground"
            const val STOPFOREGROUND_ACTION: String =
                "com.alexkang.loopboard.MediaProjectionService.action.stopforeground"
        }
    }
}
