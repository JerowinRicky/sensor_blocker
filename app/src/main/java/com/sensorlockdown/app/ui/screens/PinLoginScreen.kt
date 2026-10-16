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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.authentication.model.AuthResult
import com.sensorlockdown.app.authentication.model.RateLimitState
import com.sensorlockdown.app.ui.components.PinInputView
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.StatusAlertRed
import com.sensorlockdown.app.ui.theme.StatusAlertRedBg

@Composable
fun PinLoginScreen(
    rateLimitState: RateLimitState,
    onLoginSubmit: (String) -> AuthResult,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "LOCKDOWN ACTIVE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your 6-digit PIN to access device controls.",
                fontSize = 14.sp,
                color = Slate500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (rateLimitState.isLockedOut) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusAlertRedBg)
                        .border(1.dp, StatusAlertRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Too many failed attempts.\nTry again in ${rateLimitState.secondsRemaining} seconds.",
                        color = StatusAlertRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = StatusAlertRed,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            PinInputView(
                pin = pin,
                onPinChange = {
                    if (!rateLimitState.isLockedOut) {
                        pin = it
                        errorMessage = null
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    val result = onLoginSubmit(pin)
                    when (result) {
                        is AuthResult.Success -> {
                            onLoginSuccess()
                        }
                        is AuthResult.InvalidPin -> {
                            pin = ""
                            errorMessage = "Incorrect PIN. ${result.remainingAttemptsBeforeLockout} attempts remaining before temporary lockout."
                        }
                        is AuthResult.LockedOut -> {
                            pin = ""
                            errorMessage = "System locked out for ${result.lockoutSecondsRemaining}s."
                        }
                        is AuthResult.ValidationError -> {
                            errorMessage = result.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = pin.length == 6 && !rateLimitState.isLockedOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "ENTER", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

