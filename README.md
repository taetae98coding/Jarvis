# Jarvis

Kotlin Multiplatform + Compose Multiplatform 프로젝트 구조.
**Android / JVM(Desktop) / iOS / Wasm(Web)** 네 타깃이 하나의 Compose UI를 공유한다.

## 버전

| | 버전 |
|---|---|
| Kotlin | 2.4.20 |
| Android Gradle Plugin | 9.4.1 |
| Compose Multiplatform | 1.12.1 (material3만 별도 라인인 1.9.0) |
| Koin | 4.2.2 (`koin-compose`, `koin-compose-viewmodel`, `koin-compose-navigation3`) |
| Lifecycle (ViewModel) | 2.11.0 (`org.jetbrains.androidx.lifecycle`) |
| Navigation3 | `navigation3-ui` 1.1.2 (JetBrains), `navigation3-runtime` 1.1.7 (androidx) |
| `compose-runtime-retain` | 1.12.1 (`retain` 은 `runtime` 과 다른 아티팩트다) |
| Gradle | 9.7.1 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 33 |
| JVM toolchain | 21 |

모든 버전은 `gradle/libs.versions.toml`에서 관리한다.

## 모듈 구조

기본 패키지는 `io.github.taetae98coding.jarvis`다. 모듈마다 하위 패키지를 따로 쓰므로 같은 패키지가 여러 모듈에 걸치지 않는다.

공용 코드는 **기능마다 clean architecture 세 계층을 모듈로** 갖는다. 기능은 `appinfo` · `emulator` · `screen` · `rotation` · `terminal` · `theme` · `profiling` · `devtools` · `mcp` 아홉이고 서로를 의존하지 않는다.
화면이 없는 `mcp` 는 `domain` · `data` 둘만, Android 위젯·알림·타일이 있는 `screen` · `rotation` 은 `widget` 을 하나 더 갖는다. 의존은 한 방향이고 Gradle이 강제한다.

```
androidApp   iosApp(Xcode)   desktopApp   webApp
      └────────────┴────────────┴─────────┘
                   shared               ← Koin 시작. 기능이 늘어도 커지지 않는다
                   app:ui               ← JarvisApp(), Home 화면, FeatureGrid, 백스택
     ┌───────────────┼───────────────┬───────────────┐
 feature:appinfo  feature:emulator  feature:screen  feature:terminal  …  (기능 아홉)
     │      기능마다 ui → domain ← data   (widget → domain)
     └───────────────┴───────────────┴───────────────┘
   core:ui   core:data   core:browser   core:automation   core:widget   ← 두 기능 이상이 쓰는 것만
                 │
           core:designsystem                         ← JarvisTheme, JarvisIcons, Jarvis* 컴포넌트
```

| 모듈 | 패키지 | 내용 |
|---|---|---|
| `feature:<기능>:domain` | `.jarvis.domain.<기능>` | 모델, 리포지토리 인터페이스, 유스케이스, `<기능>DomainModule`. 의존성은 `kotlinx-coroutines-core` 와 `koin-core` 뿐 |
| `feature:<기능>:data` | `.jarvis.data.<기능>` | 리포지토리 구현과 `<기능>DataModule`. 플랫폼 API(`expect`/`actual`), 저장소, 직렬화, 프로세스·HTTP |
| `feature:<기능>:ui` | `.jarvis.ui.<기능>` | 카드·화면, ViewModel, 라우트, `<기능>UiModule`. 같은 기능의 `domain`만 본다 |
| `feature:<기능>:widget` | `.jarvis.widget.<기능>` | Android 전용 홈 화면 위젯·알림·빠른 설정 타일. `domain`만 본다 |
| `core:data` | `.jarvis.data`, `.jarvis.data.state` | `PlatformContext`(expect class), 상태 조회 규칙 3종 |
| `core:designsystem` | `.jarvis.designsystem.theme`, `.icon`, `.component` | M3 위의 `JarvisTheme`(색·글꼴·모양·치수), `JarvisIcons`(ImageVector), `JarvisCard` 등 공용 컴포넌트와 `*Defaults`. 규칙은 [디자인 시스템 스펙](docs/common/design-system.html) |
| `core:browser` | `.jarvis.browser` | 터미널 브라우저 탭과 MCP 가 함께 쓰는 브라우저 엔진(JVM 은 JCEF) |
| `core:automation` | `.jarvis.automation` | MCP 도구가 기능들의 능력을 부르는 이음새 인터페이스 |
| `core:widget` | `.jarvis.widget` | Android 위젯·알림·타일이 함께 쓰는 갱신 장치·권한 중계·알림 채널 |
| `core:ui` | `.jarvis.ui.component`, `.jarvis.ui.navigation` | `ToggleFeatureCard`, `Navigator`·`LocalNavigator`, `NavKeySerializers` |
| `app:ui` | `.jarvis.ui.app` | `JarvisApp()`, Home 라우트와 화면, `FeatureGrid`, 백스택, `appUiModule` |
| `shared` | `.jarvis.shared` | Koin 시작(`startJarvisKoin()`)과 진입점. `App()`, iOS `MainViewController()` |
| `build-logic` | — | 컨벤션 플러그인 `jarvis.kmp.library` / `jarvis.kmp.compose` / `jarvis.kmp.test`. 타깃 선언이 여기 한곳에만 있다 |
| `androidApp` | `.jarvis` | `com.android.application` — `MainActivity`가 Koin 을 세우고 `App()`을 setContent |
| `desktopApp` | `.jarvis.desktop` | Kotlin/JVM + Compose Desktop — `main()`이 Window를 띄운다 |
| `webApp` | `.jarvis.web` | Kotlin/Wasm — `main()`이 ComposeViewport에 `App()`을 붙인다 |
| `iosApp` | — | Xcode 프로젝트. SwiftUI가 `shared`의 `MainViewController()`를 감싼다 |

