# JVM (Desktop)

| 항목 | 값 |
|---|---|
| 모듈 | `:desktopApp` + `:shared` |
| Kotlin 타깃 | `jvm()` |
| Java toolchain | 21 |
| 진입점 | `io.github.taetae98coding.jarvis.desktop.MainKt` |
| 패키지 포맷 | Dmg (macOS 전용) |

이 타깃은 **Mac App Store 배포를 전제하지 않는다.** MAS 샌드박스에서는 `ProcessBuilder`로 `caffeinate`·`adb`·
`xcrun`을 띄울 수 없어서, 아래 기능 대부분이 성립하지 않는다.

## platformName

```kotlin
actual val platformName: String = "JVM ${System.getProperty("java.version")}"
```

실행 중인 JRE 버전을 보여준다. OS 이름은 넣지 않는다.

## 화면 꺼짐 방지

**데스크탑은 다른 플랫폼과 달리 Compose의 `Modifier.keepScreenOn()`을 쓸 수 없다.**
CMP 1.12.0의 `PlatformContext.setKeepScreenOnEnabled`는 본문이 `return` 하나인 빈 기본 구현이고,
Swing/AWT 씬 중 이를 오버라이드하는 것이 없어서 JVM에서는 modifier가 아무 일도 하지 않는다.
그래서 `PlatformIdleInhibitor`의 JVM actual만 따로 구현을 갖는다.

JVM에는 유휴 타이머를 막는 표준 API도 없다. 그래서 OS가 제공하는 도구를 자식 프로세스로 띄우고,
토글을 끄거나 컴포지션이 사라질 때 프로세스를 죽인다.

**지원 범위는 macOS뿐이다.** `caffeinate -di`를 띄운다 (`-d` 디스플레이 슬립 방지, `-i` 시스템 유휴 슬립 방지).

Linux(`systemd-inhibit`)와 Windows(`SetThreadExecutionState`) 분기는 반만 동작하는 코드를 남기지 않기 위해 제거했다.
되살리려면 `ScreenAwake.jvm.kt`에 OS 분기를 다시 넣으면 된다.

명령 실행 실패는 `runCatching`으로 삼켜서 `null`이 되고, 토글은 조용히 no-op이 된다.
macOS가 아닌 OS에서는 `caffeinate`가 없으므로 이 경로를 타고 no-op이 된다.

자식 프로세스는 JVM이 죽어도 살아남는다. `caffeinate`가 남으면 앱이 없는데도 화면이 계속 켜져 있으므로,
프로세스를 띄울 때 셧다운 훅을 함께 걸어 정상 종료와 `SIGTERM`까지는 정리한다.

### 한계

- **macOS 외 no-op.** 실행은 되지만 화면 꺼짐 방지는 동작하지 않는다.
- **`SIGKILL` 은 못 막는다.** 훅이 돌지 않아 `caffeinate`가 살아남고 화면이 계속 켜져 있을 수 있다.
- **실패를 알 수 없다.** 명령이 없거나 실행이 막혀도 UI는 켜진 상태로 보인다.

## 앱이 없는 동안의 화면 꺼짐 방지

**없다.** `rememberSystemScreenAwake`의 JVM actual은 `UnsupportedSystemScreenAwake`를 돌려주고 카드는 잠긴다.

macOS에서 시스템 유휴 시간을 바꾸는 건 `pmset`이고 root 권한이 필요하다. `caffeinate`는 프로세스가
살아 있는 동안만 유효해서 앱 종료 뒤를 커버하지 못한다. 앱이 관리자 권한을 요구하는 쪽으로 가지 않기로 했다.

## 에뮬레이터 개수

Android 에뮬레이터와 iOS 시뮬레이터를 각각 "설치된 개수 / 실행 중인 개수"로 센다.
SDK 커맨드라인 도구를 자식 프로세스로 띄우고 표준 출력을 파싱한다.

