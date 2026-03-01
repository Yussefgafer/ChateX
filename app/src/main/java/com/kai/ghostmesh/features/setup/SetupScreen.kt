package com.kai.ghostmesh.features.setup

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.ui.components.physicalTilt

@Composable
fun SetupScreen(
    onNewIdentity: () -> Unit,
    onRestoreIdentity: (String) -> Unit
) {
    var showRestoreDialog by remember { mutableStateOf(false) }
    var seedText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "void_pulse")
    val voidAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Void Pulse
        Box(
            modifier = Modifier.size(300.dp).alpha(voidAlpha)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )

        Column(
            modifier = Modifier.padding(32.dp).physicalTilt(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CHATEX",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "PROTOCOL VERSION 2.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(64.dp))

            Text(
                "Enter the Connected Void.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Your identity is derived from entropy. Secure, portable, and permanent.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onNewIdentity,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(16.dp))
                Text("INITIATE NEW GHOST", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showRestoreDialog = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Restore, null)
                Spacer(Modifier.width(16.dp))
                Text("RECLAIM IDENTITY", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("IDENTITY RECLAMATION") },
            text = {
                Column {
                    Text("Paste your 12-word seed phrase below to re-enter the void as your former self.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = seedText,
                        onValueChange = { seedText = it },
                        placeholder = { Text("ghost drift oracle void...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { if(seedText.isNotBlank()) onRestoreIdentity(seedText); showRestoreDialog = false }) {
                    Text("RECLAIM")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("ABORT") }
            }
        )
    }
}
