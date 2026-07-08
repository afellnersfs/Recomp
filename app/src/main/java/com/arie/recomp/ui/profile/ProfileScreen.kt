package com.arie.recomp.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arie.recomp.BuildConfig
import com.arie.recomp.ui.GlassCard
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.openUrl
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.update.UpdateChecker

@Composable
fun ProfileScreen(nav: NavHostController) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineLarge)

        LinkCard(
            icon = Icons.Filled.PhotoCamera,
            title = "Body tracking",
            subtitle = "Measurements, trends, progress photos",
            onClick = { nav.navigate("body") }
        )
        LinkCard(
            icon = Icons.Filled.Restaurant,
            title = "Fuel & water",
            subtitle = "Calories and hydration log",
            onClick = { nav.navigate("fuel") }
        )
        LinkCard(
            icon = Icons.Filled.Settings,
            title = "Settings",
            subtitle = "Program, reminders, Shabbat mode, equipment, backup",
            onClick = { nav.navigate("settings") }
        )

        GlassCard {
            SectionLabel("About")
            Spacer(Modifier.padding(4.dp))
            Text("Recomp v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
            Text(
                "Personal build — data lives on this phone only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Check for updates",
                style = MaterialTheme.typography.bodyMedium,
                color = Accent,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        openUrl(context, "https://github.com/afellnersfs/Recomp/releases/latest")
                    }
            )
        }
    }
}

@Composable
private fun LinkCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
