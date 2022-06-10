package com.alexkang.loopboard;

abstract class Sample {

    abstract String getName();

    abstract int getVolume();

    abstract void play(boolean isLooped);

    abstract void stop();

    abstract boolean isLooping();

    abstract void shutdown();

    abstract void adjustVolume(int i);

    abstract void mute(boolean x);
}
