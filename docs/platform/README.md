# 플랫폼 스펙

Jarvis가 플랫폼마다 다르게 구현하는 기능과, 각 구현이 가진 한계를 정리한 문서다.

- [Android](android.md)
- [iOS](ios.md)
- [JVM (Desktop)](jvm.md)
- [Web (Wasm)](web.md)

## 공통 규약

플랫폼 의존 코드는 전부 `io.github.taetae98coding.jarvis.shared.platform`의 `expect` 선언으로 시작한다.
공용 코드는 `expect` 시그니처만 알고, 실제 API 호출은 각 `<target>Main`의 `actual`이 맡는다.

| `expect` | 역할 |
|---|---|
| `platformName: String` | 실행 중인 플랫폼 표시용 문자열 |
| `PlatformIdleInhibitor(enabled: Boolean)` | Compose가 커버하지 않는 플랫폼의 화면 꺼짐 방지 (JVM 전용) |
| `rememberSettingsStore(): SettingsStore` | 설정을 영구 저장하는 키-값 저장소 |
| `rememberSystemScreenAwake(enabled: Boolean)` | 앱이 없는 동안의 화면 유지 + 그 기능의 상태 (Android 전용) |
| `emulatorProbe: EmulatorProbe` | Android 에뮬레이터·iOS 시뮬레이터 개수를 세는 프로브 |

`emulatorProbe`는 `App()`이 `FeatureGrid`에 넘겨 `EmulatorCard`가 쓴다. 테스트는 여기에 가짜 프로브를 끼운다.

## 상태 조회 규칙

플랫폼 상태를 `Flow`로 노출할 때는 `ObserveSystemState.kt`의 세 함수 중 하나를 쓴다.
시스템이 변경을 알려주는지 아닌지에 따라 갈린다.

| 함수 | 쓰는 경우 |
|---|---|
| `observeOnSignals(signals) { read() }` | 시스템이 콜백을 준다. 신호가 올 때마다 다시 읽는다 |
| `observeByPolling(interval) { read() }` | 콜백이 없다. 간격마다 다시 읽는다 |
| `observeSystemState(signals, interval) { read() }` | 플랫폼마다 콜백 유무가 다르다. `signals`가 null이면 폴링으로 대체 |

어느 쪽이든 **첫 값은 구독 즉시 읽고, 값이 그대로인 방출은 걸러낸다.** 그래서 구독자는 상태가 콜백으로
오는지 폴링으로 오는지 알 필요가 없다.

신호에 값을 담지 않고 매번 다시 읽는다. 플랫폼 콜백이 "무엇이 바뀌었는지" 알려주지 않는 경우가 많아서다
(iOS `NSUserDefaultsDidChangeNotification`, Android `ContentObserver`).

현재 쓰임은 이렇게 나뉘어 있다.

| 상태 | 방식 |
|---|---|
| 설정값 (`SettingsStore.observeBoolean`) | 네 플랫폼 모두 콜백이 있어 콜백. 없는 저장소는 2초 폴링으로 대체 |
| Android 시스템 화면 꺼짐 시간 | `ContentObserver` 콜백 |
| Android `WRITE_SETTINGS` 권한 상태 | 콜백이 없어 2초 폴링 |
| 에뮬레이터 개수 (JVM) | `simctl`에 변경 알림이 없어 5초 폴링 |
| 에뮬레이터 개수 (그 외) | HTTP는 한 방향이라 5초 폴링 |

## 화면 꺼짐 방지

두 층이 있다. 앱이 떠 있는 동안만 유효한 층과, 앱이 없어도 유지되는 층이다.

### 앱이 떠 있는 동안

Compose Multiplatform이 `androidx.compose.ui.keepScreenOn()`을 commonMain에 제공하므로 직접 구현하지 않는다.
`App()`의 루트 `Surface`에 `Modifier.keepScreenAwake(enabled)`로 붙이고, 화면 전환은 그 아래에서 일어나므로
어느 화면에 있든 효과가 유지된다. 상태는 `AppSettings`가 들고 `LocalAppSettings`로 내려간다.

Compose 구현은 **레퍼런스 카운팅**을 한다. 같은 효과를 요청하는 곳이 여럿이어도 마지막 하나가 사라질 때까지 풀리지 않는다.

예외는 데스크탑뿐이다. CMP 1.12.0의 `PlatformContext.setKeepScreenOnEnabled`는 본문이 비어 있고
이를 오버라이드하는 Swing/AWT 구현이 없어서 JVM에서는 modifier가 아무 일도 하지 않는다.
그래서 `PlatformIdleInhibitor` expect/actual을 나란히 두고, JVM actual만 실제 동작을 갖는다.
CMP가 데스크탑 경로를 구현하면 이 expect/actual은 통째로 걷어낼 수 있다.

### 앱이 없는 동안

