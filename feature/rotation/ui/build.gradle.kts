plugins {
    id("jarvis.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.rotation.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
        }
    }
}
