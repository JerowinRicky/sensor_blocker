package com.sensorlockdown.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.launcher.ui.components.PinDotsIndicator
import com.sensorlockdown.launcher.ui.components.PinKeypad
import com.sensorlockdown.launcher.ui.theme.DarkBackground
import com.sensorlockdown.launcher.ui.theme.DarkSurfaceVariant
import com.sensorlockdown.launcher.ui.theme.PrimaryBlue
import com.sensorlockdown.launcher.ui.theme.StatusRed
import com.sensorlockdown.launcher.ui.theme.TextPrimary
import com.sensorlockdown.launcher.ui.theme.TextSecondary
import com.sensorlockdown.launcher.ui.viewmodel.LauncherScreenState

@Composable
fun PinAuthScreen(
    screenState: LauncherScreenState,
    currentInput: String,
    errorMessage: String?,
    lockoutSeconds: Long,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onCancelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val title: String
    val subtitle: String
    val icon: ImageVector

    when (screenState) {
        LauncherScreenState.PIN_SETUP -> {
            title = "SET UP 6-DIGIT PIN"
            subtitle = "Create a master PIN to secure your launcher and protect app permissions."
            icon = Icons.Default.Shield
        }
        LauncherScreenState.PIN_SETUP_CONFIRM -> {
            title = "CONFIRM 6-DIGIT PIN"
            subtitle = "Re-enter your 6-digit PIN to confirm."
            icon = Icons.Default.Shield
        }
        LauncherScreenState.PIN_LOGIN -> {
            title = "ENTER MASTER PIN"
            subtitle = "Enter your 6-digit PIN to unlock the Secure Launcher."
            icon = Icons.Default.Lock
        }
        LauncherScreenState.SWITCH_DEFAULT_CONFIRM -> {
            title = "CHANGE DEFAULT LAUNCHER"
            subtitle = "Enter your 6-digit PIN to authorize changing back to the phone's default launcher."
            icon = Icons.Default.SwapHoriz
        }
        LauncherScreenState.APP_MANAGEMENT -> {
            title = "MANAGE ALLOWED APPS"
            subtitle = "Enter your 6-digit PIN to modify the allowed apps whitelist."
            icon = Icons.Default.Lock
        }
        LauncherScreenState.POWER_OFF_AUTH -> {
            title = "POWER OFF / RESTART"
            subtitle = "Enter your 6-digit PIN to authorize turning off or restarting the device."
            icon = Icons.Default.PowerSettingsNew
        }
        LauncherScreenState.BLOCKED_APP_AUTH -> {
            title = "APP ACCESS RESTRICTED"
            subtitle = "An unapproved app was blocked from opening via notification. Enter Master PIN to continue."
            icon = Icons.Default.Block
        }
        else -> {
            title = "SECURITY AUTHENTICATION"
            subtitle = "Enter your 6-digit PIN."
            icon = Icons.Default.Lock
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dots
                PinDotsIndicator(
                    length = 6,
                    filledCount = currentInput.length
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error / Lockout Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = StatusRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Keypad Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                PinKeypad(
                    onDigitClick = { if (lockoutSeconds == 0L) onDigitClick(it) },
                    onBackspaceClick = onBackspaceClick,
                    onClearClick = onClearClick
                )

                if (onCancelClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

