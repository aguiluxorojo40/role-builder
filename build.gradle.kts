// Raíz intencionadamente sin plugins: cada módulo declara los suyos con la
// versión del catálogo (gradle/libs.versions.toml). Si se declararan aquí
// (aunque fuera con `apply false`), el plugin de Kotlin quedaría en el
// classpath del proyecto raíz y chocaría con la petición de
// `org.jetbrains.kotlin.android` de :app. Además, :app (y AGP) solo se
// cargan cuando hay un Android SDK disponible (ver settings.gradle.kts).
