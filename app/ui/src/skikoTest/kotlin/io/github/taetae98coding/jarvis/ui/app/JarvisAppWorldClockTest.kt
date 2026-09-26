package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBarDefaults
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockAddResultTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockAddTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockAgeResultTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockBirthTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockCardLocalTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockConvertDateTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockConvertResultTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockConvertTimeTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockDDayResultTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockDiffResultTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockDiffToTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockLocalTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockScreenTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockSearchTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockTab
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockTargetTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockToZoneTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.worldClockCandidateTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.worldClockCityTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.worldClockConvertCityTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.worldClockTabTestTag
import io.github.taetae98coding.jarvis.ui.worldclock.worldClockZoneOptionTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/** docs/common/world-clock.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppWorldClockTest {
    @Test
    fun cardTicksAndOpensScreenAndBackReturnsHome() = runComposeUiTest {
        val clock = FakeWorldClock()
        setContent { TestJarvisApp(worldClock = clock) }

        scrollToCard()
        onNodeWithTag(WorldClockCardLocalTestTag, useUnmergedTree = true).assertTextEquals("00:30:05")
        clock.now.value += 1.seconds
        onNodeWithTag(WorldClockCardLocalTestTag, useUnmergedTree = true).assertTextEquals("00:30:06")

        onNodeWithTag(WorldClockTestTag).performClick()
        onNodeWithTag(WorldClockScreenTestTag).assertIsDisplayed()
        onNode(hasText("00:30:06") and hasAnyAncestor(hasTestTag(WorldClockLocalTestTag)), useUnmergedTree = true).assertExists()

        onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()
        onNodeWithTag(WorldClockTestTag).assertIsDisplayed()
    }

    @Test
    fun removesAndAddsCitiesInOrder() = runComposeUiTest {
        val cities = FakeWorldClockCities()
        setContent { TestJarvisApp(savedCities = cities) }
        openScreen()

        onNode(hasText("−1일") and hasAnyAncestor(hasTestTag(worldClockCityTestTag("America/New_York"))), useUnmergedTree = true).assertExists()
        onNode(hasText("11:30") and hasAnyAncestor(hasTestTag(worldClockCityTestTag("America/New_York"))), useUnmergedTree = true).assertExists()

        onNodeWithContentDescription("뉴욕 삭제").performClick()
        waitForIdle()
        assertEquals(listOf("Europe/London", "Asia/Tokyo"), cities.zoneIds.value)
        onNodeWithTag(worldClockCityTestTag("America/New_York")).assertDoesNotExist()

        onNodeWithTag(WorldClockAddTestTag).performScrollTo().performClick()
        onNodeWithTag(WorldClockSearchTestTag).performTextInput("파리")
        onNodeWithTag(worldClockCandidateTestTag("Europe/Paris")).performScrollTo().performClick()
        waitForIdle()

        assertEquals(listOf("Europe/London", "Asia/Tokyo", "Europe/Paris"), cities.zoneIds.value)
        onNodeWithTag(worldClockCityTestTag("Europe/Paris")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun searchHidesCitiesAlreadyAdded() = runComposeUiTest {
        setContent { TestJarvisApp() }
        openScreen()

        onNodeWithTag(WorldClockAddTestTag).performScrollTo().performClick()
        onNodeWithTag(WorldClockSearchTestTag).performTextInput("tokyo")

        onNodeWithTag(worldClockCandidateTestTag("Asia/Tokyo")).assertDoesNotExist()
        onNode(hasText("찾는 도시가 없다."), useUnmergedTree = true).assertExists()
    }

    @Test
    fun convertsTimeToChosenZoneAndSavedCities() = runComposeUiTest {
        setContent { TestJarvisApp() }
        openScreen()
        onNodeWithTag(worldClockTabTestTag(WorldClockTab.CONVERTER)).performClick()

        // 기준은 기기 시간대(서울), 바꿀 곳은 첫 저장 도시(뉴욕)다.
        onNodeWithTag(WorldClockConvertDateTestTag).performTextReplacement("2026-09-27")
        onNodeWithTag(WorldClockConvertTimeTestTag).performTextReplacement("09:00")
        onNode(hasText("2026-09-26 (토) 20:00") and hasAnyAncestor(hasTestTag(WorldClockConvertResultTestTag)), useUnmergedTree = true)
            .assertExists()
        onNode(hasText("09:00") and hasAnyAncestor(hasTestTag(worldClockConvertCityTestTag("Asia/Tokyo"))), useUnmergedTree = true).assertExists()
        onNode(hasText("01:00") and hasAnyAncestor(hasTestTag(worldClockConvertCityTestTag("Europe/London"))), useUnmergedTree = true).assertExists()

        onNodeWithTag(WorldClockToZoneTestTag).performScrollTo().performClick()
        onNodeWithTag(worldClockZoneOptionTestTag("Europe/Paris")).performScrollTo().performClick()
        onNode(hasText("2026-09-27 (일) 02:00") and hasAnyAncestor(hasTestTag(WorldClockConvertResultTestTag)), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun invalidConverterInputShowsHint() = runComposeUiTest {
        setContent { TestJarvisApp() }
        openScreen()
        onNodeWithTag(worldClockTabTestTag(WorldClockTab.CONVERTER)).performClick()

        onNodeWithTag(WorldClockConvertTimeTestTag).performTextReplacement("25:00")

        onNode(hasText("날짜는 2026-09-26, 시각은 14:30 처럼 적는다."), useUnmergedTree = true).assertExists()
        onNodeWithTag(WorldClockConvertResultTestTag).assertDoesNotExist()
    }

    @Test
    fun dateCalculatorsUseLocalToday() = runComposeUiTest {
        setContent { TestJarvisApp() }
        openScreen()
        onNodeWithTag(worldClockTabTestTag(WorldClockTab.DATES)).performClick()

        // 서울은 이미 2026-09-27 이다.
        dateResult(WorldClockDiffResultTestTag, "0일").assertExists()
        onNodeWithTag(WorldClockDiffToTestTag).performTextReplacement("2026-12-25")
        dateResult(WorldClockDiffResultTestTag, "89일").assertExists()
        dateResult(WorldClockDiffResultTestTag, "12주 5일").assertExists()
        dateResult(WorldClockDiffResultTestTag, "0년 2개월 28일").assertExists()

        dateResult(WorldClockAddResultTestTag, "2027-01-05 (화)").assertExists()

        onNodeWithTag(WorldClockBirthTestTag).performScrollTo().performTextReplacement("2000-09-27")
        dateResult(WorldClockAgeResultTestTag, "만 26세").assertExists()

        onNodeWithTag(WorldClockTargetTestTag).performScrollTo().performTextReplacement("2026-12-25")
        dateResult(WorldClockDDayResultTestTag, "D-89").assertExists()
    }

    private fun ComposeUiTest.dateResult(tag: String, text: String) =
        onNode(hasText(text) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)

    // 카드는 그리드 맨 끝이라 창이 작으면 아직 그려지지 않았을 수 있다.
    private fun ComposeUiTest.scrollToCard() {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(WorldClockTestTag))
    }

    private fun ComposeUiTest.openScreen() {
        scrollToCard()
        onNodeWithTag(WorldClockTestTag).performClick()
        onNodeWithTag(WorldClockScreenTestTag).assertIsDisplayed()
    }
}
