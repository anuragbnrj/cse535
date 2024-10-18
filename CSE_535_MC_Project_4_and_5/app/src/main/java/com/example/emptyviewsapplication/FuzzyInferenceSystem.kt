package com.example.emptyviewsapplication

import kotlin.math.abs

class FuzzyInferenceSystem {


    private fun membershipFunction(
        value: Double,
        low: Double,
        medium: Double,
        high: Double
    ): Triple<Double, Double, Double> {
        val lowValue = maxOf(0.0, 1.0 - abs((value - low) / (medium - low)))
        val mediumValue = maxOf(
            0.0, minOf(
                1.0 - abs((value - low) / (medium - low)),
                1.0 - abs((value - high) / (medium - high))
            )
        )
        val highValue = maxOf(0.0, 1.0 - abs((value - high) / (medium - high)))
        return Triple(lowValue, mediumValue, highValue)
    }


    private fun fuzzifySpeed(speed: Double): Triple<Double, Double, Double> {
        return membershipFunction(speed, 35.0, 43.0, 50.0) // Triangular MF for Speed
    }

    private fun fuzzifyAcceleration(acceleration: Double): Triple<Double, Double, Double> {
        return membershipFunction(acceleration, 10.0, 60.0, 110.0) // Triangular MF for Acceleration
    }


    private fun applyRules(
        speed: Triple<Double, Double, Double>,
        acceleration: Triple<Double, Double, Double>
    ): Double {

        // If speed is High AND acceleration is High
        return minOf(speed.third, acceleration.third)
    }

    // Defuzzification to calculate the probability of crash
    fun calculateCrashProbability(speedValue: Double, accelerationValue: Double): Double {
        val speed = fuzzifySpeed(speedValue)
        val acceleration = fuzzifyAcceleration(accelerationValue)

        return applyRules(speed, acceleration)
        // Return the probability of a crash
    }
}