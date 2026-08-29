package com.praytracker.ui.lock

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.praytracker.R
import com.praytracker.data.settings.Settings
import com.praytracker.util.Hash
import com.praytracker.ui.lock.PasscodePad

@Composable
fun LockScreen(
    settings: Settings,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    var error by remember { mutableStateOf<Boolean>(false) }
    var attempt by remember { mutableStateOf("") }

    val bioPrompt = rememberBiometricPrompt(
        activity = activity,
        onSuccess = { onUnlocked() },
    )

    val biometricAvailable = remember {
        val manager = BiometricManager.from(context)
        manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS ||
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            if (attempt.isEmpty() && error) stringResource(R.string.lock_wrong_passcode) else stringResource(R.string.lock_enter_passcode),
            style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.size(24.dp))

        if (biometricAvailable) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { bioPrompt.launch() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = stringResource(R.string.lock_use_biometric),
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.size(12.dp))
            TextButton(onClick = { bioPrompt.launch() }) {
                Text(stringResource(R.string.lock_use_biometric))
            }
            Spacer(Modifier.size(8.dp))
        }

        PasscodePad(
            onDigit = { digit ->
                val next = if (attempt.length < 4) attempt + digit else attempt
                if (next.length == 4) {
                    val hash = Hash.sha256(next)
                    if (hash == settings.appLockPasscodeHash) {
                        onUnlocked()
                    } else {
                        error = true
                        attempt = ""
                    }
                } else {
                    attempt = next
                }
            },
            onDelete = { attempt = attempt.dropLast(1) },
        )
    }
}

@Composable
private fun PasscodePad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "back")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        digits.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { label ->
                    if (label.isEmpty()) {
                        Spacer(Modifier.size(64.dp))
                    } else if (label == "back") {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable(onClick = onDelete),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("⌫", fontSize = 24.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onDigit(label) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 26.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
): androidx.activity.compose.ManagedActivityResultLauncher<BiometricPrompt.PromptInfo, BiometricPrompt.AuthenticationResult> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.BiometricPrompt(),
    ) { result ->
        onSuccess()
    }
}