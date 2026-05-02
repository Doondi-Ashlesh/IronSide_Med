package com.ironsidemedical.connect

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ironsidemedical.connect.presentation.navigation.IronSideNavGraph
import com.ironsidemedical.connect.presentation.theme.IronSideTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host.
 *
 * FLAG_SECURE prevents screenshots and screen recording - required by
 * FDA cybersecurity guidance for apps that display or store PHI.
 * Jetpack Navigation + Compose handles all screen transitions.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots / screen-capture of PHI
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        setContent {
            IronSideTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IronSideNavGraph()
                }
            }
        }
    }
}
