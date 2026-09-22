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

공용 코드는 clean architecture의 세 계층을 그대로 모듈로 나눈다. 의존은 한 방향이고 Gradle이 강제한다.

```
androidApp   iosApp(Xcode)   desktopApp   webApp
      └────────────┴────────────┴─────────┘
                   shared          ← 조립. 기능이 늘어도 커지지 않는다
          ┌──────────┼──────────┐
         ui        data         │
          └──────────┴──────── domain   ← 아무것도 의존하지 않는다
```

| 모듈 | 패키지 | 내용 |
|---|---|---|
| `domain` | `.jarvis.domain` | 모델, 리포지토리 인터페이스, 유스케이스. 의존성은 `kotlinx-coroutines-core` 뿐 |
| `data` | `.jarvis.data` | 리포지토리 구현. 플랫폼 API(`expect`/`actual`), 저장소, 직렬화, 프로세스·HTTP |
| `ui` | `.jarvis.ui` | Compose 화면과 `JarvisAppState`. `domain`만 본다 |
| `shared` | `.jarvis.shared` | 조립(composition root). `App()`, iOS `MainViewController()` |
| `androidApp` | `.jarvis` | `com.android.application` — `MainActivity`가 `App()`을 setContent |
| `desktopApp` | `.jarvis.desktop` | Kotlin/JVM + Compose Desktop — `main()`이 Window를 띄운다 |
| `webApp` | `.jarvis.web` | Kotlin/Wasm — `main()`이 ComposeViewport에 `App()`을 붙인다 |
| `iosApp` | — | Xcode 프로젝트. SwiftUI가 `shared`의 `MainViewController()`를 감싼다 |

라이브러리 모듈 넷은 모두 android / jvm / iosArm64 / iosSimulatorArm64 / wasmJs 타깃을 갖는다.
패키지는 모듈 루트 아래를 계층이 아니라 기능으로 나눈다(`appinfo`, `emulator`, `screen`, `settings`).
같은 기능은 세 모듈에서 같은 이름을 쓰므로 `emulator`로 찾으면 세 계층이 함께 나온다.

| 기능 | `domain` | `data` | `ui` |
|---|---|---|---|
| 앱 정보 | `AppInfo`, `GetAppInfoUseCase` | `platformName`, `APP_VERSION` | `AppInfoCard` |
| 에뮬레이터 개수 | `EmulatorStatus`, `EmulatorRepository` | `EmulatorDataSource`, 호스트 에이전트 | `EmulatorCard` |
| 화면 꺼짐 방지 | `ScreenAwakeSettingsRepository`, 유스케이스 7개 | `SettingsStore`, `IdleInhibitor`, `SystemScreenAwakeDataSource` | `ScreenAwakeCard`, `SystemScreenAwakeCard` |

`androidApp`만 루트 패키지를 쓰는데, Android의 `applicationId`(= `io.github.taetae98coding.jarvis`)와 맞추기 위해서다.
자세한 규칙(계층별 책임, 예외 둘, 가시성)은 [모듈 구조 스펙](docs/common/module-architecture.html)에 있다.

AGP 9부터 `com.android.application`과 `org.jetbrains.kotlin.multiplatform`을 같은 모듈에 적용할 수 없다.
그래서 공유 코드는 `com.android.kotlin.multiplatform.library`를 쓰는 라이브러리 모듈들에 두고, Android 앱은 별도 모듈로 분리했다.
Android 앱 모듈은 AGP 9의 내장 Kotlin 지원을 쓰므로 `kotlin-android` 플러그인을 따로 적용하지 않는다.

## 화면

상단 Card에 앱 버전과 실행 중인 플랫폼을 보여주고, 하단 Grid에 기능 아이템을 나열한다.
Grid는 `GridCells.Adaptive`라 창 너비에 따라 열 수가 늘어난다.
아이템을 추가하려면 `FeatureGrid`에 `item { ... }`을 더하면 된다.

지금 있는 아이템은 화면 꺼짐 방지, 화면 꺼짐 방지(시스템 전역), 에뮬레이터 개수 세 개다.

### 앱 버전

`gradle/libs.versions.toml`의 `appVersion` 하나가 원본이다.
`data`가 이 값으로 `APP_VERSION` 상수를 생성하고, Android `versionName`과 데스크톱 `packageVersion`도 같은 값을 읽는다.
iOS 번들 버전만 `iosApp/Configuration/Config.xcconfig`에서 따로 관리한다.

### 설정

토글 상태는 `ui`의 `JarvisAppState`가 들고 화면으로 내려간다.
`App()` 최상단에서 한 번 만들어지므로 화면을 옮겨 다녀도 값과 효과가 유지된다.

