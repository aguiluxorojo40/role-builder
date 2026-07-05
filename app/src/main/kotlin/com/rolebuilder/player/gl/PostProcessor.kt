package com.rolebuilder.player.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Cadena de post-procesado "HD-2D": la escena se dibuja en un FBO y se
 * compone en pantalla con desenfoque tilt-shift (efecto maqueta), bloom,
 * viñeta y un etalonaje cálido de contraste suave.
 *
 * Pasadas: escena → extracción de brillos a 1/4 → desenfoque gaussiano
 * horizontal y vertical → composición final (tilt-shift + bloom + grading).
 */
class PostProcessor {

    private var width = 0
    private var height = 0
    private var ready = false

    // FBO de escena a resolución completa y par de FBOs de bloom a 1/4.
    private val fbos = IntArray(3)
    private val texs = IntArray(3)
    // Renderbuffer de profundidad del FBO de escena (para el z-buffer 3D).
    private val depthRbo = IntArray(1)
    private var bloomW = 1
    private var bloomH = 1

    private val quad: FloatBuffer = ByteBuffer
        .allocateDirect(16 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(
            floatArrayOf(
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f,
            ),
        )

    private val brightShader = Shader(
        FULLSCREEN_VS,
        """
        #version 300 es
        precision mediump float;
        in vec2 vUv;
        uniform sampler2D uTex;
        out vec4 outColor;
        void main() {
            vec3 c = texture(uTex, vUv).rgb;
            // Solo lo más luminoso alimenta el bloom.
            vec3 bright = max(c - vec3(0.55), vec3(0.0)) * 1.8;
            outColor = vec4(bright, 1.0);
        }
        """.trimIndent(),
    )

    private val blurShader = Shader(
        FULLSCREEN_VS,
        """
        #version 300 es
        precision mediump float;
        in vec2 vUv;
        uniform sampler2D uTex;
        uniform vec2 uDir; // desplazamiento de un texel en la dirección del blur
        out vec4 outColor;
        void main() {
            vec3 sum = texture(uTex, vUv).rgb * 0.227;
            sum += texture(uTex, vUv + uDir * 1.385).rgb * 0.316;
            sum += texture(uTex, vUv - uDir * 1.385).rgb * 0.316;
            sum += texture(uTex, vUv + uDir * 3.231).rgb * 0.070;
            sum += texture(uTex, vUv - uDir * 3.231).rgb * 0.070;
            outColor = vec4(sum, 1.0);
        }
        """.trimIndent(),
    )

    private val composeShader = Shader(
        FULLSCREEN_VS,
        """
        #version 300 es
        precision mediump float;
        in vec2 vUv;
        uniform sampler2D uScene;
        uniform sampler2D uBloom;
        uniform float uTexelY;
        uniform float uStrength; // 0 = sin efecto, 1 = normal, 2 = exagerado
        out vec4 outColor;
        void main() {
            float s = uStrength;
            float s01 = clamp(s * 0.5, 0.0, 1.0);

            // Tilt-shift: nítido en la franja central, borroso arriba y abajo.
            // Con más intensidad, la franja nítida se estrecha y el blur crece.
            float band = abs(vUv.y - 0.5) * 2.0;
            float focus = mix(0.55, 0.12, s01);
            float blur = smoothstep(focus, 1.0, band);
            float o = uTexelY * blur * 5.0 * s;
            vec3 scene = texture(uScene, vUv).rgb * 0.294;
            scene += texture(uScene, vUv + vec2(0.0, o)).rgb * 0.235;
            scene += texture(uScene, vUv - vec2(0.0, o)).rgb * 0.235;
            scene += texture(uScene, vUv + vec2(0.0, o * 2.2)).rgb * 0.118;
            scene += texture(uScene, vUv - vec2(0.0, o * 2.2)).rgb * 0.118;

            vec3 c = scene + texture(uBloom, vUv).rgb * 0.65 * s;

            // Etalonaje cálido con algo de contraste, proporcional.
            c *= mix(vec3(1.0), vec3(1.06, 1.0, 0.92), s01);
            c = (c - 0.5) * (1.0 + 0.08 * s) + 0.5;

            // Viñeta.
            float d = distance(vUv, vec2(0.5));
            c *= 1.0 - smoothstep(0.55, 0.95, d) * 0.35 * s;

            outColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """.trimIndent(),
    )

    private val uBlurTex = blurShader.uniform("uTex")
    private val uBlurDir = blurShader.uniform("uDir")
    private val uBrightTex = brightShader.uniform("uTex")
    private val uScene = composeShader.uniform("uScene")
    private val uBloom = composeShader.uniform("uBloom")
    private val uTexelY = composeShader.uniform("uTexelY")
    private val uStrength = composeShader.uniform("uStrength")

    /** Intensidad global del efecto (0 = nada, 1 = normal, 2 = exagerado). */
    var strength = 1f

    /** (Re)crea los FBOs al cambiar el tamaño de la superficie. */
    fun resize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        if (ready && w == width && h == height) return
        release()
        width = w
        height = h
        bloomW = (w / 4).coerceAtLeast(1)
        bloomH = (h / 4).coerceAtLeast(1)
        GLES30.glGenFramebuffers(3, fbos, 0)
        GLES30.glGenTextures(3, texs, 0)
        createTarget(0, width, height)
        // Profundidad para el FBO de escena (fbos[0]): sin ella el z-buffer
        // no funciona al dibujar la escena 3D a textura.
        GLES30.glGenRenderbuffers(1, depthRbo, 0)
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, depthRbo[0])
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, width, height)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[0])
        GLES30.glFramebufferRenderbuffer(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, depthRbo[0],
        )
        createTarget(1, bloomW, bloomH)
        createTarget(2, bloomW, bloomH)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        ready = true
    }

    private fun createTarget(index: Int, w: Int, h: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texs[index])
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, w, h, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null,
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[index])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, texs[index], 0,
        )
    }

    val enabled: Boolean get() = ready

    /** Redirige el dibujado de la escena al FBO interno. */
    fun beginScene() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[0])
        GLES30.glViewport(0, 0, width, height)
    }

    /** Ejecuta la cadena de post-procesado y deja el resultado en pantalla. */
    fun compose() {
        GLES30.glDisable(GLES30.GL_BLEND)

        // 1. Brillos de la escena, a 1/4 de resolución.
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[1])
        GLES30.glViewport(0, 0, bloomW, bloomH)
        brightShader.use()
        bindTex(0, texs[0])
        GLES30.glUniform1i(uBrightTex, 0)
        drawQuad()

        // 2. Desenfoque separable: horizontal (1 → 2) y vertical (2 → 1).
        blurShader.use()
        GLES30.glUniform1i(uBlurTex, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[2])
        bindTex(0, texs[1])
        GLES30.glUniform2f(uBlurDir, 1f / bloomW, 0f)
        drawQuad()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[1])
        bindTex(0, texs[2])
        GLES30.glUniform2f(uBlurDir, 0f, 1f / bloomH)
        drawQuad()

        // 3. Composición final en el framebuffer de pantalla.
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        composeShader.use()
        bindTex(0, texs[0])
        bindTex(1, texs[1])
        GLES30.glUniform1i(uScene, 0)
        GLES30.glUniform1i(uBloom, 1)
        GLES30.glUniform1f(uTexelY, 1f / height)
        GLES30.glUniform1f(uStrength, strength.coerceIn(0f, 2f))
        drawQuad()

        GLES30.glEnable(GLES30.GL_BLEND)
    }

    private fun bindTex(unit: Int, id: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
    }

    private fun drawQuad() {
        quad.position(0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, quad)
        quad.position(2)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, quad)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun release() {
        if (!ready) return
        GLES30.glDeleteFramebuffers(3, fbos, 0)
        GLES30.glDeleteTextures(3, texs, 0)
        GLES30.glDeleteRenderbuffers(1, depthRbo, 0)
        ready = false
    }

    companion object {
        private val FULLSCREEN_VS = """
            #version 300 es
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aUv;
            out vec2 vUv;
            void main() {
                vUv = aUv;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """.trimIndent()
    }
}
