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
 * Carga un asset de SMW ([relPath], p. ej. `sprites/enemies.png`) desde el ALMACÉN horneado en
 * el dispositivo ([SmwAssetStore], generado desde la ROM del usuario) y, si aún no está, de los
 * `assets/` empaquetados. Es la vía correcta para los gráficos de SMW: en el repo NO va ninguno
 * (son de Nintendo), así que leerlos solo de `assets/` los daría siempre por ausentes (de ahí
 * los cuadros rojos de relleno). Null si no aparece en ninguno de los dos.
 */
fun loadStoreImageBitmap(context: Context, relPath: String): ImageBitmap? =
    com.rolebuilder.editor.snes.SmwAssetStore.open(context, relPath)
        ?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() }

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
