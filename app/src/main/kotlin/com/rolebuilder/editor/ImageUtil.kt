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
    // En el repo NO va ningún PNG de estos: los sprites de SMW son de Nintendo. Salen del
    // ALMACÉN horneado en el dispositivo desde la ROM del usuario (SmwAssetStore). Si aún no
    // se ha horneado, mapa vacío y quien llame se apaña.
    com.rolebuilder.editor.snes.SmwAssetStore.bigSpriteFiles(context).mapNotNull { (id, file) ->
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.let { id to it }
    }.toMap()
}.getOrDefault(emptyMap())
