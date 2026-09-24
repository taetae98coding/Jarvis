# CLAUDE.md

## 스펙 우선

**코드를 구현하기 전에 항상 스펙 문서를 먼저 완성한다.** 스펙이 끝나기 전에는 코드를 쓰지 않는다.
스펙은 `docs/` 아래 HTML 문서이고, 진입점은 [`docs/index.html`](docs/index.html)이다.

기능 하나를 추가하거나 바꿀 때 순서는 다음과 같다.

1. **공통 스펙** `docs/common/<기능>.html` — 사용자 지시를 원문 그대로 옮기고, 거기서 검증 가능한 요구사항을 뽑는다.
   플랫폼과 무관하게 성립해야 하는 화면·동작·상태와 commonMain 의 `expect` 시그니처까지 적는다.
2. **플랫폼 스펙** `docs/platform/{android,ios,jvm,web}.html` — 네 문서 각각에 같은 `id` 의 기능 섹션을 붙인다.
   플랫폼별로 기술 조사를 하고 구현 가능한지 판정한다.
   - 구현 가능: 어떤 API 로 구현하는지. Compose 가 대신 불러 주면 그 내부가 어떤 플랫폼 API 를 부르는지까지.
   - 기술 한계로 불가: 어떻게 우회했는지. 우회도 안 되면 어떤 대체 동작(0개 표시, no-op 등)을 하는지.
   - 버린 후보도 남긴다. 다음 사람이 같은 조사를 반복하지 않게.
3. 그다음 코드를 구현한다. 구현하다 스펙과 어긋나는 것을 발견하면 코드가 아니라 **스펙부터 고친다.**

새 문서는 `docs/templates/` 를 복사해서 만든다. 섹션 구성과 판정 범례(구현 / 우회 / 조건부 / 불가 / 미검증)는
`docs/index.html` 과 `docs/platform/index.html` 에 있다. 코드를 고칠 때 스펙도 함께 고친다.

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
// Modifier.keepScreenOn() 의 데스크탑 경로만 비어 있다. Compose Multiplatform 1.12.1 의
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
