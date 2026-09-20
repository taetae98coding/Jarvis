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
| `KeepScreenAwake(enabled: Boolean)` | 켜져 있는 동안 화면 꺼짐을 막는 Composable |
| `rememberSettingsStore(): SettingsStore` | 설정을 영구 저장하는 키-값 저장소 |

`KeepScreenAwake`는 `App()` 최상단에서 호출된다. 화면 전환은 그 아래에서 일어나므로 어느 화면에 있든 효과가 유지된다.
상태는 `AppSettings`가 들고 `LocalAppSettings`로 내려간다.

## 지원 현황 요약

| | Android | iOS | JVM | Web |
|---|---|---|---|---|
| 화면 꺼짐 방지 | ✅ | ✅ | macOS·Linux ✅ / Windows ❌ | ✅ (브라우저 지원 시) |
| 설정 영구 저장 | ✅ | ✅ | ✅ | ✅ |
| 앱 종료 후에도 화면 유지 | ❌ | ❌ | ❌ | ❌ |
| 자동 UI 테스트 | ❌ (기기 필요) | ✅ | ✅ | ✅ |

## 모든 플랫폼에 공통인 한계

### 앱이 떠 있지 않으면 화면을 켜 둘 수 없다

어떤 플랫폼에서도 **앱이 종료되거나 화면에서 내려간 상태에서 화면 꺼짐을 막을 수 없다.**
모바일 OS는 화면 꺼짐 방지를 "지금 보이는 앱"의 권한으로 취급하고, 앱이 물러나면 회수한다.
따라서 영구 저장되는 것은 **설정값**이고, 화면 꺼짐 방지는 앱이 다시 떠 있을 때 재적용된다.

### SettingsStore는 동기 API다

`SettingsStore`는 `getBoolean`/`putBoolean` 두 개짜리 동기 인터페이스다.
Compose 본문에서 바로 읽으므로 boolean 몇 개까지는 문제가 없지만, 다음 경우에는 맞지 않는다.

- 값이 커지거나 많아져 읽기가 프레임을 막는 경우
- 마이그레이션, 트랜잭션, 변경 구독(Flow)이 필요한 경우

그때는 `SettingsStore` 구현만 `androidx.datastore`로 갈아끼우면 된다.
DataStore 1.2.1은 android/jvm/ios/wasmJs 변형을 모두 퍼블리시하지만, Web 쪽은 Storage 구현을 직접 넣어야 해서
boolean 하나뿐인 현 시점에는 채택하지 않았다.

### 플랫폼 저장소 구현은 자동 테스트가 없다

테스트는 `InMemorySettingsStore`를 주입해 `AppSettings`의 읽기·쓰기 계약만 검증한다.
실제 `SharedPreferences` / `NSUserDefaults` / `Preferences` / `localStorage` 왕복은 컴파일로만 보장된다.
`java.util.prefs`만 개발 머신에서 직접 왕복 확인했다.
