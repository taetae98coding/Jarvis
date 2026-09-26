package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBarDefaults
import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.focus.FocusTimer
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import io.github.taetae98coding.jarvis.ui.battery.BatteryHomeFeature
import io.github.taetae98coding.jarvis.ui.battery.BatteryLevelTestTag
import io.github.taetae98coding.jarvis.ui.calculator.CalculatorHomeFeature
import io.github.taetae98coding.jarvis.ui.calculator.CalculatorScreenTestTag
import io.github.taetae98coding.jarvis.ui.devtools.DevToolsHomeFeature
import io.github.taetae98coding.jarvis.ui.devtools.DevToolsScreenTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorHomeFeature
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorListTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorTestTag
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerHomeFeature
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerPrimaryTestTag
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import io.github.taetae98coding.jarvis.ui.home.homeFeatureTileTestTag
import io.github.taetae98coding.jarvis.ui.profiling.ProfilingHomeFeature
import io.github.taetae98coding.jarvis.ui.profiling.ProfilingTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodeHomeFeature
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodeScreenTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationHomeFeature
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationLockTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeToggleTestTag
import io.github.taetae98coding.jarvis.ui.screen.ScreenAwakeHomeFeature
import io.github.taetae98coding.jarvis.ui.screen.ScreenAwakeTitle
import io.github.taetae98coding.jarvis.ui.screen.SystemScreenAwakeHomeFeature
import io.github.taetae98coding.jarvis.ui.terminal.TerminalHomeFeature
import io.github.taetae98coding.jarvis.ui.terminal.TerminalScreenTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.texttools.TextToolsHomeFeature
import io.github.taetae98coding.jarvis.ui.texttools.TextToolsScreenTestTag
import io.github.taetae98coding.jarvis.ui.theme.ThemeModeHomeFeature
import io.github.taetae98coding.jarvis.ui.theme.ThemeModeTestTag
import io.github.taetae98coding.jarvis.ui.theme.themeModeOptionTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterHomeFeature
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterScreenTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockHomeFeature
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockScreenTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 홈의 창 크기별 배치와 지원 기능 우선 정렬(docs/common/home-adaptive-layout.html).
 *
 * 테스트 창(1024×768, 밀도 1)은 Medium 이라 기본이 카드다. Compact 는 창 크기를 400×1200 으로 바꿔 끼운다.
 * 세로가 긴 것은 타일 열다섯 개가 모두 뷰포트 안에 들어와야 순서를 셀 수 있어서다.
 */
