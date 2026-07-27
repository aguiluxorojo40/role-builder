package com.rolebuilder.core.snes

/**
 * CATÁLOGO de extracción de SMW: todo lo que la app sabe **localizar y descargar** de la ROM,
 * ya clasificado y **separado por animación**.
 *
 * La idea del proyecto es que en el repositorio van solo las coordenadas y el cómo decodificar;
 * la ROM del usuario es la que tiene los datos. Este catálogo es el índice de esa extracción:
 * organiza los sprites en **grupos** (Mario, enemigos…), cada grupo en **items** (un sprite
 * concreto), y cada item en **clips de animación** ([AnimationClip]) — cada clip es una
 * secuencia de fotogramas que, al exportar, va a **su propia subcarpeta**
 * (`grupo/item/animación/`), con los PNG de cada fotograma y un GIF de la animación.
 *
 * Aquí, en `core` (Kotlin puro y testable), se producen las IMÁGENES y la estructura. La
 * codificación a PNG y el volcado a fichero/ZIP los hace quien llame (la app), porque dependen
 * de Android; el GIF sí sale de aquí ([Gif]).
 */
object SmwAssetCatalog {

    /**
     * Una ANIMACIÓN: su nombre (será el de la subcarpeta) y sus fotogramas en orden. Un clip
     * de un solo fotograma es una pose fija (no lleva GIF al exportar).
     */
    data class AnimationClip(val name: String, val frames: List<ArgbImage>, val delayCs: Int = 12) {
        val animated: Boolean get() = frames.size > 1
    }

    /** Un SPRITE concreto (Mario, un Koopa…) con todas sus animaciones. */
    data class AssetItem(val name: String, val clips: List<AnimationClip>)

    /** Un GRUPO de sprites de la misma familia (Mario, enemigos…). */
    data class AssetGroup(val name: String, val items: List<AssetItem>)

    /** Nivel del que se toman los GFX de sprites de enemigo (pradera genérica). */
    private const val ENEMY_REF_LEVEL = SmwBakedAssets.REF_LEVEL

    /**
     * Construye el catálogo entero desde la ROM. Cada grupo que no se pueda decodificar se
     * omite en silencio (ROM que no es SMW → lista vacía).
     */
    fun build(rom: ByteArray, header: SnesHeader): List<AssetGroup> {
        val groups = ArrayList<AssetGroup>()
        marioOverworld(rom, header)?.let { groups.add(it) }
        enemies(rom, header)?.let { groups.add(it) }
        return groups
    }

    /** Grupo "Mario (mapa)": el Mario del overworld, una animación de andar por dirección. */
    fun marioOverworld(rom: ByteArray, header: SnesHeader): AssetGroup? {
        val dirs = listOf(
            "andar_arriba" to SmwOverworldWalk.DIR_UP,
            "andar_abajo" to SmwOverworldWalk.DIR_DOWN,
            "andar_izquierda" to SmwOverworldWalk.DIR_LEFT,
            "andar_derecha" to SmwOverworldWalk.DIR_RIGHT,
        )
        val clips = dirs.mapNotNull { (name, dir) ->
            val frames = (0 until SnesGameRecipes.OW_MARIO_FRAMES).map { f ->
                SnesGameRecipes.overworldMarioSprite(rom, header, dir, f) ?: return@mapNotNull null
            }
            AnimationClip(name, frames)
        }
        if (clips.isEmpty()) return null
        return AssetGroup("Mario (mapa)", listOf(AssetItem("Mario", clips)))
    }

    /**
     * Grupo "Enemigos": un item por cada enemigo del catálogo curado, con su animación (los
     * que se mueven traen 2 fotogramas; los demás, una pose). Se dibujan con los GFX del nivel
     * de referencia. Un enemigo que no se pueda dibujar se omite.
     */
    fun enemies(rom: ByteArray, header: SnesHeader): AssetGroup? {
        val items = SmwEnemyGraphics.curatedIds.mapNotNull { id ->
            val n = SmwEnemyGraphics.animFrameCount(id)
            val frames = runCatching {
                SmwEnemyGraphics.spriteFrames(rom, header, ENEMY_REF_LEVEL, id, n)
            }.getOrNull().orEmpty()
            if (frames.isEmpty()) return@mapNotNull null
            val clipName = if (frames.size > 1) "anim" else "quieto"
            AssetItem(
                name = SmwEnemyGraphics.nameOf(id) ?: "sprite_%02X".format(id),
                clips = listOf(AnimationClip(clipName, frames)),
            )
        }
        if (items.isEmpty()) return null
        return AssetGroup("Enemigos", items)
    }

    /**
     * Un fichero a exportar: su ruta relativa dentro del paquete (con subcarpetas) y de dónde
     * sale la imagen. Es lo que la app recorre para escribir el ZIP/carpeta clasificada.
     *
     * [gif] es null para los PNG de cada fotograma; para el GIF de una animación, [image] es
     * null y [gif] trae los bytes ya codificados. Así la app solo tiene que codificar los PNG.
     */
    data class ExportEntry(val path: String, val image: ArgbImage?, val gif: ByteArray?)

    /**
     * Aplana [groups] a la lista de ficheros a escribir, con la estructura de carpetas
     * `grupo/item/animación/`. Cada clip aporta un PNG por fotograma (`frame_00.png`…) y, si
     * está animado, un GIF (`animación.gif`). Los nombres se sanean para ser válidos como
     * carpeta (sin acentos ni espacios).
     */
    fun exportEntries(groups: List<AssetGroup>): List<ExportEntry> {
        val out = ArrayList<ExportEntry>()
        for (g in groups) for (item in g.items) for (clip in item.clips) {
            val dir = "${slug(g.name)}/${slug(item.name)}/${slug(clip.name)}"
            clip.frames.forEachIndexed { i, img ->
                out.add(ExportEntry("$dir/frame_%02d.png".format(i), img, null))
            }
            if (clip.animated) {
                val w = clip.frames[0].width
                val h = clip.frames[0].height
                val gif = Gif.encode(w, h, clip.frames.map { Gif.Frame(it.pixels, clip.delayCs) })
                out.add(ExportEntry("$dir/${slug(clip.name)}.gif", null, gif))
            }
        }
        return out
    }

    /** Nombre válido como carpeta: minúsculas, sin acentos, espacios y símbolos a `_`. */
    fun slug(name: String): String {
        val stripped = name
            .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')
            .replace('Á', 'A').replace('É', 'E').replace('Í', 'I').replace('Ó', 'O').replace('Ú', 'U')
            .replace('ñ', 'n').replace('Ñ', 'N')
        return buildString {
            for (c in stripped.lowercase()) {
                append(if (c.isLetterOrDigit()) c else '_')
            }
        }.trim('_').replace(Regex("_+"), "_")
    }
}
