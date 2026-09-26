package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationStatus
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease
import io.github.taetae98coding.jarvis.ui.appinfo.AppUpdateButtonTestTag
import io.github.taetae98coding.jarvis.ui.appinfo.AppUpdateTestTag
import io.github.taetae98coding.jarvis.ui.appinfo.DeviceIdLabel
import io.github.taetae98coding.jarvis.ui.appinfo.DeviceNameLabel
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationStatus
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorFrameTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorListTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorScreenTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorScreenWakeTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorDeviceTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorLaunchTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorPlatformListTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorWakeTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationBackwardTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationForwardTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationLockTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationNotificationTestTag
import io.github.taetae98coding.jarvis.ui.rotation.deviceRotationAngleTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepScreenAwakeTestTag
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryHealth
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.battery.PowerSource
import io.github.taetae98coding.jarvis.domain.profiling.DiskActivity
import io.github.taetae98coding.jarvis.domain.profiling.DiskSpace
import io.github.taetae98coding.jarvis.domain.profiling.MemoryUsage
import io.github.taetae98coding.jarvis.domain.profiling.NetworkThroughput
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingScope
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import io.github.taetae98coding.jarvis.domain.profiling.Usage
import io.github.taetae98coding.jarvis.ui.theme.themeModeOptionTestTag
import io.github.taetae98coding.jarvis.ui.battery.BatteryChargingTestTag
import io.github.taetae98coding.jarvis.ui.battery.BatteryHealthTestTag
import io.github.taetae98coding.jarvis.ui.battery.BatteryLevelTestTag
import io.github.taetae98coding.jarvis.ui.battery.BatteryLoading
import io.github.taetae98coding.jarvis.ui.battery.BatteryLowPowerModeTestTag
import io.github.taetae98coding.jarvis.ui.battery.BatteryNone
import io.github.taetae98coding.jarvis.ui.battery.BatteryPowerSourceTestTag
import io.github.taetae98coding.jarvis.ui.battery.BatteryStatusTestTag
import io.github.taetae98coding.jarvis.ui.battery.BatteryTemperatureTestTag
import io.github.taetae98coding.jarvis.ui.profiling.ProfilingMeasuring
import io.github.taetae98coding.jarvis.ui.profiling.ProfilingUnavailable
import io.github.taetae98coding.jarvis.ui.profiling.profilingRowTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeNotificationTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeToggleTestTag
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisAppTest {
    @Test
    fun showsAppVersion() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithText(TestAppInfo.version).assertIsDisplayed()
    }

    @Test
    fun showsCurrentPlatformName() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithText(TestAppInfo.platform).assertIsDisplayed()
    }

    @Test
    fun showsDeviceNameAndId() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithText(DeviceNameLabel).assertIsDisplayed()
        onNodeWithText(TestAppInfo.deviceName).assertIsDisplayed()
        onNodeWithText(DeviceIdLabel).assertIsDisplayed()
        onNodeWithText(TestAppInfo.deviceId).assertIsDisplayed()
    }

    @Test
    fun hidesBlankDeviceRows() = runComposeUiTest {
        setContent { TestJarvisApp(appInfo = TestAppInfo.copy(deviceName = "", deviceId = "")) }

        onNodeWithText(DeviceNameLabel).assertDoesNotExist()
        onNodeWithText(DeviceIdLabel).assertDoesNotExist()
    }

    @Test
    fun appUpdateRowIsHiddenWithoutNewVersion() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onAllNodesWithTag(AppUpdateTestTag).assertCountEquals(0)
    }

    @Test
    fun appUpdateRowShowsNewVersion() = runComposeUiTest {
        setContent { TestJarvisApp(appUpdate = FakeAppUpdateRepository(TestRelease)) }

        onNodeWithText(TestRelease.version).assertIsDisplayed()
        onNodeWithText("업데이트").assertIsDisplayed()
    }

    @Test
    fun appUpdateButtonInstallsAndLocks() = runComposeUiTest {
        val appUpdate = FakeAppUpdateRepository(TestRelease).apply { gate = CompletableDeferred() }
        setContent { TestJarvisApp(appUpdate = appUpdate) }

        onNodeWithTag(AppUpdateButtonTestTag).performClick()
        waitForIdle()

        assertEquals(listOf(TestRelease), appUpdate.installed)
        onNodeWithText("설치 중…").assertIsDisplayed()
        onNodeWithTag(AppUpdateButtonTestTag).assertIsNotEnabled()
    }

    @Test
    fun appUpdateFailureShowsReasonAndRetry() = runComposeUiTest {
        val appUpdate = FakeAppUpdateRepository(TestRelease, failure = "체크섬이 맞지 않습니다.")
        setContent { TestJarvisApp(appUpdate = appUpdate) }

        onNodeWithTag(AppUpdateButtonTestTag).performClick()
        waitForIdle()

        onNodeWithText("업데이트하지 못했습니다: 체크섬이 맞지 않습니다.").assertIsDisplayed()
        onNodeWithText("다시 시도").assertIsDisplayed()

        appUpdate.failure = null
        onNodeWithTag(AppUpdateButtonTestTag).performClick()
        waitForIdle()

        assertEquals(2, appUpdate.installed.size)
    }

    @Test
    fun keepScreenAwakeDefaultsToOff() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOff()
    }

    @Test
    fun keepScreenAwakeTogglesOn() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick().assertIsOn()
    }

    @Test
    fun keepScreenAwakeIsPersisted() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        setContent { TestJarvisApp(settings = settings) }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick()

        assertTrue(settings.keepScreenAwake.value)
    }

    @Test
    fun keepScreenAwakeIsRestoredOnRelaunch() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository(keepScreenAwake = true)

        setContent { TestJarvisApp(settings = settings) }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOn()
    }

    @Test
    fun keepScreenAwakeFollowsChangesMadeOutsideTheApp() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        setContent { TestJarvisApp(settings = settings) }

        settings.setKeepScreenAwake(true)

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOn()
    }

    // 전역 화면 유지는 Android 만 지원한다. Skiko 로 렌더링하는 타깃에서는 토글이 잠겨 있어야 한다.
    @Test
    fun themeModeDefaultsToSystem() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(themeModeOptionTestTag(ThemeMode.SYSTEM)).assertIsSelected()
        onNodeWithTag(themeModeOptionTestTag(ThemeMode.LIGHT)).assertIsNotSelected()
        onNodeWithTag(themeModeOptionTestTag(ThemeMode.DARK)).assertIsNotSelected()
    }

    @Test
    fun themeModeSelectsOneOfThree() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(themeModeOptionTestTag(ThemeMode.DARK)).performClick().assertIsSelected()

        onNodeWithTag(themeModeOptionTestTag(ThemeMode.SYSTEM)).assertIsNotSelected()
        onNodeWithTag(themeModeOptionTestTag(ThemeMode.LIGHT)).assertIsNotSelected()
    }

    @Test
    fun themeModeIsPersisted() = runComposeUiTest {
        val theme = FakeThemeSettingsRepository()
        setContent { TestJarvisApp(theme = theme) }

        onNodeWithTag(themeModeOptionTestTag(ThemeMode.LIGHT)).performClick()

        assertEquals(ThemeMode.LIGHT, theme.themeMode.value)
    }

    @Test
    fun themeModeIsRestoredOnRelaunch() = runComposeUiTest {
        setContent { TestJarvisApp(theme = FakeThemeSettingsRepository(mode = ThemeMode.DARK)) }

        onNodeWithTag(themeModeOptionTestTag(ThemeMode.DARK)).assertIsSelected()
    }

    @Test
    fun themeModeFollowsChangesMadeOutsideTheApp() = runComposeUiTest {
        val theme = FakeThemeSettingsRepository()
        setContent { TestJarvisApp(theme = theme) }

        theme.themeMode.value = ThemeMode.LIGHT
        waitForIdle()

        onNodeWithTag(themeModeOptionTestTag(ThemeMode.LIGHT)).assertIsSelected()
    }

    @Test
    fun themeModeReachesThePlatform() = runComposeUiTest {
        val applied = mutableListOf<ThemeMode>()
        setContent { TestJarvisApp(themeAppearance = { applied += it }) }

        onNodeWithTag(themeModeOptionTestTag(ThemeMode.DARK)).performClick()
        waitForIdle()

        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.DARK), applied)
    }

    @Test
    fun profilingCardShowsEveryMetric() = runComposeUiTest {
        val profiling = FakeProfilingRepository()
        profiling.profiling.value = Profiling(
            cpu = Reading.Available(Usage(22.6)),
            memory = Reading.Available(MemoryUsage(usedBytes = 27_600_000_000, totalBytes = 34_400_000_000)),
            gpu = Reading.Available(Usage(15.0)),
            network = Reading.Available(NetworkThroughput(receivedBytesPerSecond = 1_200_000, sentBytesPerSecond = 30_000)),
            diskActivity = Reading.Available(DiskActivity(readPercent = 3.0, writePercent = 1.0)),
            diskSpace = Reading.Available(DiskSpace(freeBytes = 120_000_000_000, totalBytes = 494_000_000_000)),
        )
        setContent { TestJarvisApp(profiling = profiling) }

        onNodeWithTag(profilingRowTestTag(ProfilingMetric.CPU)).assertTextEquals("CPU", "23%")
        onNodeWithTag(profilingRowTestTag(ProfilingMetric.MEMORY)).assertTextEquals("메모리", "27.6 GB / 34.4 GB (80%)")
        onNodeWithTag(profilingRowTestTag(ProfilingMetric.GPU)).assertTextEquals("GPU", "15%")
        onNodeWithTag(profilingRowTestTag(ProfilingMetric.NETWORK)).assertTextEquals("네트워크", "↓ 1.2 MB/s · ↑ 30 KB/s")
        onNodeWithTag(profilingRowTestTag(ProfilingMetric.DISK_ACTIVITY)).assertTextEquals("디스크 읽기·쓰기", "읽기 3% · 쓰기 1%")
        onNodeWithTag(profilingRowTestTag(ProfilingMetric.DISK_SPACE)).assertTextEquals("디스크 남은 용량", "120 GB / 494 GB")
    }

    @Test
    fun profilingCardMarksUnavailableAndMeasuring() = runComposeUiTest {
        // 첫 표본이 오기 전이다. 지원 목록만으로 첫 프레임이 정해진다.
        val profiling = FakeProfilingRepository(supportedMetrics = setOf(ProfilingMetric.CPU, ProfilingMetric.DISK_SPACE))
        setContent { TestJarvisApp(profiling = profiling) }

        onNodeWithTag(profilingRowTestTag(ProfilingMetric.CPU)).assertTextEquals("CPU", ProfilingMeasuring)
        onNodeWithTag(profilingRowTestTag(ProfilingMetric.DISK_SPACE)).assertTextEquals("디스크 남은 용량", ProfilingMeasuring)
        listOf(ProfilingMetric.MEMORY, ProfilingMetric.GPU, ProfilingMetric.NETWORK, ProfilingMetric.DISK_ACTIVITY).forEach {
            onNodeWithTag(profilingRowTestTag(it)).assertTextContains(ProfilingUnavailable)
        }
    }

    @Test
    fun profilingCardMarksAppScope() = runComposeUiTest {
        val profiling = FakeProfilingRepository()
        profiling.profiling.value = Profiling.initial(ProfilingMetric.entries.toSet()).copy(
            cpu = Reading.Available(Usage(5.0, ProfilingScope.APP)),
            memory = Reading.Available(MemoryUsage(usedBytes = 1, totalBytes = 2)),
        )
        setContent { TestJarvisApp(profiling = profiling) }

        onNodeWithTag(profilingRowTestTag(ProfilingMetric.CPU)).assertTextEquals("CPU (이 앱)", "5%")
        // 기기 전체를 잰 줄에는 붙지 않는다.
        onNodeWithTag(profilingRowTestTag(ProfilingMetric.MEMORY)).assertTextEquals("메모리", "1 B / 2 B (50%)")
    }

    @Test
    fun batteryCardShowsEveryRow() = runComposeUiTest {
        val battery = FakeBatteryRepository()
        battery.status.value = BatteryStatus.Available(
            Battery(
                levelPercent = 87,
                charging = ChargingState.CHARGING,
                powerSource = PowerSource.USB,
                temperatureCelsius = 31.5,
                health = BatteryHealth.GOOD,
                lowPowerMode = false,
            ),
        )
        setContent { TestJarvisApp(battery = battery) }

        onNodeWithTag(BatteryLevelTestTag).assertTextEquals("잔량", "87%")
        onNodeWithTag(BatteryChargingTestTag).assertTextEquals("상태", "충전 중")
        onNodeWithTag(BatteryPowerSourceTestTag).assertTextEquals("전원", "USB")
        onNodeWithTag(BatteryTemperatureTestTag).assertTextEquals("온도", "31.5°C")
        onNodeWithTag(BatteryHealthTestTag).assertTextEquals("건강", "좋음")
        onNodeWithTag(BatteryLowPowerModeTestTag).assertTextEquals("저전력 모드", "꺼짐")
    }

    @Test
    fun batteryCardHidesRowsThePlatformDoesNotGive() = runComposeUiTest {
        val battery = FakeBatteryRepository()
        battery.status.value = BatteryStatus.Available(Battery(levelPercent = 40, charging = ChargingState.DISCHARGING))
        setContent { TestJarvisApp(battery = battery) }

        onNodeWithTag(BatteryChargingTestTag).assertTextEquals("상태", "방전 중")
        listOf(BatteryPowerSourceTestTag, BatteryTemperatureTestTag, BatteryHealthTestTag, BatteryLowPowerModeTestTag).forEach {
            onNodeWithTag(it).assertDoesNotExist()
        }
    }

    @Test
    fun batteryCardShowsLoadingThenNoBattery() = runComposeUiTest {
        val battery = FakeBatteryRepository()
        setContent { TestJarvisApp(battery = battery) }

        onNodeWithTag(BatteryStatusTestTag).assertTextEquals("상태", BatteryLoading)

        battery.status.value = BatteryStatus.NoBattery
        waitForIdle()

        onNodeWithTag(BatteryStatusTestTag).assertTextEquals("상태", BatteryNone)
        onNodeWithTag(BatteryLevelTestTag).assertDoesNotExist()
    }

    @Test
    fun systemScreenAwakeIsLockedWhereItIsNotSupported() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        setContent { TestJarvisApp(settings = settings) }

        onNodeWithTag(KeepSystemScreenAwakeToggleTestTag)
            .assertIsNotEnabled()
            .performClick()
            .assertIsOff()

        assertFalse(settings.keepSystemScreenAwake.value)
    }

    @Test
    fun systemScreenAwakeExplainsWhyItIsUnavailable() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithText("이 플랫폼에서는 앱이 없는 동안의 화면 꺼짐을 막을 수 없습니다.").assertIsDisplayed()
    }

    // 지원하는 플랫폼(Android)의 화면은 가짜 상태로만 확인할 수 있다. Android 에는 UI 테스트가 없다.
    @Test
    fun systemScreenAwakeTogglesWhereItIsSupported() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = true),
        )
        setContent {
            TestJarvisApp(settings = settings, systemScreenAwake = system)
        }

        onNodeWithTag(KeepSystemScreenAwakeToggleTestTag).performClick().assertIsOn()

        assertTrue(settings.keepSystemScreenAwake.value)
    }

    // 알림 컨트롤은 Android 만 만든다. Skiko 로 렌더링하는 타깃에서는 "알림에 고정" 행 자체가 없어야 한다.
    @Test
    fun notificationRowIsHiddenWhereNotificationsAreNotSupported() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(DeviceRotationNotificationTestTag).assertDoesNotExist()
        onNodeWithTag(KeepSystemScreenAwakeNotificationTestTag).assertDoesNotExist()
        onAllNodesWithText("알림에 고정").assertCountEquals(0)
    }

    @Test
    fun systemScreenAwakeNotificationTogglesWhereItIsSupported() = runComposeUiTest {
        val notification = FakeSystemScreenAwakeNotificationRepository(
            SystemScreenAwakeNotificationStatus(supported = true, permitted = true),
        )
        setContent { TestJarvisApp(systemScreenAwakeNotification = notification) }

        onNodeWithTag(KeepSystemScreenAwakeNotificationTestTag).assertIsOff().performClick().assertIsOn()

        assertTrue(notification.status.value.pinned)
    }

    // 회전을 지원하지 않으면 회전 카드는 지원하는 카드 열둘 뒤로 밀려 뷰포트 밖에 놓인다. 알림을 만들 수 있는
    // 플랫폼(Android)은 회전도 지원하므로 아래 테스트와 같이 회전을 지원으로 둔다.
    @Test
    fun deviceRotationNotificationTogglesWhereItIsSupported() = runComposeUiTest {
        val notification = FakeDeviceRotationNotificationRepository(
            DeviceRotationNotificationStatus(supported = true, permitted = true),
        )
        setContent { TestJarvisApp(deviceRotation = rotatable(), deviceRotationNotification = notification) }

        onNodeWithTag(DeviceRotationNotificationTestTag).assertIsOff().performClick().assertIsOn()

        assertTrue(notification.status.value.pinned)
    }

    // 권한이 없어도 값은 켜진 채 남고, 그 사실이 스위치 아래에 보인다. 알림을 만들 수 있는 플랫폼(Android)은 회전도
    // 지원하므로 회전을 지원으로 두어야 카드가 홈 첫 줄에 와서 뷰포트 안에 다 보인다(docs/common/home-adaptive-layout.html R1).
    @Test
    fun notificationRowExplainsMissingPermission() = runComposeUiTest {
        val notification = FakeDeviceRotationNotificationRepository(
            DeviceRotationNotificationStatus(supported = true, permitted = false, pinned = true),
        )
        setContent { TestJarvisApp(deviceRotation = rotatable(), deviceRotationNotification = notification) }

        onNodeWithTag(DeviceRotationNotificationTestTag).assertIsOn()
        onNodeWithText("알림 권한이 없어 표시되지 않습니다. 허용하면 바로 나타납니다.").assertIsDisplayed()
    }

    @Test
    fun showsEmulatorCounts() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(
                    android = EmulatorSummary(total = 5, running = 2, physical = 1),
                    ios = EmulatorSummary(total = 11, running = 0, physical = 3),
                ),
            )
        }

        // 실물 기기는 가상 기기 숫자에 더해지지 않고 자기 줄에만 나온다.
        onNodeWithText("실행 중 2개 / 전체 5개").assertIsDisplayed()
        onNodeWithText("연결 1개").assertIsDisplayed()
        onNodeWithText("실행 중 0개 / 전체 11개").assertIsDisplayed()
        onNodeWithText("연결 3개").assertIsDisplayed()
    }

    @Test
    fun tellsZeroApartFromUncountable() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(android = EmulatorSummary(), ios = null),
            )
        }

        onNodeWithText("실행 중 0개 / 전체 0개").assertIsDisplayed()
        onNodeWithText("연결 0개").assertIsDisplayed()
        onAllNodesWithText("셀 수 없음", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun showsUncountableWhenNothingCanCount() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onAllNodesWithText("셀 수 없음", useUnmergedTree = true).assertCountEquals(4)
    }

    @Test
    fun showsPlaceholderUntilTheRepositoryAnswers() = runComposeUiTest {
        setContent { TestJarvisApp(emulator = SilentEmulatorRepository) }

        onAllNodesWithText("확인 중…", useUnmergedTree = true).assertCountEquals(4)
    }

    @Test
    fun emulatorCountsFollowRepositoryUpdates() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(android = EmulatorSummary(total = 3, running = 0))
        setContent { TestJarvisApp(emulator = emulator) }
        onNodeWithText("실행 중 0개 / 전체 3개").assertIsDisplayed()

        emulator.status.value = EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))

        // 개수는 HomeViewModel 의 viewModelScope 를 거쳐 화면에 온다. 설정 토글처럼 화면이
        // 리포지토리의 StateFlow 를 직접 모으는 경우와 달리 한 프레임 안에 반영되지 않는다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithText("실행 중 1개 / 전체 3개").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun emulatorCardOpensDeviceList() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)),
            )
        }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()
    }

    @Test
    fun deviceListShowsNameAndState() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(
                    devices = listOf(RunningAndroidDevice, StoppedAndroidDevice, RunningSimulator),
                ),
            )
        }

        onNodeWithTag(EmulatorTestTag).performClick()

        val androidList = hasAnyAncestor(hasTestTag(emulatorPlatformListTestTag(EmulatorPlatform.ANDROID)))
        val iosList = hasAnyAncestor(hasTestTag(emulatorPlatformListTestTag(EmulatorPlatform.IOS)))

        onNodeWithText(RunningAndroidDevice.name).assertIsDisplayed()
        onNode(hasText("실행 중") and androidList).assertIsDisplayed()
        onNode(hasText("꺼짐") and androidList).assertIsDisplayed()
        onNode(hasText("실행 중") and iosList).assertIsDisplayed()
    }

    @Test
    fun deviceListSplitsByPlatform() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(
                    devices = listOf(RunningSimulator, RunningAndroidDevice, PhysicalIosDevice, PhysicalAndroidDevice),
                ),
            )
        }

        onNodeWithTag(EmulatorTestTag).performClick()

        val androidList = hasAnyAncestor(hasTestTag(emulatorPlatformListTestTag(EmulatorPlatform.ANDROID)))
        val iosList = hasAnyAncestor(hasTestTag(emulatorPlatformListTestTag(EmulatorPlatform.IOS)))

        onNode(hasText("Android") and androidList).assertIsDisplayed()
        onNode(hasText("iOS") and iosList).assertIsDisplayed()
        onNode(hasTestTag(emulatorDeviceTestTag(RunningAndroidDevice.id)) and androidList).assertIsDisplayed()
        onNode(hasTestTag(emulatorDeviceTestTag(PhysicalAndroidDevice.id)) and androidList).assertIsDisplayed()
        onNode(hasTestTag(emulatorDeviceTestTag(RunningSimulator.id)) and iosList).assertIsDisplayed()
        onNode(hasTestTag(emulatorDeviceTestTag(PhysicalIosDevice.id)) and iosList).assertIsDisplayed()
    }

    // 한쪽 열만 비어 있을 때 빈칸으로 두면 목록을 아직 못 받은 것처럼 보인다.
    @Test
    fun emptyPlatformColumnSaysSo() = runComposeUiTest {
        setContent {
            TestJarvisApp(emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)))
        }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNode(
            hasText("iOS 기기가 없습니다.") and
                hasAnyAncestor(hasTestTag(emulatorPlatformListTestTag(EmulatorPlatform.IOS))),
        ).assertIsDisplayed()
        onNodeWithText("Android 기기가 없습니다.").assertDoesNotExist()
    }

    // 꺼져 있는 기기에는 찍을 화면이 없다. 눌러도 되는 것처럼 보이면 빈 화면만 보게 된다.
    @Test
    fun stoppedDeviceCannotBeOpened() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(
                    devices = listOf(RunningAndroidDevice, StoppedAndroidDevice),
                ),
            )
        }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(emulatorDeviceTestTag(StoppedAndroidDevice.id))
            .assertIsNotEnabled()
            .performClick()

        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()
        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).assertIsEnabled()
    }

    @Test
    fun emptyDeviceListExplainsWhy() = runComposeUiTest {
        setContent { TestJarvisApp(emulator = FakeEmulatorRepository()) }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText("연결된 기기가 없거나 개발자 머신에 물어볼 수 없습니다.").assertIsDisplayed()
    }

    @Test
    fun physicalDeviceIsMarkedInTheList() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(devices = listOf(PhysicalAndroidDevice))
        setContent { TestJarvisApp(emulator = emulator) }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText(PhysicalAndroidDevice.name).assertIsDisplayed()
        onNodeWithText("실물 기기 · 유선 · 연결됨").assertIsDisplayed()
        onNodeWithTag(emulatorDeviceTestTag(PhysicalAndroidDevice.id)).assertIsEnabled()
    }

    // 유선(USB)으로 붙었는지 무선(네트워크)으로 붙었는지 목록에 함께 나온다.
    @Test
    fun physicalDeviceShowsHowItIsConnected() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(devices = listOf(PhysicalAndroidDevice, SleepingAndroidDevice))
        setContent { TestJarvisApp(emulator = emulator) }

        onNodeWithTag(EmulatorTestTag).performClick()

        // 하드웨어 시리얼 기기는 유선.
        onNodeWithText("실물 기기 · 유선 · 연결됨").assertIsDisplayed()
        // mDNS 시리얼 기기는 무선.
        onNodeWithText("실물 기기 · 무선 · 연결됨 · 화면 꺼짐").assertIsDisplayed()
    }

    // 연결됐는데 목록에 없으면 "꽂았는데 왜 없지" 가 된다. 대신 왜 누를 수 없는지 그 줄에 적는다.
    @Test
    fun deviceWithoutAScreenSaysSoInTheList() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(devices = listOf(PhysicalIosDevice))
        setContent { TestJarvisApp(emulator = emulator) }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText("실물 기기 · 무선 · 연결됨 · 화면을 볼 수 없음").assertIsDisplayed()
        onNodeWithTag(emulatorDeviceTestTag(PhysicalIosDevice.id)).assertIsNotEnabled()
    }

    // 화면이 꺼진 기기는 스트리밍을 열어도 검은 화면만 보인다. 목록에서 켤 수 있어야 한다.
    @Test
    fun sleepingDeviceCanBeWokenUp() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(
            devices = listOf(SleepingAndroidDevice, PhysicalAndroidDevice, StoppedAndroidDevice),
        )
        setContent { TestJarvisApp(emulator = emulator) }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText("실물 기기 · 무선 · 연결됨 · 화면 꺼짐").assertIsDisplayed()
        onNodeWithTag(emulatorWakeTestTag(SleepingAndroidDevice.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.woken.isNotEmpty() }
        assertEquals(listOf(SleepingAndroidDevice.id), emulator.woken.toList())
        // 깨어 있는 기기와 입력을 받지 못하는 꺼진 AVD 에는 붙지 않는다.
        onAllNodesWithTag(emulatorWakeTestTag(PhysicalAndroidDevice.id)).assertCountEquals(0)
        onAllNodesWithTag(emulatorWakeTestTag(StoppedAndroidDevice.id)).assertCountEquals(0)
    }

    // 검은 화면을 보고 목록으로 돌아가야 켤 수 있으면, 꺼진 줄 모르고 계속 기다리게 된다.
    @Test
    fun sleepingDeviceCanBeWokenUpFromTheStream() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(
            devices = listOf(SleepingAndroidDevice),
            frames = MutableStateFlow(TestFrame),
        )
        setContent { TestJarvisApp(emulator = emulator) }
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(emulatorDeviceTestTag(SleepingAndroidDevice.id)).performClick()

        onNodeWithText("기기 화면이 꺼져 있습니다.").assertIsDisplayed()
        onNodeWithTag(EmulatorScreenWakeTestTag).performClick()

        // 누른 뒤 "켜는 중" 으로 바뀌고 풀리는 것은 EmulatorScreenViewModelTest 가 본다. Wasm 에서는
        // waitUntil 이 이벤트 루프를 잡는 동안 stateIn 을 거친 값이 화면까지 오지 못한다(web.html#test).
        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.woken.isNotEmpty() }
        assertEquals(listOf(SleepingAndroidDevice.id), emulator.woken.toList())
    }

    @Test
    fun awakeDeviceShowsNoWakePrompt() = runComposeUiTest {
        setContent { TestJarvisApp(emulator = streamingEmulator()) }
        openStream()

        onAllNodesWithText("기기 화면이 꺼져 있습니다.").assertCountEquals(0)
        onAllNodesWithTag(EmulatorScreenWakeTestTag).assertCountEquals(0)
    }

    @Test
    fun stoppedDeviceCanBeLaunched() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(
            devices = listOf(RunningAndroidDevice, StoppedAndroidDevice),
        )
        setContent { TestJarvisApp(emulator = emulator) }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(emulatorLaunchTestTag(StoppedAndroidDevice.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.launched.isNotEmpty() }
        assertEquals(listOf(StoppedAndroidDevice.id), emulator.launched.toList())
        // 이미 켜져 있는 기기에는 켤 것이 없다.
        onAllNodesWithTag(emulatorLaunchTestTag(RunningAndroidDevice.id)).assertCountEquals(0)
    }

    @Test
    fun runningDeviceOpensStreamScreen() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)),
            )
        }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).performClick()

        onNodeWithTag(EmulatorScreenTestTag).assertIsDisplayed()
        // 아직 프레임이 오지 않은 상태와 가져오지 못한 상태는 다르다.
        onNodeWithText("화면을 가져오는 중…").assertIsDisplayed()
    }

    @Test
    fun tapOnStreamIsSentInDeviceCoordinates() = runComposeUiTest {
        val emulator = streamingEmulator()
        setContent { TestJarvisApp(emulator = emulator) }
        openStream()

        onNodeWithTag(EmulatorFrameTestTag).performTouchInput { click(center) }

        // 누른 순간과 뗀 순간이 각각 간다. 뗀 뒤 한 번에 보내지 않는다. 마우스 호버가 앞서 끼기도 하므로 누름만 본다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            emulator.gestures.any { it.second.let { g -> g is EmulatorGesture.Touch && g.action == TouchAction.UP } }
        }
        assertTrue(emulator.gestures.all { it.first == RunningAndroidDevice.id })
        val touches = emulator.gestures.map { it.second }.filterIsInstance<EmulatorGesture.Touch>()
        assertEquals(
            listOf(down(TestFrameWidth / 2, TestFrameHeight / 2), up(TestFrameWidth / 2, TestFrameHeight / 2)),
            touches,
        )
    }

    // 마우스가 누르지 않고 지나가면 호버가 기기로 간다. 기기 안 UI 가 호버 상태를 그린다.
    @Test
    fun mouseHoverOnStreamIsSent() = runComposeUiTest {
        val emulator = streamingEmulator()
        setContent { TestJarvisApp(emulator = emulator) }
        openStream()

        onNodeWithTag(EmulatorFrameTestTag).performMouseInput {
            moveTo(centerLeft)
            moveTo(center)
        }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.gestures.isNotEmpty() }
        val hover = assertIs<EmulatorGesture.Hover>(emulator.gestures.last().second)
        assertEquals(TestFrameWidth / 2, hover.x)
        assertEquals(TestFrameHeight / 2, hover.y)
    }

    @Test
    fun dragOnStreamIsSentAsSwipe() = runComposeUiTest {
        val emulator = streamingEmulator()
        setContent { TestJarvisApp(emulator = emulator) }
        openStream()

        onNodeWithTag(EmulatorFrameTestTag).performTouchInput { swipe(start = centerLeft, end = centerRight) }

        // 누른 지점에서 DOWN, 끄는 동안 MOVE, 뗀 지점에서 UP.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            emulator.gestures.lastOrNull()?.second.let { it is EmulatorGesture.Touch && it.action == TouchAction.UP }
        }
        val touches = emulator.gestures.map { it.second }.filterIsInstance<EmulatorGesture.Touch>()
        assertEquals(down(0, TestFrameHeight / 2), touches.first())
        assertEquals(up(TestFrameWidth - 1, TestFrameHeight / 2), touches.last())
        assertTrue(touches.any { it.action == TouchAction.MOVE })
    }

    // iOS 시뮬레이터에는 입력을 주입하는 도구가 없다. 화면은 보이되 왜 안 되는지 말해야 한다.
    @Test
    fun uncontrollableDeviceSaysSo() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(
            devices = listOf(RunningSimulator),
            frames = MutableStateFlow(TestFrame),
        )
        setContent { TestJarvisApp(emulator = emulator) }
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(emulatorDeviceTestTag(RunningSimulator.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(EmulatorFrameTestTag).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(EmulatorFrameTestTag).performTouchInput { click(center) }

        onNodeWithText("이 기기에는 제스처를 보낼 수 없습니다. 화면만 볼 수 있습니다.").assertIsDisplayed()
        assertTrue(emulator.gestures.isEmpty())
    }

    @Test
    fun backReturnsToListThenGrid() = runComposeUiTest {
        setContent {
            TestJarvisApp(
                emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)),
            )
        }
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).performClick()

        onNodeWithContentDescription("뒤로").performClick()
        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()

        onNodeWithContentDescription("뒤로").performClick()
        onNodeWithTag(EmulatorTestTag).assertIsDisplayed()
    }

    private fun streamingEmulator() = FakeEmulatorRepository(
        devices = listOf(RunningAndroidDevice),
        frames = MutableStateFlow(TestFrame),
    )

    private fun down(x: Int, y: Int) =
        EmulatorGesture.Touch(TouchAction.DOWN, x, y, TestFrameWidth, TestFrameHeight)

    private fun up(x: Int, y: Int) =
        EmulatorGesture.Touch(TouchAction.UP, x, y, TestFrameWidth, TestFrameHeight)

    private fun ComposeUiTest.openStream() {
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).performClick()

        // 프레임이 그려지기 전에 누르면 좌표를 맞출 기준이 없다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(EmulatorFrameTestTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        // 첫 프레임은 Flow 를 한 바퀴 돌고 디코딩까지 끝나야 그려진다. 브라우저에서는 그게 기본
        // 1초를 넘길 때가 있다.
        const val FrameTimeoutMillis = 10_000L
    }

    // 화면 회전을 지원하는 것은 Android · iOS · Web 이고, 그 셋의 화면은 가짜 상태로만 확인할 수 있다.
    private fun rotatable(angle: RotationAngle = RotationAngle.Degrees0) =
        FakeDeviceRotationRepository(DeviceRotationStatus(supported = true, angle = angle))

    @Test
    fun deviceRotationShowsCurrentAngle() = runComposeUiTest {
        val rotation = rotatable(RotationAngle.Degrees90)
        setContent { TestJarvisApp(deviceRotation = rotation) }
        onNodeWithText("현재 90°, 센서를 따라 회전합니다.").assertIsDisplayed()

        rotation.status.value = rotation.status.value.copy(angle = RotationAngle.Degrees270)

        onNodeWithText("현재 270°, 센서를 따라 회전합니다.").assertIsDisplayed()
    }

    @Test
    fun deviceRotationAngleButtonLocksToThatAngle() = runComposeUiTest {
        val rotation = rotatable()
        setContent { TestJarvisApp(deviceRotation = rotation) }

        onNodeWithTag(deviceRotationAngleTestTag(RotationAngle.Degrees180)).performClick()

        assertEquals(RotationAngle.Degrees180, rotation.status.value.angle)
        assertTrue(rotation.status.value.locked)
    }

    @Test
    fun deviceRotationRotatesForwardAndBackward() = runComposeUiTest {
        val rotation = rotatable(RotationAngle.Degrees90)
        setContent { TestJarvisApp(deviceRotation = rotation) }

        onNodeWithTag(DeviceRotationForwardTestTag).performClick()
        assertEquals(RotationAngle.Degrees180, rotation.status.value.angle)

        onNodeWithTag(DeviceRotationBackwardTestTag).performClick()
        assertEquals(RotationAngle.Degrees90, rotation.status.value.angle)
    }

    @Test
    fun deviceRotationLockTogglesWhereItIsSupported() = runComposeUiTest {
        val rotation = rotatable()
        setContent { TestJarvisApp(deviceRotation = rotation) }

        onNodeWithTag(DeviceRotationLockTestTag).performClick().assertIsOn()

        assertTrue(rotation.status.value.locked)
    }

    // JVM 은 실제로 지원하지 않는 타깃이라, 여기서는 가짜가 아니라 실제 판정을 검증한다.
    @Test
    fun deviceRotationIsLockedWhereItIsNotSupported() = runComposeUiTest {
        val rotation = FakeDeviceRotationRepository()
        setContent { TestJarvisApp(deviceRotation = rotation) }

        onNodeWithTag(DeviceRotationLockTestTag).assertIsNotEnabled().performClick().assertIsOff()
        onNodeWithTag(DeviceRotationForwardTestTag).assertIsNotEnabled().performClick()
        onNodeWithText("이 플랫폼에서는 화면을 돌릴 수 없습니다.").assertIsDisplayed()

        assertFalse(rotation.status.value.locked)
        assertNull(rotation.status.value.angle)
    }
}

private val TestRelease = AppRelease(version = "9.9.9", downloadUrl = "https://example.com/Jarvis.dmg", checksumUrl = "https://example.com/Jarvis.dmg.sha256")
