package com.sensorlockdown.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.lockdown.model.ControlPolicy
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.Slate700

@Composable
fun ControlItemCard(
    policy: ControlPolicy,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isActionable = policy.supportedState

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = Slate700.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = policy.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                SecurityStatusBadge(status = policy.status)
            }

            Switch(
                checked = policy.requestedState,
                onCheckedChange = { if (isActionable) onToggle(it) },
                enabled = isActionable
            )
        }

        if (policy.reason != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = policy.reason,
                fontSize = 12.sp,
                color = Slate500,
                lineHeight = 16.sp
            )
        }
    }
}