멀티플랫폼 라이브러리 모듈 서른은 모두 android / jvm / iosArm64 / iosSimulatorArm64 / wasmJs 타깃을 갖는다(`widget` 모듈 셋은 Android 전용). 그 선언은 `build-logic`의 컨벤션 플러그인에만 있다.
패키지는 기능 분리 전 이름(`.jarvis.<계층>.<기능>`)을 그대로 쓴다. 모듈 경로와 순서가 반대지만, 그 덕에 분리 과정에서 `import`가 한 줄도 바뀌지 않았다.
같은 기능은 세 모듈에서 같은 마지막 이름을 쓰므로 `emulator`로 찾으면 세 계층이 함께 나온다.

| 기능 | `feature:<기능>:domain` | `feature:<기능>:data` | `feature:<기능>:ui` |
|---|---|---|---|
| 앱 정보 | `AppInfo`, `GetAppInfoUseCase` | `platformName`, `APP_VERSION` | `AppInfoCard` |
| 에뮬레이터 개수 | `EmulatorStatus`, `EmulatorRepository` | `EmulatorDataSource`, 호스트 에이전트 | `EmulatorCard` |
| 에뮬레이터 목록·화면·제스처 | `EmulatorDevice`, `EmulatorGesture`, 유스케이스 3개 | `EmulatorDataSource`, 호스트 에이전트 | `EmulatorListScreen`, `EmulatorStreamScreen` |
| Wi-Fi 기기 페어링 | `DevicePairingRepository`, `PairingQrCode`, 유스케이스 4개 | `DevicePairingDataSource`, 호스트 에이전트 | `WifiPairingScreen`, `QrCode` |
| 화면 꺼짐 방지 | `ScreenAwakeSettingsRepository`, 유스케이스 7개 | `SettingsStore`, `IdleInhibitor`, `SystemScreenAwakeDataSource` | `ScreenAwakeCard`, `SystemScreenAwakeCard` |
| 화면 테마 | `ThemeMode`, `ThemeSettingsRepository`, `ThemeAppearanceRepository`, 유스케이스 3개 | `SettingsStore`(문자열), `ThemeAppearance` | `ThemeModeCard`, `appDarkTheme()` |
| 프로파일링 | `Profiling`, `ProfilingRepository`, `ObserveProfilingUseCase` | `ProfilingSource`, `CounterDelta` | `ProfilingCard` |
| 개발자 도구 | `DevTool`, 변환기 6개(`JsonFormatter`, `HashCalculator` …), `DevToolsSettingsRepository`, 유스케이스 6개 | `DefaultDevToolsSettingsRepository`(`SettingsStore`) | `DevToolsCard`, `DevToolsScreen` |
| 터미널 | `TerminalWorkspace`, `FileRepository`, `GitWorktreeRepository`, `CodeIntelRepository` | PTY 세션, git, 파일, LSP | 터미널 화면·패널·탭·사이드 바 |
| MCP 서버 | `McpToolbox`, `McpTools`, `DeviceLeases` | `McpServer`, `McpProtocol` | — |

## 의존성 주입 · ViewModel · 화면 이동