`rememberSystemScreenAwake(enabled)`가 담당하고 **Android만 구현이 있다.**
시스템 전역 화면 꺼짐 시간을 직접 늘리는 방식이라, 앱의 권한이 아니라 사용자가 따로 허용하는
`WRITE_SETTINGS`가 필요하다. 마켓 심사를 통과하기 어려운 권한이어서 사이드로드를 전제로 둔 기능이다.
자세한 내용은 [Android](android.md#앱이-없는-동안의-화면-꺼짐-방지)에 있다.

나머지 플랫폼은 배포 방식을 포기해도 열리지 않는다. iOS는 SpringBoard 권한이 필요하고, 브라우저는 OS
전원 설정에 닿지 못하며, macOS의 `pmset`은 root를 요구한다.

적용과 상태 읽기가 한 함수인 이유는 둘 다 화면 수명보다 오래 살아야 해서다. 효과는 카드가 스크롤 밖으로
나가도 유지되어야 하고 권한 폴링도 한 곳에서만 돌아야 하므로, `App()`에서 한 번 부르고 상태를 아래로 내린다.

## 에뮬레이터 개수와 로컬 에이전트

가상 기기를 세려면 `emulator` / `adb` / `simctl`을 실행해야 한다. 그게 가능한 건 개발자 머신에서 도는
JVM 타깃뿐이다. 나머지 타깃은 샌드박스 안에 있어서 프로세스를 띄울 수 없다.

그래서 데스크탑 앱이 개수를 세어 로컬 HTTP로 넘겨주고, 다른 타깃은 그걸 읽는다.

```
데스크탑 앱 ──(emulator/adb/simctl)──> 개수
     │
     └─ http://127.0.0.1:47890/emulators
             ▲          ▲            ▲
      10.0.2.2   localhost     localhost
       Android       iOS          Web
      에뮬레이터    시뮬레이터     브라우저
```

| 타깃 | 개수를 얻는 방법 |
|---|---|
| JVM (macOS) | SDK 도구를 직접 실행 |
| Android | `10.0.2.2:47890` (실물 기기는 `adb reverse tcp:47890 tcp:47890` 후 `127.0.0.1`) |
| iOS | `127.0.0.1:47890` (시뮬레이터만. 실물 기기에는 호스트가 없다) |
| Web | `localhost:47890` + CORS |

에이전트는 `startEmulatorHostAgent()`로 데스크탑 앱이 띄운다. 요청마다 세지 않고 5초마다 갱신되는 값을
들고 있다가 그대로 응답하므로, 구독자가 몇이든 SDK 도구는 한 번만 돈다.

## 지원 현황 요약

| | Android | iOS | JVM | Web |
|---|---|---|---|---|
| 화면 꺼짐 방지 (앱이 떠 있을 때) | ✅ Compose | ✅ Compose | ✅ 직접 구현 (macOS 전용) | ✅ Compose (브라우저 지원 시) |
| 화면 꺼짐 방지 (앱이 없을 때) | ✅ `WRITE_SETTINGS` | ❌ | ❌ | ❌ |
| 에뮬레이터 개수 | ✅ 에이전트 | ✅ 에이전트 (시뮬레이터만) | ✅ 직접 (macOS) | ✅ 에이전트 |
| 설정 영구 저장 | ✅ | ✅ | ✅ | ✅ |
| 자동 UI 테스트 | ❌ (기기 필요) | ✅ | ✅ | ✅ |

## 모든 플랫폼에 공통인 한계

### 앱이 없는 동안의 화면 유지는 Android 뿐이다

모바일·브라우저 OS는 화면 꺼짐 방지를 "지금 보이는 앱"의 권한으로 취급하고, 앱이 물러나면 회수한다.
Android만 그 권한 대신 **시스템 설정 자체**를 바꾸는 길이 열려 있다.

그래서 다른 플랫폼에서 영구 저장되는 것은 **설정값**이고, 화면 꺼짐 방지는 앱이 다시 떠 있을 때 재적용된다.

### 에이전트는 인증이 없다

`startEmulatorHostAgent()`는 루프백에만 바인딩하고 토큰도 오리진 검사도 하지 않는다.
같은 머신에서 도는 다른 프로세스는 누구나 에뮬레이터 개수를 읽을 수 있다. 개발자 머신 전용 도구라
그 정도 노출은 받아들였고, 대신 LAN에는 열지 않는다.

같은 이유로 `Access-Control-Allow-Origin: *`을 준다. webApp이 다른 포트에서 서빙되므로 오리진을
고정할 수 없다.

### SettingsStore는 동기 API다

`SettingsStore`는 `getBoolean`/`putBoolean`과 변경 신호(`changes`)로 이루어진 동기 인터페이스다.
Compose 본문에서 바로 읽으므로 boolean 몇 개까지는 문제가 없지만, 다음 경우에는 맞지 않는다.

- 값이 커지거나 많아져 읽기가 프레임을 막는 경우
- 마이그레이션, 트랜잭션, boolean 이외의 타입이 필요한 경우

그때는 `SettingsStore` 구현만 `androidx.datastore`로 갈아끼우면 된다.
DataStore 1.2.1은 android/jvm/ios/wasmJs 변형을 모두 퍼블리시하지만, Web 쪽은 Storage 구현을 직접 넣어야 해서
boolean 두 개뿐인 현 시점에는 채택하지 않았다.

boolean이 아닌 값이 필요해진 예가 이미 하나 있다. Android가 되돌릴 화면 꺼짐 시간(Int)을
`SettingsStore`가 아니라 자기 `SharedPreferences` 파일에 따로 보관한다.

### 플랫폼 저장소 구현은 자동 테스트가 없다

테스트는 `InMemorySettingsStore`를 주입해 `AppSettings`의 읽기·쓰기 계약만 검증한다.
실제 `SharedPreferences` / `NSUserDefaults` / `Preferences` / `localStorage` 왕복은 컴파일로만 보장된다.
`java.util.prefs`만 개발 머신에서 직접 왕복 확인했다.

### 셀 수 없는 것은 0개가 아니라 "셀 수 없음"이다

`EmulatorSummary`가 null이면 그 종류는 셀 방법이 없다는 뜻이고, 카드는 "셀 수 없음"으로 보여준다.
Android SDK를 못 찾았거나 `simctl`이 없거나 에이전트에 닿지 못한 경우가 그렇다.
0개(정말 하나도 없음)와 구분되므로, 화면의 숫자를 그대로 믿을 수 있다.

프로브가 아직 답하지 않은 동안은 세 번째 상태인 "확인 중…"이다.
