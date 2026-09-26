plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.focus.domain)
            // PlatformContext, SettingsStore, observeByPolling, NotificationPermission 을 쓴다.
            implementation(projects.core.data)
        }
    }
}