조립은 Koin 이 한다. 기능마다 세 개의 Koin 모듈을 갖고(`<기능>DomainModule` / `<기능>DataModule` / `<기능>UiModule`),
`shared`가 그 목록을 합쳐 플랫폼 핸들만 더한다. 네 진입점이 각각 `startJarvisKoin()`을 부르고, 두 번 불려도
첫 번째만 유효하다. 손으로 조립하던 `JarvisContainer`와 `DataModule` 클래스는 사라졌다.

화면 상태는 ViewModel 일곱이 나눠 갖는다. 앱 수명 `ScreenAwakeEffectViewModel`(화면 꺼짐 방지 효과),
홈의 카드 넷이 각자 쓰는 `AppInfoViewModel` · `ScreenAwakeViewModel` · `EmulatorStatusViewModel` · `DeviceRotationViewModel`,
그리고 에뮬레이터 화면의 `EmulatorDevicesViewModel` · `EmulatorScreenViewModel(deviceId)`.
카드는 인자를 받지 않고 자기 ViewModel 을 `koinViewModel()`로 직접 받으므로, 앱 셸은 기능의 유스케이스도 상태도 모른다.

화면 이동은 Navigation3 다. 라우트 본문은 그 기능의 Koin 모듈에서 `navigation<T>` 로 선언하고
`NavDisplay`는 `koinEntryProvider()`가 모아 준 목록만 받으므로, 화면을 더할 때 앱 셸은 고치지 않는다.
백스택 키의 직렬화 등록도 기능이 `navKeySerializers` 로 내고 셸이 `getAll` 로 모은다.

기기 화면의 마지막 프레임은 `retain` 이 들고 있다 — 컴포지션보다 오래 살아야 하지만 직렬화하고 싶지 않은 값이다.

자세한 내용은 [의존성 주입](docs/common/dependency-injection.html) · [ViewModel](docs/common/view-model.html) ·
[화면 이동](docs/common/navigation.html) · [retain](docs/common/retained-state.html) 스펙에 있다.

`androidApp`만 루트 패키지를 쓰는데, Android의 `applicationId`(= `io.github.taetae98coding.jarvis`)와 맞추기 위해서다.
Android `namespace`는 컨벤션 플러그인이 프로젝트 경로에서 만든다(`:feature:emulator:data` → `…jarvis.feature.emulator.data`). AGP는 유일한 namespace만 요구한다.
자세한 규칙(계층별 책임, 예외, 가시성)은 [모듈 구조 스펙](docs/common/module-architecture.html)에 있다.

AGP 9부터 `com.android.application`과 `org.jetbrains.kotlin.multiplatform`을 같은 모듈에 적용할 수 없다.
그래서 공유 코드는 `com.android.kotlin.multiplatform.library`를 쓰는 라이브러리 모듈들에 두고, Android 앱은 별도 모듈로 분리했다.
Android 앱 모듈은 AGP 9의 내장 Kotlin 지원을 쓰므로 `kotlin-android` 플러그인을 따로 적용하지 않는다.

## 화면

상단 Card에 앱 버전과 실행 중인 플랫폼을 보여주고, 하단 Grid에 기능 아이템을 나열한다.
Grid는 `GridCells.Adaptive`라 창 너비에 따라 열 수가 늘어난다.
아이템을 추가하려면 `FeatureGrid`에 `item { ... }`을 더하면 된다.

지금 있는 아이템은 화면 꺼짐 방지, 화면 꺼짐 방지(시스템 전역), 에뮬레이터, 화면 회전 네 개다.
에뮬레이터 카드를 누르면 그리드 대신 기기 목록이, 기기를 고르면 그 기기의 화면이 그 자리를 채운다.

### 앱 버전

`gradle/libs.versions.toml`의 `appVersion` 하나가 원본이다.
`data`가 이 값으로 `APP_VERSION` 상수를 생성하고, Android `versionName`과 데스크톱 `packageVersion`도 같은 값을 읽는다.
iOS 번들 버전만 Xcode 가 Gradle 값을 읽지 못해 `iosApp/Configuration/Config.xcconfig`에 같은 값을 적고, 어긋나면 `./gradlew check`(`checkIosAppVersion`)가 실패한다.
빌드 번호(Android `versionCode`, iOS `CURRENT_PROJECT_VERSION`)는 `MAJOR×10000 + MINOR×100 + PATCH`다.

### 설정

토글 상태는 `feature:screen:ui`의 `ScreenAwakeViewModel`이 들고 카드로 내려간다.
화면 꺼짐 방지 효과는 앱 수명 `ScreenAwakeEffectViewModel`의 `viewModelScope`에서 걸리므로 화면을 옮겨 다녀도 유지된다.

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

### 에뮬레이터 화면과 제스처

