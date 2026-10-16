package com.sensorlockdown.app

import android.app.Application
import android.media.AudioManager
import com.sensorlockdown.app.authentication.PinAuthenticationService
import com.sensorlockdown.app.authentication.PinRateLimiter
import com.sensorlockdown.app.devicepolicy.DevicePolicyController
import com.sensorlockdown.app.lockdown.engine.CapabilityEvaluator
import com.sensorlockdown.app.lockdown.engine.LockdownEngine
import com.sensorlockdown.app.persistence.LockdownDatabase
import com.sensorlockdown.app.persistence.SecurityPreferences
import com.sensorlockdown.app.privacy.PrivacyController
import com.sensorlockdown.app.security.AuditLogger
import com.sensorlockdown.app.security.DeviceIntegrityChecker
import com.sensorlockdown.app.sensors.SensorDiscoveryService

class LockdownApplication : Application() {

    lateinit var database: LockdownDatabase
        private set
    lateinit var securityPreferences: SecurityPreferences
        private set
    lateinit var auditLogger: AuditLogger
        private set
    lateinit var pinRateLimiter: PinRateLimiter
        private set
    lateinit var pinAuthenticationService: PinAuthenticationService
        private set
    lateinit var devicePolicyController: DevicePolicyController
        private set
    lateinit var sensorDiscoveryService: SensorDiscoveryService
        private set
    lateinit var privacyController: PrivacyController
        private set
    lateinit var capabilityEvaluator: CapabilityEvaluator
        private set
    lateinit var lockdownEngine: LockdownEngine
        private set
    lateinit var deviceIntegrityChecker: DeviceIntegrityChecker
        private set

    override fun onCreate() {
        super.onCreate()
        database = LockdownDatabase.getInstance(this)
        securityPreferences = SecurityPreferences(this)
        auditLogger = AuditLogger(database.auditLogDao())
        pinRateLimiter = PinRateLimiter(securityPreferences)
        pinAuthenticationService = PinAuthenticationService(securityPreferences, pinRateLimiter, auditLogger)

        devicePolicyController = DevicePolicyController(this)
        sensorDiscoveryService = SensorDiscoveryService(this)
        privacyController = PrivacyController(this, devicePolicyController)
        capabilityEvaluator = CapabilityEvaluator(
            context = this,
            devicePolicyController = devicePolicyController,
            privacyController = privacyController,
            sensorDiscoveryService = sensorDiscoveryService
        )

        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        lockdownEngine = LockdownEngine(
            capabilityEvaluator = capabilityEvaluator,
            devicePolicyController = devicePolicyController,
            privacyController = privacyController,
            audioManager = audioManager,
            policyDao = database.policyDao(),
            auditLogger = auditLogger
        )

        deviceIntegrityChecker = DeviceIntegrityChecker(this)
    }
}

