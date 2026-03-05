package com.kai.ghostmesh.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

object GhostMotion {
    /**
     * Standard Material 3 Emphasized Easing
     */
    val EmphasizedEasing = CubicBezierEasing(0.05f, 0.0f, 0.133333f, 0.06f) // Custom Profile: M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1

    /**
     * Standard Morph Spring: 200f Stiffness, 0.6f Damping (per docs/MD3E.md)
     */
    val MorphSpring = spring<Float>(
        stiffness = 200f,
        dampingRatio = 0.6f
    )

    /**
     * Standard Tactile Spring: Medium-High stiffness for responsive clicks.
     */
    val TactileSpring = spring<Float>(
        stiffness = 1500f,
        dampingRatio = 0.85f
    )

    val TactileSpringDp = spring<Dp>(
        stiffness = 1500f,
        dampingRatio = 0.85f
    )

    /**
     * Mass/Proximity Spring: Low stiffness for physical simulation.
     */
    val MassSpring = spring<Float>(
        stiffness = Spring.StiffnessLow,
        dampingRatio = 0.85f
    )

    val MassSpringDp = spring<Dp>(
        stiffness = Spring.StiffnessLow,
        dampingRatio = 0.85f
    )
}
