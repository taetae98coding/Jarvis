# CLAUDE.md

## 플랫폼 지원 범위

Android / iOS / Web은 기능을 전부 구현한다.
**데스크톱(JVM)은 macOS만 구현한다.** Linux·Windows 분기는 새로 만들지 않고, 호출되면 예외 없이 조용히 no-op으로 빠진다.

```kotlin
// 데스크탑은 macOS 만 지원한다. 다른 OS 에는 `caffeinate` 가 없어서 실행이 실패하고 no-op 이 된다.
private fun startIdleInhibitor(): Process? =
    runCatching { ProcessBuilder("caffeinate", "-di").start() }.getOrNull()
```

## 주석 정책

주석은 **코드와 문서를 읽어도 알 수 없는 것**만 남긴다.
지웠을 때 다음 사람이 잘못된 수정을 할 위험이 생기면 남기고, 읽는 시간만 조금 늘어날 뿐이면 지운다.

주석과 문서는 한국어로 쓴다. 식별자, API 이름, 로그·빌드 출력에 그대로 나오는 문자열은 원문을 유지한다.

### 남길 주석

왜 이 선택을 했는지, 무엇을 버렸는지:

```kotlin
// Compose Multiplatform 1.12+ 부터 iosX64(Intel 시뮬레이터) 아티팩트를 더 이상 배포하지 않는다.
listOf(iosArm64(), iosSimulatorArm64())
```

우회 코드가 어떤 버그·동작 때문에 존재하는지 (걷어낼 시점을 알 수 있게 버전이나 이슈 번호와 함께):

```kotlin
override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
    // boolForKey 는 키가 없을 때도 false 를 반환하므로, 기본값은 따로 확인해야 한다.
    if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)
```

외부 제약이 코드 형태를 결정했고 그 이유가 코드에 드러나지 않을 때:

```kotlin
// Modifier.keepScreenOn() 의 데스크탑 경로만 비어 있다. Compose Multiplatform 1.12.0 의
// PlatformContext.setKeepScreenOnEnabled 는 본문이 빈 기본 구현이고 이를 오버라이드하는 Swing/AWT
// 구현이 없어서, JVM 에서는 modifier 가 아무 일도 하지 않는다.
@Composable
internal expect fun PlatformIdleInhibitor(enabled: Boolean)
```

호출 위치·순서에 의미가 있을 때:

```kotlin
// 루트 Surface 에 붙여서 특정 화면의 수명과 무관하게 효과가 유지되도록 한다.
Surface(modifier = Modifier.fillMaxSize().keepScreenAwake(settings.keepScreenAwake)) {
```

### 지울 주석

```kotlin
// 사용자 목록을 가져온다   ← 코드를 그대로 옮긴 설명
fun getUsers(): List<User>

/** 플랫폼 이름 */          ← 선언 이름을 반복하는 KDoc
val platformName: String

// TODO: 리팩터링 필요      ← 언제 누가 무엇을 할지 없는 TODO

// val legacy = oldStore()  ← 주석 처리된 죽은 코드, 히스토리는 git이 관리한다
```

```properties
# Android                   ← 접두사로 이미 드러나는 구획 라벨
android.useAndroidX=true
```

### 유지보수

코드를 고칠 때 주석도 함께 고친다. 코드와 어긋난 주석은 없느니만 못하다.
