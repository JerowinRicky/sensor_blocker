package com.sensorlockdown.launcher.security

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class PinSecurityManagerTest {

    private val mockContext = mock(Context::class.java)
    private val mockPrefs = mock(SharedPreferences::class.java)
    private val mockEditor = mock(SharedPreferences.Editor::class.java)

    private val storage = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        storage.clear()
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)

        `when`(mockPrefs.contains(anyString())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            storage.containsKey(key)
        }
        `when`(mockPrefs.getString(anyString(), any())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val def = invocation.arguments[1]
            (storage[key] as? String) ?: def
        }
        `when`(mockPrefs.getInt(anyString(), anyInt())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val def = invocation.arguments[1] as Int
            (storage[key] as? Int) ?: def
        }
        `when`(mockPrefs.getLong(anyString(), anyLong())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val def = invocation.arguments[1] as Long
            (storage[key] as? Long) ?: def
        }

        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as String
            storage[key] = value
            mockEditor
        }
        `when`(mockEditor.putInt(anyString(), anyInt())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as Int
            storage[key] = value
            mockEditor
        }
        `when`(mockEditor.putLong(anyString(), anyLong())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as Long
            storage[key] = value
            mockEditor
        }
    }

    @Test
    fun setupPin_valid6DigitPin_returnsTrue() {
        val manager = PinSecurityManager(mockContext)
        val success = manager.setupPin("123456")
        assertThat(success).isTrue()
        assertThat(manager.isPinConfigured()).isTrue()
    }

    @Test
    fun setupPin_invalidPin_returnsFalse() {
        val manager = PinSecurityManager(mockContext)
        assertThat(manager.setupPin("12345")).isFalse()
        assertThat(manager.setupPin("1234567")).isFalse()
        assertThat(manager.setupPin("12a456")).isFalse()
    }

    @Test
    fun verifyPin_correctPin_returnsTrue() {
        val manager = PinSecurityManager(mockContext)
        manager.setupPin("654321")
        val isMatch = manager.verifyPin("654321")
        assertThat(isMatch).isTrue()
    }

    @Test
    fun verifyPin_wrongPin_returnsFalse() {
        val manager = PinSecurityManager(mockContext)
        manager.setupPin("654321")
        val isMatch = manager.verifyPin("111111")
        assertThat(isMatch).isFalse()
    }
}
