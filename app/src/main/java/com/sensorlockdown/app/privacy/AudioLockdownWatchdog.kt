package com.sensorlockdown.app.privacy

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Robust audio lockdown watchdog.
 * Prevents the Android Telephony / InCallService subsystem from unmuting the microphone
 * when an outgoing or incoming phone call connects.
 */
class AudioLockdownWatchdog(private val context: Context) {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var isMicrophoneLockdownActive: Boolean = false
        private set

    @Volatile
    var isSpeakerLockdownActive: Boolean = false
        private set

    // Legacy PhoneStateListener for Android < 12
    private var legacyListener: PhoneStateListener? = null

    // Modern TelephonyCallback for Android 12+ (API 31+)
    private var modernCallback: Any? = null

    init {
        registerTelephonyCallListener()
    }

    private fun registerTelephonyCallListener() {
        val tm = telephonyManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state)
                    }
                }
                modernCallback = callback
                tm.registerTelephonyCallback(context.mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state)
                    }
                }
                legacyListener = listener
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (e: Exception) {
            // Telephony state listen failed (e.g. tablet without cellular radio)
        }
    }

    private fun handleCallState(state: Int) {
        if (!isMicrophoneLockdownActive) return
        val am = audioManager ?: return

        // When a call connects (OFFHOOK) or rings, telephony server un-mutes mic by default.
        // We immediately counter it and force microphone mute back on!
        try {
            am.isMicrophoneMute = true
            if (isSpeakerLockdownActive) {
                am.setStreamMute(AudioManager.STREAM_VOICE_CALL, true)
                am.setStreamMute(AudioManager.STREAM_RING, true)
            }
        } catch (e: Exception) {
            // Ignore security exception if restricted
        }
    }

    fun startEnforcement(micLockdown: Boolean, speakerLockdown: Boolean) {
        isMicrophoneLockdownActive = micLockdown
        isSpeakerLockdownActive = speakerLockdown

        applyImmediateMute()

        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                if (isMicrophoneLockdownActive) {
                    val am = audioManager
                    if (am != null && !am.isMicrophoneMute) {
                        try {
                            am.isMicrophoneMute = true
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                }

                if (isSpeakerLockdownActive) {
                    val am = audioManager
                    if (am != null) {
                        try {
                            am.setStreamMute(AudioManager.STREAM_MUSIC, true)
                            am.setStreamMute(AudioManager.STREAM_RING, true)
                            am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
                            am.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                            am.setStreamMute(AudioManager.STREAM_VOICE_CALL, true)
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                }

                delay(250) // High-frequency 250ms check to beat OS dialer overrides
            }
        }
    }

    fun stopEnforcement() {
        isMicrophoneLockdownActive = false
        isSpeakerLockdownActive = false
        watchdogJob?.cancel()
        watchdogJob = null

        val am = audioManager ?: return
        try {
            am.isMicrophoneMute = false
            am.setStreamMute(AudioManager.STREAM_MUSIC, false)
            am.setStreamMute(AudioManager.STREAM_RING, false)
            am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
            am.setStreamMute(AudioManager.STREAM_SYSTEM, false)
            am.setStreamMute(AudioManager.STREAM_VOICE_CALL, false)
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun applyImmediateMute() {
        val am = audioManager ?: return
        try {
            if (isMicrophoneLockdownActive) {
                am.isMicrophoneMute = true
            }
            if (isSpeakerLockdownActive) {
                am.setStreamMute(AudioManager.STREAM_MUSIC, true)
                am.setStreamMute(AudioManager.STREAM_RING, true)
                am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
                am.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                am.setStreamMute(AudioManager.STREAM_VOICE_CALL, true)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }
}
