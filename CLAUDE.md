# CLAUDE.md

Kotlin Multiplatform + Compose Multiplatform 앱. Android / iOS / JVM(Desktop) / Wasm(Web) 이 UI 를 공유한다.
구조·버전·실행 방법은 [`README.md`](README.md), 스펙은 [`docs/index.html`](docs/index.html) 에 있다.

## 스펙 우선

코드보다 스펙을 먼저 완성한다. 구현하다 스펙과 어긋나면 코드가 아니라 스펙부터 고친다.

1. **공통 스펙** `docs/common/<기능>.html` — 사용자 지시를 원문 그대로 옮기고 검증 가능한 요구사항(R1, R2…)을 뽑는다.
   화면·동작·상태와 commonMain 의 `expect` 시그니처까지 적고 `docs/common/index.html` 에 링크한다.
2. **플랫폼 스펙** `docs/platform/{android,ios,jvm,web}.html` 네 곳에 같은 `id` 의 섹션을 붙이고 판정한다.
   어떤 API 로 구현하는지(Compose 가 대신 부르면 그 안의 플랫폼 API 까지), 불가면 우회나 대체 동작, 버린 후보까지 남긴다.

새 문서는 `docs/templates/` 를 복사한다. 판정 범례(구현 / 우회 / 조건부 / 불가 / 미검증)는 `docs/platform/index.html` 에 있다.

## 플랫폼 범위

Android / iOS / Web 은 전부 구현한다. **데스크톱은 macOS 만** 구현하고, Linux·Windows 분기는 만들지 않는다.
다른 OS 에서 호출되면 예외 없이 no-op 으로 빠진다(예: `runCatching { ProcessBuilder("caffeinate", "-di").start() }.getOrNull()`).

## 모듈 구조

[모듈 구조](docs/common/module-architecture.html) · [의존성 주입](docs/common/dependency-injection.html) · [ViewModel](docs/common/view-model.html) 스펙을 따른다.

- 기능마다 `feature:<기능>:{domain,data,ui}` 셋, Android 시스템 표면(위젯·알림·타일)이 있으면 `widget` 을 더한다.
  의존은 `ui → domain ← data`(`widget → domain`) 한 방향이고, **기능끼리는 서로 의존하지 않는다.**
