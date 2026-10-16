package com.sensorlockdown.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.lockdown.model.EnforcementStatus
import com.sensorlockdown.app.ui.theme.StatusAlertRed
import com.sensorlockdown.app.ui.theme.StatusAlertRedBg
import com.sensorlockdown.app.ui.theme.StatusEnforcedGreen
import com.sensorlockdown.app.ui.theme.StatusEnforcedGreenBg
import com.sensorlockdown.app.ui.theme.StatusPartialOrange
import com.sensorlockdown.app.ui.theme.StatusPartialOrangeBg
import com.sensorlockdown.app.ui.theme.StatusUnsupportedGray
import com.sensorlockdown.app.ui.theme.StatusUnsupportedGrayBg

@Composable
fun SecurityStatusBadge(
    status: EnforcementStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text) = when (status) {
        EnforcementStatus.ENFORCED -> Triple(StatusEnforcedGreenBg, StatusEnforcedGreen, "ENFORCED ✓")
        EnforcementStatus.PARTIALLY_ENFORCED -> Triple(StatusPartialOrangeBg, StatusPartialOrange, "PARTIAL ⚠")
        EnforcementStatus.REQUESTED -> Triple(StatusUnsupportedGrayBg, StatusUnsupportedGray, "REQUESTED")
        EnforcementStatus.UNSUPPORTED -> Triple(StatusUnsupportedGrayBg, StatusUnsupportedGray, "UNSUPPORTED")
        EnforcementStatus.NOT_AVAILABLE -> Triple(StatusUnsupportedGrayBg, StatusUnsupportedGray, "NOT AVAILABLE")
        EnforcementStatus.NOT_PRESENT -> Triple(StatusUnsupportedGrayBg, StatusUnsupportedGray, "NOT PRESENT")
        EnforcementStatus.ERROR -> Triple(StatusAlertRedBg, StatusAlertRed, "ERROR ✕")
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

