package com.rolebuilder.player.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Batch de quads texturizados en 3D para el HD-2D real. Vértice = pos(3) +
 * uv(2) + color(4) = 9 floats.
 *
 * Tres formas de emitir:
 *  - [draw]: quad tumbado en el plano del suelo (Y=0), en casillas
 *    (x, y-sur → x, 0, y). Tiles, sombras, charcos de luz, efectos de suelo.
 *  - [drawBillboard]: quad de PIE anclado por la base a un punto del suelo,
 *    orientado a la cámara. No se estira: la perspectiva lo hace. Personajes,
 *    árboles, casas.
 *  - [screenOverlay]: quad a pantalla completa (tinte, destello).
 */
class SpriteBatch(private val maxQuads: Int = 4096) {

    private val shader = Shader(
        """
        #version 300 es
        layout(location = 0) in vec3 aPos;
        layout(location = 1) in vec2 aUv;
        layout(location = 2) in vec4 aColor;
        uniform mat4 uVp;
        out vec2 vUv;
        out vec4 vColor;
        void main() {
            vUv = aUv;
            vColor = aColor;
            gl_Position = uVp * vec4(aPos, 1.0);
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
            vec4 c = texture(uTex, vUv) * vColor;
            if (c.a < 0.02) discard; // recorta el fondo transparente para el z-buffer
            outColor = c;
        }
        """.trimIndent(),
    )

    private val uVp = shader.uniform("uVp")
    private val uTex = shader.uniform("uTex")

    private val vertices = FloatArray(maxQuads * 4 * FLOATS_PER_VERTEX)
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private val indexBuffer: ShortBuffer

    private var quadCount = 0
    private var texture: Texture? = null
    private var vp = FloatArray(16)
    private val identity = FloatArray(16).also { android.opengl.Matrix.setIdentityM(it, 0) }
    private var additive = false
    private var screenSpace = false

    private val camRight = floatArrayOf(1f, 0f, 0f)
    private val camUp = floatArrayOf(0f, 1f, 0f)

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

    fun begin(vpMatrix: FloatArray, right: FloatArray, up: FloatArray) {
        vp = vpMatrix
        right.copyInto(camRight)
        up.copyInto(camUp)
        quadCount = 0
        texture = null
        additive = false
        screenSpace = false
    }

    /** Cambia entre mezcla alfa normal y aditiva (luces). Vacía lo pendiente. */
    fun setAdditive(value: Boolean) {
        if (additive == value) return
        flush()
        additive = value
    }

    /**
     * Quad tumbado en el suelo (Y=0). [x], [y] en casillas (y = sur).
     * [rotation] gira alrededor del centro dentro del plano del suelo.
     */
    fun draw(
        tex: Texture,
        x: Float, y: Float, w: Float, h: Float,
        u0: Float = 0f, v0: Float = 0f, u1: Float = 1f, v1: Float = 1f,
        r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f,
        flipX: Boolean = false,
        rotation: Float = 0f,
    ) {
        prepare(tex)
        val uA = if (flipX) u1 else u0
        val uB = if (flipX) u0 else u1
        var i = quadCount * 4 * FLOATS_PER_VERTEX
        if (rotation == 0f) {
            i = putVertex(i, x, 0f, y, uA, v0, r, g, b, a)
            i = putVertex(i, x + w, 0f, y, uB, v0, r, g, b, a)
            i = putVertex(i, x + w, 0f, y + h, uB, v1, r, g, b, a)
            putVertex(i, x, 0f, y + h, uA, v1, r, g, b, a)
        } else {
            val cx = x + w / 2f
            val cz = y + h / 2f
            val cos = kotlin.math.cos(rotation)
            val sin = kotlin.math.sin(rotation)
            val hw = w / 2f
            val hh = h / 2f
            fun px(dx: Float, dz: Float) = cx + dx * cos - dz * sin
            fun pz(dx: Float, dz: Float) = cz + dx * sin + dz * cos
            i = putVertex(i, px(-hw, -hh), 0f, pz(-hw, -hh), uA, v0, r, g, b, a)
            i = putVertex(i, px(hw, -hh), 0f, pz(hw, -hh), uB, v0, r, g, b, a)
            i = putVertex(i, px(hw, hh), 0f, pz(hw, hh), uB, v1, r, g, b, a)
            putVertex(i, px(-hw, hh), 0f, pz(-hw, hh), uA, v1, r, g, b, a)
        }
        quadCount++
    }

