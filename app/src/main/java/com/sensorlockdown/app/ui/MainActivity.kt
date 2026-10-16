package com.sensorlockdown.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.sensorlockdown.app.ui.navigation.LockdownNavGraph
import com.sensorlockdown.app.ui.theme.SensorLockdownTheme
import com.sensorlockdown.app.ui.viewmodel.LockdownViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LockdownViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply FLAG_SECURE window shield to prevent screenshots / screen scraping
        if (viewModel.getScreenCaptureProtection()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContent {
            SensorLockdownTheme {
                val navController = rememberNavController()
                LockdownNavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRateLimitState()
        viewModel.refreshPolicy()
    }
}

