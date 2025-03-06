package com.alexkang.loopboard

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

class SampleListAdapter internal constructor(
    private val context: Context,
    private val recordedSamples: List<RecordedSample>
) : BaseAdapter() {
    //public int getCount() {return importedSamples.size() + recordedSamples.size();}
    override fun getCount(): Int {
        return recordedSamples.size
    }


    override fun getItem(position: Int): RecordedSample? {
        return if (position < recordedSamples.size) {
            recordedSamples[position]
        } else {
            null
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val sample = getItem(position) ?: return convertView!!

        if (convertView == null) {
            convertView =
                LayoutInflater
                    .from(context)
                    .inflate(R.layout.sound_clip_row, parent, false)
        }

        //Button stopButton = convertView.findViewById(R.id.stop);
        val rerecordButton = convertView!!.findViewById<CheckBox>(R.id.rerecord)
        val octaveButton = convertView.findViewById<CheckBox>(R.id.octave)
        val loopButton = convertView.findViewById<CheckBox>(R.id.loop)
        val randomModButton = convertView.findViewById<CheckBox>(R.id.randomizer)
        val sineModButton = convertView.findViewById<CheckBox>(R.id.sine)
        val sawModButton = convertView.findViewById<CheckBox>(R.id.saw)
        //Button playButton = convertView.findViewById(R.id.play);
        val volumeSlider = convertView.findViewById<SeekBar>(R.id.volume_slider)
        val pitchSlider = convertView.findViewById<SeekBar>(R.id.pitch_slider)
        val lengthSlider = convertView.findViewById<SeekBar>(R.id.length_slider)
        val modulatorSpeedSlider = convertView.findViewById<SeekBar>(R.id.randomizer_speed_slider)
        val modulatorIntensitySlider =
            convertView.findViewById<SeekBar>(R.id.randomizer_intensity_slider)


        //randomizerHandler = new Handler();
        //r = new Random();


        // Choose which buttons to show.
        rerecordButton.visibility = View.VISIBLE


        //__________________________________________________________Buttons__________________________________________________________

        //Button Initialization
        rerecordButton.isChecked = sample.isReRecording
        loopButton.isChecked = sample.isLooping
        octaveButton.isChecked = sample.isHighOctave()
        randomModButton.isChecked = sample.isModulatingRandom
        sineModButton.isChecked = sample.isModulatingSine
        sawModButton.isChecked = sample.isModulatingSaw

        //Rerecord button
        rerecordButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                sample.startReRecording(context)
            } else {
                sample.stopReRecording()
                loopButton.isChecked = false
            }
        }

        //Loop(Play) Button
        loopButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            sample.adjustVolume(volumeSlider.progress)
            sample.adjustPitch(pitchSlider.progress)
            if (isChecked) {
                sample.play(true)
            } else {
                sample.stop()
            }
        }

        //Octave Toggle Button
        octaveButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            sample.setHighOctave(isChecked)
        }


        //Random Pitch Modulation Button
        randomModButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                sample.startRandomMod()
            } else {
                sample.stopRandomMod()
                sample.adjustPitch(pitchSlider.progress)
            }
        }


        //"Sine wave" (actually a triangle wave) Pitch Modulation Button
        sineModButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                sample.startSineMod()
            } else {
                sample.stopSineMod()
                sample.adjustPitch(pitchSlider.progress)
            }
        }

        //Saw-wave Pitch Modulation Button
        sawModButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                sample.startSawMod()
            } else {
                sample.stopSawMod()
                sample.adjustPitch(pitchSlider.progress)
            }
        }

        //__________________________________________________________Sliders__________________________________________________________

        //Slider Initialization
        volumeSlider.max = VOLUME_SLIDER_MAX
        pitchSlider.max = PITCH_SLIDER_MAX
        lengthSlider.max = PLAY_LENGTH_SLIDER_MAX
        modulatorSpeedSlider.max = RANDOMIZER_SPEED_SLIDER_MAX
        modulatorIntensitySlider.max = RANDOMIZER_INTENSITY_SLIDER_MAX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pitchSlider.min = PITCH_SLIDER_MIN
            lengthSlider.min = PLAY_LENGTH_SLIDER_MIN
            modulatorSpeedSlider.min = RANDOMIZER_SPEED_SLIDER_MIN
            modulatorIntensitySlider.min =
                RANDOMIZER_INTENSITY_SLIDER_MIN
        }
        volumeSlider.progress = sample.volume
        pitchSlider.progress = sample.pitch
        lengthSlider.progress = sample.length
        modulatorSpeedSlider.progress = sample.modulatorSpeed
        modulatorIntensitySlider.progress = sample.modulatorIntensity

        //Volume Slider
        volumeSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                sample.adjustVolume(i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        //Pitch Slider
        pitchSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                //workaround for old devices that don't support min slider value
                if (i >= PITCH_SLIDER_MIN) sample.adjustPitch(i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        //Play Length Slider. high "play length" value makes the loop shorter and vice versa.
        lengthSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                //workaround for old devices that don't support min slider value
                if (i >= PLAY_LENGTH_SLIDER_MIN) sample.adjustPlayLength(i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        //Pitch Modulation Speed Slider
        modulatorSpeedSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                //workaround for old devices that don't support min slider value
                if (i >= RANDOMIZER_SPEED_SLIDER_MIN) sample.modulatorSpeed = i
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })


        //Pitch Modulation "Intensity" Slider. limits how high / low the modulator can set the sample's pitch value.
        modulatorIntensitySlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                //workaround for old devices that don't support min slider value
                if (i >= RANDOMIZER_INTENSITY_SLIDER_MIN) sample.modulatorIntensity = i
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        return convertView
    }

    companion object {
        private const val VOLUME_SLIDER_MAX = 100
        private const val PITCH_SLIDER_MAX = Utils.SAMPLE_RATE_HZ_TIMES_TWO
        private const val PITCH_SLIDER_MIN = 1
        private const val PLAY_LENGTH_SLIDER_MAX = 100
        private const val PLAY_LENGTH_SLIDER_MIN = 2
        private const val RANDOMIZER_SPEED_SLIDER_MAX = 200
        private const val RANDOMIZER_SPEED_SLIDER_MIN = 1
        private const val RANDOMIZER_INTENSITY_SLIDER_MAX = 7
        private const val RANDOMIZER_INTENSITY_SLIDER_MIN = 0
    }
}
