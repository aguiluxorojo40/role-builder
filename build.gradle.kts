// Los plugins de Android (AGP, kotlin-android, compose) se declaran solo en :app,
// que únicamente se incluye cuando hay un Android SDK disponible (ver settings.gradle.kts).
// Así :core puede compilarse y testearse en entornos JVM puros sin acceso a Google Maven.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
