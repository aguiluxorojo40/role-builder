package com.rolebuilder.player.gl

import android.opengl.GLES30
import android.util.Log

/** Programa GLSL compilado. */
class Shader(vertexSrc: String, fragmentSrc: String) {

    val program: Int

    init {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vs)
            GLES30.glAttachShader(it, fs)
            GLES30.glLinkProgram(it)
            val status = IntArray(1)
            GLES30.glGetProgramiv(it, GLES30.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                Log.e("Shader", "Error al enlazar: ${GLES30.glGetProgramInfoLog(it)}")
            }
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
    }

    fun use() = GLES30.glUseProgram(program)

    fun attrib(name: String): Int = GLES30.glGetAttribLocation(program, name)

    fun uniform(name: String): Int = GLES30.glGetUniformLocation(program, name)

    private fun compile(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("Shader", "Error al compilar: ${GLES30.glGetShaderInfoLog(shader)}")
        }
        return shader
    }
}
