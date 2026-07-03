package com.rolebuilder.player.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Batch de quads texturizados: acumula vértices y los dibuja de una vez.
 * Vértice = pos(2) + uv(2) + color(4) = 8 floats.
 */
class SpriteBatch(private val maxQuads: Int = 4096) {

    private val shader = Shader(
        """
        #version 300 es
        layout(location = 0) in vec2 aPos;
        layout(location = 1) in vec2 aUv;
        layout(location = 2) in vec4 aColor;
        uniform mat4 uMvp;
        out vec2 vUv;
        out vec4 vColor;
        void main() {
            vUv = aUv;
            vColor = aColor;
            gl_Position = uMvp * vec4(aPos, 0.0, 1.0);
        }
        """.trimIndent(),
        """
        #version 300 es
        precision mediump float;
        in vec2 vUv;
        in vec4 vColor;
        uniform sampler2D uTex;
        out vec4 outColor;
        void main() {
            outColor = texture(uTex, vUv) * vColor;
        }
        """.trimIndent(),
    )

    private val uMvp = shader.uniform("uMvp")
    private val uTex = shader.uniform("uTex")

    private val vertices = FloatArray(maxQuads * 4 * FLOATS_PER_VERTEX)
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private val indexBuffer: ShortBuffer

    private var quadCount = 0
    private var texture: Texture? = null
    private var mvp: FloatArray = FloatArray(16)
    private var additive = false

    init {
        val indices = ShortArray(maxQuads * 6)
        for (q in 0 until maxQuads) {
            val v = q * 4
            val i = q * 6
            indices[i] = v.toShort()
            indices[i + 1] = (v + 1).toShort()
            indices[i + 2] = (v + 2).toShort()
            indices[i + 3] = v.toShort()
            indices[i + 4] = (v + 2).toShort()
            indices[i + 5] = (v + 3).toShort()
        }
        indexBuffer = ByteBuffer
            .allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
        indexBuffer.position(0)
    }

    fun begin(mvpMatrix: FloatArray) {
        mvp = mvpMatrix
        quadCount = 0
        texture = null
        additive = false
    }

    /** Cambia entre mezcla alfa normal y aditiva (luces). Vacía lo pendiente. */
    fun setAdditive(value: Boolean) {
        if (additive == value) return
        flush()
        additive = value
    }

    /** Dibuja un rectángulo (x, y) - (x+w, y+h) con las UV dadas y tinte. */
    fun draw(
        tex: Texture,
        x: Float, y: Float, w: Float, h: Float,
        u0: Float = 0f, v0: Float = 0f, u1: Float = 1f, v1: Float = 1f,
        r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f,
        flipX: Boolean = false,
    ) {
        if (texture !== tex) {
            flush()
            texture = tex
        }
        if (quadCount >= maxQuads) flush()

        val uA = if (flipX) u1 else u0
        val uB = if (flipX) u0 else u1
        var i = quadCount * 4 * FLOATS_PER_VERTEX
        // 0: arriba-izquierda, 1: arriba-derecha, 2: abajo-derecha, 3: abajo-izquierda
        i = putVertex(i, x, y, uA, v0, r, g, b, a)
        i = putVertex(i, x + w, y, uB, v0, r, g, b, a)
        i = putVertex(i, x + w, y + h, uB, v1, r, g, b, a)
        putVertex(i, x, y + h, uA, v1, r, g, b, a)
        quadCount++
    }

    private fun putVertex(i: Int, x: Float, y: Float, u: Float, v: Float, r: Float, g: Float, b: Float, a: Float): Int {
        vertices[i] = x
        vertices[i + 1] = y
        vertices[i + 2] = u
        vertices[i + 3] = v
        vertices[i + 4] = r
        vertices[i + 5] = g
        vertices[i + 6] = b
        vertices[i + 7] = a
        return i + FLOATS_PER_VERTEX
    }

    fun end() = flush()

    private fun flush() {
        val tex = texture ?: return
        if (quadCount == 0) return

        shader.use()
        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        tex.bind(0)
        GLES30.glUniform1i(uTex, 0)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(
            GLES30.GL_SRC_ALPHA,
            if (additive) GLES30.GL_ONE else GLES30.GL_ONE_MINUS_SRC_ALPHA,
        )

        vertexBuffer.position(0)
        vertexBuffer.put(vertices, 0, quadCount * 4 * FLOATS_PER_VERTEX)

        val stride = FLOATS_PER_VERTEX * 4
        vertexBuffer.position(0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer.position(2)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer.position(4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, stride, vertexBuffer)

        indexBuffer.position(0)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, quadCount * 6, GLES30.GL_UNSIGNED_SHORT, indexBuffer)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
        quadCount = 0
    }

    companion object {
        private const val FLOATS_PER_VERTEX = 8
    }
}
