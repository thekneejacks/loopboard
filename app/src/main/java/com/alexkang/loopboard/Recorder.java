package com.alexkang.loopboard;



import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Recorder {

    private static final int AUDIO_CUTOFF_LENGTH = (int) (Utils.SAMPLE_RATE_HZ / 3.675);
    private static final int MIN_RECORDING_SIZE = 8000;
    private static final String TAG = "Recorder";
    //private final MediaProjection mediaProjection;

    private ExecutorService recordExecutor;
    private AudioRecord audioRecord;
    private final MediaProjection mediaProjection;
    private AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration;
    private final AudioFormat audioFormat;
    private volatile boolean isRecording = false;

    interface RecorderCallback {
        void onAudioRecorded(byte[] recordedBytes);
    }

    Recorder(MediaProjection mProjection) {
        this.mediaProjection = mProjection;
        if(mediaProjection != null) createAudioPlaybackCaptureConfiguration();
        audioFormat = new AudioFormat.Builder()
                .setSampleRate(Utils.SAMPLE_RATE_HZ)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();
        //refresh();
    }

    synchronized void startRecording(RecorderCallback recorderCallback) {
        if (isRecording) {
            Log.d(TAG, "startRecording called while another recording is in progress");
            return;
        }

        refresh();


        isRecording = true;
        recordExecutor.execute(() -> {
            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                isRecording = false;
                Log.e(TAG, "startRecording failed because the AudioRecord was uninitialized");
                return;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[Utils.MIN_BUFFER_SIZE];

            // Remove a small first chunk of the recording to avoid the sound of the user tapping
            // the button.

            if(mediaProjection == null) { // but only when we're using the microphone
                audioRecord.read(
                        new byte[AUDIO_CUTOFF_LENGTH], 0, AUDIO_CUTOFF_LENGTH);
            }

            // Keep recording until stopRecording() is invoked.
            while (isRecording) {
                audioRecord.read(buffer, 0, Utils.MIN_BUFFER_SIZE);
                output.write(buffer, 0, Utils.MIN_BUFFER_SIZE);
            }

            try {
                output.flush();
                byte[] recordedBytes = output.toByteArray();
                output.close();

                // Discard this recording if it was too short.
                if (recordedBytes.length < MIN_RECORDING_SIZE) {
                    return;
                }

                recorderCallback.onAudioRecorded(recordedBytes);
            } catch (IOException e) {
                Log.e(TAG, "Error while ending a recording");
            }
        });


    }

    synchronized void stopRecording() {
        if (!isRecording) {
            Log.d(TAG, "stopRecording called even though no recordings are in progress");
            shutdown(); //anyway
            return;
        }

        // Mark ourselves as not recording so the ongoing recording knows to stop.
        isRecording = false;
        // to facilitate multiple recorders, release recordExecutor and audioRecord every time we stop recording
        shutdown();
    }

    synchronized void refresh() {
        shutdown();
        if(mediaProjection == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //Microphone mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioRecord = new AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(Utils.MIN_BUFFER_SIZE)
                        .build();
            } else {
                audioRecord =
                        new AudioRecord(
                                MediaRecorder.AudioSource.MIC,
                                Utils.SAMPLE_RATE_HZ,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                Utils.MIN_BUFFER_SIZE);
            }
        } else { // Audio capture mode
            audioRecord = new AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(Utils.MIN_BUFFER_SIZE)
                    .build();
        }

        recordExecutor = Executors.newSingleThreadExecutor();

    }

    synchronized void shutdown() {
        if (audioRecord != null) {
            audioRecord.release();
        }
        if (recordExecutor != null) {
            recordExecutor.shutdown();
        }
    }

    private void createAudioPlaybackCaptureConfiguration() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        try {
            AudioPlaybackCaptureConfiguration.Builder audioPlaybackCaptureConfigurationBuilder =
                    new AudioPlaybackCaptureConfiguration.Builder(mediaProjection).addMatchingUsage(AudioAttributes.USAGE_MEDIA);
            audioPlaybackCaptureConfiguration =
                    audioPlaybackCaptureConfigurationBuilder.build();
        } catch (Exception e) {
            Log.e(TAG, "createAudioPlaybackCaptureConfiguration: Error:", e);
            e.printStackTrace();
        }
    }

}
