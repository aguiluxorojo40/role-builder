// Sin plugins en la raíz, a propósito: cada módulo resuelve los suyos con
// versión en su propio classpath. Si la raíz cargara el plugin de Kotlin
// (que incluye también kotlin-android), :app no podría aplicarlo — desde el
// classpath raíz no se ven las clases de AGP del classpath de :app.
// Además, :app solo se incluye cuando hay Android SDK (ver settings.gradle.kts),
// para poder compilar y testear :core en entornos JVM puros sin Google Maven.
