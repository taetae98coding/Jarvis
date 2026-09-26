plugins {
    id("jarvis.kmp.compose")
    // 라우트 키(UnitConverterRoute)가 @Serializable 이어야 백스택이 저장·복원된다.
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.unitconverter.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
        }

        jvmTest.dependencies {
            // JVM 에서 runComposeUiTest 가 요구하는 현재 호스트용 Skiko 네이티브 런타임.
            implementation(compose.desktop.currentOs)
        }
    }
}
