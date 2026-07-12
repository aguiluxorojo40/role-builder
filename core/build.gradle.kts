plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

sourceSets {
    create("tools") {
        java.srcDir("src/tools/kotlin")
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    "toolsImplementation"(sourceSets.main.get().output)
    "toolsImplementation"(libs.kotlinx.serialization.json)
}

// Genera el proyecto por defecto (tileset, sprites y JSON) dentro de los assets de :app.
tasks.register<JavaExec>("generateDefaultAssets") {
    group = "rolebuilder"
    description = "Genera los assets del proyecto por defecto en app/src/main/assets"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.AssetGeneratorKt")
    systemProperty("java.awt.headless", "true")
    args(rootDir.resolve("app/src/main/assets/default_project").absolutePath)
}

// Extrae una hoja de tiles (PNG + Tileset JSON) desde una ROM de SNES usando el
// decodificador de :core. Pasa los argumentos con --args, por ejemplo:
//   ./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out out --offset 0x2000 --format 4bpp"
//   ./gradlew :core:extractSnesTileset --args="--demo out"
tasks.register<JavaExec>("extractSnesTileset") {
    group = "rolebuilder"
    description = "Extrae assets gráficos de una ROM de SNES (usa --args para las opciones)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SnesExtractorKt")
    systemProperty("java.awt.headless", "true")
}

// Herramienta de validación de SFX y sprites de SMW contra la ROM real. Ejemplo:
//   ./gradlew :core:probeSmw --args="--rom smw.sfc --out probe --level 0x105"
tasks.register<JavaExec>("probeSmw") {
    group = "rolebuilder"
    description = "Valida SFX (voz real) y sprites (tabla OAM) de SMW desde una ROM (usa --args)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SmwProbeKt")
    systemProperty("java.awt.headless", "true")
}

// Renderer OAM fiel: compone un sprite de SMW desde sus entradas OAM reales (para
// reconstruir sprites grandes tal y como los dibuja el juego). Ejemplo:
//   ./gradlew :core:composeSmwSprite --args="--rom smw.sfc --level 0x106 --out spr.png --spec '0xA0,0,0,16,0x00'"
tasks.register<JavaExec>("composeSmwSprite") {
    group = "rolebuilder"
    description = "Compone un sprite de SMW desde entradas OAM reales (usa --args)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SmwOamComposerKt")
    systemProperty("java.awt.headless", "true")
}

// Rehornea el atlas de enemigos (assets/sprites/enemies.png) desde una ROM de SMW, en
// el orden de SmwEnemyGraphics.curatedIds. Ejecútalo al ampliar el roster:
//   ./gradlew :core:bakeSmwEnemies --args="--rom smw.sfc"
tasks.register<JavaExec>("bakeSmwEnemies") {
    group = "rolebuilder"
    description = "Rehornea assets/sprites/enemies.png desde una ROM de SMW (usa --args)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SmwEnemyAtlasBakerKt")
    systemProperty("java.awt.headless", "true")
    workingDir = rootDir
}

// Vuelca qué enemigos hay en cada nivel de SMW a docs/enemigos_por_nivel.md:
//   ./gradlew :core:dumpLevelEnemies --args="--rom smw.sfc"
tasks.register<JavaExec>("dumpLevelEnemies") {
    group = "rolebuilder"
    description = "Informe de enemigos por nivel de SMW desde la ROM (usa --args)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SmwLevelEnemiesKt")
    systemProperty("java.awt.headless", "true")
    workingDir = rootDir
}

// Busca el mejor nivel de horneado (paleta canónica) para cada sprite grande:
//   ./gradlew :core:scoutBigSpritePalettes --args="--rom smw.sfc --out scout [--only 0x9F]"
tasks.register<JavaExec>("scoutBigSpritePalettes") {
    group = "rolebuilder"
    description = "Lámina por sprite grande con su render en los niveles donde aparece (usa --args)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SmwBigSpritePaletteScoutKt")
    systemProperty("java.awt.headless", "true")
}

// Hornea los sprites GRANDES (multi-tesela) a assets/sprites/big/big_<id>.png:
//   ./gradlew :core:bakeSmwBigSprites --args="--rom smw.sfc"
tasks.register<JavaExec>("bakeSmwBigSprites") {
    group = "rolebuilder"
    description = "Hornea los sprites grandes de SMW a assets/sprites/big (usa --args)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SmwBigSpriteBakerKt")
    systemProperty("java.awt.headless", "true")
    workingDir = rootDir
}
