plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.emulator.domain)
            // Claude 의 기기 도구 이음새(docs/common/mcp-server.html#modules).
            implementation(projects.core.automation)
            // 폴링 조회 규칙을 쓴다.
            implementation(projects.core.data)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            // scrcpy 가 보내는 H.264 를 디코딩한다(docs/platform/jvm.html#device-mirroring). javacpp 는 ffmpeg 가
            // 끌어오지만 BytePointer 를 직접 참조하므로 명시한다. 네이티브는 macOS 두 아키텍처만 런타임에 붙인다.
            // KMP 소스셋 의존성에는 variantOf 가 없어 분류자를 좌표 문자열로 붙인다.
            val ffmpeg = libs.versions.bytedeco.ffmpeg.get()
            val javacpp = libs.versions.bytedeco.javacpp.get()

            implementation(libs.bytedeco.ffmpeg)
            implementation(libs.bytedeco.javacpp)
            runtimeOnly("org.bytedeco:ffmpeg:$ffmpeg:macosx-arm64")
            runtimeOnly("org.bytedeco:ffmpeg:$ffmpeg:macosx-x86_64")
            runtimeOnly("org.bytedeco:javacpp:$javacpp:macosx-arm64")
            runtimeOnly("org.bytedeco:javacpp:$javacpp:macosx-x86_64")
        }

        wasmJsMain.dependencies {
            // fetch, window. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }
    }
}
