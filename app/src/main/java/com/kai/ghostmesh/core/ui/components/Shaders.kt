package com.kai.ghostmesh.core.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createNoiseBrush(time: Float, width: Float, height: Float): ShaderBrush {
        val shader = RuntimeShader(NOISE_SHADER_SRC)
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("time", time)
        return ShaderBrush(shader)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createDissolveShader(progress: Float, width: Float, height: Float): RuntimeShader {
        val shader = RuntimeShader(DISSOLVE_SHADER_SRC)
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("progress", progress)
        return shader
    }
}
