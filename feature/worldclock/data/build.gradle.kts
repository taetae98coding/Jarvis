plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.worldclock.domain)
            // PlatformContext, SettingsStore, observeByPolling·observeSystemState 를 쓴다.
            implementation(projects.core.data)
        }
    }
}
