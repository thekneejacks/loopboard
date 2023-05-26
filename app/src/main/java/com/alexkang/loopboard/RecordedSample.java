package com.alexkang.loopboard;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

class RecordedSample extends Sample {

    private static final String TAG = "RecordedSample";
    private final String name;
    private boolean isLooping;
    private byte[] bytes;
    private AudioTrack audioTrack;
    private int volume;
    private int pitch;
    private int play_length;

    private boolean highOctave;
    private int randomizerInterval;
    private int randomizerIntensity;
    private final Handler modulationHandler = new Handler();
    Random r = new Random();


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
        this.highOctave = false;
        this.randomizerInterval = 1;
        this.randomizerIntensity = 0;
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
    int getRandomizerInterval() {return randomizerInterval; }

    @Override
    int getRandomizerIntensity() {return randomizerIntensity; }

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
        //if(this.isMuted) return;

        float finalvolume = (float) (targetVolume / 100.0);
        //Log.d("actual volume:",Float.toString(finalvolume));
        audioTrack.setVolume(finalvolume);
    }

    @Override
    synchronized void adjustPitch(int i) {
        this.pitch = i;
        if(this.highOctave) audioTrack.setPlaybackRate(i*2);
        else audioTrack.setPlaybackRate(i);
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
    synchronized void setHighOctave(boolean x){
        this.highOctave = x;
        int i = this.pitch;
        if(x) audioTrack.setPlaybackRate(i*2);
        else audioTrack.setPlaybackRate(i);
    }

    @Override
    synchronized void adjustRandomizerInterval(int i) {
        this.randomizerInterval = i;
    }

    @Override
    synchronized void adjustRandomizerIntensity(int i) {
        this.randomizerIntensity = i;
    }

    @Override
    synchronized void startRandomizer() {
        randomizerTask.run();
    }

    @Override
    synchronized void startSineMod() {
        sineModTask.run();
    }

    @Override
    synchronized void startSawMod() {
        sawModTask.run();
    }


    @Override
    synchronized void removeModulationCallbacks(int i) {
        switch (i){
            case 0:
                modulationHandler.removeCallbacks(randomizerTask);
                break;
            case 1:
                modulationHandler.removeCallbacks(sineModTask);
                break;
            default:
                modulationHandler.removeCallbacks(sawModTask);
                break;
        }
    }

    @Override
    synchronized boolean isLooping() {
        return isLooping;
    }

    @Override
    synchronized void shutdown() {
        modulationHandler.removeCallbacksAndMessages(null);
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



    Runnable randomizerTask = new Runnable() {
        @Override
        public void run() {
            int intervalModifier = randomizerInterval;
            int rangeModifier = randomizerIntensity;
            int min = 1 + rangeModifier * 5512;
            int max = 88200 - rangeModifier * 5512;
            int rand = r.nextInt((max - min) + min) + Math.round(min/2);
            //pitchSlider.setProgress(rand);
            adjustPitch(rand);
            modulationHandler.postDelayed(this, (2000 / intervalModifier));
            //Log.d("repeat: ",Integer.toString(rand) + "/" + Integer.toString(intervalModifier));
        }
    };

    Runnable sineModTask = new Runnable() {
        boolean climbing = true;
        @Override
        public void run() {
            int intervalModifier = randomizerInterval;
            int rangeModifier = randomizerIntensity;
            int min = 5512 + rangeModifier * 5512;
            int max = 88200 - rangeModifier * 5512;
            int i = pitch;
            //Log.d("sinewave",Integer.toString(i));
            int i2;
            if(climbing){
                if(i >= max - 10){
                    climbing = false;
                }
                else{
                    i2 = i + 100 * intervalModifier;
                    //pitchSlider.setProgress(i2);
                    adjustPitch(i2);
                }
            }
            else {
                if(i <= min + 10){
                    climbing = true;
                }
                else {
                    i2 = i - 100 * intervalModifier;
                    //pitchSlider.setProgress(i2);
                    adjustPitch(i2);
                }
            }

            modulationHandler.postDelayed(this, (20));
        }
    };

    Runnable sawModTask = new Runnable() {
        @Override
        public void run() {
            int intervalModifier = randomizerInterval;
            int rangeModifier = randomizerIntensity;
            int min = 5512 + (rangeModifier * 5512);
            int max = 88200 - (rangeModifier * 5512);
            int i = pitch;
            int i2;

            if(i >= max - 10){
                i2 = min;
            }
            else{
                i2 = i + 100 * intervalModifier;
            }
            adjustPitch(i2);

            modulationHandler.postDelayed(this, (20));
        }
    };
}
