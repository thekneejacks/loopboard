package com.alexkang.loopboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat


class MediaProjectionService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action

        if (action == Utils.ACTION.STOPFOREGROUND_ACTION || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            stopForeground(true)
            stopSelfResult(startId)
            return START_NOT_STICKY
        } else if (action == Utils.ACTION.STARTFOREGROUND_ACTION) {
            val CHANNEL_ID = "loopboard_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Loopboard channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()

            startForeground(1, notification)
            //return super.onStartCommand(intent, flags, startId);
            return START_REDELIVER_INTENT
        }

        return START_STICKY
    }
}