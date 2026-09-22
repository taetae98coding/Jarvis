# Android

| 항목 | 값 |
|---|---|
| 모듈 | `:androidApp` (앱) + `:shared` (`com.android.kotlin.multiplatform.library`) |
| `applicationId` | `io.github.taetae98coding.jarvis` |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 24 |
| 권한 | `INTERNET`, `WRITE_SETTINGS` |
| 진입점 | `MainActivity` — `enableEdgeToEdge()` 후 `setContent { App() }` |

## platformName

```kotlin
actual val platformName: String = "Android ${Build.VERSION.SDK_INT}"
```

API 레벨을 그대로 보여준다. 마케팅 버전(예: "Android 17")이 아니라 `37` 같은 정수다.

## 화면 꺼짐 방지

Compose의 `Modifier.keepScreenOn()`을 쓴다. 안드로이드 구현은 `AndroidComposeView`가 요청 수를 세어
`View.setKeepScreenOn(count > 0)`을 부르는 것이고, 결국 윈도우에 `FLAG_KEEP_SCREEN_ON`이 붙는다.

권한은 필요 없다. `WAKE_LOCK` 퍼미션도 쓰지 않는다.

### 한계

- **윈도우가 보이는 동안만 유효하다.** 홈으로 나가거나 앱을 종료하면 시스템이 플래그를 무시하고, 화면은 평소대로 꺼진다. 그 뒤까지 켜 두려면 아래의 전역 토글이 필요하다.
- 화면 분할·PiP처럼 앱이 부분적으로만 보이는 상태에서의 동작은 OS 재량이라 보장하지 않는다.
- Compose 밖에서 같은 `View`의 `keepScreenOn`을 직접 건드리면 Compose의 카운트와 어긋난다. 지금은 그런 코드가 없다.

## 앱이 없는 동안의 화면 꺼짐 방지

`SystemScreenAwake.android.kt`가 `Settings.System.SCREEN_OFF_TIMEOUT`을 직접 늘린다.
앱 윈도우에 붙는 플래그가 아니라 **시스템 전역 값**이라서, 앱을 종료해도 화면이 꺼지지 않는다.

```kotlin
Settings.System.putInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, Int.MAX_VALUE)
```

`Int.MAX_VALUE` 밀리초는 약 24.8일이다. 설정 앱의 선택지(보통 최대 30분)보다 크지만 값 자체에 상한은 없다.

### 왜 이 방법인가

`PowerManager.SCREEN_BRIGHT_WAKE_LOCK`은 API 17에서 deprecated된 뒤 실제로 화면을 켜지 못하고,
foreground service를 띄워도 마찬가지다. 앱의 권한으로는 길이 없어서 시스템 설정을 바꾸는 쪽으로 돌았다.

같은 목적의 다른 경로도 있고, 전부 마켓 배포와는 양립하지 않는다.

| 경로 | 왜 택하지 않았나 |
|---|---|
| `SYSTEM_ALERT_WINDOW` 오버레이 + foreground service | 전역 설정을 건드리지 않아 깔끔하지만 프로세스가 계속 살아 있어야 하고 알림이 상주한다 |
| DeviceOwner (`dpm set-device-owner`) | 가장 확실하지만 공장초기화 상태에서만 프로비저닝된다 |
| `WRITE_SECURE_SETTINGS` + `STAY_ON_WHILE_PLUGGED_IN` | `adb shell pm grant`로만 부여되는 서명 권한이라 설치만으로는 켤 수 없다 |

`WRITE_SETTINGS`는 이 중 **사용자가 앱 안에서 허용할 수 있고 되돌릴 수 있는** 유일한 경로다.

### 권한

`WRITE_SETTINGS`는 런타임 권한 다이얼로그가 없다. `Settings.ACTION_MANAGE_WRITE_SETTINGS` 화면으로 보내
사용자가 직접 켜야 하고, 상태는 `Settings.System.canWrite(context)`로 읽는다.

권한 상태를 알려주는 콜백이 없어서 2초 폴링으로 본다. 사용자가 설정 화면에서 허용하고 돌아온 걸
알아채야 그때 효과를 걸 수 있다. 반면 화면 꺼짐 시간 값은 `ContentObserver`가 알려준다.

lint는 `WRITE_SETTINGS`를 시스템 앱 전용으로 보고 경고하므로 매니페스트에서 `tools:ignore`로 눌러 뒀다.
실제로는 appop으로 부여되는 권한이다.

### 원래 값 되돌리기

남의 설정을 바꾸는 것이므로 되돌릴 값을 우리가 보관한다.
`jarvis.screen_off_timeout` SharedPreferences 파일에 원래 타임아웃(밀리초)을 넣어 두고, 토글을 끌 때 복원한다.

- 토글을 켤 때 **이미 저장값이 있으면 덮어쓰지 않는다.** 앱을 껐다 켜면 효과가 다시 적용되는데, 그때 늘려 둔 값을 원본으로 저장해 버리면 되돌릴 곳을 잃는다.
- boolean만 다루는 `SettingsStore`에 Int를 넣을 수 없고 이 기능이 있는 플랫폼도 Android뿐이라, 공용 저장소가 아니라 여기서만 쓰는 파일에 둔다.

### 한계

