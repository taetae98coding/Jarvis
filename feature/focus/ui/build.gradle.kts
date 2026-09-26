plugins {
    id("jarvis.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.focus.domain)
            implementation(projects.core.designsystem)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
