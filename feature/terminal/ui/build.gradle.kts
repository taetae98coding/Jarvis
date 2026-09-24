plugins {
    id("jarvis.kmp.compose")
    // 라우트 키(TerminalRoute)가 @Serializable 이어야 백스택이 저장·복원된다.
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.terminal.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
        }

        androidMain.dependencies {
            // 시스템 웹뷰. JVM 은 :core:browser 의 Chromium 을 쓴다(docs/common/terminal-browser.html#implementation).
            implementation(libs.compose.webview)
        }

        jvmMain.dependencies {
            implementation(projects.core.browser)
        }
    }
}