- **권한을 잃으면 되돌릴 수 없다.** 사용자가 설정에서 권한을 끈 뒤 토글을 끄면 화면 꺼짐 시간이 늘어난 채로 남는다. 저장값은 지우지 않으므로 권한이 돌아오면 복원된다.
- **앱을 삭제하면 늘려 둔 값이 남는다.** 삭제 시점에 복원할 훅이 없다. 토글을 끄고 지워야 한다.
- **제조사 ROM이 값을 줄일 수 있다.** 전력 관리 정책이 자체적으로 개입하는 경우까지는 막지 못한다.
- **다른 앱이나 설정 앱이 같은 값을 바꾸면 마지막에 쓴 쪽이 이긴다.** `ContentObserver`로 변화를 보고 화면에 반영하긴 하지만, 소유권을 다투지는 않는다.

## 에뮬레이터 개수

기기 샌드박스 안에서는 SDK 도구를 띄울 수 없다. 그래서 같은 머신의 데스크탑 앱이 띄운 에이전트에 물어본다.
자세한 구조는 [공통 문서](README.md#에뮬레이터-개수와-로컬-에이전트)에 있다.

```kotlin
private val HostAliases = listOf("10.0.2.2", "127.0.0.1")
```

`10.0.2.2`는 에뮬레이터가 호스트 머신의 루프백을 부르는 주소다. 실물 기기에는 그런 주소가 없어서
`adb reverse tcp:47890 tcp:47890`으로 포워딩한 뒤 자기 루프백으로 닿는 경로를 함께 둔다.
순서대로 시도해서 처음 응답한 쪽을 쓴다.

평문 HTTP라 `res/xml/network_security_config.xml`에서 이 두 주소에만 cleartext를 허용한다.
개발자 머신마다 TLS 인증서를 심을 수 없어 암호화를 포기하는 대신, 예외 범위를 호스트 루프백으로 좁혔다.

### 한계

- **데스크탑 앱이 떠 있어야 한다.** 꺼져 있으면 연결이 거절되고 카드는 "셀 수 없음"을 보여준다.
- **실물 기기는 `adb reverse`가 필요하다.** 매번 수동으로 걸어야 하고, USB를 뽑으면 끊긴다.
- 요청 타임아웃은 1초다. 에이전트는 미리 세 둔 값을 주므로 그보다 오래 걸리면 없는 것으로 본다.

## 설정 저장

`SharedPreferences` 파일 하나(`jarvis.settings`, `MODE_PRIVATE`)를 쓴다.
`LocalContext.current`에서 `applicationContext`를 꺼내므로 Activity 재생성과 무관하게 같은 파일을 본다.

변경은 `registerOnSharedPreferenceChangeListener`가 알려주고, 쓰기는 `apply()`라 비동기로 디스크에 반영된다.

전역 화면 유지 기능이 쓰는 `jarvis.screen_off_timeout` 파일은 이와 별개다. 사용자 설정이 아니라
되돌릴 값을 적어 두는 내부 기록이라 섞지 않았다.

### 한계

- 첫 접근 시 `getSharedPreferences`가 디스크를 읽는다. 지금은 boolean 두 개라 무시할 수준이지만 값이 늘면 프레임을 막을 수 있다.
- `apply()`는 커밋을 보장하지 않는다. 쓰기 직후 프로세스가 강제 종료되면 마지막 값이 유실될 수 있다.
- 백업 대상이다(`android:allowBackup="true"`). 기기 이전 시 설정이 따라간다.

## 빌드·실행

```bash
./gradlew :androidApp:assembleDebug     # APK 빌드
./gradlew :androidApp:installDebug      # 기기/에뮬레이터 설치

adb reverse tcp:47890 tcp:47890         # 실물 기기에서 에뮬레이터 개수를 보려면
```

`MainActivity`는 `configChanges`를 넓게 선언해 회전·다크모드 전환 시 Activity가 재생성되지 않는다.
Compose가 직접 변경을 처리한다.

## 테스트

| 테스트 | 실행 위치 |
|---|---|
| `PlatformTest`, `AppSettingsTest`, `ObserveSystemStateTest`, `HostAgentTest` | `./gradlew :shared:testAndroidHostTest` (호스트 JVM) |
| Compose UI 테스트 | **없음** |

Compose UI 테스트(`AppTest`)는 Skiko로 렌더링하는 타깃에서만 돌고 Android는 빠져 있다.
호스트 JVM에는 렌더링할 Android 런타임이 없기 때문이다.

- **Robolectric**: 4.17 + JDK 25 조합에서 `FileDescriptor` 내부 접근이 깨져 부팅하지 못했다.
- **계측 테스트**: Compose Gradle 플러그인 1.12.0과 AGP KMP `withDeviceTest`를 함께 쓰면
  `copyAndroidDeviceTestComposeResourcesToAndroidAssets` 태스크의 `outputDirectory`가 설정되지 않아 빌드가 깨진다.

둘 중 하나가 풀리면 Android도 UI 테스트에 넣을 수 있다.

그래서 `WRITE_SETTINGS` 경로와 `10.0.2.2` 요청은 자동 테스트가 없다. 시스템 설정을 실제로 바꾸는
코드라 기기 없이는 검증할 수 없고, 지금은 컴파일과 수동 확인에 의존한다.
