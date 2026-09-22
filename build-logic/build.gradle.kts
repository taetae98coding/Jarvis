plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // 컨벤션 플러그인이 plugins { } 로 적용할 플러그인은 여기 컴파일 클래스패스에 있어야 한다.
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.android)
    implementation(libs.plugin.compose)
    implementation(libs.plugin.composeCompiler)
}
