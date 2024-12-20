package com.alexkang.loopboard;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class MediaProjectionService extends Service {

    public MediaProjectionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if(action.equals(Utils.ACTION.STOPFOREGROUND_ACTION) || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            stopForeground(true);
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        else if(action.equals(Utils.ACTION.STARTFOREGROUND_ACTION)) {
            String CHANNEL_ID = "loopboard_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Loopboard channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
            //return super.onStartCommand(intent, flags, startId);
            return START_REDELIVER_INTENT;
        }

        return START_STICKY;
    }
}