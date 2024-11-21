package com.alexkang.loopboard;


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

    abstract void play(boolean isLooped);

    abstract void stop();

    abstract void startRandomMod();

    abstract void startSineMod();

    abstract void startSawMod();

    abstract boolean isLooping();

    abstract void stopRandomMod();

    abstract void stopSineMod();

    abstract void stopSawMod();

    abstract void shutdown();

    abstract void adjustVolume(int i);

    abstract void adjustPitch(int i);

    abstract void adjustPlayLength(int i);

    abstract void setHighOctave(boolean x);

    abstract void setModulatorSpeed(int i);

    abstract void setModulatorIntensity(int i);
}