@OptIn(ExperimentalTestApi::class)
class JarvisAppHomeLayoutTest {
    @Test
    fun mediumWindowShowsCardsNotTiles() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsDisplayed()
        onAllNodes(isHomeTile()).assertCountEquals(0)
    }

    @Test
    fun compactWindowShowsTilesNotCards() = runComposeUiTest {
        setContent { TestJarvisApp(windowSize = CompactWindow) }

        onNodeWithTag(homeFeatureTileTestTag(ScreenAwakeHomeFeature)).assertIsDisplayed()
        onAllNodes(isHomeTile()).assertCountEquals(AllHomeFeatures.size)
        onAllNodesWithTag(KeepScreenAwakeTestTag).assertCountEquals(0)
        onAllNodesWithTag(ThemeModeTestTag).assertCountEquals(0)
    }

    // Skiko 타깃의 기본 가짜는 시스템 전역 화면 꺼짐 방지·화면 회전을 지원하지 않고, 에뮬레이터는 둘 다 셀 수 없다.
    // 배터리는 아직 답이 없어 지원으로 둔다.
    @Test
    fun compactWindowPutsSupportedFeaturesFirst() = runComposeUiTest {
        setContent { TestJarvisApp(windowSize = CompactWindow) }

        assertEquals(
            listOf(
                ScreenAwakeHomeFeature, ThemeModeHomeFeature, ProfilingHomeFeature, TerminalHomeFeature,
                BatteryHomeFeature, FocusTimerHomeFeature, DevToolsHomeFeature, UnitConverterHomeFeature,
                TextToolsHomeFeature, CalculatorHomeFeature, QrCodeHomeFeature, WorldClockHomeFeature,
            ),
            tilesInVisualOrder().take(12),
        )
        assertEquals(
            setOf(SystemScreenAwakeHomeFeature, EmulatorHomeFeature, DeviceRotationHomeFeature),
            tilesInVisualOrder().drop(12).toSet(),
        )
    }

    @Test
    fun compactWindowLocksUnsupportedTiles() = runComposeUiTest {
        setContent { TestJarvisApp(windowSize = CompactWindow) }

        onNodeWithTag(homeFeatureTileTestTag(DeviceRotationHomeFeature)).assertIsNotEnabled()
        onNodeWithTag(homeFeatureTileTestTag(SystemScreenAwakeHomeFeature)).assertIsNotEnabled()
        onNodeWithTag(homeFeatureTileTestTag(EmulatorHomeFeature)).assertIsNotEnabled()
        onNodeWithTag(homeFeatureTileTestTag(ScreenAwakeHomeFeature)).assertIsEnabled()
    }

    // 지원 여부는 상태라 바뀔 수 있다. 바뀌면 순서와 잠금이 따라온다.
    @Test
    fun orderFollowsSupportChanges() = runComposeUiTest {
        val rotation = FakeDeviceRotationRepository()
        setContent { TestJarvisApp(windowSize = CompactWindow, deviceRotation = rotation) }

        rotation.status.value = DeviceRotationStatus(supported = true, permitted = true, angle = null)
        waitForIdle()

        onNodeWithTag(homeFeatureTileTestTag(DeviceRotationHomeFeature)).assertIsEnabled()
        // 목록 순서(화면 꺼짐 방지, 테마, 프로파일링, 에뮬레이터, 회전, 터미널)를 지키며 지원하는 것만 앞으로 온다.
        assertEquals(
            listOf(ScreenAwakeHomeFeature, ThemeModeHomeFeature, ProfilingHomeFeature, DeviceRotationHomeFeature, TerminalHomeFeature),
            tilesInVisualOrder().take(5),
        )
    }

    @Test
    fun mediumWindowPutsSupportedCardsInTheFirstRow() = runComposeUiTest {
        setContent { TestJarvisApp() }

        // 1024dp 창은 220dp 카드 네 열이라 목록 앞의 지원하는 넷이 첫 줄을 채우고, 지원하지 않는 셋은 지원하는
        // 열둘 뒤라 그보다 아래에 온다.
        val supportedTops = listOf(KeepScreenAwakeTestTag, ThemeModeTestTag, ProfilingTestTag, TerminalTestTag)
            .map { onNodeWithTag(it).getBoundsInRoot().top }
        val unsupportedTops = listOf(KeepSystemScreenAwakeTestTag, EmulatorTestTag, DeviceRotationTestTag)
            .map { onNodeWithTag(it).getBoundsInRoot().top }

        assertEquals(1, supportedTops.toSet().size, "지원하는 카드 넷이 한 줄에 있어야 한다: $supportedTops")
        assertTrue(unsupportedTops.all { it > supportedTops.first() }, "지원하지 않는 카드는 아래 줄이어야 한다: $unsupportedTops")
    }

    @Test
    fun tileOpensFeatureScreenWhereTheSettingIsMade() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        setContent { TestJarvisApp(windowSize = CompactWindow, settings = settings) }

        onNodeWithTag(homeFeatureTileTestTag(ScreenAwakeHomeFeature)).performClick()

        // 제목은 상단 막대와 카드 머리 두 곳에 있다.
        onAllNodesWithText(ScreenAwakeTitle).assertCountEquals(2)
        onNodeWithTag(KeepScreenAwakeTestTag).performClick().assertIsOn()
        assertTrue(settings.keepScreenAwake.value)

        onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()

        onNodeWithTag(homeFeatureTileTestTag(ScreenAwakeHomeFeature)).assertIsDisplayed()
        onAllNodesWithTag(KeepScreenAwakeTestTag).assertCountEquals(0)
    }

    @Test
    fun themeTileOpensScreenWithTheSameOptions() = runComposeUiTest {
        val theme = FakeThemeSettingsRepository()
        setContent { TestJarvisApp(windowSize = CompactWindow, theme = theme) }

        onNodeWithTag(homeFeatureTileTestTag(ThemeModeHomeFeature)).performClick()
        onNodeWithTag(themeModeOptionTestTag(ThemeMode.DARK)).performClick().assertIsSelected()

        assertEquals(ThemeMode.DARK, theme.themeMode.value)
    }

    @Test
    fun systemScreenAwakeTileOpensScreenWhereItIsSupported() = runComposeUiTest {
        val system = FakeSystemScreenAwakeRepository(SystemScreenAwakeStatus(supported = true, permitted = true))
        setContent { TestJarvisApp(windowSize = CompactWindow, systemScreenAwake = system) }

        onNodeWithTag(homeFeatureTileTestTag(SystemScreenAwakeHomeFeature)).assertIsEnabled().performClick()

        onNodeWithTag(KeepSystemScreenAwakeToggleTestTag).performClick().assertIsOn()
    }

    @Test
    fun rotationTileOpensScreenWhereItIsSupported() = runComposeUiTest {
        val rotation = FakeDeviceRotationRepository(DeviceRotationStatus(supported = true, permitted = true, angle = null))
        setContent { TestJarvisApp(windowSize = CompactWindow, deviceRotation = rotation) }

        onNodeWithTag(homeFeatureTileTestTag(DeviceRotationHomeFeature)).performClick()

        onNodeWithTag(DeviceRotationLockTestTag).performClick().assertIsOn()
    }

    @Test
    fun profilingTileOpensScreen() = runComposeUiTest {
        setContent { TestJarvisApp(windowSize = CompactWindow) }

        onNodeWithTag(homeFeatureTileTestTag(ProfilingHomeFeature)).performClick()

        onNodeWithTag(ProfilingTestTag).assertIsDisplayed()
    }

    @Test
    fun emulatorTileOpensDeviceList() = runComposeUiTest {
        setContent {
            TestJarvisApp(windowSize = CompactWindow, emulator = FakeEmulatorRepository(android = EmulatorSummary()))
        }

        onNodeWithTag(homeFeatureTileTestTag(EmulatorHomeFeature)).assertIsEnabled().performClick()

        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()
    }

    @Test
    fun terminalTileOpensTerminal() = runComposeUiTest {
        setContent { TestJarvisApp(windowSize = CompactWindow) }

        onNodeWithTag(homeFeatureTileTestTag(TerminalHomeFeature)).performClick()

        onNodeWithTag(TerminalScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun screenFeatureTilesOpenTheirScreens() = runComposeUiTest {
        setContent { TestJarvisApp(windowSize = CompactWindow) }

        listOf(
            DevToolsHomeFeature to DevToolsScreenTestTag,
            UnitConverterHomeFeature to UnitConverterScreenTestTag,
            TextToolsHomeFeature to TextToolsScreenTestTag,
            CalculatorHomeFeature to CalculatorScreenTestTag,
            QrCodeHomeFeature to QrCodeScreenTestTag,
            WorldClockHomeFeature to WorldClockScreenTestTag,
        ).forEach { (feature, screenTag) ->
            onNodeWithTag(homeFeatureTileTestTag(feature)).assertIsEnabled().performClick()
            onNodeWithTag(screenTag).assertIsDisplayed()

            onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()
            onNodeWithTag(homeFeatureTileTestTag(feature)).assertIsDisplayed()
        }
    }

    // 배터리가 없으면 잠겼다가, 배터리를 읽으면 풀리고 카드를 담은 기능 화면을 연다.
    @Test
    fun batteryTileOpensScreen() = runComposeUiTest {
        val battery = FakeBatteryRepository()
        battery.status.value = BatteryStatus.NoBattery
        setContent { TestJarvisApp(windowSize = CompactWindow, battery = battery) }

        onNodeWithTag(homeFeatureTileTestTag(BatteryHomeFeature)).assertIsNotEnabled()

        battery.status.value = BatteryStatus.Available(Battery(levelPercent = 87, charging = ChargingState.CHARGING))
        waitForIdle()

        onNodeWithTag(homeFeatureTileTestTag(BatteryHomeFeature)).assertIsEnabled().performClick()
        onNodeWithTag(BatteryLevelTestTag).assertIsDisplayed()
    }

    @Test
    fun focusTimerTileOpensScreenWhereTheTimerStarts() = runComposeUiTest {
        val session = FakeFocusSessionRepository()
        setContent { TestJarvisApp(windowSize = CompactWindow, focusSession = session) }

        onNodeWithTag(homeFeatureTileTestTag(FocusTimerHomeFeature)).performClick()
        onNodeWithTag(FocusTimerPrimaryTestTag).performClick()
        waitForIdle()

        assertTrue(session.session.value.timer is FocusTimer.Running)
    }

    private fun ComposeUiTest.tilesInVisualOrder(): List<HomeFeature> {
        val tags = onAllNodes(isHomeTile()).fetchSemanticsNodes()
            .sortedWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
            .map { it.config[SemanticsProperties.TestTag] }

        return tags.map { tag -> AllHomeFeatures.first { homeFeatureTileTestTag(it) == tag } }
    }

    private fun isHomeTile(): SemanticsMatcher =
        SemanticsMatcher("is home tile") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(HomeTileTagPrefix) == true
        }
}

private val CompactWindow = IntSize(400, 1200)

private val HomeTileTagPrefix = homeFeatureTileTestTag(ScreenAwakeHomeFeature).removeSuffix(ScreenAwakeHomeFeature.id)

private val AllHomeFeatures: List<HomeFeature> = listOf(
    ScreenAwakeHomeFeature,
    SystemScreenAwakeHomeFeature,
    ThemeModeHomeFeature,
    ProfilingHomeFeature,
    EmulatorHomeFeature,
    DeviceRotationHomeFeature,
    TerminalHomeFeature,
    BatteryHomeFeature,
    FocusTimerHomeFeature,
    DevToolsHomeFeature,
    UnitConverterHomeFeature,
    TextToolsHomeFeature,
    CalculatorHomeFeature,
    QrCodeHomeFeature,
    WorldClockHomeFeature,
)
