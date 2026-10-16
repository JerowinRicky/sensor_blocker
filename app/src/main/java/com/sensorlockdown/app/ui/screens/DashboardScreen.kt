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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.sensorlockdown.app.devicepolicy.DeviceManagementState
import com.sensorlockdown.app.lockdown.model.Category
import com.sensorlockdown.app.lockdown.model.ControlId
import com.sensorlockdown.app.lockdown.model.LockdownPolicy
import com.sensorlockdown.app.lockdown.model.OverallLockdownStatus
import com.sensorlockdown.app.ui.components.CategoryHeader
import com.sensorlockdown.app.ui.components.ControlItemCard
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.Slate700
import com.sensorlockdown.app.ui.theme.StatusAlertRed
import com.sensorlockdown.app.ui.theme.StatusEnforcedGreen
import com.sensorlockdown.app.ui.theme.StatusEnforcedGreenBg
import com.sensorlockdown.app.ui.theme.StatusPartialOrange
import com.sensorlockdown.app.ui.theme.StatusPartialOrangeBg
import com.sensorlockdown.app.ui.viewmodel.ActivationProgress
import com.sensorlockdown.app.ui.viewmodel.DeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    deviceInfo: DeviceInfo,
    lockdownPolicy: LockdownPolicy,
    activationProgress: ActivationProgress,
    onToggleControl: (ControlId, Boolean) -> Unit,
    onActivateLockdown: () -> Unit,
    onDeactivateLockdown: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAuditLogs: () -> Unit,
    onNavigateToDeviceInfo: () -> Unit,
    onActivateDeviceAdmin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showActivationDialog by remember { mutableStateOf(false) }
    var showDeactivationDialog by remember { mutableStateOf(false) }
    var showAdminPromptDialog by remember { mutableStateOf<ControlId?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DEVICE CONTROLS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToDeviceInfo) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Device Info")
                    }
                    IconButton(onClick = onNavigateToAuditLogs) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Audit Logs")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                if (lockdownPolicy.isActive) {
                    Button(
                        onClick = { showDeactivationDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusAlertRed,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "DEACTIVATE LOCKDOWN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                } else {
                    Button(
                        onClick = { showActivationDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "ACTIVATE LOCKDOWN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Activation Progress Banner
            if (activationProgress.isActivating) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "Applying lockdown: ${activationProgress.currentControl?.displayName ?: "..."}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val progressFloat = if (activationProgress.totalSteps > 0) {
                                activationProgress.step.toFloat() / activationProgress.totalSteps.toFloat()
                            } else 0f
                            LinearProgressIndicator(
                                progress = progressFloat,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Device Status Header Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, Slate700.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DEVICE STATUS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                letterSpacing = 1.sp
                            )
                            val (dotColor, statusText) = when (lockdownPolicy.overallStatus) {
                                OverallLockdownStatus.FULLY_ACTIVE -> Pair(StatusEnforcedGreen, "ACTIVE")
                                OverallLockdownStatus.PARTIALLY_ACTIVE -> Pair(StatusPartialOrange, "PARTIALLY ACTIVE")
                                OverallLockdownStatus.FAILED -> Pair(StatusAlertRed, "FAILED")
                                OverallLockdownStatus.INACTIVE -> Pair(Slate500, "INACTIVE")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(dotColor, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = statusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = dotColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${deviceInfo.manufacturer} ${deviceInfo.model} (Android ${deviceInfo.androidVersion})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val isOwner = deviceInfo.managementState == DeviceManagementState.DEVICE_OWNER
                        Text(
                            text = "Device Owner: ${if (isOwner) "YES (Active)" else "NO (Unmanaged)"}",
                            fontSize = 13.sp,
                            color = if (isOwner) StatusEnforcedGreen else StatusPartialOrange,
                            fontWeight = FontWeight.Medium
                        )

                        val enforcedCount = lockdownPolicy.controls.values.count { it.enforcedState }
                        val totalControls = lockdownPolicy.controls.size
                        Text(
                            text = "Enforcement: $enforcedCount / $totalControls controls enforced",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            }

            // Device Administrator Activation Banner
            if (deviceInfo.managementState == DeviceManagementState.NORMAL_APP) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusPartialOrangeBg)
                            .border(1.dp, StatusPartialOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = StatusPartialOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "DEVICE ADMINISTRATOR AVAILABLE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPartialOrange,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Activate Device Administrator directly on this phone to enable camera lock and screen capture blocking.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onActivateDeviceAdmin,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusPartialOrange,
                                    contentColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = "ACTIVATE DEVICE ADMINISTRATOR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Controls Grouped by Category
            for (category in Category.values()) {
                val categoryControls = lockdownPolicy.controls.values.filter { it.controlId.category == category }
                if (categoryControls.isNotEmpty()) {
                    item {
                        CategoryHeader(category = category)
                    }
                    for (control in categoryControls) {
                        item(key = control.controlId.name) {
                            ControlItemCard(
                                policy = control,
                                onToggle = { enable ->
                                    if (enable && (control.controlId == ControlId.CAMERA || control.controlId == ControlId.SCREEN_CAPTURE) &&
                                        deviceInfo.managementState == DeviceManagementState.NORMAL_APP
                                    ) {
                                        showAdminPromptDialog = control.controlId
                                    }
                                    onToggleControl(control.controlId, enable)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Confirmation Dialog for Activation
    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = { showActivationDialog = false },
            title = { Text(text = "Activate Lockdown?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "The selected restrictions will be applied to this managed device.\n\n" +
                            "Some controls may be unsupported by Android or this device and will be honestly reported.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showActivationDialog = false
                        onActivateLockdown()
                    }
                ) {
                    Text("ACTIVATE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showActivationDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Confirmation Dialog for Deactivation
    if (showDeactivationDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivationDialog = false },
            title = { Text(text = "Deactivate Lockdown?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "All applied system-wide restrictions will be released and hardware access will be restored.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivationDialog = false
                        onDeactivateLockdown()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAlertRed)
                ) {
                    Text("DEACTIVATE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeactivationDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Prompt Dialog for Device Administrator Activation
    showAdminPromptDialog?.let { targetControl ->
        AlertDialog(
            onDismissRequest = { showAdminPromptDialog = null },
            title = { Text(text = "Device Administrator Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "${targetControl.displayName} requires Device Administrator privilege to enforce system-wide lockdown.\n\n" +
                            "Would you like to activate Device Administrator now directly from this phone?",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdminPromptDialog = null
                        onActivateDeviceAdmin()
                    }
                ) {
                    Text("ACTIVATE NOW")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPromptDialog = null }) {
                    Text("DISMISS")
                }
            }
        )
    }
}

