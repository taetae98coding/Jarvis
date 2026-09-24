plugins {
    id("jarvis.kmp.compose")
    // Home 라우트 키가 @Serializable 이어야 백스택이 저장·복원된다.
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // 앱 셸은 기능의 화면만 본다. 기능의 data 는 :shared 가 조립한다.
            api(projects.feature.appinfo.ui)
            api(projects.feature.emulator.ui)
            api(projects.feature.screen.ui)
            api(projects.feature.rotation.ui)
            api(projects.feature.terminal.ui)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            // JVM 에서 runComposeUiTest 가 요구하는 현재 호스트용 Skiko 네이티브 런타임.
            implementation(compose.desktop.currentOs)
        }
    }
}
