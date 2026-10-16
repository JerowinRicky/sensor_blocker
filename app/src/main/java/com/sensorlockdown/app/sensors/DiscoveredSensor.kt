package com.sensorlockdown.app.sensors

data class DiscoveredSensor(
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val powerMilliAmps: Float,
    val maximumRange: Float,
    val resolution: Float,
    val isPresent: Boolean
)

