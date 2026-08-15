plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.pitest)
}

// Mutation testing: "tests que testean los tests". Pitest muta el bytecode de
// :core (invierte condiciones, borra llamadas...) y re-ejecuta la suite; cada
// mutante que sobrevive es una zona donde los tests pasan aunque el código
// esté roto. Informe en core/build/reports/pitest (lo publica el CI).
pitest {
    targetClasses.set(listOf("com.rolebuilder.core.*"))
    // El generador de assets usa AWT y no lo cubre la suite: fuera del análisis.
    excludedClasses.set(listOf("com.rolebuilder.core.tools.*"))
    threads.set(4)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    // Medimos sin bloquear el build: el umbral se decidirá con datos reales.
    failWhenNoMutations.set(false)
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
    // Las SONDAS de auditoría van aparte de la suite, y no por manía de orden: vivían
    // en src/test con @Test pero SIN UN SOLO assert —2.862 líneas y 211 println— así
    // que corrían en cada `:core:test`, tardaban, y salían en verde pasara lo que
    // pasara. Un verde que no verifica nada es peor que no tener el test, porque el
    // informe deja de significar lo que parece. Aquí siguen siendo lo que son: una
    // herramienta para MIRAR datos de la ROM, que se lanza a mano (ver la tarea
    // `sondas`) y que no cuenta como cobertura de nada.
    create("sondas") {
        java.srcDir("src/sondas/kotlin")
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    "toolsImplementation"(sourceSets.main.get().output)
    "toolsImplementation"(libs.kotlinx.serialization.json)
    // Las sondas siguen usando @Test como forma cómoda de lanzarse una a una.
    // Hace falta la variante -junit y no el `kotlin("test")` a secas: el alias
    // `kotlin.test.Test` sobre JUnit4 vive ahí, y la selección automática de variante
    // que hace el plugin solo se aplica al source set `test`.
    "sondasImplementation"(sourceSets.main.get().output)
    "sondasImplementation"(kotlin("test-junit"))
    "sondasImplementation"(libs.junit)
}

// Las sondas leen tripas de `main` declaradas `internal` (SmwLayer1 y compañía). El
// source set `test` las ve por ser "friend module" de `main`; uno nuevo no, salvo que se
// asocien las compilaciones explícitamente. Sin esto habría que abrir a `public` medio
// paquete snes solo para que unas sondas de usar y tirar compilen, que es al revés de
// como debe mandar el diseño.
kotlin.target.compilations.getByName("sondas")
    .associateWith(kotlin.target.compilations.getByName("main"))

/**
 * Lanza las sondas de auditoría. Necesitan la ROM del usuario, que nunca se versiona:
 *
 *     SMW_ROM=/ruta/smw.sfc ./gradlew :core:sondas
 *
 * Sin `SMW_ROM` cada sonda se salta sola y no imprime nada, que es justo el motivo por
 * el que no pueden vivir en la suite: sin ROM no ejercitan absolutamente nada.
 */
tasks.register<Test>("sondas") {
    group = "verification"
    description = "Ejecuta las sondas de auditoría de la ROM (necesitan SMW_ROM)"
    testClassesDirs = sourceSets["sondas"].output.classesDirs
    classpath = sourceSets["sondas"].runtimeClasspath
    // Su producto es lo que imprimen: sin esto Gradle se lo traga.
    testLogging.showStandardStreams = true
}

// Se COMPILAN en cada `:core:test` aunque no se ejecuten. Sacarlas de la suite corría el
// riesgo de dejarlas pudrirse en silencio con el primer refactor —que es peor que el
// problema de partida—, y compilarlas cuesta segundos.
tasks.named("test") {
    dependsOn("compileSondasKotlin")
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

tasks.register<JavaExec>("checkWiring") {
    group = "verification"
    description = "Comprueba que todo lo curado llega de verdad al atlas y al dibujo"
    classpath = sourceSets["tools"].runtimeClasspath
    mainClass.set("com.rolebuilder.core.tools.WiringCheck")
    systemProperty("java.awt.headless", "true")
}
