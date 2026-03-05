package com.kai.ghostmesh.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

object GhostMotion {
    /**
     * Standard Material 3 Emphasized Easing
     */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

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
