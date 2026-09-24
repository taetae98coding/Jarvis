plugins {
    id("jarvis.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.appinfo.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
        }
    }
}
