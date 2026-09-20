# Jarvis

Kotlin Multiplatform + Compose Multiplatform 프로젝트 구조.
**Android / JVM(Desktop) / iOS / Wasm(Web)** 네 타깃이 하나의 Compose UI를 공유한다.

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
| `shared` | `.jarvis.shared` | KMP 라이브러리. 타깃은 android / jvm / iosArm64 / iosSimulatorArm64 / wasmJs |
| `androidApp` | `.jarvis` | `com.android.application` — `MainActivity`가 `App()`을 setContent |
| `desktopApp` | `.jarvis.desktop` | Kotlin/JVM + Compose Desktop — `main()`이 Window를 띄운다 |
| `webApp` | `.jarvis.web` | Kotlin/Wasm — `main()`이 ComposeViewport에 `App()`을 붙인다 |
| `iosApp` | — | Xcode 프로젝트. SwiftUI가 `shared`의 `MainViewController()`를 감싼다 |

`shared` 내부는 세 갈래로 나뉜다.

| 패키지 | 내용 |
|---|---|
| `.jarvis.shared` | `App.kt` — 첫 화면 |
| `.jarvis.shared.ui` | `AppInfoCard`, `FeatureGrid`, `ToggleFeatureCard` |
| `.jarvis.shared.settings` | `AppSettings` — 앱 스코프 설정 상태 |
| `.jarvis.shared.platform` | `platformName`, `KeepScreenAwake`, `SettingsStore` 등 플랫폼별 expect/actual |

`androidApp`만 루트 패키지를 쓰는데, Android의 `applicationId`(= `io.github.taetae98coding.jarvis`)와 맞추기 위해서다.

AGP 9부터 `com.android.application`과 `org.jetbrains.kotlin.multiplatform`을 같은 모듈에 적용할 수 없다.
그래서 공유 코드는 `com.android.kotlin.multiplatform.library`를 쓰는 `shared`에 두고, Android 앱은 별도 모듈로 분리했다.
Android 앱 모듈은 AGP 9의 내장 Kotlin 지원을 쓰므로 `kotlin-android` 플러그인을 따로 적용하지 않는다.

## 화면

상단 Card에 앱 버전과 실행 중인 플랫폼을 보여주고, 하단 Grid에 기능 아이템을 나열한다.
Grid는 `GridCells.Adaptive`라 창 너비에 따라 열 수가 늘어난다.
아이템을 추가하려면 `FeatureGrid`에 `item { ... }`을 더하면 된다.

### 앱 버전

`gradle/libs.versions.toml`의 `appVersion` 하나가 원본이다.
`shared`가 이 값으로 `APP_VERSION` 상수를 생성하고, Android `versionName`과 데스크톱 `packageVersion`도 같은 값을 읽는다.
iOS 번들 버전만 `iosApp/Configuration/Config.xcconfig`에서 따로 관리한다.

### 설정

토글 상태는 `AppSettings`가 들고 있고 `LocalAppSettings`로 내려간다.
`App()` 최상단에 있으므로 화면을 옮겨 다녀도 값과 효과가 유지되고, 새 화면은 `LocalAppSettings.current`로 바로 읽는다.

값은 `SettingsStore`에 저장되어 앱을 완전히 껐다 켜도 복원된다.

| 플랫폼 | 저장소 |
|---|---|
| Android | `SharedPreferences` |
| iOS | `NSUserDefaults` |
| JVM | `java.util.prefs.Preferences` |
| Web | `localStorage` |

### 화면 꺼짐 방지

플랫폼마다 유휴 타이머를 막는 방식이 달라 `KeepScreenAwake`를 expect/actual로 나눴다.

| 플랫폼 | 구현 |
|---|---|
| Android | `View.keepScreenOn` |
| iOS | `UIApplication.idleTimerDisabled` |
| Web | Screen Wake Lock API (`navigator.wakeLock`) |
| JVM (macOS) | `caffeinate -di` 프로세스 |
| JVM (Linux) | `systemd-inhibit --what=idle` 프로세스 |
| JVM (Windows) | 미지원 — `SetThreadExecutionState` 호출이 필요해 아직 연결하지 않았다 |

**앱이 종료된 상태에서는 화면을 켜 둘 수 없다.** Android `FLAG_KEEP_SCREEN_ON`은 해당 윈도우가 보이는 동안만,
iOS `idleTimerDisabled`는 앱이 foreground인 동안만 유효하고 시스템이 회수한다.
Android의 `SCREEN_BRIGHT_WAKE_LOCK`은 API 17에서 deprecated된 뒤 화면을 켜 두지 못하며 foreground service로도 우회할 수 없고, iOS에는 해당 API 자체가 없다.
따라서 유지되는 것은 **설정값**이고, 화면 꺼짐 방지는 앱이 떠 있는 동안 다시 적용된다.

Web은 탭이 숨겨지면 브라우저가 wake lock을 회수하므로 `visibilitychange`에서 다시 요청한다.

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
| `shared/src/commonTest` | `PlatformTest`, `AppSettingsTest` — expect/actual 구현과 설정 읽기·쓰기 검증 | 전 타깃 (Android host 포함) |
| `shared/src/skikoTest` | `AppTest` — 앱 버전·플랫폼 표시, 토글 동작, 설정 저장·복원 검증 | jvm / wasmJs / ios |

`skikoTest`는 `applyDefaultHierarchyTemplate`으로 정의한 중간 소스셋이라 jvm/wasmJs/ios가 함께 쓴다.
Android는 호스트에 렌더링할 Android 런타임이 없어 이 그룹에서 빠진다.

```bash
./gradlew :shared:jvmTest                              # JVM
./gradlew :shared:testAndroidHostTest                  # Android host (PlatformTest)
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew :shared:wasmJsBrowserTest                  # Wasm (헤드리스 Chrome 필요)
./gradlew :shared:iosSimulatorArm64Test                # iOS (Xcode 필요)
```

## 문서

플랫폼별 구현 방식과 한계는 [`docs/platform`](docs/platform/README.md)에 정리했다.

## 플랫폼별 코드 추가하기

`shared/src/commonMain`에 `expect`를 선언하고 각 `<target>Main`에 `actual`을 구현한다.
현재는 `platformName`(`Platform.kt`)이 그 예시다.