값은 `domain`의 `ScreenAwakeSettingsRepository` 계약을 통해 `data`의 `SettingsStore`에 저장되어
앱을 완전히 껐다 켜도 복원된다.

| 플랫폼 | 저장소 | 변경 알림 |
|---|---|---|
| Android | `SharedPreferences` | `OnSharedPreferenceChangeListener` |
| iOS | `NSUserDefaults` | `NSUserDefaultsDidChangeNotification` |
| JVM | `java.util.prefs.Preferences` | `PreferenceChangeListener` |
| Web | `localStorage` | `storage` 이벤트 + 자기 쓰기 신호 |

### 상태 조회 규칙

플랫폼 상태를 `Flow`로 노출할 때는 한 가지 규칙을 따른다.
**시스템이 변경 콜백을 주면 콜백으로, 주지 않으면 N초마다 다시 읽는다.**
어느 쪽이든 첫 값은 구독 즉시 읽고, 값이 그대로인 방출은 걸러낸다.

`ObserveSystemState.kt`의 `observeOnSignals` / `observeByPolling` / `observeSystemState` 세 함수가 그 규칙이고,
설정값·Android 권한·에뮬레이터 개수가 모두 이 위에 올라가 있다.
어느 상태가 어느 방식인지는 [상태 조회 규칙 스펙](docs/common/state-observation.html)에 표로 있다.

### 에뮬레이터 개수

Emulator 카드가 개발자 머신의 Android 에뮬레이터와 iOS 시뮬레이터를 "실행 중 / 전체"로 보여준다.

개수를 세려면 SDK 커맨드라인 도구를 실행해야 하고, 그게 가능한 건 개발자 머신에서 도는 JVM 타깃뿐이다.
그래서 **데스크탑 앱이 개수를 세어 로컬 HTTP로 넘겨주고, 나머지 타깃은 그걸 읽는다.**

| 플랫폼 | 구현 |
|---|---|
| JVM (macOS) | `emulator -list-avds` + `adb devices` + `xcrun simctl list devices` |
| Android | `http://10.0.2.2:47890` (실물 기기는 `adb reverse` 후 `127.0.0.1`) |
| iOS | `http://127.0.0.1:47890` (시뮬레이터만) |
| Web | `http://localhost:47890` + CORS |

Android SDK는 `ANDROID_HOME` → `ANDROID_SDK_ROOT` → `~/Library/Android/sdk` 순으로 찾는다.

값은 5초마다 다시 세고 바뀐 결과만 흘려보낸다. **에뮬레이터를 켜고 끄면 몇 초 안에 숫자가 따라온다.**
값을 처음 받기 전까지 카드는 "확인 중…"을 보여준다.

셀 방법이 없을 때는 0개가 아니라 "셀 수 없음"으로 보여준다. SDK를 못 찾은 경우와 데스크탑 앱이 꺼져 있는
경우가 그렇다. 화면에 0이 보이면 정말 0개라는 뜻이다.

### 화면 꺼짐 방지

두 개의 토글이 있다. 앱이 떠 있는 동안만 막는 것과, 앱이 없어도 막는 것이다.

**앱이 떠 있는 동안**은 Compose의 `Modifier.keepScreenOn()`을 루트 `Surface`에 붙인다.
Compose가 플랫폼별 API를 대신 호출한다.

| 플랫폼 | `Modifier.keepScreenOn()`이 호출하는 것 |
|---|---|
| Android | `View.keepScreenOn` |
| iOS | `UIApplication.idleTimerDisabled` |
| Web | Screen Wake Lock API (`navigator.wakeLock`) |
| JVM (Desktop) | **없음** — `caffeinate -di` 프로세스를 직접 띄운다 (macOS 전용) |

데스크탑만 Compose가 비워 둔 자리라, 그 한 칸을 `data`의 `IdleInhibitor` expect/actual이 메운다.
설정을 따라 언제 걸고 풀지는 `domain`의 `ApplyKeepScreenAwakeUseCase`가 정한다.

**앱이 없는 동안**은 `data`의 `SystemScreenAwakeDataSource`가 담당하고 **Android만 구현이 있다.**
시스템 전역 `SCREEN_OFF_TIMEOUT`을 직접 늘리는 방식이라 사용자가 따로 허용하는 `WRITE_SETTINGS`가 필요하고,
마켓 심사를 통과하기 어려운 권한이어서 사이드로드를 전제로 둔 기능이다.
원래 값은 앱이 보관해 두고 토글을 끌 때 되돌린다.