    /**
     * Billboard de pie orientado a la cámara: ancho [w] y alto [h] en
     * casillas, con la BASE centrada en el punto del suelo ([cx], [cy])
     * elevada [baseY] casillas (0 = a ras de suelo).
     */
    fun drawBillboard(
        tex: Texture,
        cx: Float, cy: Float, w: Float, h: Float,
        baseY: Float = 0f,
        u0: Float = 0f, v0: Float = 0f, u1: Float = 1f, v1: Float = 1f,
        r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f,
        flipX: Boolean = false,
    ) {
        prepare(tex)
        val uA = if (flipX) u1 else u0
        val uB = if (flipX) u0 else u1
        val hw = w / 2f
        val bx = cx; val by = baseY; val bz = cy
        val rx = camRight[0] * hw; val ry = camRight[1] * hw; val rz = camRight[2] * hw
        val ux = camUp[0] * h; val uy = camUp[1] * h; val uz = camUp[2] * h
        var i = quadCount * 4 * FLOATS_PER_VERTEX
        // tl, tr, br, bl
        i = putVertex(i, bx - rx + ux, by - ry + uy, bz - rz + uz, uA, v0, r, g, b, a)
        i = putVertex(i, bx + rx + ux, by + ry + uy, bz + rz + uz, uB, v0, r, g, b, a)
        i = putVertex(i, bx + rx, by + ry, bz + rz, uB, v1, r, g, b, a)
        putVertex(i, bx - rx, by - ry, bz - rz, uA, v1, r, g, b, a)
        quadCount++
    }

    /** Quad a pantalla completa en espacio de recorte (tinte, destello). */
    fun screenOverlay(tex: Texture, r: Float, g: Float, b: Float, a: Float) {
        flush()
        screenSpace = true
        prepare(tex)
        var i = quadCount * 4 * FLOATS_PER_VERTEX
        i = putVertex(i, -1f, 1f, 0f, 0f, 0f, r, g, b, a)
        i = putVertex(i, 1f, 1f, 0f, 1f, 0f, r, g, b, a)
        i = putVertex(i, 1f, -1f, 0f, 1f, 1f, r, g, b, a)
        putVertex(i, -1f, -1f, 0f, 0f, 1f, r, g, b, a)
        quadCount++
        flush()
        screenSpace = false
    }

    private fun prepare(tex: Texture) {
        if (texture !== tex) {
            flush()
            texture = tex
        }
        if (quadCount >= maxQuads) flush()
    }

    private fun putVertex(i: Int, x: Float, y: Float, z: Float, u: Float, v: Float, r: Float, g: Float, b: Float, a: Float): Int {
        vertices[i] = x
        vertices[i + 1] = y
        vertices[i + 2] = z
        vertices[i + 3] = u
        vertices[i + 4] = v
        vertices[i + 5] = r
        vertices[i + 6] = g
        vertices[i + 7] = b
        vertices[i + 8] = a
        return i + FLOATS_PER_VERTEX
    }

    fun end() = flush()

    /** Vacía lo pendiente; úsalo para separar pasadas con distinto estado GL. */
    fun flush() {
        val tex = texture ?: return
        if (quadCount == 0) return

        shader.use()
        GLES30.glUniformMatrix4fv(uVp, 1, false, if (screenSpace) identity else vp, 0)
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
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer.position(3)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer.position(5)
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
        private const val FLOATS_PER_VERTEX = 9
    }
}