- `core:*` 에는 두 기능 이상이 쓰는 것만 둔다. 한 기능만 쓰면 그 기능이 갖는다.
- `domain` 은 Compose·플랫폼 API 없이 `kotlinx-coroutines-core` 와 `koin-core` 만 쓴다.
  플랫폼 API 의 `expect`/`actual` 은 `data`(두 기능 이상이면 `core`) 에 둔다. Compose 타입에 묶여 `ui` 에 둘 수밖에 없으면
  [예외 표](docs/common/module-architecture.html#exceptions) 에 한 줄을 더한다.
- 기능마다 `<기능>DomainModule` / `DataModule` / `UiModule` 을 내놓는다. 리포지토리는 `single`, 유스케이스는 `factory`,
  ViewModel 은 `viewModel` 로 등록하고 화면은 `koinViewModel()` 로만 받는다.
- ViewModel 은 `internal` 이고 생성자로 유스케이스만 받는다. 기능이 밖에 내놓는 것은 인자 없는 카드·화면과 Koin 모듈뿐이다.
- 타깃 선언은 `build-logic` 컨벤션 플러그인에만 있다. 모듈 빌드 파일에는 그 모듈만 다른 것을 적는다.
- 새 기능이 고치는 기존 파일은 `settings.gradle.kts`, `shared` 의 Koin 모듈 목록, 카드를 놓는 `FeatureGrid` 뿐이다.

## 상태 조회

[상태 조회 규칙](docs/common/state-observation.html) 을 따른다.

- 바뀌는 상태는 `Flow` 로 받는다. `suspend fun getX(): T` 조회 API 를 만들지 않는다. `suspend` 는 명령(쓰기·요청)에만 쓰고,
  명령이 지금 값을 알아야 하면 `observeX().first()` 로 읽는다. 동기 스냅샷 `readX()` 는 `stateIn` 초기값으로만 쓴다.
- 콜백부터 찾는다. 리스너·알림·콜백이 있으면 `callbackFlow` 로 감싸 `observeOnSignals` 에 넘기고, 정말 없을 때만
  `observeByPolling(<이름 붙은 Duration 상수>)` 로 읽는다. 콜백이 없는 이유와 버린 후보는 플랫폼 스펙에 남긴다.
- Flow 는 cold 로 내놓는다. 수집이 시작될 때 등록하고 `awaitClose` 에서 해제한다. data·domain 은 `StateFlow`·`SharedFlow` 를
  노출하지 않고 앱 수명 스코프로 `stateIn`·`shareIn` 하지 않는다(비싼 조회만 `shareIn(WhileSubscribed(replayExpirationMillis = 0))`).
- ViewModel 은 `stateIn(viewModelScope, WhileSubscribed(), 초기값)`, 화면은 `collectAsStateWithLifecycle()`.
  `Eagerly` 는 구독과 무관하게 이어져야 하는 작업에만, 이유를 주석으로 적고 쓴다.

## UI

[디자인 시스템](docs/common/design-system.html) 을 따른다.

- `feature/*/ui`, `app/ui`, `core/ui` 는 `dp`·`sp` 숫자와 `Color(…)` 리터럴을 직접 쓰지 않고 `JarvisTheme` 토큰이나 `*Defaults` 를 거친다.
- 기본값이 있는 컴포넌트는 `<컴포넌트>Defaults` 를 갖고, 모양·여백·상태 변화는 Style API(`Style { … }`) 로 정의한다.
- 아이콘은 `JarvisIcons` 로 그린다. `←`·`×` 같은 글자 아이콘을 쓰지 않고, 아이콘만 있는 버튼은 `contentDescription` 을 준다.

## 빌드와 검증

- 워크트리에는 `local.properties` 가 없다. Android 태스크 전에 `sdk.dir` 을 적는다.
- 끝내기 전에 바꾼 모듈의 테스트를 돌린다. 여러 모듈을 건드렸으면 `./gradlew build` 로 전부 돌린다.

```bash
printf 'sdk.dir=%s\n' "$HOME/Library/Android/sdk" > local.properties
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  ./gradlew build        # 전 타깃 컴파일·lint·테스트·iOS release 링크·checkIosAppVersion (처음부터면 1시간 안팎)
./gradlew jvmTest        # 빠른 확인
```

- `jvmTest` 만으로는 `compileCommonMainKotlinMetadata` 오류가 잡히지 않는다. commonMain 은 모든 타깃 stdlib 에 있는 API 만 쓴다
  (예: `MatchGroup.range` 는 플랫폼마다 있지만 common 에는 없다). `:<모듈>:compileCommonMainKotlinMetadata` 로 확인한다.
- Wasm UI 테스트에서 `waitUntil` 은 하나뿐인 이벤트 루프를 막는다. 그동안 `delay`·`withTimeout`·`Dispatchers.Default` 로 넘긴 일은
  끝나지 못하므로, 그것을 기다리는 테스트는 `@IgnoreOnWasm` 을 붙이고 이유를 적는다([웹 테스트](docs/platform/web.html#test)).
- 머신 부하가 높으면 UI 테스트가 시간 초과로 흔들린다. 실패하면 코드를 고치기 전에 `--tests '<클래스>'` 로 따로 돌려 재현되는지부터 본다.

- 앱 버전은 `gradle/libs.versions.toml` 의 `appVersion` 과 `iosApp/Configuration/Config.xcconfig` 두 곳을 함께 고친다(`checkIosAppVersion`).
- 커밋 메시지는 한국어로, 제목은 사용자가 겪는 변화를 한 문장으로("~하게 함", "~을 고침") 쓴다.
  본문은 `-` 목록으로 동작을 적고 마지막 줄에 `스펙: docs/common/<기능>.html` 을 단다.

## 주석

주석은 **코드와 문서를 읽어도 알 수 없는 것**만 남긴다. 지웠을 때 다음 사람이 잘못 고칠 위험이 있으면 남긴다.
한국어로 쓰고, 식별자·API 이름·로그와 빌드 출력의 문자열은 원문을 유지한다.

- 남긴다: 왜 이렇게 했고 무엇을 버렸는지, 우회가 어떤 버그 때문인지(걷어낼 시점을 알 수 있게 버전·이슈 번호와 함께),
  코드에 드러나지 않는 외부 제약, 호출 위치·순서의 의미.
- 지운다: 코드를 옮겨 적은 설명, 선언 이름을 되풀이하는 KDoc, 누가 언제 할지 없는 TODO, 주석 처리한 죽은 코드, 구획 라벨.

```kotlin
override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
    // boolForKey 는 키가 없을 때도 false 를 반환하므로, 기본값은 따로 확인해야 한다.
    if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)
```

코드를 고칠 때 주석과 스펙도 함께 고친다. 어긋난 주석은 없느니만 못하다.
