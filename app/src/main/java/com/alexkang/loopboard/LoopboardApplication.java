package com.alexkang.loopboard;

import android.app.Application;
import android.content.Context;
import android.media.AudioPlaybackCaptureConfiguration;

import java.util.concurrent.ExecutorService;

public class LoopboardApplication extends Application {
    private static Context context;
    private AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration;
    private ExecutorService executorService;

    public void onCreate() {
        super.onCreate();
        LoopboardApplication.context = getApplicationContext();
    }

    public static LoopboardApplication getApplication(Context context) {
        return (LoopboardApplication) context.getApplicationContext();
    }
    public AudioPlaybackCaptureConfiguration getAudioPlaybackCaptureConfiguration() {
        return audioPlaybackCaptureConfiguration;
    }
    public ExecutorService getExecutorService() {
        return executorService;
    }
    public void setAudioPlaybackCaptureConfiguration(AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration) {
        this.audioPlaybackCaptureConfiguration = audioPlaybackCaptureConfiguration;
    }
    public void setExecutorService(ExecutorService executorService){
        this.executorService = executorService;
    }
    public void shutdown(){
        executorService.shutdown();
    }
}
