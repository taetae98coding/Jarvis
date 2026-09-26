package io.github.taetae98coding.jarvis.ui.worldclock

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.worldclock.City
import io.github.taetae98coding.jarvis.domain.worldclock.CityTime
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDate
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDateTime
import io.github.taetae98coding.jarvis.domain.worldclock.WorldClockState
import io.github.taetae98coding.jarvis.domain.worldclock.ZonedTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class WorldClockCardTest {
    @Test
    fun showsLocalTimeWithSecondsAndFirstThreeCities() = runComposeUiTest {
        var clicks = 0
        setContent {
            JarvisTheme { WorldClockCard(state = State, onClick = { clicks++ }) }
        }

        onNodeWithTag(WorldClockCardLocalTestTag, useUnmergedTree = true).assertTextEquals("00:30:05")
        onNode(hasText("서울 · 9월 27일 (일) · UTC+9"), useUnmergedTree = true).assertExists()
        cityValue("America/New_York", "−1일 11:30").assertExists()
        cityValue("Asia/Tokyo", "00:30").assertExists()
        cityValue("Europe/London", "−1일 16:30").assertExists()
        // 네 번째 도시는 화면에서만 보인다.
        onNodeWithTag(worldClockCardCityTestTag("UTC"), useUnmergedTree = true).assertDoesNotExist()

        onNodeWithTag(WorldClockCardLocalTestTag, useUnmergedTree = true).performClick()
        assertEquals(1, clicks)
    }

    private fun ComposeUiTest.cityValue(zoneId: String, value: String) =
        onNode(hasText(value) and hasAnyAncestor(hasTestTag(worldClockCardCityTestTag(zoneId))), useUnmergedTree = true)

    private companion object {
        val Seoul = ZonedTime("Asia/Seoul", CivilDateTime(CivilDate(2026, 9, 27), 0, 30, 5), 9 * 3600)

        fun city(zoneId: String, name: String, dateTime: CivilDateTime, offsetHours: Int, dayDifference: Int) = CityTime(
            city = City(zoneId, name, name),
            time = ZonedTime(zoneId, dateTime, offsetHours * 3600),
            dayDifference = dayDifference,
            offsetDifferenceSeconds = (offsetHours - 9) * 3600,
        )

        val State = WorldClockState(
            now = Instant.fromEpochSeconds(1_790_000_000),
            local = CityTime(City("Asia/Seoul", "서울", "Seoul"), Seoul, 0, 0),
            cities = listOf(
                city("America/New_York", "뉴욕", CivilDateTime(CivilDate(2026, 9, 26), 11, 30), -4, -1),
                city("Asia/Tokyo", "도쿄", CivilDateTime(CivilDate(2026, 9, 27), 0, 30), 9, 0),
                city("Europe/London", "런던", CivilDateTime(CivilDate(2026, 9, 26), 16, 30), 1, -1),
                city("UTC", "협정 세계시", CivilDateTime(CivilDate(2026, 9, 26), 15, 30), 0, -1),
            ),
            savedZoneIds = listOf("America/New_York", "Asia/Tokyo", "Europe/London", "UTC"),
        )
    }
}
