plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.mcp.domain)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
