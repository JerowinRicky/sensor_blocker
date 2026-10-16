package com.sensorlockdown.emergency

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EmergencyRecoveryClientTest {

    @Test
    fun `recoveryResponse data class handles success`() {
        val resp = RecoveryResponse(
            isSuccess = true,
            recoverySecret = "ABCD-EFGH-1234-5678",
            timestamp = 1000L
        )
        assertThat(resp.isSuccess).isTrue()
        assertThat(resp.recoverySecret).isEqualTo("ABCD-EFGH-1234-5678")
    }

    @Test
    fun `recoveryResponse data class handles failure`() {
        val resp = RecoveryResponse(
            isSuccess = false,
            errorMessage = "Signature mismatch"
        )
        assertThat(resp.isSuccess).isFalse()
        assertThat(resp.errorMessage).contains("Signature mismatch")
    }
}

