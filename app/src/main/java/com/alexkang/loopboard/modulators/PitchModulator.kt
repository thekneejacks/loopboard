package com.alexkang.loopboard.modulators

import android.content.Context
import com.alexkang.loopboard.RecordedSample
import com.alexkang.loopboard.Utils
import java.util.concurrent.ExecutorService
import kotlin.random.Random

object PitchModulator {
    private var r: Random = Random
    private lateinit var modulatorExecutor: ExecutorService

    @Synchronized
    fun startRandomMod(sample: RecordedSample) {
        if (sample.isModulatingRandom) {
            //no two random modulators should run simultaneously
            return
        }
        sample.isModulatingRandom = true
        modulatorExecutor = Utils.getExecutorService()
        modulatorExecutor.submit(object : Runnable {
            var intervalModifier: Int = 0
            var rangeModifier: Int = 0
            var min: Int = 0
            var max: Int = 0
            var rand: Int = 0

            override fun run() {
                while (sample.isModulatingRandom) {
                    intervalModifier = sample.modulatorSpeed
                    rangeModifier = sample.modulatorIntensity
                    min = rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    max =
                        Utils.SAMPLE_RATE_HZ_TIMES_TWO - rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    rand = r.nextInt((max - min) + min) + Math.round((min / 2).toFloat())
                    sample.adjustPitch(rand)
                    try {
                        Thread.sleep((2000 / sample.modulatorSpeed).toLong())
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        })
    }

    @Synchronized
    fun startSineMod(sample: RecordedSample) {
        if (sample.isModulatingSine) {
            //no two sine modulators should run simultaneously
            return
        }
        sample.isModulatingSine = true
        modulatorExecutor = Utils.getExecutorService()
        modulatorExecutor!!.submit(object : Runnable {
            var climbing: Boolean = true
            var intervalModifier: Int = 0
            var rangeModifier: Int = 0
            var min: Int = 0
            var max: Int = 0
            var i: Int = 0

            override fun run() {
                while (sample.isModulatingSine) {
                    intervalModifier = sample.modulatorSpeed
                    rangeModifier = sample.modulatorIntensity
                    min = rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    max =
                        Utils.SAMPLE_RATE_HZ_TIMES_TWO - rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    i = sample.pitch

                    if (climbing) {
                        if (i >= max - 10) {
                            climbing = false
                        } else {
                            sample.adjustPitch(i + (10 * intervalModifier))
                            //adjustPitch(i + 100);
                        }
                    } else {
                        if (i <= min + 10) {
                            climbing = true
                        } else {
                            sample.adjustPitch(i - (10 * intervalModifier))
                            //adjustPitch(i - 100);
                        }
                    }
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        })
    }

    @Synchronized
    fun startSawMod(sample: RecordedSample) {
        if (sample.isModulatingSaw) {
            //no two saw modulators should run simultaneously
            return
        }
        sample.isModulatingSaw = true
        modulatorExecutor = Utils.getExecutorService()
        modulatorExecutor!!.submit(object : Runnable {
            var intervalModifier: Int = 0
            var rangeModifier: Int = 0
            var min: Int = 0
            var max: Int = 0
            var i: Int = 0

            override fun run() {
                while (sample.isModulatingSaw) {
                    intervalModifier = sample.modulatorSpeed
                    rangeModifier = sample.modulatorIntensity
                    min = rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT
                    max =
                        Utils.SAMPLE_RATE_HZ_TIMES_TWO - (rangeModifier * Utils.SAMPLE_RATE_HZ_DIVIDED_BY_EIGHT)
                    i = sample.pitch
                    if (i >= max - 10) {
                        sample.adjustPitch(min)
                    } else {
                        sample.adjustPitch(i + 10 * intervalModifier)
                    }
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        })
    }

}