| 대상 | 실행하는 명령 | 세는 방법 |
|---|---|---|
| Android 설치 | `<sdk>/emulator/emulator -list-avds` | 비어 있지 않은 줄 수 |
| Android 실행 | `<sdk>/platform-tools/adb devices` | `emulator-`로 시작하는 시리얼 수 |
| iOS 설치 | `xcrun simctl list devices available` | UDID가 붙은 줄 수 |
| iOS 실행 | 같은 출력 | 상태가 `Booted`인 줄 수 |

`<sdk>`는 `ANDROID_HOME` → `ANDROID_SDK_ROOT` → `~/Library/Android/sdk` 순으로 찾는다.
마지막은 Android Studio가 macOS에서 쓰는 기본 위치다.

`adb devices`는 시리얼 접두사로 에뮬레이터와 실물 기기를 가른다.
`39061FDJH00CNS`(USB)나 `192.168.0.10:5555`(무선)은 세지 않는다.

`simctl` 출력의 기기 줄은 `iPhone 17 (66C9B671-...-DF9528508CD7) (Shutdown)` 꼴이다.
이름이 아니라 UDID를 기준으로 잡는데, `iPad mini (A17 Pro)`처럼 이름 자체에 괄호가 들어가기 때문이다.

**SDK를 못 찾으면 0개가 아니라 null**을 돌려준다. `simctl`이 없을 때의 iOS도 마찬가지다.
카드는 그 경우 "셀 수 없음"을 보여주므로, 화면의 0은 정말 0개라는 뜻이다.

### 5초마다 다시 센다

에뮬레이터는 이 앱 밖에서 켜지고 지워지는데 그걸 알려주는 이벤트가 없다. `adb track-devices`는 있지만
`simctl`에는 대응물이 없어서, 양쪽을 합친 상태는 폴링으로만 볼 수 있다.

```kotlin
private val emulatorStatuses: SharedFlow<EmulatorStatus> =
    observeByPolling(interval = PollInterval, read = ::countEmulators)
        .shareIn(emulatorScope, SharingStarted.WhileSubscribed(), replay = 1)
```

`shareIn`으로 묶어서 데스크탑 UI와 로컬 에이전트가 같은 값을 본다. 구독자가 둘이어도 SDK 도구는 한 번만
돌고, `replay = 1` 덕분에 나중에 붙는 구독자는 다음 폴링을 기다리지 않는다.

### xcrun이 아니라 Xcode.app을 직접 보는 경우

`xcrun simctl`은 `xcode-select`가 정식 Xcode를 가리킬 때만 동작한다.
Command Line Tools만 선택된 머신에서는 `xcrun: error: unable to find utility "simctl"`로 실패하지만,
`/Applications/Xcode.app/Contents/Developer/usr/bin/simctl`은 그대로 있다.
그래서 `xcrun --find simctl`이 실패하면 이 기본 경로를 한 번 더 본다.

### 출력을 파이프가 아니라 임시 파일로 받는 이유

`adb`는 데몬을 fork 하면서 부모의 표준 출력을 물려받는다.
파이프로 읽으면 `adb devices`가 끝난 뒤에도 데몬이 쓰기 끝을 잡고 있어 읽기가 끝나지 않고,
그러면 10초 타임아웃도 걸리지 않는다. 출력 파일에는 그런 독자가 없다.

### 한계

- **macOS 전제다.** iOS 시뮬레이터는 macOS에만 있고 SDK 기본 경로도 macOS 것만 넣었다. 다른 OS에서는 `ANDROID_HOME`이 잡힐 때 Android 쪽만 동작한다.
- **SDK는 있는데 개별 명령이 실패하면 0개로 보인다.** SDK 디렉터리 자체가 없는 경우만 "셀 수 없음"으로 구분한다.
- **Android 실행 개수는 adb에 묶여 있다.** `platform-tools`가 없으면 에뮬레이터가 떠 있어도 0이 된다.
- **첫 호출이 느릴 수 있다.** `adb devices`가 데몬을 띄워야 하면 1초쯤 걸린다. 프로브는 `Dispatchers.IO`에서 돌아 UI를 막지는 않는다.

