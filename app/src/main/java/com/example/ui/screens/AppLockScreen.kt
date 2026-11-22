package com.example.ui.screens

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.VerdantThemeTokens

@Composable
fun AppLockScreen(
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    val context = LocalContext.current
    val extended = VerdantThemeTokens.colors
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun launchBiometricPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val prompt = BiometricPrompt.Builder(context)
                    .setTitle("Unlock Verdant")
                    .setSubtitle("Verify your identity to access financial data")
                    .setNegativeButton("Use PIN", context.mainExecutor) { _, _ -> }
                    .build()
                prompt.authenticate(
                    CancellationSignal(),
                    context.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            onBiometricSuccess()
                        }
                    }
                )
            } catch (_: Exception) {
                // Fallback gracefully to 4-digit PIN
            }
        }
    }

    LaunchedEffect(Unit) {
        launchBiometricPrompt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_verdant_icon_1795332177129),
            contentDescription = "Verdant Logo",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Verdant Locked",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Enter your 4-digit PIN or use biometrics",
            style = MaterialTheme.typography.bodySmall,
            color = extended.mutedText
        )
        Spacer(Modifier.height(28.dp))

        // 4 PIN Dots
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(4) { idx ->
                val filled = idx < pin.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary else extended.border
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = extended.expense
            )
        } else {
            Spacer(Modifier.height(28.dp))
        }

        // Minimal Keypad
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("BIO", "0", "DEL")
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { key ->
                        Surface(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable {
                                    when (key) {
                                        "DEL" -> {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            errorMessage = null
                                        }
                                        "BIO" -> launchBiometricPrompt()
                                        else -> {
                                            if (pin.length < 4) {
                                                val next = pin + key
                                                pin = next
                                                errorMessage = null
                                                if (next.length == 4) {
                                                    if (!onVerifyPin(next)) {
                                                        errorMessage = "Incorrect PIN. Try again."
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, extended.border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (key) {
                                    "DEL" -> Icon(
                                        Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    "BIO" -> Icon(
                                        Icons.Outlined.Fingerprint,
                                        contentDescription = "Use Biometrics",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    else -> Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { launchBiometricPrompt() },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, extended.border)
            ) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock with Biometrics")
            }
        }
    }
}
