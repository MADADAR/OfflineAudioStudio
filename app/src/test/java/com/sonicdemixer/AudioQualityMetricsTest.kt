package com.sonicdemixer

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioQualityMetricsTest {
    private fun tone(size: Int, frequency: Double = 440.0, rate: Double = 44100.0) =
        FloatArray(size) { i -> sin(2.0 * PI * frequency * i / rate).toFloat() * 0.4f }

    @Test fun reconstructionIsLosslessWhenStemsSumToSource() {
        val source = tone(8192)
        val stems = listOf(
            FloatArray(source.size) { source[it] * 0.25f },
            FloatArray(source.size) { source[it] * 0.25f },
            FloatArray(source.size) { source[it] * 0.25f },
            FloatArray(source.size) { source[it] * 0.25f }
        )
        assertTrue(AudioQualityMetrics.reconstructionSnrDb(source, stems) > 100.0)
    }

    @Test fun reconstructionDetectsMissingStemEnergy() {
        val source = tone(8192)
        val incomplete = listOf(FloatArray(source.size) { source[it] * 0.5f })
        assertTrue(AudioQualityMetrics.reconstructionSnrDb(source, incomplete) < 7.0)
    }

    @Test fun lagEstimatorFindsSampleOffset() {
        val source = tone(4096, frequency = 997.0)
        val delay = 17
        val shifted = FloatArray(source.size)
        for (i in delay until source.size) shifted[i] = source[i - delay]
        assertEquals(delay, AudioQualityMetrics.estimateLagSamples(source, shifted, 32))
    }

    @Test fun signalInspectionCountsNonFiniteAndClippedSamples() {
        val report = AudioQualityMetrics.inspect(floatArrayOf(0f, 0.2f, 1f, -1f, Float.NaN, Float.POSITIVE_INFINITY))
        assertEquals(2, report.nonFiniteSamples)
        assertEquals(2, report.clippedSamples)
        assertFalse(report.isFinite)
        assertTrue(report.peak >= 1f)
    }

    @Test fun mismatchedStemLengthIsRejected() {
        try {
            AudioQualityMetrics.reconstructionSnrDb(FloatArray(8), listOf(FloatArray(7)))
            fail("Expected length validation")
        } catch (_: IllegalArgumentException) { }
    }
}
