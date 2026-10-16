package com.sensorlockdown.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sensorlockdown.app.ui.screens.AuditLogsScreen
import com.sensorlockdown.app.ui.screens.DashboardScreen
import com.sensorlockdown.app.ui.screens.DeviceManagementInfoScreen
import com.sensorlockdown.app.ui.screens.LockdownResultScreen
import com.sensorlockdown.app.ui.screens.PinLoginScreen
import com.sensorlockdown.app.ui.screens.PinSetupScreen
import com.sensorlockdown.app.ui.screens.SecuritySettingsScreen
import com.sensorlockdown.app.ui.screens.WelcomeScreen
import com.sensorlockdown.app.ui.viewmodel.LockdownViewModel

@Composable
fun LockdownNavGraph(
    navController: NavHostController,
    viewModel: LockdownViewModel
) {
    val isPinConfigured = viewModel.isPinSet()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val rateLimitState by viewModel.rateLimitState.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val lockdownPolicy by viewModel.lockdownPolicy.collectAsState()
    val activationProgress by viewModel.activationProgress.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val integrityResult by viewModel.integrityResult.collectAsState()

    val startDestination = when {
        !isPinConfigured -> Screen.Welcome.route
        !isAuthenticated -> Screen.PinLogin.route
        else -> Screen.Dashboard.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Screen.PinSetup.route) }
            )
        }

        composable(Screen.PinSetup.route) {
            PinSetupScreen(
                onPinCreated = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onSubmitPin = { pin, confirm -> viewModel.setupInitialPin(pin, confirm) }
            )
        }

        composable(Screen.PinLogin.route) {
            PinLoginScreen(
                rateLimitState = rateLimitState,
                onLoginSubmit = { viewModel.loginWithPin(it) },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.PinLogin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                deviceInfo = deviceInfo,
                lockdownPolicy = lockdownPolicy,
                activationProgress = activationProgress,
                onToggleControl = { id, en -> viewModel.toggleControlSelection(id, en) },
                onActivateLockdown = {
                    viewModel.activateLockdown {
                        navController.navigate(Screen.LockdownResult.route)
                    }
                },
                onDeactivateLockdown = {
                    viewModel.deactivateLockdown {}
                },
                onNavigateToSettings = { navController.navigate(Screen.SecuritySettings.route) },
                onNavigateToAuditLogs = {
                    viewModel.refreshAuditLogs()
                    navController.navigate(Screen.AuditLogs.route)
                },
                onNavigateToDeviceInfo = { navController.navigate(Screen.DeviceManagementInfo.route) }
            )
        }

        composable(Screen.LockdownResult.route) {
            LockdownResultScreen(
                policy = lockdownPolicy,
                onReturnToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.LockdownResult.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen(
                integrityResult = integrityResult,
                screenCaptureEnabled = viewModel.getScreenCaptureProtection(),
                onToggleScreenCapture = { viewModel.setScreenCaptureProtection(it) },
                onChangePinSubmit = { cur, new, conf -> viewModel.changePin(cur, new, conf) },
                emergencyRecoverySecret = viewModel.getEmergencyRecoverySecret(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AuditLogs.route) {
            AuditLogsScreen(
                logs = auditLogs,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DeviceManagementInfo.route) {
            DeviceManagementInfoScreen(
                deviceInfo = deviceInfo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

