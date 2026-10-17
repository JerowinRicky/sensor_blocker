package com.sensorlockdown.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sensorlockdown.launcher.service.LauncherSecurityService
import com.sensorlockdown.launcher.ui.screens.AppSelectionScreen
import com.sensorlockdown.launcher.ui.screens.LauncherHomeScreen
import com.sensorlockdown.launcher.ui.screens.PinAuthScreen
import com.sensorlockdown.launcher.ui.theme.SecureLauncherTheme
import com.sensorlockdown.launcher.ui.viewmodel.LauncherScreenState
import com.sensorlockdown.launcher.ui.viewmodel.LauncherViewModel

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.onScreenTurnedOff()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // As a system home launcher, back press must not exit the launcher when in HOME state
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (viewModel.screenState.value) {
                    LauncherScreenState.HOME -> {
                        // Do nothing, keep launcher in foreground
                    }
                    LauncherScreenState.SWITCH_DEFAULT_CONFIRM,
                    LauncherScreenState.APP_MANAGEMENT,
                    LauncherScreenState.POWER_OFF_AUTH,
                    LauncherScreenState.BLOCKED_APP_AUTH -> {
                        viewModel.cancelAction()
                    }
                    else -> {
                        // Other setup/login states - cannot exit
                    }
                }
            }
        })

        handleIncomingIntent(intent)

        setContent {
            SecureLauncherTheme {
                val screenState by viewModel.screenState.collectAsState()
                val currentPin by viewModel.currentPinInput.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val lockoutSeconds by viewModel.lockoutSeconds.collectAsState()
                val allApps by viewModel.allInstalledApps.collectAsState()
                val allowedApps by viewModel.allowedApps.collectAsState()
                val selectedPackages by viewModel.selectedPackagesInSetup.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()

                when (screenState) {
                    LauncherScreenState.PIN_SETUP,
                    LauncherScreenState.PIN_SETUP_CONFIRM,
                    LauncherScreenState.PIN_LOGIN -> {
                        PinAuthScreen(
                            screenState = screenState,
                            currentInput = currentPin,
                            errorMessage = errorMessage,
                            lockoutSeconds = lockoutSeconds,
                            onDigitClick = { viewModel.onDigitEntered(it) },
                            onBackspaceClick = { viewModel.onBackspace() },
                            onClearClick = { viewModel.onClearPin() }
                        )
                    }
                    LauncherScreenState.SWITCH_DEFAULT_CONFIRM,
                    LauncherScreenState.APP_MANAGEMENT,
                    LauncherScreenState.POWER_OFF_AUTH,
                    LauncherScreenState.BLOCKED_APP_AUTH -> {
                        PinAuthScreen(
                            screenState = screenState,
                            currentInput = currentPin,
                            errorMessage = errorMessage,
                            lockoutSeconds = lockoutSeconds,
                            onDigitClick = { viewModel.onDigitEntered(it) },
                            onBackspaceClick = { viewModel.onBackspace() },
                            onClearClick = { viewModel.onClearPin() },
                            onCancelClick = { viewModel.cancelAction() }
                        )
                    }
                    LauncherScreenState.SETUP_APPS -> {
                        AppSelectionScreen(
                            allApps = allApps,
                            selectedPackages = selectedPackages,
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onToggleApp = { viewModel.toggleAppSelection(it) },
                            onSelectAll = { viewModel.selectAllApps() },
                            onDeselectAll = { viewModel.deselectAllApps() },
                            onSaveAndContinue = { viewModel.saveAllowedApps() }
                        )
                    }
                    LauncherScreenState.HOME -> {
                        LauncherHomeScreen(
                            allowedApps = allowedApps,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            onLaunchApp = { viewModel.launchApp(it) },
                            onRequestUninstall = { viewModel.requestUninstall(it) },
                            onLockLauncher = { viewModel.lockLauncher() },
                            onRequestManageApps = { viewModel.requestManageApps() },
                            onRequestSwitchDefault = { viewModel.requestSwitchDefaultLauncher() },
                            onRequestPowerOff = { viewModel.requestPowerOff() },
                            onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val blockedPkg = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE)
        if (!blockedPkg.isNullOrEmpty()) {
            viewModel.handleBlockedPackage(blockedPkg)
            return
        }

        val requestPower = intent.getBooleanExtra(EXTRA_REQUEST_POWER_AUTH, false)
        if (requestPower) {
            viewModel.handlePowerAuthRequest()
            return
        }

        val lockedOnBoot = intent.getBooleanExtra(EXTRA_LOCKED_ON_BOOT, false)
        if (lockedOnBoot) {
            viewModel.lockLauncher()
            return
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {
            // Already unregistered
        }
    }

    override fun onResume() {
        super.onResume()
        LauncherSecurityService.isLauncherForegroundOrLockedState =
            viewModel.screenState.value != LauncherScreenState.HOME
        viewModel.refreshApps()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_REQUEST_POWER_AUTH = "extra_request_power_auth"
        const val EXTRA_LOCKED_ON_BOOT = "extra_locked_on_boot"
    }
}
