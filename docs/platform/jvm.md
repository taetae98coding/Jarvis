# JVM (Desktop)

| 항목 | 값 |
|---|---|
| 모듈 | `:desktopApp` + `:shared` |
| Kotlin 타깃 | `jvm()` |
| Java toolchain | 21 |
| 진입점 | `io.github.taetae98coding.jarvis.desktop.MainKt` |
| 패키지 포맷 | Dmg (macOS 전용) |

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

### 한계

- **macOS 외 no-op.** 실행은 되지만 화면 꺼짐 방지는 동작하지 않는다.
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
