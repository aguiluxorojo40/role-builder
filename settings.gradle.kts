pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "role-builder"

include(":core")

// :app necesita el Android SDK. Se incluye solo si hay un SDK disponible para que
// :core pueda compilarse y testearse en entornos sin SDK (CI ligera, JVM pura).
val localProps = file("local.properties")
val hasSdk = localProps.exists() && localProps.readText().contains("sdk.dir") ||
    System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null
if (hasSdk) {
    include(":app")
} else {
    logger.lifecycle("Android SDK no encontrado: se omite el módulo :app (solo :core disponible).")
}
