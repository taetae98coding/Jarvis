plugins {
    id("jarvis.kmp.compose")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        // 프레임워크는 이 모듈만 만든다. 정적 프레임워크가 기능 모듈들의 klib 을 모두 링크해
        // 들어가므로 Xcode 는 이 하나만 임베드하면 된다.
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 앱 모듈은 :shared 만 의존한다. 화면은 :app:ui 를 통해 그대로 보이고, 기능의 data 는
            // Koin 모듈을 합치는 데만 쓰이므로 밖으로 내보내지 않는다.
            api(projects.app.ui)

            implementation(projects.core.data)
            implementation(projects.feature.appinfo.data)
            implementation(projects.feature.emulator.data)
            implementation(projects.feature.screen.data)
            implementation(projects.feature.rotation.data)
            implementation(projects.feature.terminal.data)
        }

        jvmMain.dependencies {
            // viewModelScope 가 Dispatchers.Main.immediate 다. JVM 에서 그 디스패처는 이 아티팩트가 있어야
            // 존재한다 — Compose Desktop 은 데려오지 않는다. 없으면 viewModelScope 가 조용히 EDT 밖에서
            // 돌고, Dispatchers.Main 을 직접 쓰는 곳은 "Module with the Main dispatcher is missing" 으로 죽는다.
            implementation(libs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            // Koin 정의가 전부 해석되는지 보는 테스트. 다섯 타깃이 같은 모듈 선언을 쓰므로
            // 대표로 JVM 에서만 돈다.
            implementation(libs.kotlin.test)
        }
    }
}
