package com.praytracker.ui.lock

import android.os.Build
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
import java.util.concurrent.Executors

@Composable
fun LockScreen(
    settings: Settings,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    var error by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf("") }

    val textAppName = stringResource(R.string.app_name)
    val textEnter = stringResource(R.string.lock_enter_passcode)
    val textWrong = stringResource(R.string.lock_wrong_passcode)
    val textBiometric = stringResource(R.string.lock_use_biometric)
    val textCancel = stringResource(R.string.lock_cancel)

    val biometricPrompt = remember { createBiometricPrompt(activity, onUnlocked, textCancel) }
    val promptInfo = remember(textBiometric, textCancel) {
        createPromptInfo(textBiometric, textCancel)
    }

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
            textAppName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            if (error) textWrong else textEnter,
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
                    .clickable { biometricPrompt.authenticate(promptInfo) },
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
            TextButton(onClick = { biometricPrompt.authenticate(promptInfo) }) {
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

private fun createBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    cancelText: String,
): BiometricPrompt {
    val executor = Executors.newSingleThreadExecutor()
    return BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
}

private fun createPromptInfo(title: String, cancelText: String): BiometricPrompt.PromptInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
            .setConfirmationRequired(false)
            .build()
    } else {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(cancelText)
            .build()
    }