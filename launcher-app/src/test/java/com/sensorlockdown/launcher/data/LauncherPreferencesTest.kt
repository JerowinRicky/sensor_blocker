package com.sensorlockdown.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull

class LauncherPreferencesTest {

    private val mockContext = mock(Context::class.java)
    private val mockPrefs = mock(SharedPreferences::class.java)
    private val mockEditor = mock(SharedPreferences.Editor::class.java)
    private val storage = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        storage.clear()
        `when`(mockContext.getSharedPreferences(any(), anyInt())).thenReturn(mockPrefs)

        `when`(mockPrefs.getBoolean(anyString(), anyBoolean())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val def = invocation.arguments[1] as Boolean
            (storage[key] as? Boolean) ?: def
        }

        `when`(mockPrefs.getStringSet(anyString(), anyOrNull())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val def = invocation.arguments[1] as? Set<String>
            @Suppress("UNCHECKED_CAST")
            (storage[key] as? Set<String>) ?: def
        }

        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as Boolean
            storage[key] = value
            mockEditor
        }

        `when`(mockEditor.putStringSet(anyString(), anyOrNull())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            @Suppress("UNCHECKED_CAST")
            val value = invocation.arguments[1] as? Set<String>
            if (value != null) {
                storage[key] = value
            }
            mockEditor
        }
    }

    @Test
    fun defaultPreferences_haveSensibleSecurityDefaults() {
        val prefs = LauncherPreferences(mockContext)
        assertThat(prefs.isNotificationDrawerBlocked()).isTrue()
        assertThat(prefs.isPowerMenuProtected()).isTrue()
        assertThat(prefs.isLockOnScreenOff()).isTrue()
        assertThat(prefs.isInitialSetupCompleted()).isFalse()
        assertThat(prefs.getAllowedPackages()).isEmpty()
    }

    @Test
    fun setNotificationDrawerBlocked_persistsValue() {
        val prefs = LauncherPreferences(mockContext)
        prefs.setNotificationDrawerBlocked(false)
        assertThat(prefs.isNotificationDrawerBlocked()).isFalse()
        prefs.setNotificationDrawerBlocked(true)
        assertThat(prefs.isNotificationDrawerBlocked()).isTrue()
    }

    @Test
    fun setPowerMenuProtected_persistsValue() {
        val prefs = LauncherPreferences(mockContext)
        prefs.setPowerMenuProtected(false)
        assertThat(prefs.isPowerMenuProtected()).isFalse()
    }

    @Test
    fun allowedPackages_addAndRemoveWorkCorrectly() {
        val prefs = LauncherPreferences(mockContext)
        prefs.addAllowedPackage("com.whatsapp")
        prefs.addAllowedPackage("com.google.android.youtube")

        assertThat(prefs.isPackageAllowed("com.whatsapp")).isTrue()
        assertThat(prefs.isPackageAllowed("com.google.android.youtube")).isTrue()
        assertThat(prefs.isPackageAllowed("com.unapproved.app")).isFalse()

        prefs.removeAllowedPackage("com.whatsapp")
        assertThat(prefs.isPackageAllowed("com.whatsapp")).isFalse()
        assertThat(prefs.isPackageAllowed("com.google.android.youtube")).isTrue()
    }
}
