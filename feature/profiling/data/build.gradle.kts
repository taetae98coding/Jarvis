plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.profiling.domain)
            // PlatformContext 와 observeByPolling 을 쓴다.
            implementation(projects.core.data)
        }
    }
}
