package com.alexkang.loopboard;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;

class ImportedSample extends Sample {

    private final Context context;
    private final File sampleFile;

    private MediaPlayer mediaPlayer;

    ImportedSample(Context context, File sampleFile) {
        this.context = context;
        this.sampleFile = sampleFile;
    }

    @Override
    String getName() {
        return sampleFile.getName();
    }

    @Override
    int getVolume() {
        return 0;
    }

    @Override
    int getPitch() {
        return 0;
    }

    @Override
    int getLength() {
        return 0;
    }

    @Override
    int getRandomizerInterval() {
        return 0;
    }

    @Override
    int getRandomizerIntensity() {
        return 0;
    }

    @Override
    synchronized void play(boolean isLooped) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, Uri.parse(sampleFile.getAbsolutePath()));
        }
        mediaPlayer.setLooping(isLooped);

        try {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        } catch (IllegalStateException e) {
            // Ignore. We tried out best.
        }
    }

    @Override
    synchronized void stop() {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.stop();
        } catch (IllegalStateException e) {
            // Ignore. We tried out best.
        }
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    void startRandomizer() {

    }

    @Override
    void startSineMod() {

    }

    @Override
    void startSawMod() {

    }

    @Override
    void removeModulationCallbacks(int i) {

    }

    @Override
    synchronized void adjustVolume(int targetVolume) {
        float log1 = (float)(Math.log(50-targetVolume)/Math.log(50));
        mediaPlayer.setVolume(log1,log1);
    }

    @Override
    void adjustPitch(int i) {

    }

    @Override
    void adjustPlayLength(int i) {

    }

    @Override
    void adjustRandomizerInterval(int i) {

    }

    @Override
    void adjustRandomizerIntensity(int i) {

    }

    @Override
    void mute(boolean x) {

    }

    @Override
    synchronized boolean isLooping() {
        return mediaPlayer != null && mediaPlayer.isLooping();
    }

    @Override
    synchronized void shutdown() {
        stop();
    }
}
