plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.screen.domain)
            // PlatformContext, 조회 규칙, SettingsStore, NotificationPermission 을 쓴다.
            implementation(projects.core.data)
        }
    }
}