iOS·Web·JVM은 배포 방식을 포기해도 열리지 않는다. iOS는 시스템 권한이 필요하고, 브라우저는 OS 전원
설정에 닿지 못하며, macOS의 `pmset`은 root를 요구한다. 이 타깃에서는 카드가 잠긴 채로 이유를 보여준다.

## 실행

```bash
# Desktop (JVM) — 에뮬레이터 개수 에이전트도 같이 뜬다
./gradlew :desktopApp:run

# Web (Wasm) — http://localhost:8080
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Android — 디바이스/에뮬레이터 연결 후
./gradlew :androidApp:installDebug

# iOS — Xcode 정식 설치 필요 (Command Line Tools만으로는 iOS SDK가 없다)
open iosApp/iosApp.xcodeproj
```

Android SDK 위치는 `local.properties`의 `sdk.dir` 또는 `ANDROID_HOME`으로 알려줘야 한다.

Xcode 빌드 시 `Compile Kotlin Framework` 스크립트 단계가 `:shared:embedAndSignAppleFrameworkForXcode`를 호출해
Kotlin 프레임워크를 만들어 앱에 임베드한다. 서명 팀은 `iosApp/Configuration/Config.xcconfig`의 `TEAM_ID`에 넣는다.

Android·iOS·Web에서 에뮬레이터 개수를 보려면 데스크탑 앱을 함께 띄워 둬야 한다.
실물 Android 기기라면 `adb reverse tcp:47890 tcp:47890`도 필요하다.

## 테스트

| 소스셋 | 내용 | 실행 타깃 |
|---|---|---|
| `domain/src/commonTest` | 유스케이스 — 화면 유지 적용 규칙, 권한 요청 규칙 | 전 타깃 (Android host 포함) |
| `data/src/commonTest` | `PlatformNameTest`, `ScreenAwakeSettingsRepositoryTest`, `ObserveSystemStateTest`, `HostAgentTest` | 전 타깃 (Android host 포함) |
| `data/src/jvmTest` | `EmulatorParsingTest`, `HostAgentServerTest` — 명령 출력 파싱과 에이전트 HTTP 왕복 | jvm |
| `ui/src/skikoTest` | `JarvisAppTest` — 앱 버전·플랫폼 표시, 토글 동작, 설정 반영, 에뮬레이터 개수 표시 | jvm / wasmJs / ios |

`skikoTest`는 `ui`가 `applyDefaultHierarchyTemplate`으로 정의한 중간 소스셋이라 jvm/wasmJs/ios가 함께 쓴다.
Android는 호스트에 렌더링할 Android 런타임이 없어 이 그룹에서 빠진다.

UI 테스트는 가짜 **리포지토리**만 끼워서 돈다. `ui`가 `data`를 의존하지 않는다는 것이 테스트로 드러난다.

```bash
./gradlew build                                        # 전부
./gradlew :domain:jvmTest :data:jvmTest :ui:jvmTest    # JVM
./gradlew :domain:testAndroidHostTest :data:testAndroidHostTest   # Android host
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew :ui:wasmJsBrowserTest                      # Wasm (헤드리스 Chrome 필요)
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  ./gradlew :ui:iosSimulatorArm64Test                  # iOS (Xcode 필요)
```

## 문서

스펙은 [`docs/index.html`](docs/index.html)에서 시작한다. 코드를 구현하기 전에 스펙을 먼저 완성한다(`CLAUDE.md` 참고).

- [`docs/common`](docs/common/index.html) — 공통 스펙. 기능마다 사용자 지시, 요구사항, 공통 구현, 검증
- [`docs/platform`](docs/platform/index.html) — 플랫폼 스펙. 플랫폼마다 기술 조사, 구현 가능 여부, 우회 방법, 한계

## 플랫폼별 코드 추가하기

`data/src/commonMain`에 `expect`를 선언하고 각 `<target>Main`에 `actual`을 구현한다.
현재는 `platformName`(`data/appinfo/PlatformName.kt`)이 가장 단순한 예시다.
`ui`와 `domain`에는 `expect`를 두지 않는다. 예외 둘은 [모듈 구조 스펙](docs/common/module-architecture.html#exceptions)에 적혀 있다.

한 타깃에서만 가능한 기능이라면 나머지 `actual`을 "지원하지 않음"으로 두는 쪽을 택했다.
`createSystemScreenAwakeDataSource`가 그렇게 구현되어 있고, 화면에서 카드를 감추는 대신 잠긴 채로 이유를 보여준다.
이유는 [공통 스펙](docs/common/index.html#contract)에 적어 뒀다.

기능을 하나 더할 때는 `shared`에 파일을 더하지 않는다.
세 모듈의 같은 기능 패키지에 더하고, `JarvisContainer`의 조립 한 줄만 `shared`에 추가한다.
