package com.praytracker.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.praytracker.PrayerTrackerApp
import com.praytracker.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onStart: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerTrackerApp
    val scope = rememberCoroutineScope()

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        scope.launch {
            val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (granted) {
                val location = app.container.locationProvider.requestLocation()
                if (location != null) {
                    val label = app.container.locationProvider.reverseGeocode(location)
                    app.container.settingsRepository.update {
                        it.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            locationLabel = label ?: "Detected location",
                            lastLocationTimeMs = System.currentTimeMillis(),
                        )
                    }
                }
            }
            onStart()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(16.dp))
        Text(
            stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(40.dp))
        Button(
            onClick = {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_location_permission))
        }
        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onStart) {
            Text(stringResource(R.string.onboarding_skip))
        }
    }
}