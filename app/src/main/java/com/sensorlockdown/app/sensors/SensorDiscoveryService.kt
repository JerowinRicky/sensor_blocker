package com.sensorlockdown.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.sensorlockdown.app.lockdown.model.ControlId
import com.sensorlockdown.app.lockdown.model.EnforcementStatus

/**
 * Discovers and audits physical device sensors.
 *
 * CRITICAL ARCHITECTURAL PRINCIPLE:
 * Android AOSP does not provide a public, non-system API to disable hardware sensor buses
 * (such as I2C/SPI accelerometer or gyroscope registers) for external apps.
 * Simply calling sensorManager.unregisterListener() only removes this app's own listener
 * and does NOT restrict other apps on the device.
 *
 * This service honestly audits presence and reports UNSUPPORTED for raw sensor lockdown.
 */
class SensorDiscoveryService(private val context: Context) {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun discoverAllSensors(): List<DiscoveredSensor> {
        val manager = sensorManager ?: return emptyList()
        val allSensors = manager.getSensorList(Sensor.TYPE_ALL)

        return allSensors.map { sensor ->
            DiscoveredSensor(
                type = sensor.type,
                name = sensor.name,
                vendor = sensor.vendor,
                version = sensor.version,
                powerMilliAmps = sensor.power,
                maximumRange = sensor.maximumRange,
                resolution = sensor.resolution,
                isPresent = true
            )
        }
    }

    fun hasSensor(sensorType: Int): Boolean {
        val manager = sensorManager ?: return false
        return manager.getDefaultSensor(sensorType) != null
    }

    /**
     * Determines whether a specific hardware control has the physical sensor present.
     */
    fun checkSensorPresence(controlId: ControlId): Boolean {
        return when (controlId) {
            ControlId.ACCELEROMETER -> hasSensor(Sensor.TYPE_ACCELEROMETER)
            ControlId.GYROSCOPE -> hasSensor(Sensor.TYPE_GYROSCOPE)
            ControlId.MAGNETOMETER -> hasSensor(Sensor.TYPE_MAGNETIC_FIELD)
            ControlId.PROXIMITY -> hasSensor(Sensor.TYPE_PROXIMITY)
            ControlId.LIGHT_SENSOR -> hasSensor(Sensor.TYPE_LIGHT)
            ControlId.BAROMETER -> hasSensor(Sensor.TYPE_PRESSURE)
            else -> true
        }
    }

    /**
     * Honest assessment of whether a sensor bus restriction can be enforced system-wide.
     */
    fun getSensorRestrictionEnforceability(controlId: ControlId): Pair<EnforcementStatus, String?> {
        val present = checkSensorPresence(controlId)
        if (!present) {
            return Pair(
                EnforcementStatus.NOT_PRESENT,
                "Physical hardware sensor not detected on this device."
            )
        }

        return Pair(
            EnforcementStatus.UNSUPPORTED,
            "Android AOSP does not expose a public device-wide API to halt this sensor bus. " +
                    "Unregistering local listeners does not restrict other applications."
        )
    }
}

