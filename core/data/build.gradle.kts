plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        wasmJsMain.dependencies {
            // SettingsStore 의 localStorage, window. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }
    }

    compilerOptions {
        // PlatformContext 가 expect class 다. Beta 기능이라는 안내일 뿐이라 경고를 끈다.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
