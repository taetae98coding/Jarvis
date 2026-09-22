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
| `.jarvis.shared.ui` | `AppInfoCard`, `FeatureGrid`, `ToggleFeatureCard`, `EmulatorCard` |
| `.jarvis.shared.settings` | `AppSettings` — 앱 스코프 설정 상태 |
| `.jarvis.shared.platform` | `platformName`, `PlatformIdleInhibitor`, `SettingsStore`, `emulatorProbe` 등 플랫폼별 expect/actual |

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

### 에뮬레이터 개수

Emulator 카드가 이 머신의 Android 에뮬레이터와 iOS 시뮬레이터를 "실행 중 / 전체"로 보여준다.

개수를 세려면 개발자 머신에서 SDK 커맨드라인 도구를 실행해야 해서, 실제 숫자가 나오는 건 JVM 타깃뿐이다.

| 플랫폼 | 구현 |
|---|---|
| JVM (macOS) | `emulator -list-avds` + `adb devices` + `xcrun simctl list devices` |
| Android / iOS / Web | 항상 0개 — 샌드박스 밖의 프로세스를 띄울 수 없다 |

Android SDK는 `ANDROID_HOME` → `ANDROID_SDK_ROOT` → `~/Library/Android/sdk` 순으로 찾는다.

JVM 프로브는 5초마다 다시 세고 바뀐 결과만 흘려보낸다. 프로브가 답하기 전까지 카드는 "확인 중…"을 보여준다.

### 화면 꺼짐 방지

Compose의 `Modifier.keepScreenOn()`을 루트 `Surface`에 붙인다. Compose가 플랫폼별 API를 대신 호출한다.

| 플랫폼 | `Modifier.keepScreenOn()`이 호출하는 것 |
|---|---|
| Android | `View.keepScreenOn` |
| iOS | `UIApplication.idleTimerDisabled` |
| Web | Screen Wake Lock API (`navigator.wakeLock`) |
| JVM (Desktop) | **없음** — `caffeinate -di` 프로세스를 직접 띄운다 (macOS 전용) |

데스크탑만 Compose가 비워 둔 자리라, 그 한 칸을 `PlatformIdleInhibitor` expect/actual이 메운다.

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
| `shared/src/skikoTest` | `AppTest` — 앱 버전·플랫폼 표시, 토글 동작, 설정 저장·복원, 에뮬레이터 개수 표시 검증 | jvm / wasmJs / ios |
| `shared/src/jvmTest` | `EmulatorParsingTest` — `emulator`·`adb`·`simctl` 출력 파싱 검증 | jvm |

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

스펙은 [`docs/index.html`](docs/index.html)에서 시작한다. 코드를 구현하기 전에 스펙을 먼저 완성한다(`CLAUDE.md` 참고).

- [`docs/common`](docs/common/index.html) — 공통 스펙. 기능마다 사용자 지시, 요구사항, 공통 구현, 검증
- [`docs/platform`](docs/platform/index.html) — 플랫폼 스펙. 플랫폼마다 기술 조사, 구현 가능 여부, 우회 방법, 한계

## 플랫폼별 코드 추가하기

`shared/src/commonMain`에 `expect`를 선언하고 각 `<target>Main`에 `actual`을 구현한다.
현재는 `platformName`(`Platform.kt`)이 그 예시다.

한 타깃에서만 가능한 기능이라면 나머지 `actual`을 빈 값으로 두는 쪽을 택했다.
`emulatorProbe`가 그렇게 구현되어 있고, 이유는 [공통 스펙](docs/common/index.html#limit-zero)에 적어 뒀다.
