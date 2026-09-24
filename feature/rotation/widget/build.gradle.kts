plugins {
    id("jarvis.android.library")
    alias(libs.plugins.composeCompiler)
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ui 모듈과 같은 방향이다. domain 만 보고 data 는 보지 못한다.
    implementation(projects.feature.rotation.domain)
    implementation(projects.core.widget)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.compose.runtime)
}
