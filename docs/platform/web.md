# Web (Wasm)

| 항목 | 값 |
|---|---|
| 모듈 | `:webApp` + `:shared` |
| Kotlin 타깃 | `wasmJs` (Kotlin/Wasm, WasmGC) |
| 진입점 | `main()` → `ComposeViewport(document.body!!)` |
| 번들 | webpack, `webApp.js` + `.wasm` 두 개 (앱 ~2 MB, Skiko ~8 MB) |

`index.html`의 스크립트 태그에는 `defer`가 붙어 있다. `document.body`가 파싱된 뒤에 `main()`이 돌아야 하기 때문이다.

## platformName

```kotlin
actual val platformName: String = "Wasm (Kotlin/Wasm)"
```

브라우저 종류나 OS는 넣지 않는다. 필요하면 `navigator.userAgent`를 붙일 수 있다.

## 화면 꺼짐 방지

Compose의 `Modifier.keepScreenOn()`을 쓴다. Compose 내부의 `WakeLockManager`가
[Screen Wake Lock API](https://developer.mozilla.org/docs/Web/API/Screen_Wake_Lock_API)를 호출한다.

```js
navigator.wakeLock.request('screen')  // → WakeLockSentinel
sentinel.release()
```

해제하려면 request가 돌려준 sentinel 객체가 그대로 필요하고, 브라우저는 **탭이 숨겨지면 wake lock을 회수한다.**
둘 다 Compose가 내부에서 처리한다. 예전에는 이걸 직접 `@JsFun` 브릿지와 `globalThis` 전역 세 개로 들고 있었는데,
Compose 구현으로 넘기면서 전부 걷어냈다.

### 한계

- **보안 컨텍스트 필요.** HTTPS 또는 `localhost`에서만 `navigator.wakeLock`이 존재한다. 그 외에는 토글이 no-op이 된다.
- **브라우저 지원 필요.** Chrome/Edge 84+, Safari 16.4+, Firefox 126+. 미지원 브라우저에서는 조용히 아무 일도 일어나지 않는다.
- **실패가 드러나지 않는다.** 배터리 절약 모드처럼 브라우저가 wake lock을 거부해도 UI는 켜진 상태로 보인다.
- **탭을 닫으면 끝.** 다른 플랫폼과 마찬가지로 페이지가 살아 있는 동안만 유효하다.

## 앱이 없는 동안의 화면 꺼짐 방지

**없다.** `rememberSystemScreenAwake`의 wasmJs actual은 `UnsupportedSystemScreenAwake`를 돌려주고 카드는 잠긴다.

브라우저는 OS의 전원 설정에 닿을 수 없다. Screen Wake Lock은 페이지가 살아 있는 동안만 유효하고,
그 바깥을 건드리는 API 자체가 없다.

## 에뮬레이터 개수

브라우저 샌드박스에는 파일시스템도 프로세스 접근도 없다. 호스트의 SDK에 닿으려면 짝이 되는 서버가
필요해서, 데스크탑 앱이 띄운 에이전트를 그 서버로 쓴다.
구조는 [공통 문서](README.md#에뮬레이터-개수와-로컬-에이전트)에 있다.

```kotlin
val response = window.fetch(hostAgentUrl("localhost")).await<Response>()
```

`await`의 타입 인자를 적어 주는 이유는 `Promise`가 out 변성이어서 반환 타입만으로는 추론되지 않기 때문이다.

에이전트는 개발 서버(`:8080`)와 포트가 달라 **교차 출처**가 된다. 단순 GET이라 preflight는 없고,
에이전트가 붙이는 `Access-Control-Allow-Origin: *` 하나로 응답을 읽을 수 있다.

### 한계

- **페이지가 평문 HTTP여야 한다.** 페이지를 HTTPS로 서빙하면 `http://localhost:47890` 요청이 mixed content로 막힌다. 개발 서버(`wasmJsBrowserDevelopmentRun`)는 HTTP라 문제가 없지만, 배포된 HTTPS 페이지에서는 개수가 항상 "셀 수 없음"이다.
- **에이전트가 브라우저와 같은 머신에 있어야 한다.** `localhost`는 페이지를 띄운 머신이다.
- 데스크탑 앱이 꺼져 있으면 `fetch`가 `TypeError`로 거절되고 "셀 수 없음"이 된다. 연결 실패와 잘못된 응답을 가릴 필요가 없어서 둘 다 null로 묶었다.

## 설정 저장

`localStorage`에 `jarvis.settings.<key>` 형태로 저장한다.

`localStorage`는 오리진 전체가 공유하므로, 다른 플랫폼 저장소가 기본으로 갖는 앱 단위 격리가 없다.
그래서 키에 직접 네임스페이스를 붙인다.

변경 신호는 두 갈래를 합친다. `storage` 이벤트는 같은 오리진의 **다른** 문서가 쓴 변경만 알려주므로,
이 문서 자신이 쓴 변경은 `putBoolean`에서 직접 신호를 낸다.

### 한계

- **오리진 단위다.** 같은 오리진의 다른 페이지가 값을 읽거나 덮어쓸 수 있다.
- 시크릿 모드나 저장소 차단 설정에서는 접근이 예외를 던질 수 있다. 지금은 잡지 않는다.
- 문자열만 저장되므로 boolean은 `"true"`/`"false"`로 직렬화된다. `toBooleanStrictOrNull()`이 실패하면 기본값으로 돌아간다.
- 오리진당 대략 5 MB 제한. 설정 몇 개에는 충분하다.

## 빌드·실행

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun   # http://localhost:8080
./gradlew :webApp:wasmJsBrowserDistribution     # 배포 번들
```

### 한계

- Kotlin/Wasm은 **WasmGC를 지원하는 브라우저**가 필요하다(Chrome/Edge 119+, Firefox 120+, Safari 18.2+).
- Skiko wasm이 8 MB를 넘어 첫 로딩이 무겁다. 캐시 헤더를 신경 써야 한다.
- 렌더링이 canvas라 브라우저 텍스트 선택·검색·접근성 트리가 DOM 앱과 다르게 동작한다.

## 테스트

```bash
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew :shared:wasmJsBrowserTest
```

`PlatformTest`, `AppSettingsTest`, `ObserveSystemStateTest`, `HostAgentTest`, `AppTest`(Compose UI)가
헤드리스 Chrome에서 돈다. `CHROME_BIN`을 지정하지 않으면 Karma가 브라우저를 찾지 못한다.

`:shared`의 `wasmJs` 타깃에 `binaries.executable()`이 있는 이유도 이 테스트 때문이다.
실행 바이너리가 없으면 webpack 번들에 Skiko 런타임이 들어가지 않아 UI 테스트가 렌더링하지 못한다.

`fetch`로 실제 요청을 보내는 부분은 테스트하지 않는다. 에이전트가 떠 있는지에 결과가 달라지기 때문이다.
