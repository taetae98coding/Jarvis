plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
    // 작업 공간을 JSON 한 문서로 저장한다(docs/common/terminal-panels.html#implementation).
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.terminal.domain)
            implementation(projects.core.data)
            implementation(libs.androidx.datastore.core.okio)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            // JDK 에는 pty 를 여는 API 가 없다. 후보 비교는 docs/platform/jvm.html#terminal 에 있다.
            implementation(libs.pty4j)
        }
    }
}
