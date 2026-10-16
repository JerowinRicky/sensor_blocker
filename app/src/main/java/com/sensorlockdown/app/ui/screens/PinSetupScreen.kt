package com.sensorlockdown.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.authentication.model.AuthResult
import com.sensorlockdown.app.ui.components.PinInputView
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.StatusAlertRed

@Composable
fun PinSetupScreen(
    onPinCreated: () -> Unit,
    onSubmitPin: (String, String) -> AuthResult,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) } // 1: Create, 2: Confirm
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
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
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (step == 1) "Create Security PIN" else "Confirm Security PIN",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (step == 1) "Enter a 6-digit PIN to secure lockdown controls." else "Re-enter your 6-digit PIN to confirm.",
                fontSize = 14.sp,
                color = Slate500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = StatusAlertRed,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (step == 1) {
                PinInputView(
                    pin = pin,
                    onPinChange = {
                        pin = it
                        errorMessage = null
                    }
                )
            } else {
                PinInputView(
                    pin = confirmPin,
                    onPinChange = {
                        confirmPin = it
                        errorMessage = null
                    }
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            if (step == 1) {
                Button(
                    onClick = {
                        if (pin.length != 6) {
                            errorMessage = "PIN must be exactly 6 digits."
                        } else {
                            errorMessage = null
                            step = 2
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = pin.length == 6,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "CONTINUE", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        val result = onSubmitPin(pin, confirmPin)
                        when (result) {
                            is AuthResult.Success -> onPinCreated()
                            is AuthResult.ValidationError -> errorMessage = result.message
                            is AuthResult.InvalidPin -> errorMessage = "Invalid PIN."
                            is AuthResult.LockedOut -> errorMessage = "System locked out for ${result.lockoutSecondsRemaining}s."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = confirmPin.length == 6,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "CREATE PIN", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        step = 1
                        confirmPin = ""
                        errorMessage = null
                    }
                ) {
                    Text(text = "Back to PIN entry", fontSize = 13.sp)
                }
            }
        }
    }
}

