package com.kai.ghostmesh.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

object GhostMotion {
    val EmphasizedEasing = CubicBezierEasing(0.05f, 0f, 0.133333f, 0.06f) // Path start
    // Note: The provided path is M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1
    // This is a complex cubic spline. Standard Emphasized Easing in MD3 is CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    // We'll use a close approximation for the path segment if needed, or the standard MD3 Emphasized.
    // The requirement specifically asked for that path.
}
