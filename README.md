# Jarvis

Kotlin Multiplatform + Compose Multiplatform 프로젝트 구조.
**Android / JVM(Desktop) / iOS / Wasm(Web)** 네 타깃이 하나의 Compose UI(`Hello World`)를 공유한다.

## 버전

| | 버전 |
|---|---|
| Kotlin | 2.4.20 |
| Android Gradle Plugin | 9.4.1 |
| Compose Multiplatform | 1.12.0 (material3만 별도 라인인 1.9.0) |
| Gradle | 9.7.1 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |
| JVM toolchain | 21 |

모든 버전은 `gradle/libs.versions.toml`에서 관리한다.

## 모듈 구조

기본 패키지는 `io.github.taetae98coding.jarvis`다. 모듈마다 하위 패키지를 따로 쓰므로 같은 패키지가 여러 모듈에 걸치지 않는다.

| 모듈 | 패키지 | 내용 |
|---|---|---|
| `shared` | `.jarvis.shared` | KMP 라이브러리. 타깃은 android / jvm / iosArm64 / iosSimulatorArm64 / wasmJs. 공용 Compose UI(`App.kt`)와 플랫폼별 expect/actual 구현(`Platform.kt`) |
| `androidApp` | `.jarvis` | `com.android.application` — `MainActivity`가 `App()`을 setContent |
| `desktopApp` | `.jarvis.desktop` | Kotlin/JVM + Compose Desktop — `main()`이 Window를 띄운다 |
| `webApp` | `.jarvis.web` | Kotlin/Wasm — `main()`이 ComposeViewport에 `App()`을 붙인다 |
| `iosApp` | — | Xcode 프로젝트. SwiftUI가 `shared`의 `MainViewController()`를 감싼다 |

`androidApp`만 루트 패키지를 쓰는데, Android의 `applicationId`(= `io.github.taetae98coding.jarvis`)와 맞추기 위해서다.

AGP 9부터 `com.android.application`과 `org.jetbrains.kotlin.multiplatform`을 같은 모듈에 적용할 수 없다.
그래서 공유 코드는 `com.android.kotlin.multiplatform.library`를 쓰는 `shared`에 두고, Android 앱은 별도 모듈로 분리했다.
Android 앱 모듈은 AGP 9의 내장 Kotlin 지원을 쓰므로 `kotlin-android` 플러그인을 따로 적용하지 않는다.

## 실행

```bash
# Desktop (JVM)
./gradlew :desktopApp:run

# Web (Wasm) — http://localhost:8080
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Android — 디바이스/에뮬레이터 연결 후
./gradlew :androidApp:installDebug

# iOS — Xcode 정식 설치 필요 (Command Line Tools만으로는 프레임워크 링크 불가)
open iosApp/iosApp.xcodeproj
```

Xcode 빌드 시 `Compile Kotlin Framework` 스크립트 단계가 `:shared:embedAndSignAppleFrameworkForXcode`를 호출해
Kotlin 프레임워크를 만들어 앱에 임베드한다. 서명 팀은 `iosApp/Configuration/Config.xcconfig`의 `TEAM_ID`에 넣는다.

## 테스트

| 소스셋 | 내용 | 실행 타깃 |
|---|---|---|
| `shared/src/commonTest` | `PlatformTest` — expect/actual 구현 검증 | 전 타깃 (Android host 포함) |
| `shared/src/skikoTest` | `AppTest` — Compose UI에 "Hello World"가 실제로 표시되는지 검증 | jvm / wasmJs / ios |

`skikoTest`는 `applyDefaultHierarchyTemplate`으로 정의한 중간 소스셋이라 jvm/wasmJs/ios가 함께 쓴다.
Android는 호스트에 렌더링할 Android 런타임이 없어 이 그룹에서 빠진다.

```bash
./gradlew :shared:jvmTest                              # JVM
./gradlew :shared:testAndroidHostTest                  # Android host (PlatformTest)
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew :shared:wasmJsBrowserTest                  # Wasm (헤드리스 Chrome 필요)
./gradlew :shared:iosSimulatorArm64Test                # iOS (Xcode 필요)
```

## 플랫폼별 코드 추가하기

`shared/src/commonMain`에 `expect`를 선언하고 각 `<target>Main`에 `actual`을 구현한다.
현재는 `platformName`(`Platform.kt`)이 그 예시다.
