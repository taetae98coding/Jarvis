# JVM (Desktop)

| 항목 | 값 |
|---|---|
| 모듈 | `:desktopApp` + `:shared` |
| Kotlin 타깃 | `jvm()` |
| Java toolchain | 21 |
| 진입점 | `io.github.taetae98coding.jarvis.desktop.MainKt` |
| 패키지 포맷 | Dmg / Msi / Deb |

## platformName

```kotlin
actual val platformName: String = "JVM ${System.getProperty("java.version")}"
```

실행 중인 JRE 버전을 보여준다. OS 이름은 넣지 않는다.

## 화면 꺼짐 방지

JVM에는 유휴 타이머를 막는 표준 API가 없다. 그래서 OS가 제공하는 도구를 자식 프로세스로 띄우고,
토글을 끄거나 컴포지션이 사라질 때 프로세스를 죽인다.

| OS | 실행하는 명령 |
|---|---|
| macOS | `caffeinate -di` (`-d` 디스플레이 슬립 방지, `-i` 시스템 유휴 슬립 방지) |
| Linux | `systemd-inhibit --what=idle --mode=block --why=Jarvis sleep infinity` |
| Windows | **없음 — 토글이 아무 일도 하지 않는다** |

명령 실행 실패는 `runCatching`으로 삼켜서 `null`이 되고, 토글은 조용히 no-op이 된다.

### 한계

- **Windows 미지원.** `kernel32.dll`의 `SetThreadExecutionState(ES_CONTINUOUS | ES_DISPLAY_REQUIRED | ES_SYSTEM_REQUIRED)`를 호출해야 하는데, JNA 같은 네이티브 브릿지 의존성이 필요해 연결하지 않았다.
- **Linux는 systemd 전제.** `systemd-inhibit`이 없는 배포판에서는 실패하고 no-op이 된다.
- **강제 종료 시 자식 프로세스가 남는다.** 창을 닫으면 컴포지션이 정리되며 `destroy()`가 불리지만, JVM이 `SIGKILL`로 죽으면 `caffeinate`가 살아남아 화면이 계속 켜져 있을 수 있다. 셧다운 훅을 걸면 `SIGTERM`까지는 막을 수 있다.
- **실패를 알 수 없다.** 명령이 없거나 실행이 막혀도 UI는 켜진 상태로 보인다.

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

- **한 번만 읽는다.** 카드가 화면에 들어올 때 한 번 세고 끝이다. 에뮬레이터를 켜고 끄면 앱을 다시 띄워야 반영된다.
- **macOS 전제다.** iOS 시뮬레이터는 macOS에만 있고 SDK 기본 경로도 macOS 것만 넣었다. 다른 OS에서는 `ANDROID_HOME`이 잡힐 때 Android 쪽만 동작한다.
- **실패와 0개를 구분하지 않는다.** SDK가 없거나 명령이 실패하면 화면에는 0개로 보인다.
- **Android 실행 개수는 adb에 묶여 있다.** `platform-tools`가 없으면 에뮬레이터가 떠 있어도 0이 된다.
- **첫 호출이 느릴 수 있다.** `adb devices`가 데몬을 띄워야 하면 1초쯤 걸린다. 프로브는 `Dispatchers.IO`에서 돌아 UI를 막지는 않는다.

## 설정 저장

`java.util.prefs.Preferences.userRoot().node("io/github/taetae98coding/jarvis")`를 쓴다.
OS별 백엔드는 macOS `~/Library/Preferences`, Linux `~/.java/.userPrefs`, Windows 레지스트리다.

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

`PlatformTest`, `AppSettingsTest`, `AppTest`(Compose UI), `EmulatorParsingTest` 전부 돌아간다.
`runComposeUiTest`가 Skiko 네이티브 런타임을 필요로 해서 `jvmTest`에 `compose.desktop.currentOs`를 넣어 둔다.

`EmulatorParsingTest`는 `simctl`/`adb`/`emulator` 출력 문자열만 가지고 개수 계산을 검증한다.
명령을 실제로 띄우는 부분은 테스트하지 않는다 — 머신에 SDK가 깔려 있는지에 결과가 달라지기 때문이다.
