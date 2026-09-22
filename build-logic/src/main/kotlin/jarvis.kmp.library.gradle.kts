import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    jvmToolchain(21)

    android {
        // AGP 는 모듈마다 유일한 namespace 만 요구한다. 패키지와 일치시키지 않는 이유는
        // docs/common/module-architecture.html#decision-package 에 있다.
        namespace = "io.github.taetae98coding.jarvis" +
            path.removePrefix(":").split(":").joinToString("") { ".$it" }
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
    }

    jvm()

    // Compose Multiplatform 1.12+ 부터 iosX64(Intel 시뮬레이터) 아티팩트를 더 이상 배포하지 않는다.
    // 의존 모듈에 없는 타깃은 프레임워크에 링크되지 않으므로 모든 모듈이 같은 목록을 쓴다.
    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // 모든 모듈이 Flow 로 상태를 주고받고, core 를 뺀 모든 모듈이 Koin 모듈을 내놓는다.
            api(libs.findLibrary("kotlinx-coroutines-core").get())
            api(libs.findLibrary("koin-core").get())
        }
    }
}
