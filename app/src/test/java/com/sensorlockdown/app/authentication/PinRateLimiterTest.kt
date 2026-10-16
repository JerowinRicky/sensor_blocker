package com.sensorlockdown.app.authentication

import com.google.common.truth.Truth.assertThat
import com.sensorlockdown.app.persistence.SecurityPreferences
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PinRateLimiterTest {

    private val preferences: SecurityPreferences = mock()
    private var currentTime = 1_000_000L
    private lateinit var rateLimiter: PinRateLimiter

    @Before
    fun setup() {
        rateLimiter = PinRateLimiter(preferences, timeProvider = { currentTime })
    }

    @Test
    fun `calculateLockoutSeconds - tiers match security specification`() {
        assertThat(rateLimiter.calculateLockoutSeconds(1)).isEqualTo(0L)
        assertThat(rateLimiter.calculateLockoutSeconds(4)).isEqualTo(0L)
        assertThat(rateLimiter.calculateLockoutSeconds(5)).isEqualTo(30L)
        assertThat(rateLimiter.calculateLockoutSeconds(9)).isEqualTo(30L)
        assertThat(rateLimiter.calculateLockoutSeconds(10)).isEqualTo(300L)
        assertThat(rateLimiter.calculateLockoutSeconds(14)).isEqualTo(300L)
        assertThat(rateLimiter.calculateLockoutSeconds(15)).isEqualTo(1800L)
        assertThat(rateLimiter.calculateLockoutSeconds(20)).isEqualTo(1800L)
    }

    @Test
    fun `recordFailure - triggers lockout at 5th failure`() {
        whenever(preferences.getFailedAttempts()).thenReturn(4)

        val lockout = rateLimiter.recordFailure()
        assertThat(lockout).isEqualTo(30L)
        verify(preferences).setFailedAttempts(5)
        verify(preferences).setLockoutExpiry(currentTime + 30_000L)
    }

    @Test
    fun `getRateLimitState - returns active lockout if expiry in future`() {
        whenever(preferences.getLockoutExpiry()).thenReturn(currentTime + 15_000L)
        whenever(preferences.getFailedAttempts()).thenReturn(5)

        val state = rateLimiter.getRateLimitState()
        assertThat(state.isLockedOut).isTrue()
        assertThat(state.secondsRemaining).isEqualTo(15L)
    }

    @Test
    fun `recordSuccess - clears failure counters and lockout`() {
        rateLimiter.recordSuccess()
        verify(preferences).clearRateLimiting()
    }
}

