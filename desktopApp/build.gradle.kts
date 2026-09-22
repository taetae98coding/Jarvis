import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "io.github.taetae98coding.jarvis.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Jarvis"
            packageVersion = libs.versions.appVersion.get()
        }
    }
}
