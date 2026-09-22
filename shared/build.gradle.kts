import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "io.github.taetae98coding.jarvis.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvm()

    // Compose Multiplatform 1.12+ 부터 iosX64(Intel 시뮬레이터) 아티팩트를 더 이상 배포하지 않는다.
    // 나머지 모듈의 타깃 목록도 여기에 맞춘다. 의존 모듈에 없는 타깃은 프레임워크에 링크되지 않는다.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        // 프레임워크는 이 모듈만 만든다. 정적 프레임워크가 :ui, :data, :domain 의 klib 을 모두
        // 링크해 들어가므로 Xcode 는 이 하나만 임베드하면 된다.
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // 이 모듈에는 UI 테스트가 없지만, Compose 플러그인의 checkComposeUiTestConfigurationForWasmJs
        // 는 Compose 를 쓰는 wasmJs 타깃이면 실행 바이너리를 요구한다 (CMP-4906).
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // 앱 모듈은 :shared 만 의존한다. 화면과 Compose 는 :ui 를 통해 그대로 보이고,
            // :data 는 조립에만 쓰이므로 밖으로 내보내지 않는다.
            api(projects.ui)
            implementation(projects.domain)
            implementation(projects.data)
        }
    }
}
