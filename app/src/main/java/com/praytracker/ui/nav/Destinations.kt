package com.praytracker.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val TODAY = "today"
    const val HISTORY = "history"
    const val TASBIH = "tasbih"
    const val MORE = "more"
    const val QIBLA = "qibla"
    const val RAMADAN = "ramadan"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val ABOUT = "about"
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Only icons from material-icons-core are used to keep the app small.
 */
object BottomBarDestinations {
    val today = BottomDestination(Routes.TODAY, "Today", Icons.Outlined.DateRange)
    val history = BottomDestination(Routes.HISTORY, "History", Icons.Outlined.List)
    val tasbih = BottomDestination(Routes.TASBIH, "Tasbih", Icons.Outlined.Favorite)
    val more = BottomDestination(Routes.MORE, "More", Icons.Outlined.MoreVert)
    val all = listOf(today, history, tasbih, more)
    val topLevelRoutes = all.map { it.route }
}