## 로컬 에뮬레이터 에이전트

`startEmulatorHostAgent()`가 JDK 내장 `com.sun.net.httpserver`로 작은 서버를 띄운다.
`main()`이 `use {}`로 감싸므로 창을 닫으면 함께 닫힌다.

```
GET http://127.0.0.1:47890/emulators
→ {"android":{"total":0,"running":0},"ios":{"total":11,"running":0}}
```

| 응답 | 언제 |
|---|---|
| `200` + JSON | 정상 |
| `503` | 아직 한 번도 세지 못했다. 0개로 답해서 "셀 수 없음"과 섞이게 하지 않는다 |
| `405` | GET 이외의 메서드 |

헤더는 세 개를 붙인다. `Content-Type: application/json`,
브라우저 클라이언트를 위한 `Access-Control-Allow-Origin: *`,
그리고 개수가 캐시에 남지 않도록 `Cache-Control: no-store`.

- **루프백에만 바인딩한다.** 에뮬레이터의 `10.0.2.2`와 `adb reverse`는 호스트 루프백으로 들어오므로 이걸로 충분하고, 같은 네트워크의 다른 기기에는 열리지 않는다.
- **포트가 이미 쓰이면 조용히 no-op**이 된다. 데스크탑 앱 자신은 에이전트 없이도 개수를 세므로 앱 동작에는 영향이 없다.
- **핸들러에 스레드 풀을 두지 않았다.** 미리 세 둔 값을 직렬화하는 것뿐이라 즉시 끝난다.
- 포트 `47890`은 고정이다. 클라이언트가 포트를 미리 알아야 해서 OS가 골라주는 임의 포트를 쓸 수 없다.

인증이 없다는 점은 [공통 문서](README.md#에이전트는-인증이-없다)에 적어 뒀다.

## 설정 저장

`java.util.prefs.Preferences.userRoot().node("io/github/taetae98coding/jarvis")`를 쓴다.
macOS 백엔드는 `~/Library/Preferences`다. 변경은 `PreferenceChangeListener`가 알려준다.

### 한계

- 앱 전용 저장소가 아니라 **JVM 사용자 전역 설정 트리**의 한 노드다. 앱을 지워도 값이 남는다.
- 값 하나가 8 KB, 키가 80자를 넘을 수 없다.
- 백엔드에 따라 첫 접근 시 경고 로그가 뜰 수 있다.

## 빌드·실행

```bash
./gradlew :desktopApp:run                # 실행
./gradlew :desktopApp:createDistributable  # 네이티브 번들
```

기본 창 크기는 900 × 640 dp다.

## 테스트

```bash
./gradlew :shared:jvmTest
```

`PlatformTest`, `AppSettingsTest`, `ObserveSystemStateTest`, `HostAgentTest`, `AppTest`(Compose UI),
`EmulatorParsingTest`, `HostAgentServerTest` 전부 돌아간다.
`runComposeUiTest`가 Skiko 네이티브 런타임을 필요로 해서 `jvmTest`에 `compose.desktop.currentOs`를 넣어 둔다.

`EmulatorParsingTest`는 `simctl`/`adb`/`emulator` 출력 문자열만 가지고 개수 계산을 검증한다.
명령을 실제로 띄우는 부분은 테스트하지 않는다 — 머신에 SDK가 깔려 있는지에 결과가 달라지기 때문이다.

`HostAgentServerTest`는 에이전트를 빈 포트에 실제로 띄우고 `HttpURLConnection`으로 왕복한다.
고정 포트를 쓰면 개발자 머신에서 실제로 도는 에이전트와 부딪히므로, `ServerSocket(0)`으로 빈 포트를
받아 쓴다. 포트를 놓아준 뒤 에이전트가 잡기까지 이론적인 경합이 있다.
