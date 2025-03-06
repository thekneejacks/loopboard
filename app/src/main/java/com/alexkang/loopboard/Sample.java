package com.alexkang.loopboard;


import android.content.Context;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.projection.MediaProjection;

abstract class Sample {

    abstract String getName();

    abstract int getVolume();

    abstract int getPitch();

    abstract int getLength();

    abstract int getModulatorSpeed();

    abstract int getModulatorIntensity();

    abstract boolean isHighOctave();

    abstract boolean isModulatingRandom();

    abstract boolean isModulatingSine();

    abstract boolean isModulatingSaw();

    abstract boolean isReRecording();

    abstract void play(boolean isLooped);

    abstract void stop();

    abstract void startRandomMod();

    abstract void startSineMod();

    abstract void startSawMod();

    abstract boolean isLooping();

    abstract void stopRandomMod();

    abstract void stopSineMod();

    abstract void stopSawMod();

    abstract void setIsCapturingAudio(boolean t);

    abstract void setAudioPlaybackCaptureConfiguration(Context context);

    abstract void shutdown();

    abstract void adjustVolume(int i);

    abstract void adjustPitch(int i);

    abstract void adjustPlayLength(int i);

    abstract void setHighOctave(boolean x);

    abstract void setModulatorSpeed(int i);

    abstract void setModulatorIntensity(int i);

    //I think each sample should have its own recorder. what could go wrong
    abstract void startReRecording(Context context);

    abstract void stopReRecording();
}
