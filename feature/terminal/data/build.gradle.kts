plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.terminal.domain)
            implementation(projects.core.data)
        }

        jvmMain.dependencies {
            // JDK 에는 pty 를 여는 API 가 없다. 후보 비교는 docs/platform/jvm.html#terminal 에 있다.
            implementation(libs.pty4j)
        }
    }
}
