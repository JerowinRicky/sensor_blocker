package com.sensorlockdown.app.lockdown

import com.google.common.truth.Truth.assertThat
import com.sensorlockdown.app.lockdown.model.Category
import com.sensorlockdown.app.lockdown.model.ControlId
import com.sensorlockdown.app.lockdown.model.ControlPolicy
import com.sensorlockdown.app.lockdown.model.EnforcementStatus
import com.sensorlockdown.app.lockdown.model.LockdownPolicy
import com.sensorlockdown.app.lockdown.model.OverallLockdownStatus
import org.junit.Test

class LockdownPolicyTest {

    @Test
    fun `control categories are correctly mapped`() {
        assertThat(ControlId.CAMERA.category).isEqualTo(Category.PRIVACY_INPUTS)
        assertThat(ControlId.MICROPHONE.category).isEqualTo(Category.PRIVACY_INPUTS)
        assertThat(ControlId.LOCATION.category).isEqualTo(Category.PRIVACY_INPUTS)

        assertThat(ControlId.GYROSCOPE.category).isEqualTo(Category.MOTION_ENVIRONMENTAL_SENSORS)
        assertThat(ControlId.ACCELEROMETER.category).isEqualTo(Category.MOTION_ENVIRONMENTAL_SENSORS)

        assertThat(ControlId.BLUETOOTH.category).isEqualTo(Category.CONNECTIVITY)
        assertThat(ControlId.USB_DATA.category).isEqualTo(Category.CONNECTIVITY)

        assertThat(ControlId.SCREEN_CAPTURE.category).isEqualTo(Category.SYSTEM_CONTROLS)
        assertThat(ControlId.APP_UNINSTALLATION.category).isEqualTo(Category.SYSTEM_CONTROLS)
    }

    @Test
    fun `overall status calculation - all enforced yields FULLY_ACTIVE`() {
        val controls = mapOf(
            ControlId.CAMERA to ControlPolicy(
                controlId = ControlId.CAMERA,
                title = "Camera",
                description = "",
                requestedState = true,
                supportedState = true,
                enforcedState = true,
                status = EnforcementStatus.ENFORCED
            )
        )
        val policy = LockdownPolicy(
            isActive = true,
            controls = controls,
            overallStatus = OverallLockdownStatus.FULLY_ACTIVE
        )
        assertThat(policy.overallStatus).isEqualTo(OverallLockdownStatus.FULLY_ACTIVE)
        assertThat(policy.isActive).isTrue()
    }

    @Test
    fun `overall status calculation - partial enforcement yields PARTIALLY_ACTIVE`() {
        val controls = mapOf(
            ControlId.CAMERA to ControlPolicy(
                controlId = ControlId.CAMERA,
                title = "Camera",
                description = "",
                requestedState = true,
                supportedState = true,
                enforcedState = true,
                status = EnforcementStatus.ENFORCED
            ),
            ControlId.GYROSCOPE to ControlPolicy(
                controlId = ControlId.GYROSCOPE,
                title = "Gyroscope",
                description = "",
                requestedState = true,
                supportedState = false,
                enforcedState = false,
                status = EnforcementStatus.UNSUPPORTED
            )
        )
        val policy = LockdownPolicy(
            isActive = true,
            controls = controls,
            overallStatus = OverallLockdownStatus.PARTIALLY_ACTIVE
        )
        assertThat(policy.overallStatus).isEqualTo(OverallLockdownStatus.PARTIALLY_ACTIVE)
    }
}

