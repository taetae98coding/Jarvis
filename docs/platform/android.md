# Android

| 항목 | 값 |
|---|---|
| 모듈 | `:androidApp` (앱) + `:shared` (`com.android.kotlin.multiplatform.library`) |
| `applicationId` | `io.github.taetae98coding.jarvis` |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 24 |
| 진입점 | `MainActivity` — `enableEdgeToEdge()` 후 `setContent { App() }` |

## platformName

```kotlin
actual val platformName: String = "Android ${Build.VERSION.SDK_INT}"
```

API 레벨을 그대로 보여준다. 마케팅 버전(예: "Android 17")이 아니라 `37` 같은 정수다.

## 화면 꺼짐 방지

`LocalView.current`의 `keepScreenOn`을 켠다. 내부적으로 윈도우에 `FLAG_KEEP_SCREEN_ON`이 붙는다.

```kotlin
val view = LocalView.current
DisposableEffect(view, enabled) {
    view.keepScreenOn = enabled
    onDispose { view.keepScreenOn = false }
}
```

권한은 필요 없다. `WAKE_LOCK` 퍼미션도 쓰지 않는다.

### 한계

- **윈도우가 보이는 동안만 유효하다.** 홈으로 나가거나 앱을 종료하면 시스템이 플래그를 무시하고, 화면은 평소대로 꺼진다.
- **백그라운드에서 화면을 켜 둘 방법이 없다.** `PowerManager.SCREEN_BRIGHT_WAKE_LOCK`은 API 17에서 deprecated된 뒤 실제로 화면을 켜지 못하고, foreground service를 띄워도 마찬가지다. 이건 구현 누락이 아니라 플랫폼 정책이다.
- 화면 분할·PiP처럼 앱이 부분적으로만 보이는 상태에서의 동작은 OS 재량이라 보장하지 않는다.

## 에뮬레이터 개수

항상 0개다. 앱은 기기 샌드박스 안에 있고, 에뮬레이터 목록을 아는 SDK 도구는 개발자 머신에 있다.
기기가 그 에뮬레이터 자신일 수도 있지만, 그 경우에도 호스트를 들여다볼 방법은 없다.

## 설정 저장

`SharedPreferences` 파일 하나(`jarvis.settings`, `MODE_PRIVATE`)를 쓴다.
`LocalContext.current`에서 `applicationContext`를 꺼내므로 Activity 재생성과 무관하게 같은 파일을 본다.

쓰기는 `apply()`라 비동기로 디스크에 반영된다.

### 한계

- 첫 접근 시 `getSharedPreferences`가 디스크를 읽는다. 지금은 boolean 하나라 무시할 수준이지만 값이 늘면 프레임을 막을 수 있다.
- `apply()`는 커밋을 보장하지 않는다. 쓰기 직후 프로세스가 강제 종료되면 마지막 값이 유실될 수 있다.
- 백업 대상이다(`android:allowBackup="true"`). 기기 이전 시 설정이 따라간다.

## 빌드·실행

```bash
./gradlew :androidApp:assembleDebug     # APK 빌드
./gradlew :androidApp:installDebug      # 기기/에뮬레이터 설치
```

`MainActivity`는 `configChanges`를 넓게 선언해 회전·다크모드 전환 시 Activity가 재생성되지 않는다.
Compose가 직접 변경을 처리한다.

## 테스트

| 테스트 | 실행 위치 |
|---|---|
| `PlatformTest`, `AppSettingsTest` | `./gradlew :shared:testAndroidHostTest` (호스트 JVM) |
| Compose UI 테스트 | **없음** |

Compose UI 테스트(`AppTest`)는 Skiko로 렌더링하는 타깃에서만 돌고 Android는 빠져 있다.
호스트 JVM에는 렌더링할 Android 런타임이 없기 때문이다.

- **Robolectric**: 4.17 + JDK 25 조합에서 `FileDescriptor` 내부 접근이 깨져 부팅하지 못했다.
- **계측 테스트**: Compose Gradle 플러그인 1.12.0과 AGP KMP `withDeviceTest`를 함께 쓰면
  `copyAndroidDeviceTestComposeResourcesToAndroidAssets` 태스크의 `outputDirectory`가 설정되지 않아 빌드가 깨진다.

둘 중 하나가 풀리면 Android도 UI 테스트에 넣을 수 있다.
