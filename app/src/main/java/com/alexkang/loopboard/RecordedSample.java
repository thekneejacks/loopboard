package com.alexkang.loopboard;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

class RecordedSample extends Sample {

    private static final String TAG = "RecordedSample";
    private final String name;
    private byte[] bytes;
    private AudioTrack audioTrack;
    private final Recorder reRecorder;
    private int volume;
    private int pitch;
    private int play_length;
    private int modulatorSpeed;
    private int modulatorIntensity;
    private boolean isLooping;
    private boolean isHighOctave;
    private boolean isModulatingRandom;
    private boolean isModulatingSine;
    private boolean isModulatingSaw;
    private boolean isReRecording;
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
        this.modulatorSpeed = 1;
        this.modulatorIntensity = 0;
        this.reRecorder = new Recorder();
        this.isReRecording = false;
    }

    @Override String getName() {
        return name;
    }

    @Override int getVolume() {return volume;}

    @Override int getPitch() { return pitch; }

    @Override int getLength() {return play_length; }

    @Override int getModulatorSpeed() {return modulatorSpeed; }

    @Override int getModulatorIntensity() {return modulatorIntensity; }

    @Override boolean isLooping() {
        return isLooping;
    }

    @Override boolean isHighOctave() {
        return isHighOctave;
    }

    @Override boolean isModulatingRandom() {
        return isModulatingRandom;
    }

    @Override boolean isModulatingSine() {
        return isModulatingSine;
    }

    @Override boolean isModulatingSaw() {
        return isModulatingSaw;
    }
    @Override boolean isReRecording() { return isReRecording;}

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
        float finalvolume = (float) (targetVolume / 100.0);
        audioTrack.setVolume(finalvolume);
    }

    @Override
    synchronized void adjustPitch(int i) {
        this.pitch = i;
        if(this.isHighOctave) audioTrack.setPlaybackRate(i*2);
        else audioTrack.setPlaybackRate(i);
    }

    @Override
    synchronized void adjustPlayLength(int i) {
        this.play_length = i;
        if(isLooping) play(true);
    }

    @Override
    synchronized void setHighOctave(boolean x){
        this.isHighOctave = x;
        int i = this.pitch;
        if(x) audioTrack.setPlaybackRate(i*2);
        else audioTrack.setPlaybackRate(i);
    }

    @Override
    synchronized void setModulatorSpeed(int i) { this.modulatorSpeed = i; }

    @Override
    synchronized void setModulatorIntensity(int i) { this.modulatorIntensity = i; }

    @Override
    synchronized void startRandomMod() {
        if(this.isModulatingRandom) {
            //no two random modulators should run simultaneously
            return;
        }
        this.isModulatingRandom = true;
        Thread randomModThread = new Thread(new Runnable() {
            int intervalModifier;
            int rangeModifier;
            int min;
            int max;
            int rand;

            @Override
            public void run() {
                while (isModulatingRandom) {
                    intervalModifier = modulatorSpeed;
                    rangeModifier = modulatorIntensity;
                    min = rangeModifier * 5512;
                    max = 88200 - rangeModifier * 5512;
                    rand = r.nextInt((max - min) + min) + Math.round(min / 2);
                    adjustPitch(rand);
                    try {
                        Thread.sleep(2000 / modulatorSpeed);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        randomModThread.start();

    }

    @Override
    synchronized void startSineMod() {
        if(this.isModulatingSine) {
            //no two sine modulators should run simultaneously
            return;
        }
        this.isModulatingSine = true;
        Thread sineModThread = new Thread(new Runnable() {
            boolean climbing = true;
            int intervalModifier;
            int rangeModifier;
            int min;
            int max;
            int i;

            @Override
            public void run() {
                while (isModulatingSine) {
                    intervalModifier = modulatorSpeed;
                    rangeModifier = modulatorIntensity;
                    min = rangeModifier * 5512;
                    max = 88200 - rangeModifier * 5512;
                    i = pitch;

                    if (climbing) {
                        if (i >= max - 10) {
                            climbing = false;
                        } else {
                            adjustPitch(i + (10 * intervalModifier));
                            //adjustPitch(i + 100);
                        }
                    } else {
                        if (i <= min + 10) {
                            climbing = true;
                        } else {
                            adjustPitch(i - (10 * intervalModifier));
                            //adjustPitch(i - 100);
                        }
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        sineModThread.start();
    }

    @Override
    synchronized void startSawMod() {
        if(this.isModulatingSaw) {
            //no two saw modulators should run simultaneously
            return;
        }
        this.isModulatingSaw = true;
        Thread sawModThread = new Thread(new Runnable() {
            int intervalModifier;
            int rangeModifier;
            int min;
            int max;
            int i;

            @Override
            public void run() {
                while (isModulatingSaw) {
                    intervalModifier = modulatorSpeed;
                    rangeModifier = modulatorIntensity;
                    min = rangeModifier * 5512;
                    max = 88200 - (rangeModifier * 5512);
                    i = pitch;
                    if (i >= max - 10) {
                        adjustPitch(min);
                    } else {
                        adjustPitch(i + 10 * intervalModifier);
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        sawModThread.start();
    }

    @Override
    synchronized void stopRandomMod() {
        this.isModulatingRandom = false;
    }

    @Override
    synchronized void stopSineMod() {
        this.isModulatingSine = false;
    }

    @Override
    synchronized void stopSawMod() {
        this.isModulatingSaw = false;
    }

    @Override
    synchronized void shutdown() {
        //Stop all modulations
        this.stopRandomMod();
        this.stopSineMod();
        this.stopSawMod();
        this.stopReRecording();
        audioTrack.release();
    }

    /** Update a recorded sample and save it to disk. */
    synchronized void save(Context context, byte[] bytes) {
        this.stop();
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

    //I think each sample should have its own recorder. what could go wrong
    @Override synchronized void startReRecording(Context context){
        if(this.isReRecording) return;
        this.isReRecording = true;
        reRecorder.startRecording(recordedBytes -> this.save(context, recordedBytes));
    }
    @Override synchronized void stopReRecording(){
        reRecorder.stopRecording();
        this.isReRecording = false;
    }
}
