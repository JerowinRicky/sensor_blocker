package com.sensorlockdown.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.devicepolicy.DeviceManagementState
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.Slate700
import com.sensorlockdown.app.ui.theme.StatusEnforcedGreen
import com.sensorlockdown.app.ui.theme.StatusPartialOrange
import com.sensorlockdown.app.ui.viewmodel.DeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementInfoScreen(
    deviceInfo: DeviceInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOwner = deviceInfo.managementState == DeviceManagementState.DEVICE_OWNER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DEVICE MANAGEMENT SETUP", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
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
            // Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Slate700.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "MANAGEMENT AUTHORITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = deviceInfo.managementState.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOwner) StatusEnforcedGreen else StatusPartialOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = deviceInfo.managementState.description,
                        fontSize = 13.sp,
                        color = Slate500,
                        lineHeight = 18.sp
                    )
                }
            }

            // Provisioning Instructions Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Slate700.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "HOW TO PROVISION DEVICE OWNER (ADB)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Device must be factory reset (clean state with no accounts added).\n" +
                                "2. Enable Developer Options & USB Debugging.\n" +
                                "3. Connect device via USB to your workstation.\n" +
                                "4. Run the following command in terminal:",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate700.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "adb shell dpm set-device-owner com.sensorlockdown.app/.devicepolicy.LockdownDeviceAdminReceiver",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Limitations Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Slate700.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "PLATFORM RESTRICTIONS & TRANSPARENCY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Normal Apps cannot enforce device-wide restrictions on other apps.\n" +
                                "• Standard Android AOSP does NOT expose a public API to disable raw motion sensors (Gyroscope, Accelerometer, Magnetometer).\n" +
                                "• Microphone privacy toggle is restricted to System UI on Android 12+; this app falls back to audio muting.\n" +
                                "• In managed mode, Camera, Screen Capture, USB, Bluetooth, Location, and App Removal are fully enforceable.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

