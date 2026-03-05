package com.kai.ghostmesh.core.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.jellyClickable(
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = this.pointerInput(enabled) {
    if (enabled) {
        detectTapGestures(onTap = { onClick() })
    }
}

@Composable
fun Modifier.physicalTilt(): Modifier = this
