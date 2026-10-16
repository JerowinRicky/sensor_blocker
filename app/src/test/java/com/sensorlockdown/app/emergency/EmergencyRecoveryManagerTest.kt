package com.sensorlockdown.app.emergency

import android.content.Context
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import com.sensorlockdown.app.persistence.SecurityPreferences
import com.sensorlockdown.app.security.AuditLogger
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EmergencyRecoveryManagerTest {

    private val context: Context = mock()
    private val packageManager: PackageManager = mock()
    private val preferences: SecurityPreferences = mock()
    private val auditLogger: AuditLogger = mock()
    private lateinit var manager: EmergencyRecoveryManager

    @Before
    fun setup() {
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.packageName).thenReturn("com.sensorlockdown.app")
        manager = EmergencyRecoveryManager(context, preferences, auditLogger, callingUidProvider = { 1000 })
    }

    @Test
    fun `getAuthorizedRecoverySecret - null caller rejected and logged as unauthorized`() {
        val result = manager.getAuthorizedRecoverySecret(null)
        assertThat(result).isNull()
        verify(auditLogger).logEvent(
            eventType = eq(AuditLogger.EventType.UNAUTHORIZED_ATTEMPT),
            actor = eq("UNKNOWN_CALLER"),
            details = any(),
            isSuccess = eq(false)
        )
    }

    @Test
    fun `isCallerAuthorized - returns false if signatures do not match`() {
        whenever(packageManager.getPackageUid("com.malicious.app", 0)).thenReturn(1000)
        whenever(packageManager.checkSignatures("com.sensorlockdown.app", "com.malicious.app"))
            .thenReturn(PackageManager.SIGNATURE_NO_MATCH)

        val isAuth = manager.isCallerAuthorized("com.malicious.app")
        assertThat(isAuth).isFalse()
    }
}

