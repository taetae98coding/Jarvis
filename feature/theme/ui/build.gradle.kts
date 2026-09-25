plugins {
    id("jarvis.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.theme.domain)
            implementation(projects.core.designsystem)
        }
    }
}
