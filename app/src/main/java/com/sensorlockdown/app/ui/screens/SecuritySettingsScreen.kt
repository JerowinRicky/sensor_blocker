package com.sensorlockdown.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.authentication.model.AuthResult
import com.sensorlockdown.app.security.DeviceIntegrityChecker
import com.sensorlockdown.app.ui.components.PinInputView
import com.sensorlockdown.app.ui.components.UninstallAppDialog
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.Slate700
import com.sensorlockdown.app.ui.theme.StatusAlertRed
import com.sensorlockdown.app.ui.theme.StatusEnforcedGreen
import com.sensorlockdown.app.ui.theme.StatusPartialOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    integrityResult: DeviceIntegrityChecker.IntegrityResult,
    screenCaptureEnabled: Boolean,
    onToggleScreenCapture: (Boolean) -> Unit,
    onChangePinSubmit: (String, String, String) -> AuthResult,
    onUninstallAppSubmit: (String) -> AuthResult = { AuthResult.Success },
    isUninstallShieldActive: Boolean = false,
    onOpenAccessibilitySettings: () -> Unit = {},
    emergencyRecoverySecret: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SECURITY SETTINGS", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Authentication Card
            SettingSectionCard(title = "AUTHENTICATION") {
                Button(
                    onClick = { showChangePinDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Change 6-Digit PIN")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showRecoveryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("View Emergency Recovery Secret")
                }
            }

            // Window Security Card
            SettingSectionCard(title = "DISPLAY & MEMORY PROTECTION") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FLAG_SECURE Window Shield",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Blocks screenshots, screen casting, and task switcher snapshots.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                    Switch(
                        checked = screenCaptureEnabled,
                        onCheckedChange = onToggleScreenCapture
                    )
                }
            }

            // Device Integrity Check
            SettingSectionCard(title = "DEVICE INTEGRITY AUDIT") {
                val isCompromised = integrityResult.isRootSuspected
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isCompromised) StatusAlertRed else StatusEnforcedGreen,
                                RoundedCornerShape(5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (isCompromised) "POTENTIALLY COMPROMISED" else "INTEGRITY INTACT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isCompromised) StatusAlertRed else StatusEnforcedGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "USB Debugging: ${if (integrityResult.isAdbEnabled) "ENABLED (Warning)" else "Disabled"}",
                    fontSize = 13.sp,
                    color = Slate500
                )
                Text(
                    text = "ROM Build Tags: ${if (integrityResult.isTestKeysBuild) "Test-Keys (Custom)" else "Release-Keys (Official)"}",
                    fontSize = 13.sp,
                    color = Slate500
                )

                if (integrityResult.findings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    for (finding in integrityResult.findings) {
                        Text(text = "• $finding", fontSize = 12.sp, color = StatusAlertRed)
                    }
                }
            }

            // App Drawer Uninstall Shield Card
            SettingSectionCard(title = "APP DRAWER UNINSTALL SHIELD") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Outside Uninstallation Shield",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Intercepts uninstall attempts from App Drawer, Launcher, and Settings, prompting for 6-digit PIN.",
                            fontSize = 12.sp,
                            color = Slate500,
                            lineHeight = 16.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isUninstallShieldActive) StatusEnforcedGreen else StatusPartialOrange,
                                RoundedCornerShape(5.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isUninstallShieldActive) {
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("ENABLE APP DRAWER SHIELD (ACCESSIBILITY)")
                    }
                } else {
                    Text(
                        text = "✓ Shield Active: Outside uninstallation from App Drawer and Settings is locked.",
                        color = StatusEnforcedGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Uninstall Protection Section
            SettingSectionCard(title = "UNINSTALL PROTECTION") {
                Text(
                    text = "This application is protected against unauthorized removal. To uninstall this application or clear device policies, verification of your 6-digit PIN is strictly required.",
                    fontSize = 12.sp,
                    color = Slate500,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showUninstallDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusAlertRed,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("UNINSTALL APPLICATION (PIN REQUIRED)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            onSubmit = onChangePinSubmit,
            onDismiss = { showChangePinDialog = false }
        )
    }

    // Emergency Recovery Secret Dialog
    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text(text = "Emergency Recovery Credential", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "This credential can be queried via authenticated IPC by the companion Lockdown Emergency application.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate700.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emergencyRecoverySecret ?: "UNCONFIGURED",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Do not transcribe or copy this code into untrusted apps or notes.",
                        fontSize = 11.sp,
                        color = StatusAlertRed
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showRecoveryDialog = false }) {
                    Text("CLOSE")
                }
            }
        )
    }

    // Uninstall App Authorization Dialog
    if (showUninstallDialog) {
        UninstallAppDialog(
            onConfirmUninstall = onUninstallAppSubmit,
            onDismiss = { showUninstallDialog = false }
        )
    }
}

@Composable
private fun SettingSectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Slate700.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate500,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ChangePinDialog(
    onSubmit: (String, String, String) -> AuthResult,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableStateOf(1) } // 1: Old PIN, 2: New PIN, 3: Confirm New PIN
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (stage) {
                    1 -> "Enter Current PIN"
                    2 -> "Enter New 6-Digit PIN"
                    else -> "Confirm New PIN"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (errorText != null) {
                    Text(text = errorText!!, color = StatusAlertRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                when (stage) {
                    1 -> PinInputView(pin = oldPin, onPinChange = { oldPin = it; errorText = null })
                    2 -> PinInputView(pin = newPin, onPinChange = { newPin = it; errorText = null })
                    3 -> PinInputView(pin = confirmNewPin, onPinChange = { confirmNewPin = it; errorText = null })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (stage) {
                        1 -> if (oldPin.length == 6) stage = 2 else errorText = "Must be 6 digits"
                        2 -> if (newPin.length == 6) stage = 3 else errorText = "Must be 6 digits"
                        3 -> {
                            val result = onSubmit(oldPin, newPin, confirmNewPin)
                            when (result) {
                                is AuthResult.Success -> onDismiss()
                                is AuthResult.ValidationError -> errorText = result.message
                                is AuthResult.InvalidPin -> errorText = "Incorrect current PIN."
                                is AuthResult.LockedOut -> errorText = "Locked out for ${result.lockoutSecondsRemaining}s"
                            }
                        }
                    }
                }
            ) {
                Text(if (stage == 3) "SAVE PIN" else "NEXT")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}


