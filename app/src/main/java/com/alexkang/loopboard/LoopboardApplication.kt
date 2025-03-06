package com.alexkang.loopboard

import android.app.Application
import android.content.Context
import android.media.AudioPlaybackCaptureConfiguration
import java.util.concurrent.ExecutorService

class LoopboardApplication : Application() {
    @JvmField
    var audioPlaybackCaptureConfiguration: AudioPlaybackCaptureConfiguration? = null
    @JvmField
    var executorService: ExecutorService? = null

    fun shutdown() {
        executorService!!.shutdown()
    }

    companion object {
        @JvmStatic
        fun getApplication(context: Context): LoopboardApplication {
            return context.applicationContext as LoopboardApplication
        }
    }
}
