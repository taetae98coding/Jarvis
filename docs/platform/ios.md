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

`UIApplication.sharedApplication.idleTimerDisabled`를 토글한다.

```kotlin
DisposableEffect(enabled) {
    UIApplication.sharedApplication.idleTimerDisabled = enabled
    onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
}
```

권한도, Info.plist 항목도 필요 없다.

### 한계

- **앱이 foreground일 때만 유효하다.** 백그라운드로 가면 시스템이 유휴 타이머를 되살리고, 다시 활성화되면 프로퍼티 값에 따라 재적용된다. 앱 종료 후 화면을 켜 둘 방법은 없다.
- **프로세스 전역 프로퍼티다.** 앱 다른 곳에서 같은 값을 건드리면 마지막에 쓴 쪽이 이긴다. 지금은 `App()` 한 곳에서만 쓴다.
- 저전력 모드나 OS 정책에 따라 무시될 수 있다.

## 에뮬레이터 개수

항상 0개다. iOS에는 서드파티 앱이 쓸 수 있는 프로세스 실행 API가 없어 `simctl`도 `adb`도 부를 수 없다.
Mac의 시뮬레이터 안에서 돌고 있을 때도 마찬가지다.

## 설정 저장

`NSUserDefaults.standardUserDefaults`를 쓴다. 앱 샌드박스 단위라 별도 네임스페이스가 필요 없다.

`boolForKey`는 키가 없을 때도 `false`를 돌려주므로, 기본값을 지키려면 `objectForKey`로 존재 여부를 먼저 본다.

```kotlin
if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)
```

### 한계

- 앱 삭제 시 함께 지워진다. iCloud 동기화는 하지 않는다(`NSUbiquitousKeyValueStore` 아님).
- 쓰기는 즉시 디스크에 동기화되지 않는다. 시스템이 알아서 flush 한다.

## 빌드·실행

```bash
open iosApp/iosApp.xcodeproj
```

Xcode 빌드 시 `Compile Kotlin Framework` 스크립트 단계가 `:shared:embedAndSignAppleFrameworkForXcode`를 호출해
프레임워크를 만들고 앱에 임베드한다. 서명 팀은 `Config.xcconfig`의 `TEAM_ID`에 넣는다.

### 한계

- **Xcode 정식 설치가 필요하다.** Command Line Tools만 있으면 Kotlin 컴파일은 되지만 프레임워크 링크 단계에서 `xcrun`이 실패한다.
- 앱 버전(`MARKETING_VERSION`)은 Gradle의 `appVersion`과 자동으로 연동되지 않는다. 화면에 보이는 값은 공용 `APP_VERSION`이고, 번들 버전은 xcconfig에서 따로 관리한다.

## 테스트

```bash
./gradlew :shared:iosSimulatorArm64Test
```

`PlatformTest`, `AppSettingsTest`, `AppTest`(Compose UI) 전부 돌아간다. 단, 위와 같은 이유로 Xcode가 있어야 한다.
