plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

// Análisis estático (detekt): audita el código sin necesidad de la ROM. Se apoya en la
// config por defecto + reglas propias en config/detekt/detekt.yml, y usa una baseline para
// "grandfather" la deuda existente: así CI falla solo con problemas NUEVOS.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true) // se sube a GitHub Code Scanning
        xml.required.set(false)
        md.required.set(false)
    }
    jvmTarget = "17"
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

tasks.register<JavaExec>("dumpOverworldDoc") {
    group = "rolebuilder"
    description = "Documenta la capa estática del overworld de SMW (Star Road + eventos) a Markdown"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SmwOverworldDocKt")
    systemProperty("java.awt.headless", "true")
}

// Vuelca los fotogramas de la tabla OAM de los Koopas para MIRAR cuál es el caparazón
// (los 6/7/8), en vez de deducirlo. Necesita la ROM del usuario, que nunca se versiona:
//   ./gradlew :core:dumpShellFrames --args="--rom smw.sfc --level 0x105 --out out/"
tasks.register<JavaExec>("dumpShellFrames") {
    group = "verification"
    description = "Vuelca los fotogramas OAM de los Koopas (el caparazón son los 6/7/8)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.ShellFrameDump")
    systemProperty("java.awt.headless", "true")
}

// Lista los sprites que un nivel coloca de verdad (id, nombre, si lleva caparazón).
tasks.register<JavaExec>("listLevelEnemies") {
    group = "verification"
    description = "Lista los sprites que coloca un nivel de la ROM"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.LevelEnemyList")
    systemProperty("java.awt.headless", "true")
}

// Traza de un enemigo en NUESTRO motor, en el formato CSV del arnés de comparación
// (ver scripts/enemy_trace.py y scripts/enemy_compare.py).
tasks.register<JavaExec>("traceEnemy") {
    group = "verification"
    description = "Traza el movimiento de un enemigo para compararlo con el emulador."
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.EnemyTrace")
}

// Vuelca el sprite de ids concretos tal y como los devuelve core, para separar
// "el grafico de core esta mal" de "el renderer lo dibuja mal".
tasks.register<JavaExec>("dumpEnemy") {
    group = "verification"
    description = "Vuelca el sprite de los ids que se pidan desde la ROM"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.EnemyDump")
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("scanKoopaLevels") {
    group = "verification"
    description = "Busca que niveles colocan Koopas y cuantos"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.KoopaLevelScan")
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("dumpKoopaAppPath") {
    group = "verification"
    description = "Vuelca los Koopas por la MISMA via que usa la app (spriteFrames)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.KoopaAppPathDump")
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("dumpHud") {
    group = "verification"
    description = "Dibuja el HUD REAL de SMW desde la ROM (y su fuente de Layer 3)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.HudDump")
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("dumpKoopaStacked") {
    group = "verification"
    description = "Pinta 0x00-0x07 forzando DOS teselas apiladas (quien lleva caparazon)"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.KoopaStackedDump")
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("dumpScene") {
    group = "verification"
    description = "Dibuja el nivel CON sus enemigos, como deberia verse en la app"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.SceneWithEnemies")
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("auditGfx") {
    group = "verification"
    description = "Cuenta que enemigos se quedan SIN grafico, por frecuencia"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.MissingGfxAudit")
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("cribaGfx") {
    group = "verification"
    description = "Hoja de contacto de los ids sin grafico, para ver cuales se pueden curar"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.GfxTriage")
    systemProperty("java.awt.headless", "true")
}
