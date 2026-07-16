package com.rolebuilder.editor

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.rolebuilder.core.io.ProjectIo
import java.io.File

/** Carga una imagen del proyecto como ImageBitmap, o null si falta. */
fun loadImageBitmap(projectDir: File, name: String): ImageBitmap? {
    val file = ProjectIo.imageFile(projectDir, name)
    if (!file.exists()) return null
    return BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
}

/** Carga una imagen empaquetada en assets/ como ImageBitmap, o null si falta. */
fun loadAssetImageBitmap(context: Context, assetPath: String): ImageBitmap? =
    runCatching {
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }.getOrNull()
