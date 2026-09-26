plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.calculator.domain)
            // PlatformContext 와 SettingsStore 를 쓴다.
            implementation(projects.core.data)
        }
    }
}
