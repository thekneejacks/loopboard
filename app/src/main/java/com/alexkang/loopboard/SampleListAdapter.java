package com.alexkang.loopboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;

import java.util.List;
import java.util.Random;

public class SampleListAdapter extends BaseAdapter {

    private final Context context;
    private final Recorder recorder;
    private final List<ImportedSample> importedSamples;
    private final List<RecordedSample> recordedSamples;

    Random r;

    SampleListAdapter(
            Context context,
            Recorder recorder,
            List<ImportedSample> importedSamples,
            List<RecordedSample> recordedSamples) {
        this.context = context;
        this.recorder = recorder;
        this.importedSamples = importedSamples;
        this.recordedSamples = recordedSamples;
    }

    @Override
    public int getCount() {
        return importedSamples.size() + recordedSamples.size();
    }


    @Override
    public Sample getItem(int position) {
        if (position < importedSamples.size()) {
            return importedSamples.get(position);
        } else if (position - importedSamples.size() < recordedSamples.size()) {
            return recordedSamples.get(position - importedSamples.size());
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    @SuppressLint("ClickableViewAccessibility")
    public View getView(final int position, View convertView, ViewGroup parent) {
        Sample sample = getItem(position);
        if (sample == null) {
            return convertView;
        }

        if (convertView == null) {
            convertView =
                    LayoutInflater
                            .from(context)
                            .inflate(R.layout.sound_clip_row, parent, false);
        }

        //Button stopButton = convertView.findViewById(R.id.stop);
        Button rerecordButton = convertView.findViewById(R.id.rerecord);
        CheckBox muteButton = convertView.findViewById(R.id.mute);
        CheckBox loopButton = convertView.findViewById(R.id.loop);
        CheckBox randomizerButton = convertView.findViewById(R.id.randomizer);
        CheckBox sineModButton = convertView.findViewById(R.id.sine);
        CheckBox sawModButton = convertView.findViewById(R.id.saw);
        //Button playButton = convertView.findViewById(R.id.play);
        SeekBar volumeSlider = convertView.findViewById(R.id.volume_slider);
        SeekBar pitchSlider = convertView.findViewById(R.id.pitch_slider);
        SeekBar lengthSlider = convertView.findViewById(R.id.length_slider);
        SeekBar randomizerSpeedSlider = convertView.findViewById(R.id.randomizer_speed_slider);
        SeekBar randomizerIntensitySlider = convertView.findViewById(R.id.randomizer_intensity_slider);
        //randomizerHandler = new Handler();
        r = new Random();


        // Update the state of the loop button.
        loopButton.setChecked(sample.isLooping());

        // Choose which buttons to show.
        if (sample instanceof ImportedSample) {
            // Show the stop button and hide the rerecord button.
            //stopButton.setVisibility(View.VISIBLE);
            rerecordButton.setVisibility(View.GONE);
        } else {
            // Hide the stop button and show the rerecord button.
            //stopButton.setVisibility(View.GONE);
            rerecordButton.setVisibility(View.VISIBLE);
        }


        //_____________________________Buttons_____________________________

        rerecordButton.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.setPressed(true);
                recorder.startRecording(
                        recordedBytes -> ((RecordedSample) sample).save(context, recordedBytes));
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.setPressed(false);
                recorder.stopRecording();
                loopButton.setChecked(false);
            }
            return true;
        });
        loopButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sample.adjustVolume(volumeSlider.getProgress());
            sample.adjustPitch(pitchSlider.getProgress());
            if (isChecked) {
                sample.play(true);
            } else {
                sample.stop();
            }
        });

        muteButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sample.mute(true);
            } else {
                sample.mute(false);
            }
        });





        //sample.removeModulationCallbacks(2);
        randomizerButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //sineModButton.setChecked(false);
            if (isChecked) {
                sample.startRandomizer();
            } else {
                sample.removeModulationCallbacks(0);
                sample.adjustPitch(pitchSlider.getProgress());
            }
        });


        sineModButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //randomizerButton.setChecked(false);
            if (isChecked) {
                //pitchSlider.setProgress(44100);
                sample.startSineMod();
            } else {
                sample.removeModulationCallbacks(1);
                sample.adjustPitch(pitchSlider.getProgress());
            }
        });

        sawModButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //randomizerButton.setChecked(false);
            if (isChecked) {
                //pitchSlider.setProgress(44100);
                sample.startSawMod();
            } else {
                sample.removeModulationCallbacks(2);
                sample.adjustPitch(pitchSlider.getProgress());
            }
        });

        //_____________________________Sliders_____________________________

        volumeSlider.setMax(100);
        volumeSlider.setProgress(sample.getVolume());
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sample.adjustVolume(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        pitchSlider.setMax(88200);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pitchSlider.setMin(1);
        }
        pitchSlider.setProgress(sample.getPitch());
        pitchSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sample.adjustPitch(i);
                /*if(i >= 0) {
                    sample.adjustPitch(50 + (50 * (i/100)));
                }
                else{
                    sample.adjustPitch(50 / (Math.abs(i/100) + 1));
                }*/
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        lengthSlider.setMax(100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lengthSlider.setMin(2);
        }
        lengthSlider.setProgress(sample.getLength());
        lengthSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) { ;
                sample.adjustPlayLength(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        randomizerSpeedSlider.setMax(100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            randomizerSpeedSlider.setMin(1);
        }
        randomizerSpeedSlider.setProgress(sample.getRandomizerInterval());
        randomizerSpeedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) { ;
                sample.adjustRandomizerInterval(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        randomizerIntensitySlider.setMax(5);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            randomizerIntensitySlider.setMin(1);
        }
        randomizerIntensitySlider.setProgress(sample.getRandomizerIntensity());
        randomizerIntensitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) { ;
                sample.adjustRandomizerIntensity(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return convertView;
    }
}
