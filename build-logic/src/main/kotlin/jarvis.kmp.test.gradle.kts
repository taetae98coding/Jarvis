import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("jarvis.kmp.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android {
        // commonTest 를 Android 호스트에서도 돌리려면 이 선언이 있어야 한다.
        withHostTest {}
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.findLibrary("kotlin-test").get())
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
