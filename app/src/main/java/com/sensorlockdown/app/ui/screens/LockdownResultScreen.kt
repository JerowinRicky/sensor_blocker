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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.lockdown.model.EnforcementStatus
import com.sensorlockdown.app.lockdown.model.LockdownPolicy
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.Slate700
import com.sensorlockdown.app.ui.theme.StatusAlertRed
import com.sensorlockdown.app.ui.theme.StatusEnforcedGreen
import com.sensorlockdown.app.ui.theme.StatusPartialOrange

@Composable
fun LockdownResultScreen(
    policy: LockdownPolicy,
    onReturnToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalRequested = policy.controls.values.count { it.requestedState }
    val enforced = policy.controls.values.count { it.status == EnforcementStatus.ENFORCED }
    val partial = policy.controls.values.count { it.status == EnforcementStatus.PARTIALLY_ENFORCED }
    val unsupported = policy.controls.values.count { it.status == EnforcementStatus.UNSUPPORTED }
    val notPresent = policy.controls.values.count { it.status == EnforcementStatus.NOT_PRESENT || it.status == EnforcementStatus.NOT_AVAILABLE }
    val errors = policy.controls.values.count { it.status == EnforcementStatus.ERROR }

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

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = StatusEnforcedGreen,
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LOCKDOWN ACTIVE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Device policies have been applied and verified.",
                fontSize = 14.sp,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Slate700.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultRow(label = "Controls Requested", count = totalRequested, color = MaterialTheme.colorScheme.onSurface)
                    Divider(color = Slate700.copy(alpha = 0.2f))
                    ResultRow(label = "Verified Enforced", count = enforced, color = StatusEnforcedGreen)
                    ResultRow(label = "Partially Enforced", count = partial, color = StatusPartialOrange)
                    ResultRow(label = "Unsupported by Android/OEM", count = unsupported, color = Slate500)
                    ResultRow(label = "Hardware Not Present", count = notPresent, color = Slate500)
                    if (errors > 0) {
                        ResultRow(label = "Policy Application Errors", count = errors, color = StatusAlertRed)
                    }
                }
            }
        }

        Button(
            onClick = onReturnToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "RETURN TO DASHBOARD", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun ResultRow(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = count.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

