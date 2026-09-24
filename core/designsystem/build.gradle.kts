plugins {
    id("jarvis.kmp.compose")
}

kotlin {
    sourceSets {
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
