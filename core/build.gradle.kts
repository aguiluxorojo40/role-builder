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

// Traza de un enemigo en NUESTRO motor, en el formato CSV del arnés de comparación
// (ver scripts/enemy_trace.py y scripts/enemy_compare.py).
tasks.register<JavaExec>("traceEnemy") {
    group = "verification"
    description = "Traza el movimiento de un enemigo para compararlo con el emulador."
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.EnemyTrace")
}