에뮬레이터 카드를 누르면 가상 기기 목록이 열리고, **실행 중인 기기를 고르면 그 화면을 보면서 탭과 스와이프를 보낼 수 있다.**
꺼져 있는 기기는 찍을 화면이 없어서 목록에서 잠겨 있다.

| 플랫폼 | 화면 | 제스처 |
|---|---|---|
| Android 에뮬레이터 | `adb -s <시리얼> exec-out screencap -p` | `adb shell input tap` / `input swipe` |
| iOS 시뮬레이터 | `simctl io <UDID> screenshot --type=png -` | **없다.** `simctl`에 입력을 주입하는 명령이 없다 |

프레임은 500ms마다 PNG 한 장을 받는 폴링이다. 영상 스트림이 아니다.
`screencap` 한 장이 0.3~1초 걸려서 실제로는 초당 한두 장이고, 그 이유와 버린 후보(scrcpy, MJPEG, WebSocket)는
[공통 스펙](docs/common/emulator-control.html#implementation)에 있다.

탭 좌표는 프레임 해상도를 기준으로 기기 픽셀로 바꿔서 보낸다. 프레임의 픽셀 크기가 곧 기기 디스플레이
해상도라 별도 조회가 필요 없다. 제스처를 받지 못하는 기기에서는 화면만 보이고 그 이유가 화면에 뜬다.

개수와 같은 에이전트를 쓴다. JVM 외의 타깃은 `GET /emulators/devices`, `GET /emulators/screen?id=…`,
`POST /emulators/gesture` 세 엔드포인트로 데스크탑 앱에 물어본다.

### Wi-Fi 기기 페어링

기기 목록 상단의 Wi-Fi 버튼을 누르면 Android Studio 의 "Pair Devices Using Wi-Fi" 와 같은 화면이 열린다.
**QR 코드** 탭은 `WIFI:T:ADB;S:jarvis-…;P:…;;` QR 을 그리고, 기기가 스캔해 그 이름으로 mDNS 알림을 내면
알아서 `adb pair` 한다. **페어링 코드** 탭은 `adb mdns services` 에 나온 대기 기기마다 6자리 코드를 받는다.
페어링하면 `adb` 서버가 스스로 연결하고, 기기는 다음 목록 갱신 때 실물 기기로 나타난다.

QR 은 의존성 없이 `feature:emulator:ui` 의 인코더(`QrCode.kt`)가 그린다. iPhone·iPad 는 공개 도구로 무선 첫
페어링을 할 수 없어서(iOS 26 이하는 케이블 필수, iOS 27 은 Xcode Device Hub 만 가능) 안내 문구만 보여 준다.
JVM 외의 타깃은 에이전트의 `GET /emulators/pairing/services`, `POST /emulators/pairing/pair` 로 묻는다.
조사와 판정은 [공통 스펙](docs/common/wireless-pairing.html)에 있다.

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

Android·iOS·Web에서 에뮬레이터 개수·목록·화면을 보려면 데스크탑 앱을 함께 띄워 둬야 한다.
실물 Android 기기라면 `adb reverse tcp:47890 tcp:47890`도 필요하다.

## release 빌드

```bash
./gradlew :androidApp:assembleRelease :androidApp:bundleRelease   # 서명된 APK·AAB (R8)
./gradlew :desktopApp:packageReleaseDmg                           # macOS DMG
./gradlew :webApp:wasmJsBrowserDistribution                       # webApp/build/dist/wasmJs/productionExecutable
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild \
  -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release \
  -destination generic/platform=iOS -archivePath build/ios/Jarvis.xcarchive -allowProvisioningUpdates archive
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild -exportArchive \
  -archivePath build/ios/Jarvis.xcarchive -exportPath build/ios/export \
  -exportOptionsPlist iosApp/ExportOptions.plist -allowProvisioningUpdates
```

Android 서명 키는 저장소 밖에 두고 `~/.gradle/gradle.properties`의 `jarvis.signing.storeFile` · `storePassword` · `keyAlias` · `keyPassword`로 알려 준다.
없으면 미서명 산출물을 만들고 경고한다. 체크리스트와 플랫폼별 판정·배포 방법은 [release 빌드 스펙](docs/common/release-build.html)에 있다.
앱 아이콘 원본은 `branding/`의 SVG이고 `branding/render-icons.sh`가 플랫폼 아이콘을 다시 만든다.

## 테스트

| 소스셋 | 내용 | 실행 타깃 |
|---|---|---|
| `feature/*/domain/src/commonTest` | 유스케이스 — 화면 유지 적용 규칙, 권한 요청 규칙, 회전 각도 규칙, 실행·깨우기 가드 | 전 타깃 (Android host 포함) |
| `feature/*/data/src/commonTest` | `PlatformNameTest`, `ScreenAwakeSettingsRepositoryTest`, `HostAgentTest` | 전 타깃 (Android host 포함) |
| `core/data/src/commonTest` | `ObserveSystemStateTest` — 신호·폴링 조회 규칙 | 전 타깃 (Android host 포함) |
| `feature/emulator/data/src/jvmTest` | `EmulatorParsingTest`, `HostAgentServerTest` — 명령 출력 파싱과 에이전트 HTTP 왕복 | jvm |
| `feature/emulator/ui/src/jvmTest` | `EmulatorDevicesViewModelTest` — 실행 잠금 규칙 (`viewModelScope`) | jvm |
| `app/ui/src/skikoTest` | `JarvisAppTest` — 앱 버전·플랫폼 표시, 토글 동작, 설정 반영, 화면 이동, 에뮬레이터 목록·화면·제스처 | jvm / wasmJs / ios |
| `app/ui/src/skikoTest` | `@IgnoreOnWasm` — 이벤트 루프가 막힌 동안 끝나야 하는 일(`delay`, `Dispatchers.Default`, 휠)을 기다리는 터미널 테스트 아홉만 Wasm 에서 건너뛴다([웹 테스트](docs/platform/web.html#test)). Wasm 도 JVM 처럼 테스트의 `Dispatchers.Main` 을 `Unconfined` 로 둔다 | jvm / ios |
| `app/ui/src/jvmTest` | `JarvisAppLaunchLockTest` — 실행 잠금이 화면에 그려지는 것 (Wasm 에서는 이벤트 루프가 막혀 JVM 에만 둔다) | jvm |
| `shared/src/jvmTest` | `JarvisKoinTest` — 기능들의 Koin 모듈을 합친 그래프가 모든 정의를 해석한다 | jvm |

`skikoTest`는 컨벤션 플러그인이 `applyDefaultHierarchyTemplate`으로 정의한 중간 소스셋이라 jvm/wasmJs/ios가 함께 쓴다.
Android는 호스트에 렌더링할 Android 런타임이 없어 이 그룹에서 빠진다.

UI 테스트는 가짜 **리포지토리**만 끼워서 돈다. 기능의 `ui`가 `data`를 의존하지 않는다는 것이 테스트로 드러난다.

```bash
./gradlew build                     # 전부
./gradlew jvmTest                   # JVM (모든 모듈)
./gradlew testAndroidHostTest       # Android host
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew wasmJsBrowserTest       # Wasm (헤드리스 Chrome 필요)
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  ./gradlew iosSimulatorArm64Test   # iOS (Xcode 필요)
```

## 문서

스펙은 [`docs/index.html`](docs/index.html)에서 시작한다. 코드를 구현하기 전에 스펙을 먼저 완성한다(`CLAUDE.md` 참고).

- [`docs/common`](docs/common/index.html) — 공통 스펙. 기능마다 사용자 지시, 요구사항, 공통 구현, 검증
- [`docs/platform`](docs/platform/index.html) — 플랫폼 스펙. 플랫폼마다 기술 조사, 구현 가능 여부, 우회 방법, 한계

## 플랫폼별 코드 추가하기

그 기능의 `data` 모듈 `src/commonMain`에 `expect`를 선언하고 각 `<target>Main`에 `actual`을 구현한다.
현재는 `platformName`(`feature/appinfo/data`의 `data/appinfo/PlatformName.kt`)이 가장 단순한 예시다.
두 기능 이상이 쓰는 플랫폼 핸들만 `core:data`로 내린다(`PlatformContext`).
`domain` 에는 `expect`를 두지 않고, `ui` 에는 Compose 타입에 묶인 것(`Modifier.keepScreenAwake`, 브라우저 화면, `ImageBitmap` 변환)만 둔다. 그 목록은 [모듈 구조 스펙](docs/common/module-architecture.html#exceptions)에 적혀 있다.

한 타깃에서만 가능한 기능이라면 나머지 `actual`을 "지원하지 않음"으로 두는 쪽을 택했다.
`createSystemScreenAwakeDataSource`가 그렇게 구현되어 있고, 화면에서 카드를 감추는 대신 잠긴 채로 이유를 보여준다.
이유는 [공통 스펙](docs/common/index.html#contract)에 적어 뒀다.

기능을 하나 더할 때는 `feature/<기능>/{domain,data,ui}` 세 모듈을 만들고, 고치는 기존 파일은
`settings.gradle.kts` · `shared`의 모듈 목록 · 카드를 놓는 `FeatureGrid` 뿐이다.
