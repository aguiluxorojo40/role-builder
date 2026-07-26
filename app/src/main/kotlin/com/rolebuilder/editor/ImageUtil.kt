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

/**
 * Carga los sprites GRANDES empaquetados (assets/sprites/big/big_<id>.png): id de sprite
 * (hex del nombre) → ImageBitmap. Son los jefes y enemigos "grandes" (Bowser, Reznor, Big
 * Boo, Dino-Torch…) que no caben en el atlas cuadrado `enemies.png`. El mismo conjunto que
 * el motor dibuja al jugar (ver `PlatformerActivity.loadBigSprites`). Vacío si no hay carpeta.
 */
fun loadBigSprites(context: Context): Map<Int, ImageBitmap> = runCatching {
    // OJO: en este repo NO se versiona ningún PNG aquí. Los sprites de SMW son material
    // con copyright de Nintendo; la app los extrae de la ROM del usuario en el dispositivo.
    // Sin ficheros esto devuelve un mapa vacío y quien llame se apaña, que es lo correcto.
    (context.assets.list("sprites/big") ?: emptyArray()).mapNotNull { name ->
        val id = Regex("big_([0-9a-fA-F]+)\\.png").matchEntire(name)?.groupValues?.get(1)
            ?.toInt(16) ?: return@mapNotNull null
        loadAssetImageBitmap(context, "sprites/big/$name")?.let { id to it }
    }.toMap()
}.getOrDefault(emptyMap())
