package com.rolebuilder.core.snes

/**
 * Genera desde la ROM del usuario los assets de SMW que ANTES venían empaquetados en el repo.
 *
 * ## Por qué existe esto
 * El proyecto se apoya en una idea sencilla: en el repositorio van **solo las coordenadas y el
 * cómo decodificar**, nunca datos de Nintendo. Aun así se habían colado en `app/src/main/
 * assets/` los gráficos de Mario, el atlas de enemigos, los powerups, la moneda, los sprites
 * grandes de jefes y los efectos de sonido — todos sacados del ROM. Quitarlos sin más habría
 * costado funcionalidad en el modo PROYECTO (el que se juega sin ROM), que era el único que
 * los usaba: el modo ROM ya generaba todo en vivo.
 *
 * La solución es hornearlos **en el dispositivo** a partir de la ROM del usuario, una sola vez,
 * y guardarlos fuera del repositorio. Aquí está la parte de `core` (Kotlin puro y testable):
 * producir las imágenes. Quien llame se encarga de escribirlas donde toque.
 *
 * ## El atlas de enemigos
 * Su geometría no es libre: el renderer lo indexa por [SmwEnemyGraphics.curatedIds] (una
 * columna por id, en ese orden) y [SmwEnemyGraphics.ATLAS_FRAMES] fotogramas apilados en
 * vertical, con celdas de [SmwEnemyGraphics.ATLAS_CELL] px. Se respeta exactamente para que el
 * atlas horneado encaje con el que espera el renderer.
 */
object SmwBakedAssets {

    /**
     * Nivel del que se toman los GFX de sprites. Se usa 0x106 (`YOSHI'S ISLAND 2`) porque su
     * conjunto de gráficos de sprites es el genérico de pradera, que cubre a los enemigos
     * comunes del atlas. Es el mismo criterio con el que se horneaban antes.
     */
    const val REF_LEVEL = 0x106

    /** Ruta (relativa, como en los antiguos assets) → imagen generada. */
    fun images(rom: ByteArray, header: SnesHeader, level: Int = REF_LEVEL): Map<String, ArgbImage> {
        val out = LinkedHashMap<String, ArgbImage>()
        // Mario en sus cuatro estados, con el MISMO índice de powerup que usa el reproductor:
        // 0 pequeño, 1 grande, 2 capa, 3 fuego.
        val marioFiles = listOf(
            "sprites/mario.png" to 0,
            "sprites/mario_big.png" to 1,
            "sprites/mario_cape.png" to 2,
            "sprites/mario_fire.png" to 3,
        )
        for ((path, powerup) in marioFiles) {
            SnesGameRecipes.smwMarioSheet(rom, header, powerup)?.let { out[path] = it }
        }
        SnesGameRecipes.smwCoinSheet(rom, header, level)?.let { out["sprites/coin.png"] = it }
        SmwEnemyGraphics.powerupSheet(rom, header, level)?.let { out["sprites/powerups.png"] = it }
        enemyAtlas(rom, header, level)?.let { out["sprites/enemies.png"] = it }
        shellSheet(rom, header, level)?.let { out["sprites/shells.png"] = it }
        return out
    }

    /**
     * Hoja de CAPARAZONES: una fila por Koopa con caparazón (0x04-0x07) y una columna por
     * fotograma del ciclo de giro, en celdas de 16×16.
     *
     * El caparazón NO cabe en el atlas de enemigos —ese tiene 2 fotogramas por id, y aquí
     * hacen falta 4— y hasta ahora solo existía en la ruta ROM en vivo: en modo PROYECTO, que
     * tira de los assets horneados, el caparazón seguía siendo el domo de color pintado a
     * mano. Con esta hoja, las dos rutas enseñan el caparazón de verdad.
     */
    fun shellSheet(rom: ByteArray, header: SnesHeader, level: Int = REF_LEVEL): ArgbImage? {
        val ids = (0x04..0x07).toList()
        val frames = SmwEnemyGraphics.SHELL_SPIN_FRAMES.size
        val img = ArgbImage(frames * SHELL_CELL, ids.size * SHELL_CELL)
        var any = false
        ids.forEachIndexed { fila, id ->
            val fr = runCatching { SmwEnemyGraphics.shellFrames(rom, header, level, id) }
                .getOrNull().orEmpty()
            if (fr.isEmpty()) return@forEachIndexed
            for (c in 0 until frames) {
                blit(fr.getOrNull(c) ?: fr.last(), img, c * SHELL_CELL, fila * SHELL_CELL)
                any = true
            }
        }
        return if (any) img else null
    }

