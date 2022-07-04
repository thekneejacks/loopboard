package com.alexkang.loopboard;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.PlaybackParams;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class RecordedSample extends Sample {

    private static final String TAG = "RecordedSample";
    private final String name;
    private boolean isLooping;
    private byte[] bytes;
    private AudioTrack audioTrack;
    private int volume;
    private int pitch;
    private int play_length;
    private boolean isMuted;


    /**
     * Open a PCM {@link File} and initialize it to play back. This is the correct way to obtain a
     * {@link RecordedSample} object.
     *
     * @return A sample object ready to be played, or null if an error occurred.
     */
    static RecordedSample openSavedSample(Context context, String fileName) {
        try {
            // Read the file into bytes.
            FileInputStream input = context.openFileInput(fileName);
            byte[] output = new byte[input.available()];
            int bytesRead = input.read(output);
            input.close();

            // Make sure we actually read all the bytes.
            if (bytesRead == output.length) {
                RecordedSample recordedSample = new RecordedSample(fileName);
                recordedSample.loadNewSample(output);
                return recordedSample;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, String.format(
                    "refreshRecordings: Unable to open sample %s", fileName));
        } catch (IOException e) {
            Log.e(TAG, String.format(
                    "refreshRecordings: Error while reading sample %s", fileName));
        }
        return null;
    }

    private RecordedSample(String name) {
        this.name = name;
        this.volume = 100;
        this.pitch = 44100;
        this.play_length = 2;
        this.isMuted = false;
    }

    @Override
    String getName() {
        return name;
    }

    @Override
    int getVolume() {return volume;}

    @Override
    int getPitch() { return pitch; }

    @Override
    int getLength() {return play_length; }

    @Override
    synchronized void play(boolean isLooped) {
        // Stop any ongoing playback.
        audioTrack.stop();
        audioTrack.reloadStaticData();

        // Set looping, if needed.
        if (isLooped) {
            // The actual amount of frames in a PCM file is half of the raw byte size.
            Log.d("debug length",Integer.toString(this.play_length));
            audioTrack.setLoopPoints(0, bytes.length / this.play_length, -1);
        } else {
            audioTrack.setLoopPoints(0, 0, 0);
        }

        // Play the sample and update the loop status.
        audioTrack.play();
        isLooping = isLooped;
    }

    @Override
    synchronized void stop() {
        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.pause();
        }
        isLooping = false;
    }

    @Override
    synchronized void adjustVolume(int targetVolume) {
        this.volume = targetVolume;
        if(this.isMuted) return;

        float finalvolume = (float) (targetVolume / 100.0);
        //Log.d("actual volume:",Float.toString(finalvolume));
        audioTrack.setVolume(finalvolume);
    }

    @Override
    synchronized void adjustPitch(int i) {
        this.pitch = i;
        audioTrack.setPlaybackRate(i);
        /*this.pitch = i;
        PlaybackParams params = new PlaybackParams();
        params.setPitch((float) (i / 50.0));
        params.setSpeed((float) (i / 50.0));
        audioTrack.setPlaybackParams(params);*/
    }

    @Override
    synchronized void adjustPlayLength(int i) {
        this.play_length = i;
        if(isLooping) play(true);
    }


    @Override
    synchronized void mute(boolean x){
        this.isMuted = x;
        if(x) audioTrack.setVolume(0);
        else {
            float finalvolume = (float) (this.volume / 100.0);
            audioTrack.setVolume(finalvolume);
        }
    }

    @Override
    synchronized boolean isLooping() {
        return isLooping;
    }

    @Override
    synchronized void shutdown() {
        audioTrack.release();
    }

    /** Update a recorded sample and save it to disk. */
    synchronized void save(Context context, byte[] bytes) {
        loadNewSample(bytes);
        Utils.saveRecording(context, name, bytes);
    }



    /** Updates the recorded sample. Overwrites any previous recording in this sample. */
    private void loadNewSample(byte[] bytes) {
        this.bytes = bytes;
        this.audioTrack =
                new AudioTrack(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build(),
                        new AudioFormat.Builder()
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(Utils.SAMPLE_RATE_HZ)
                                .build(),
                        bytes.length,
                        AudioTrack.MODE_STATIC,
                        AudioManager.AUDIO_SESSION_ID_GENERATE);

        // Write the audio bytes to the audioTrack to play back.
        audioTrack.write(bytes, 0, bytes.length);
    }
}
