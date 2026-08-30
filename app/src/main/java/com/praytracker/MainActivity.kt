package com.praytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.praytracker.ui.MainViewModel
import com.praytracker.ui.components.OnboardingDialog
import com.praytracker.ui.screens.AboutScreen
import com.praytracker.ui.screens.MoreScreen
import com.praytracker.ui.screens.PrayerTimesScreen
import com.praytracker.ui.screens.QiblaScreen
import com.praytracker.ui.screens.RamadanScreen
import com.praytracker.ui.screens.SettingsScreen
import com.praytracker.ui.screens.TasbihScreen
import com.praytracker.ui.theme.PrayerTimesTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsChanged by viewModel.settings.settingsChanged.collectAsState()
            // Theme is computed inside a flow at emission time (fresh getter), so the
            // value composing state here always matches the last settings write.
            val themeMode by viewModel.settings.settingsChanged
                .map { viewModel.settings.appTheme }
                .distinctUntilChanged()
                .collectAsState(initial = viewModel.settings.appTheme)
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            PrayerTimesTheme(darkTheme = isDarkTheme) {
                // Reading and passing down settingsChanged subscribes this root scope to
                // every settings write so preference getters are re-sampled on change.
                MainApp(viewModel = viewModel, settingsChanged = settingsChanged)
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun MainApp(viewModel: MainViewModel, settingsChanged: Long) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSubPage by viewModel.currentMoreSubPage.collectAsState()

    var showOnboarding by remember { mutableStateOf(viewModel.settings.isFirstLaunch) }

    if (showOnboarding) {
        OnboardingDialog(
            viewModel = viewModel,
            onDismiss = { showOnboarding = false }
        )
    }

    // Subpage navigation routing
    when (currentSubPage) {
        "SETTINGS" -> {
            BackHandler { viewModel.selectMoreSubPage(null) }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectMoreSubPage(null) },
                onNavigateToAbout = { viewModel.selectMoreSubPage("ABOUT") }
            )
        }
        "RAMADAN" -> {
            BackHandler { viewModel.selectMoreSubPage(null) }
            RamadanScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectMoreSubPage(null) }
            )
        }
        "ABOUT" -> {
            BackHandler { viewModel.selectMoreSubPage(null) }
            AboutScreen(
                onBack = { viewModel.selectMoreSubPage(null) }
            )
        }
        else -> {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            icon = { Icon(imageVector = Icons.Default.Schedule, contentDescription = "Prayers") },
                            label = { Text("Prayers", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_prayers")
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            icon = { Icon(imageVector = Icons.Default.TouchApp, contentDescription = "Tasbih") },
                            label = { Text("Tasbih", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_tasbih")
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            icon = { Icon(imageVector = Icons.Default.CompassCalibration, contentDescription = "Qibla") },
                            label = { Text("Qibla", fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_qibla")
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { viewModel.selectTab(3) },
                            icon = { Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More") },
                            label = { Text("More", fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_more")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        0 -> PrayerTimesScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { viewModel.selectMoreSubPage("SETTINGS") },
                            onNavigateToRamadan = { viewModel.selectMoreSubPage("RAMADAN") }
                        )
                        1 -> TasbihScreen(
                            viewModel = viewModel
                        )
                        2 -> QiblaScreen(
                            viewModel = viewModel
                        )
                        3 -> MoreScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { viewModel.selectMoreSubPage("SETTINGS") },
                            onNavigateToRamadan = { viewModel.selectMoreSubPage("RAMADAN") },
                            onNavigateToAbout = { viewModel.selectMoreSubPage("ABOUT") }
                        )
                    }
                }
            }
        }
    }
}
