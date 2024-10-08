package com.alexkang.loopboard;


abstract class Sample {

    abstract String getName();

    abstract int getVolume();

    abstract int getPitch();

    abstract int getLength();

    abstract int getRandomizerInterval();

    abstract int getRandomizerIntensity();

    abstract void play(boolean isLooped);

    abstract void stop();

    abstract void startRandomizer();

    abstract void startSineMod();

    abstract void startSawMod();

    abstract void removeModulationCallbacks(int i);

    abstract boolean isLooping();

    abstract void shutdown();

    abstract void adjustVolume(int i);

    abstract void adjustPitch(int i);

    abstract void adjustPlayLength(int i);

    abstract void setHighOctave(boolean x);

    abstract void adjustRandomizerInterval(int i);

    abstract void adjustRandomizerIntensity(int i);
}
