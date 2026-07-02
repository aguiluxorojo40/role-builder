package com.rolebuilder.player.gl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import java.io.File

/** Textura 2D con filtrado NEAREST (pixel-art). */
class Texture(bitmap: Bitmap) {

    val id: Int
    val width: Int = bitmap.width
    val height: Int = bitmap.height

    init {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        id = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    fun bind(unit: Int = 0) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
    }

    fun dispose() {
        GLES30.glDeleteTextures(1, intArrayOf(id), 0)
    }

    companion object {
        /** Textura 1x1 blanca para quads de color plano. */
        fun white(): Texture {
            val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            bmp.setPixel(0, 0, android.graphics.Color.WHITE)
            return Texture(bmp).also { bmp.recycle() }
        }

        /** Carga un PNG del disco; si falta, devuelve un tablero magenta. */
        fun fromFile(file: File): Texture {
            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: missingBitmap()
            return Texture(bmp).also { bmp.recycle() }
        }

        private fun missingBitmap(): Bitmap {
            val bmp = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            bmp.setPixel(0, 0, android.graphics.Color.MAGENTA)
            bmp.setPixel(1, 1, android.graphics.Color.MAGENTA)
            bmp.setPixel(1, 0, android.graphics.Color.BLACK)
            bmp.setPixel(0, 1, android.graphics.Color.BLACK)
            return bmp
        }
    }
}
