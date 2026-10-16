package com.sensorlockdown.app.lockdown

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sensorlockdown.app.devicepolicy.DevicePolicyController
import com.sensorlockdown.app.lockdown.engine.CapabilityEvaluator
import com.sensorlockdown.app.lockdown.model.ControlId
import com.sensorlockdown.app.lockdown.model.EnforcementStatus
import com.sensorlockdown.app.privacy.PrivacyController
import com.sensorlockdown.app.sensors.SensorDiscoveryService
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CapabilityEvaluatorTest {

    private val context: Context = mock()
    private val devicePolicyController: DevicePolicyController = mock()
    private val privacyController: PrivacyController = mock()
    private val sensorDiscoveryService: SensorDiscoveryService = mock()
    private lateinit var evaluator: CapabilityEvaluator

    @Before
    fun setup() {
        evaluator = CapabilityEvaluator(
            context,
            devicePolicyController,
            privacyController,
            sensorDiscoveryService
        )
    }

    @Test
    fun `evaluateControl - Camera requires admin privilege`() {
        whenever(devicePolicyController.isAdminActive()).thenReturn(false)
        val policy = evaluator.evaluateControl(ControlId.CAMERA, requestedState = true)

        assertThat(policy.supportedState).isTrue()
        assertThat(policy.status).isEqualTo(EnforcementStatus.REQUESTED)
        assertThat(policy.reason).contains("Device Administrator")
    }

    @Test
    fun `evaluateControl - Camera supported when admin is active`() {
        whenever(devicePolicyController.isAdminActive()).thenReturn(true)
        whenever(devicePolicyController.isCameraDisabled()).thenReturn(false)

        val policy = evaluator.evaluateControl(ControlId.CAMERA, requestedState = true)
        assertThat(policy.supportedState).isTrue()
        assertThat(policy.status).isEqualTo(EnforcementStatus.REQUESTED)
    }

    @Test
    fun `evaluateControl - Gyroscope honestly reports UNSUPPORTED and explains AOSP limitations`() {
        whenever(sensorDiscoveryService.getSensorRestrictionEnforceability(ControlId.GYROSCOPE))
            .thenReturn(Pair(EnforcementStatus.UNSUPPORTED, "Android AOSP does not expose a public device-wide API to halt this sensor bus."))

        val policy = evaluator.evaluateControl(ControlId.GYROSCOPE, requestedState = true)
        assertThat(policy.supportedState).isFalse()
        assertThat(policy.status).isEqualTo(EnforcementStatus.UNSUPPORTED)
        assertThat(policy.reason).contains("Android AOSP does not expose a public device-wide API")
    }

    @Test
    fun `evaluateControl - USB Data requires Device Owner`() {
        whenever(devicePolicyController.isDeviceOwner()).thenReturn(false)
        val policy = evaluator.evaluateControl(ControlId.USB_DATA, requestedState = true)

        assertThat(policy.supportedState).isFalse()
        assertThat(policy.status).isEqualTo(EnforcementStatus.UNSUPPORTED)
    }
}

