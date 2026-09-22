# iOS

| 항목 | 값 |
|---|---|
| 모듈 | `iosApp` (Xcode 프로젝트) + `:shared` |
| Kotlin 타깃 | `iosArm64`, `iosSimulatorArm64` |
| 프레임워크 | `Shared` (static) |
| Deployment target | iOS 16.0 |
| Device family | iPhone + iPad (`1,2`) |
| Bundle ID | `iosApp/Configuration/Config.xcconfig`의 `BUNDLE_ID` |
| 진입점 | `iOSApp.swift` → `ContentView` → `MainViewControllerKt.MainViewController()` |

`iosX64`(인텔 시뮬레이터)는 없다. Compose Multiplatform 1.12부터 해당 아티팩트를 퍼블리시하지 않는다.

## platformName

```kotlin
actual val platformName: String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
```

예: `iOS 18.0`.

## 화면 꺼짐 방지

Compose의 `Modifier.keepScreenOn()`을 쓴다. iOS 구현은 `UIKitIdleTimerManager`가 요청자를 집합으로 들고
있다가 `UIApplication.sharedApplication.idleTimerDisabled`를 토글하는 것이다.

권한도, Info.plist 항목도 필요 없다.

### 한계

- **앱이 foreground일 때만 유효하다.** 백그라운드로 가면 시스템이 유휴 타이머를 되살리고, 다시 활성화되면 프로퍼티 값에 따라 재적용된다.
- **프로세스 전역 프로퍼티다.** Compose를 거치는 요청끼리는 `UIKitIdleTimerManager`가 조정해 주지만, Compose 밖에서 `idleTimerDisabled`를 직접 쓰면 마지막에 쓴 쪽이 이긴다.
- 저전력 모드나 OS 정책에 따라 무시될 수 있다.

## 앱이 없는 동안의 화면 꺼짐 방지

**없다.** `rememberSystemScreenAwake`의 iOS actual은 `UnsupportedSystemScreenAwake`를 돌려주고 카드는 잠긴다.

iOS에는 자동 잠금 시간을 읽거나 쓰는 공개 API가 없다. `idleTimerDisabled`는 포그라운드 앱에만 주어지는
권한이고, 그 밖의 경로(SpringBoardServices 계열)는 시스템 권한이 필요해서 **앱스토어 배포를 포기해도
열리지 않는다.** 사이드로드 앱도 같은 샌드박스에 들어가기 때문이다.

`UIBackgroundModes: audio`로 무음 재생을 걸면 프로세스는 살아남지만 화면은 그대로 꺼진다.

## 에뮬레이터 개수

iOS는 서드파티 앱에 프로세스 실행 API를 주지 않아서 `simctl`도 `adb`도 부를 수 없다.
대신 **시뮬레이터에서는 `127.0.0.1`이 시뮬레이터를 띄운 Mac**이므로, 데스크탑 앱이 띄운 에이전트에 물어보면
숫자를 알 수 있다. 구조는 [공통 문서](README.md#에뮬레이터-개수와-로컬-에이전트)에 있다.

요청은 `NSURLSession.sharedSession`으로 보낸다. 평문 HTTP라 ATS가 기본적으로 막으므로 Info.plist에
예외를 열어 뒀다.

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key>
    <true/>
</dict>
```

`NSAllowsLocalNetworking`은 루프백과 로컬 네트워크에만 평문을 허용한다. `NSAllowsArbitraryLoads`처럼
전체를 여는 것이 아니다.

응답 본문은 `NSData`의 바이트를 직접 읽어 UTF-8로 디코딩한다. `NSString`으로 감싼 뒤 `String`으로
캐스팅하는 관용구는 정적 타입이 이어지지 않아 컴파일러가 경고한다.

### 한계

- **실물 기기에서는 항상 "셀 수 없음"이다.** `127.0.0.1`이 기기 자신이고 거기에는 에이전트가 없다. Mac의 LAN 주소를 쓰려면 에이전트를 루프백 밖으로 열어야 해서 하지 않았다.
- 시뮬레이터에서도 데스크탑 앱이 떠 있어야 한다.

## 설정 저장

`NSUserDefaults.standardUserDefaults`를 쓴다. 앱 샌드박스 단위라 별도 네임스페이스가 필요 없다.

`boolForKey`는 키가 없을 때도 `false`를 돌려주므로, 기본값을 지키려면 `objectForKey`로 존재 여부를 먼저 본다.

```kotlin
if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)
```

변경은 `NSUserDefaultsDidChangeNotification`이 알려준다. 어떤 키가 바뀌었는지는 알려주지 않아서
값을 다시 읽고 걸러낸다.

### 한계

- 앱 삭제 시 함께 지워진다. iCloud 동기화는 하지 않는다(`NSUbiquitousKeyValueStore` 아님).
- 쓰기는 즉시 디스크에 동기화되지 않는다. 시스템이 알아서 flush 한다.

## 빌드·실행

```bash
open iosApp/iosApp.xcodeproj
```

Xcode 빌드 시 `Compile Kotlin Framework` 스크립트 단계가 `:shared:embedAndSignAppleFrameworkForXcode`를 호출해
프레임워크를 만들고 앱에 임베드한다. 서명 팀은 `Config.xcconfig`의 `TEAM_ID`에 넣는다.

`xcode-select`가 Command Line Tools를 가리키는 머신에서는 Gradle 태스크에
`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`를 넘겨야 컴파일이 된다.

### 한계

- **Xcode 정식 설치가 필요하다.** Command Line Tools만 있으면 iOS SDK가 없어 컴파일 단계부터 실패한다.
- 앱 버전(`MARKETING_VERSION`)은 Gradle의 `appVersion`과 자동으로 연동되지 않는다. 화면에 보이는 값은 공용 `APP_VERSION`이고, 번들 버전은 xcconfig에서 따로 관리한다.

## 테스트

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  ./gradlew :shared:iosSimulatorArm64Test
```

`PlatformTest`, `AppSettingsTest`, `ObserveSystemStateTest`, `HostAgentTest`, `AppTest`(Compose UI) 전부 돌아간다.
단, 위와 같은 이유로 Xcode가 있어야 한다.

`NSURLSession`으로 실제 요청을 보내는 부분은 테스트하지 않는다. `HostAgentTest`는 프로브에 가짜
fetch 함수를 끼워 폴링과 "셀 수 없음" 처리만 검증한다.
