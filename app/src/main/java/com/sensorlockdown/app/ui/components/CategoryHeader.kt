package com.sensorlockdown.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensorlockdown.app.lockdown.model.Category
import com.sensorlockdown.app.ui.theme.Slate500
import com.sensorlockdown.app.ui.theme.Slate700

@Composable
fun CategoryHeader(
    category: Category,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = category.displayName.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate500,
            letterSpacing = 1.sp
        )
        Divider(
            modifier = Modifier.padding(top = 6.dp),
            thickness = 1.dp,
            color = Slate700.copy(alpha = 0.3f)
        )
    }
}