    /** Lado de la celda de [shellSheet]: el caparazón es UNA tesela 16×16. */
    const val SHELL_CELL = 16

    /**
     * Atlas de enemigos con la geometría EXACTA que espera el renderer: una columna por cada
     * id de [SmwEnemyGraphics.curatedIds] y [SmwEnemyGraphics.ATLAS_FRAMES] fotogramas
     * apilados. Null si no se puede dibujar ninguno.
     */
    fun enemyAtlas(rom: ByteArray, header: SnesHeader, level: Int = REF_LEVEL): ArgbImage? {
        val ids = SmwEnemyGraphics.curatedIds
        val cell = SmwEnemyGraphics.ATLAS_CELL
        val rows = SmwEnemyGraphics.ATLAS_FRAMES
        if (ids.isEmpty()) return null
        val atlas = ArgbImage(ids.size * cell, rows * cell)
        var any = false
        ids.forEachIndexed { col, id ->
            val frames = runCatching {
                SmwEnemyGraphics.spriteFrames(rom, header, level, id, rows)
            }.getOrNull().orEmpty()
            if (frames.isEmpty()) return@forEachIndexed
            for (r in 0 until rows) {
                // Si un enemigo trae menos fotogramas de los que pide el atlas, se repite el
                // último: mejor que dejar el hueco en blanco y que parpadee al animar.
                val src = frames.getOrNull(r) ?: frames.last()
                blit(src, atlas, col * cell, r * cell)
                any = true
            }
        }
        return if (any) atlas else null
    }

    /**
     * Los sprites GRANDES (jefes y enemigos que no caben en la celda del atlas): id → imagen.
     * Antes se empaquetaban como `sprites/big/big_<id>.png`.
     */
    fun bigSprites(rom: ByteArray, header: SnesHeader, level: Int = REF_LEVEL): Map<Int, ArgbImage> {
        val out = LinkedHashMap<Int, ArgbImage>()
        for (id in SmwEnemyGraphics.curatedIds) {
            if (!SmwEnemyGraphics.isTall(id)) continue
            // ⚠ AQUÍ HABÍA UN FALLO QUE SOLO SE VEÍA JUGANDO. Se usaba [spriteImage], que
            // devuelve SIEMPRE 16×16 —un solo bloque—, para ids que son ALTOS por
            // definición: `isTall` es el bit 0x40 de `Spr0to13Prop`, o sea "se dibuja como
            // DOS teselas 16×16 apiladas". Los ocho Koopa con caparazón (0x04-0x0B) se
            // horneaban a media altura, así que en el modo PROYECTO —el único que tira de
            // estos ficheros— salía medio Koopa flotando.
            //
            // No se notaba en el modo ROM porque ahí el reproductor calcula los fotogramas
            // en vivo con [SmwEnemyGraphics.spriteFrames], que sí monta el 16×32. Los dos
            // caminos daban cosas distintas para el mismo enemigo; ahora usan la misma
            // fuente de verdad.
            val frames = runCatching {
                if (SmwEnemyGraphics.isWinged(id)) {
                    SmwEnemyGraphics.wingedKoopaFrames(rom, header, level, id)
                } else {
                    SmwEnemyGraphics.spriteFrames(rom, header, level, id)
                }
            }.getOrNull()
            val img = frames?.firstOrNull()
                ?: runCatching { SmwEnemyGraphics.spriteImage(rom, header, level, id) }.getOrNull()
            if (img != null) out[id] = img
        }
        // Los de DIBUJO PROPIO (Rex, Pokey, Thwomp, las plataformas de guía…) NO están en
        // [SmwEnemyGraphics.curatedIds], así que este bucle es la única vía por la que
        // llegan al dispositivo. Sin él solo existían si alguien los horneaba a mano con
        // `extractSnesTileset --custom-enemy`, y en el móvil salían como rectángulo.
        for (id in SmwEnemyGraphics.customEnemyIds) {
            if (id in out) continue
            val img = runCatching { SmwEnemyGraphics.customEnemyImage(rom, header, level, id) }.getOrNull()
            if (img != null) out[id] = img
        }
        return out
    }

    /** Copia [src] sobre [dst] en (ox, oy) respetando la transparencia. */
    private fun blit(src: ArgbImage, dst: ArgbImage, ox: Int, oy: Int) {
        for (y in 0 until src.height) for (x in 0 until src.width) {
            val p = src.get(x, y)
            if (p ushr 24 == 0) continue
            val dx = ox + x; val dy = oy + y
            if (dx in 0 until dst.width && dy in 0 until dst.height) dst.set(dx, dy, p)
        }
    }
}
