plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.battery.domain)
            // PlatformContext 와 observeOnSignals·observeByPolling 을 쓴다.
            implementation(projects.core.data)
        }

        wasmJsMain.dependencies {
            // BatteryManager 를 EventTarget 으로 다룬다. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }
    }
}
