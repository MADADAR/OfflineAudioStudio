package com.sonicdemixer

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Deterministic signal checks used by the separation test suite and by diagnostics.
 * Lag convention: a positive result means candidate is delayed relative to reference.
 */
object AudioQualityMetrics {
    data class SignalReport(
        val sampleCount: Int,
        val nonFiniteSamples: Int,
        val clippedSamples: Int,
        val peak: Float,
        val rms: Double
    ) {
        val isFinite: Boolean get() = nonFiniteSamples == 0
    }

    fun inspect(signal: FloatArray, clipThreshold: Float = 0.999f): SignalReport {
        require(clipThreshold in 0f..1f)
        var bad = 0
        var clipped = 0
        var peak = 0f
        var sumSquares = 0.0
        for (sample in signal) {
            if (!sample.isFinite()) {
                bad++
            } else {
                val magnitude = abs(sample)
                if (magnitude > peak) peak = magnitude
                if (magnitude >= clipThreshold) clipped++
                sumSquares += sample.toDouble() * sample
            }
        }
        val valid = signal.size - bad
        return SignalReport(signal.size, bad, clipped, peak,
            if (valid == 0) 0.0 else sqrt(sumSquares / valid))
    }

    /** SNR in dB between reference and the sum of all estimated stems. */
    fun reconstructionSnrDb(reference: FloatArray, stems: List<FloatArray>): Double {
        require(stems.isNotEmpty())
        require(stems.all { it.size == reference.size }) { "Stem lengths differ from reference" }
        var signalPower = 0.0
        var errorPower = 0.0
        for (i in reference.indices) {
            var reconstructed = 0.0
            for (stem in stems) reconstructed += stem[i]
            val target = reference[i].toDouble()
            signalPower += target * target
            val error = target - reconstructed
            errorPower += error * error
        }
        if (errorPower == 0.0) return Double.POSITIVE_INFINITY
        if (signalPower == 0.0) return Double.NEGATIVE_INFINITY
        return 10.0 * ln(signalPower / errorPower) / ln(10.0)
    }

    /** Normalized cross-correlation lag search; positive means candidate occurs later. */
    fun estimateLagSamples(reference: FloatArray, candidate: FloatArray, maxLag: Int): Int {
        require(reference.isNotEmpty() && candidate.isNotEmpty())
        require(maxLag >= 0)
        val limit = minOf(maxLag, reference.size - 1, candidate.size - 1)
        var bestLag = 0
        var bestScore = Double.NEGATIVE_INFINITY
        for (lag in -limit..limit) {
            val startRef = max(0, -lag)
            val startCandidate = max(0, lag)
            val count = minOf(reference.size - startRef, candidate.size - startCandidate)
            if (count <= 1) continue
            var dot = 0.0
            var refPower = 0.0
            var candidatePower = 0.0
            for (i in 0 until count) {
                val a = reference[startRef + i].toDouble()
                val b = candidate[startCandidate + i].toDouble()
                dot += a * b
                refPower += a * a
                candidatePower += b * b
            }
            val denom = sqrt(refPower * candidatePower)
            val score = if (denom == 0.0) Double.NEGATIVE_INFINITY else dot / denom
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }
        return bestLag
    }
}
