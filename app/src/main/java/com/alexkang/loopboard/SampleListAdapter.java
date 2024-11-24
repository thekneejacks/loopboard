package com.alexkang.loopboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;

import java.util.List;
import java.util.Random;

public class SampleListAdapter extends BaseAdapter {

    private final Context context;
    private final Recorder recorder;
    //private final List<ImportedSample> importedSamples;
    private final List<RecordedSample> recordedSamples;

    private static final int VOLUME_SLIDER_MAX = 100;
    private static final int PITCH_SLIDER_MAX = 88200;
    private static final int PITCH_SLIDER_MIN = 1;
    private static final int PLAY_LENGTH_SLIDER_MAX = 100;
    private static final int PLAY_LENGTH_SLIDER_MIN = 2;
    private static final int RANDOMIZER_SPEED_SLIDER_MAX = 200;
    private static final int RANDOMIZER_SPEED_SLIDER_MIN = 1;
    private static final int RANDOMIZER_INTENSITY_SLIDER_MAX = 5;
    private static final int RANDOMIZER_INTENSITY_SLIDER_MIN = 0;

    Random r;

    SampleListAdapter(
            Context context,
            Recorder recorder,
            //List<ImportedSample> importedSamples,
            List<RecordedSample> recordedSamples) {
        this.context = context;
        this.recorder = recorder;
        //this.importedSamples = importedSamples;
        this.recordedSamples = recordedSamples;
    }

    @Override
    //public int getCount() {return importedSamples.size() + recordedSamples.size();}
    public int getCount() { return recordedSamples.size(); }


    @Override
    public Sample getItem(int position) {
        /*if (position < importedSamples.size()) {
            return importedSamples.get(position);
        } else if (position - importedSamples.size() < recordedSamples.size()) {
            return recordedSamples.get(position - importedSamples.size());
        } else {
            return null;
        }*/
        if (position < recordedSamples.size()) {
            return recordedSamples.get(position);
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
        CheckBox rerecordButton = convertView.findViewById(R.id.rerecord);
        CheckBox octaveButton = convertView.findViewById(R.id.octave);
        CheckBox loopButton = convertView.findViewById(R.id.loop);
        CheckBox randomModButton = convertView.findViewById(R.id.randomizer);
        CheckBox sineModButton = convertView.findViewById(R.id.sine);
        CheckBox sawModButton = convertView.findViewById(R.id.saw);
        //Button playButton = convertView.findViewById(R.id.play);
        SeekBar volumeSlider = convertView.findViewById(R.id.volume_slider);
        SeekBar pitchSlider = convertView.findViewById(R.id.pitch_slider);
        SeekBar lengthSlider = convertView.findViewById(R.id.length_slider);
        SeekBar modulatorSpeedSlider = convertView.findViewById(R.id.randomizer_speed_slider);
        SeekBar modulatorIntensitySlider = convertView.findViewById(R.id.randomizer_intensity_slider);
        //randomizerHandler = new Handler();
        r = new Random();


        // Choose which buttons to show.
        rerecordButton.setVisibility(View.VISIBLE);



        //__________________________________________________________Buttons__________________________________________________________

        //Button Initialization
        rerecordButton.setChecked(false); //this should only be checked when we are recording stuff.
        loopButton.setChecked(sample.isLooping());
        octaveButton.setChecked(sample.isHighOctave());
        randomModButton.setChecked(sample.isModulatingRandom());
        sineModButton.setChecked(sample.isModulatingSine());
        sawModButton.setChecked(sample.isModulatingSaw());

        //Rerecord button
        rerecordButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                recorder.startRecording(
                        recordedBytes -> {
                            assert sample instanceof RecordedSample;
                            ((RecordedSample) sample).save(context, recordedBytes);
                        });
            } else {
                recorder.stopRecording();
                loopButton.setChecked(false);
            }
        });

        //Loop(Play) Button
        loopButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sample.adjustVolume(volumeSlider.getProgress());
            sample.adjustPitch(pitchSlider.getProgress());
            if (isChecked) {
                sample.play(true);
            } else {
                sample.stop();
            }
        });

        //Octave Toggle Button
        octaveButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sample.setHighOctave(isChecked);
        });


        //Random Pitch Modulation Button
        randomModButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sample.startRandomMod();
            } else {
                sample.stopRandomMod();
                sample.adjustPitch(pitchSlider.getProgress());
            }
        });


        //"Sine wave" (actually a triangle wave) Pitch Modulation Button
        sineModButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //randomModButton.setChecked(false);
            if (isChecked) {
                //pitchSlider.setProgress(44100);
                sample.startSineMod();
            } else {
                sample.stopSineMod();
                sample.adjustPitch(pitchSlider.getProgress());
            }
        });

        //Saw-wave Pitch Modulation Button
        sawModButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //randomModButton.setChecked(false);
            if (isChecked) {
                //pitchSlider.setProgress(44100);
                sample.startSawMod();
            } else {
                sample.stopSawMod();
                sample.adjustPitch(pitchSlider.getProgress());
            }
        });

        //__________________________________________________________Sliders__________________________________________________________

        //Slider Initialization
        volumeSlider.setMax(VOLUME_SLIDER_MAX);
        pitchSlider.setMax(PITCH_SLIDER_MAX);
        lengthSlider.setMax(PLAY_LENGTH_SLIDER_MAX);
        modulatorSpeedSlider.setMax(RANDOMIZER_SPEED_SLIDER_MAX);
        modulatorIntensitySlider.setMax(RANDOMIZER_INTENSITY_SLIDER_MAX);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pitchSlider.setMin(PITCH_SLIDER_MIN);
            lengthSlider.setMin(PLAY_LENGTH_SLIDER_MIN);
            modulatorSpeedSlider.setMin(RANDOMIZER_SPEED_SLIDER_MIN);
            modulatorIntensitySlider.setMin(RANDOMIZER_INTENSITY_SLIDER_MIN);
        }
        volumeSlider.setProgress(sample.getVolume());
        pitchSlider.setProgress(sample.getPitch());
        lengthSlider.setProgress(sample.getLength());
        modulatorSpeedSlider.setProgress(sample.getModulatorSpeed());
        modulatorIntensitySlider.setProgress(sample.getModulatorIntensity());

        //Volume Slider
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sample.adjustVolume(i);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Pitch Slider
        pitchSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //workaround for old devices
                if(i >= PITCH_SLIDER_MIN) sample.adjustPitch(i);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Play Length Slider. high "play length" value makes the loop shorter and vice versa.
        lengthSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(i >= PLAY_LENGTH_SLIDER_MIN) sample.adjustPlayLength(i);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Pitch Modulation Speed Slider
        modulatorSpeedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(i >= RANDOMIZER_SPEED_SLIDER_MIN) sample.setModulatorSpeed(i);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        //Pitch Modulation "Intensity" Slider. limits how high / low the modulator can set the sample's pitch value.
        modulatorIntensitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(i >= RANDOMIZER_INTENSITY_SLIDER_MIN) sample.setModulatorIntensity(i);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return convertView;
    }
}
