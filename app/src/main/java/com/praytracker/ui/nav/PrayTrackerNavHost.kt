package com.praytracker.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.praytracker.ui.about.AboutScreen
import com.praytracker.ui.backup.BackupScreen
import com.praytracker.ui.history.HistoryScreen
import com.praytracker.ui.more.MoreScreen
import com.praytracker.ui.qibla.QiblaScreen
import com.praytracker.ui.ramadan.RamadanScreen
import com.praytracker.ui.settings.SettingsScreen
import com.praytracker.ui.tasbih.TasbihScreen
import com.praytracker.ui.today.TodayScreen

@Composable
fun PrayTrackerNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in BottomBarDestinations.topLevelRoutes) {
                AppBottomBar(navController, currentRoute)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.TODAY) {
                TodayScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen()
            }
            composable(Routes.TASBIH) {
                TasbihScreen()
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenQibla = { navController.navigate(Routes.QIBLA) },
                    onOpenRamadan = { navController.navigate(Routes.RAMADAN) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.QIBLA) {
                QiblaScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.RAMADAN) {
                RamadanScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                )
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    navController: androidx.navigation.NavHostController,
    currentRoute: String?,
) {
    NavigationBar {
        BottomBarDestinations.all.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    showMoreSettings: Boolean = false,
    onSettings: () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showMoreSettings) {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Settings")
                        }
                    }
                },
            )
        },
        snackbarHost = { snackbarHost() },
    ) { padding ->
        content(padding)
    }
}