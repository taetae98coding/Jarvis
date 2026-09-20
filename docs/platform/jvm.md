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

`PlatformTest`, `AppSettingsTest`, `AppTest`(Compose UI) 전부 돌아간다.
`runComposeUiTest`가 Skiko 네이티브 런타임을 필요로 해서 `jvmTest`에 `compose.desktop.currentOs`를 넣어 둔다.
