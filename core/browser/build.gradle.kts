plugins {
    id("jarvis.kmp.compose")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.automation)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            implementation(libs.jcefmaven)
            // 데스크톱은 macOS 만 지원한다. Apple Silicon 네이티브만 넣고, Intel Mac 은 첫 실행에 jcefmaven 이 내려받는다.
            runtimeOnly(libs.jcef.natives.macosx.arm64)
        }
    }
}
