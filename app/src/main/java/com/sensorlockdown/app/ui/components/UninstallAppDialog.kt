package com.sensorlockdown.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.authentication.model.AuthResult
import com.sensorlockdown.app.ui.theme.StatusAlertRed

@Composable
fun UninstallAppDialog(
    title: String = "Authorize Uninstallation",
    description: String = "Enter your 6-digit security PIN to authorize deactivation and complete uninstallation of Sensor Lockdown.",
    onConfirmUninstall: (String) -> AuthResult,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                PinInputView(
                    pin = enteredPin,
                    onPinChange = {
                        enteredPin = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = StatusAlertRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin.length != 6) {
                        errorMessage = "Please enter full 6-digit PIN"
                        return@Button
                    }
                    when (val result = onConfirmUninstall(enteredPin)) {
                        is AuthResult.Success -> {
                            onDismiss()
                        }
                        is AuthResult.InvalidPin -> {
                            errorMessage = "Incorrect PIN. Attempts remaining: ${result.remainingAttemptsBeforeLockout}"
                        }
                        is AuthResult.LockedOut -> {
                            errorMessage = "Locked out for ${result.lockoutSecondsRemaining}s"
                        }
                        is AuthResult.ValidationError -> {
                            errorMessage = result.message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusAlertRed)
            ) {
                Text("VERIFY & UNINSTALL")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
