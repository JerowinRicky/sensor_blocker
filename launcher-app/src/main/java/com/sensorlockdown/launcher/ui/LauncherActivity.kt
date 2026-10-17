package com.sensorlockdown.launcher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sensorlockdown.launcher.ui.screens.AppSelectionScreen
import com.sensorlockdown.launcher.ui.screens.LauncherHomeScreen
import com.sensorlockdown.launcher.ui.screens.PinAuthScreen
import com.sensorlockdown.launcher.ui.theme.SecureLauncherTheme
import com.sensorlockdown.launcher.ui.viewmodel.LauncherScreenState
import com.sensorlockdown.launcher.ui.viewmodel.LauncherViewModel

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

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
                    LauncherScreenState.APP_MANAGEMENT -> {
                        viewModel.cancelAction()
                    }
                    else -> {
                        // Other setup/login states
                    }
                }
            }
        })

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
                    LauncherScreenState.APP_MANAGEMENT -> {
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
                            onLaunchApp = { viewModel.launchApp(it) },
                            onRequestUninstall = { viewModel.requestUninstall(it) },
                            onLockLauncher = { viewModel.lockLauncher() },
                            onRequestManageApps = { viewModel.requestManageApps() },
                            onRequestSwitchDefault = { viewModel.requestSwitchDefaultLauncher() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }
}

