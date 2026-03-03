package com.kai.ghostmesh.core.ui.components

import android.os.Build
import androidx.compose.ui.graphics.ShaderBrush

object GhostShaders {

    private const val NOISE_SHADER_SRC = """
        uniform float2 size;
        uniform float time;

        float rand(float2 co) {
            return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            float n = rand(fragCoord * 0.1 + time * 0.05);
            return half4(half3(n * 0.08), 1.0);
        }
    """

    private const val DISSOLVE_SHADER_SRC = """
        uniform float2 size;
        uniform float progress;
        uniform shader child;

        float rand(float2 co) {
            return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            half4 color = child.eval(fragCoord);
            float n = rand(fragCoord * 0.02);
            float melt = rand(float2(fragCoord.x, 0.0)) * 0.2;
            float threshold = progress + melt * progress;
            float alpha = smoothstep(threshold, threshold + 0.1, n);
            return color * alpha;
        }
    """

    fun createNoiseBrush(time: Float, width: Float, height: Float): ShaderBrush? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = android.graphics.RuntimeShader(NOISE_SHADER_SRC)
            shader.setFloatUniform("size", width, height)
            shader.setFloatUniform("time", time)
            return ShaderBrush(shader)
        }
        return null
    }

    // Note: Returns Any? to avoid direct RuntimeShader reference in return type for older SDKs if used in non-guarded way.
    // However, in Compose, it's safer to keep it as it is but ensure calls are guarded.
    fun createDissolveShader(progress: Float, width: Float, height: Float): Any? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = android.graphics.RuntimeShader(DISSOLVE_SHADER_SRC)
            shader.setFloatUniform("size", width, height)
            shader.setFloatUniform("progress", progress)
            return shader
        }
        return null
    }
}
