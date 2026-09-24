plugins {
    id("jarvis.kmp.compose")
    // 라우트 키(EmulatorRoute)가 @Serializable 이어야 백스택이 저장·복원된다.
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.emulator.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
