package com.arie.recomp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.arie.recomp.data.Graph
import com.arie.recomp.ui.AppRoot
import com.arie.recomp.ui.theme.RecompTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        // Health Connect deep-links here to show why we read health data.
        val privacyRequested =
            intent?.action == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" ||
                intent?.action == "android.intent.action.VIEW_PERMISSION_USAGE"
        setContent {
            RecompTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(showPrivacyInitially = privacyRequested)
                }
            }
        }
    }
}
