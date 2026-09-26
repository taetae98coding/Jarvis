package io.github.taetae98coding.jarvis.ui.worldclock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.worldclock.City
import io.github.taetae98coding.jarvis.domain.worldclock.CityTime
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDate
import io.github.taetae98coding.jarvis.domain.worldclock.LocalTimeKind
import io.github.taetae98coding.jarvis.domain.worldclock.WorldCities
import io.github.taetae98coding.jarvis.domain.worldclock.WorldClockState
import io.github.taetae98coding.jarvis.domain.worldclock.ZonedTime

const val WorldClockScreenTestTag = "worldclock:screen"
const val WorldClockLocalTestTag = "worldclock:local"
const val WorldClockAddTestTag = "worldclock:add"
const val WorldClockSearchTestTag = "worldclock:search"
const val WorldClockFromZoneTestTag = "worldclock:from"
const val WorldClockToZoneTestTag = "worldclock:to"
const val WorldClockConvertDateTestTag = "worldclock:convert-date"
const val WorldClockConvertTimeTestTag = "worldclock:convert-time"
const val WorldClockConvertNowTestTag = "worldclock:convert-now"
const val WorldClockConvertResultTestTag = "worldclock:convert-result"
const val WorldClockDiffFromTestTag = "worldclock:diff-from"
const val WorldClockDiffToTestTag = "worldclock:diff-to"
const val WorldClockDiffResultTestTag = "worldclock:diff-result"
const val WorldClockAddBaseTestTag = "worldclock:add-base"
const val WorldClockAddDaysTestTag = "worldclock:add-days"
const val WorldClockAddResultTestTag = "worldclock:add-result"
const val WorldClockBirthTestTag = "worldclock:birth"
const val WorldClockAgeResultTestTag = "worldclock:age-result"
const val WorldClockTargetTestTag = "worldclock:target"
const val WorldClockDDayResultTestTag = "worldclock:dday-result"

fun worldClockTabTestTag(tab: WorldClockTab): String = "worldclock:tab:${tab.name.lowercase()}"

fun worldClockCityTestTag(zoneId: String): String = "worldclock:city:$zoneId"

fun worldClockCandidateTestTag(zoneId: String): String = "worldclock:candidate:$zoneId"

fun worldClockZoneOptionTestTag(zoneId: String): String = "worldclock:zone-option:$zoneId"

fun worldClockConvertCityTestTag(zoneId: String): String = "worldclock:convert-city:$zoneId"

enum class WorldClockTab(internal val label: String) {
    CLOCKS("세계 시계"),
    CONVERTER("시간대 변환"),
    DATES("날짜 계산"),
}

@Composable
internal fun WorldClockScreen(
    viewModel: WorldClockViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(WorldClockTab.CLOCKS) }
    val spacing = JarvisTheme.dimens.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(WorldClockScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        JarvisTopBar(title = WorldClockTitle, onBack = onBack)

        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            WorldClockTab.entries.forEach { option ->
                Tab(
                    selected = option == tab,
                    onClick = { tab = option },
                    text = { Text(option.label) },
                    modifier = Modifier.testTag(worldClockTabTestTag(option)),
                )
            }
        }

        when (tab) {
            WorldClockTab.CLOCKS -> ClocksTab(
                state = state,
                candidates = { query -> viewModel.candidates(query, state.savedZoneIds) },
                onAdd = viewModel::onAddCity,
                onRemove = viewModel::onRemoveCity,
            )

            WorldClockTab.CONVERTER -> ConverterTab(state = state, viewModel = viewModel)

            WorldClockTab.DATES -> DatesTab(today = state.local.time.dateTime.date, viewModel = viewModel)
        }
    }
}

@Composable
private fun ClocksTab(
    state: WorldClockState,
    candidates: (String) -> List<City>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val local = state.local

    JarvisCard(modifier = Modifier.fillMaxWidth().testTag(WorldClockLocalTestTag)) {
        Text(
            text = "내 시간대 · ${local.city.name}",
            style = JarvisTheme.typography.labelLarge,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = local.time.dateTime.clockText(withSeconds = true), style = JarvisTheme.typography.displayMedium)
        Text(
            text = "${local.time.dateTime.date.shortLabel()} · ${utcOffsetLabel(local.time.offsetSeconds)}",
            style = JarvisTheme.typography.bodyMedium,
        )
    }

    if (state.cities.isEmpty()) {
        Text(
            text = "놓은 도시가 없다. 아래 \"도시 추가\" 로 더한다.",
            style = JarvisTheme.typography.bodyMedium,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }

    state.cities.forEach { CityRow(it, onRemove) }

    var adding by rememberSaveable { mutableStateOf(false) }

    if (!adding) {
        OutlinedButton(onClick = { adding = true }, modifier = Modifier.testTag(WorldClockAddTestTag)) {
            Icon(imageVector = JarvisIcons.Add, contentDescription = null, modifier = Modifier.size(JarvisTheme.dimens.iconSize.small))
            Text("도시 추가")
        }
        return
    }

    val query = rememberTextFieldState()
    val text = query.text.toString()
    val found = remember(text, state.savedZoneIds) { candidates(text) }

    OutlinedTextField(
        state = query,
        modifier = Modifier.fillMaxWidth().testTag(WorldClockSearchTestTag),
        placeholder = { Text("도시·시간대 검색 (예: 뉴욕, London, Asia/Seoul)") },
        lineLimits = TextFieldLineLimits.SingleLine,
    )

    TextButton(onClick = { adding = false }) { Text("닫기") }

    if (found.isEmpty()) {
        Text(
            text = "찾는 도시가 없다.",
            style = JarvisTheme.typography.bodyMedium,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }

    found.forEach { city ->
        ListItem(
            headlineContent = { Text(city.name) },
            supportingContent = { Text(city.zoneId) },
            trailingContent = { Icon(imageVector = JarvisIcons.Add, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = JarvisTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(worldClockCandidateTestTag(city.zoneId))
                .clickable {
                    onAdd(city.zoneId)
                    adding = false
                },
        )
    }
}

@Composable
private fun CityRow(city: CityTime, onRemove: (String) -> Unit) {
    val spacing = JarvisTheme.dimens.spacing

    JarvisCard(modifier = Modifier.fillMaxWidth().testTag(worldClockCityTestTag(city.city.zoneId))) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = city.city.name, style = JarvisTheme.typography.titleMedium)
                Text(
                    text = listOf(
                        city.time.dateTime.date.shortLabel(),
                        utcOffsetLabel(city.time.offsetSeconds),
                        offsetDifferenceLabel(city.offsetDifferenceSeconds),
                    ).joinToString(" · "),
                    style = JarvisTheme.typography.bodySmall,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = city.time.dateTime.clockText(withSeconds = false), style = JarvisTheme.typography.headlineSmall)
                dayDifferenceLabel(city.dayDifference)?.let {
                    Text(text = it, style = JarvisTheme.typography.labelMedium, color = JarvisTheme.colorScheme.primary)
                }
            }

            JarvisIconButton(
                icon = JarvisIcons.Close,
                contentDescription = "${city.city.name} 삭제",
                onClick = { onRemove(city.city.zoneId) },
            )
        }
    }
}

@Composable
private fun ConverterTab(state: WorldClockState, viewModel: WorldClockViewModel) {
    val spacing = JarvisTheme.dimens.spacing
    val options = remember(state.local.city.zoneId) { viewModel.zoneOptions(state.local.city.zoneId) }

    var fromZone by rememberSaveable { mutableStateOf(state.local.city.zoneId) }
    var toZone by rememberSaveable {
        mutableStateOf(state.cities.firstOrNull { it.city.zoneId != fromZone }?.city?.zoneId ?: WorldCities.UtcZoneId)
    }
    // 탭을 처음 열 때의 기준 시각으로 한 번만 채운다. 그 뒤로는 사용자가 친 값이 남는다.
    val start = remember { state.local.time.dateTime }
    val date = rememberTextFieldState(start.date.toString())
    val time = rememberTextFieldState(start.time.toString())

    ZonePicker(
        label = "기준 시간대",
        selected = fromZone,
        options = options,
        onSelect = { fromZone = it },
        modifier = Modifier.testTag(WorldClockFromZoneTestTag),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s), verticalAlignment = Alignment.CenterVertically) {
        InputField(date, label = "날짜", placeholder = "2026-09-26", testTag = WorldClockConvertDateTestTag, modifier = Modifier.weight(1f))
        InputField(time, label = "시각", placeholder = "14:30", testTag = WorldClockConvertTimeTestTag, modifier = Modifier.weight(1f))
    }

    OutlinedButton(
        onClick = {
            viewModel.nowIn(fromZone)?.let {
                date.setTextAndPlaceCursorAtEnd(it.date.toString())
                time.setTextAndPlaceCursorAtEnd(it.time.toString())
            }
        },
        modifier = Modifier.testTag(WorldClockConvertNowTestTag),
    ) {
        Text("지금")
    }

    ZonePicker(
        label = "바꿀 시간대",
        selected = toZone,
        options = options,
        onSelect = { toZone = it },
        modifier = Modifier.testTag(WorldClockToZoneTestTag),
    )

    val dateText = date.text.toString()
    val timeText = time.text.toString()
    val others = state.savedZoneIds.filter { it != fromZone && it != toZone }
    val result = remember(dateText, timeText, fromZone, toZone, others) {
        viewModel.convert(dateText, timeText, fromZone, listOf(toZone) + others)
    }

    when (result) {
        ConversionResult.InvalidInput -> ErrorText("날짜는 2026-09-26, 시각은 14:30 처럼 적는다.")

        ConversionResult.UnknownZone -> ErrorText("이 기기가 모르는 시간대다.")

        is ConversionResult.Converted -> {
            val conversion = result.conversion
            val main = conversion.targets.firstOrNull { it.time.zoneId == toZone }

            when (conversion.kind) {
                LocalTimeKind.GAP -> NoteText(
                    "서머타임이 시작돼 건너뛴 시각이다. ${WorldCities.cityOf(fromZone).name} 에서는 ${conversion.source.label()} 로 본다.",
                )

                LocalTimeKind.OVERLAP -> NoteText("서머타임이 끝나 두 번 있는 시각이다. 앞의 것(${utcOffsetLabel(conversion.source.offsetSeconds)})으로 본다.")

                LocalTimeKind.UNIQUE -> Unit
            }

            if (main != null) {
                JarvisCard(modifier = Modifier.fillMaxWidth().testTag(WorldClockConvertResultTestTag)) {
                    Text(
                        text = main.city.name,
                        style = JarvisTheme.typography.labelLarge,
                        color = JarvisTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = main.time.label(), style = JarvisTheme.typography.headlineMedium)
                    Text(
                        text = listOfNotNull(
                            utcOffsetLabel(main.time.offsetSeconds),
                            "기준보다 ${offsetDifferenceLabel(main.offsetDifferenceSeconds)}",
                            dayDifferenceLabel(main.dayDifference),
                        ).joinToString(" · "),
                        style = JarvisTheme.typography.bodyMedium,
                    )
                }
            }

            conversion.targets.filter { it.time.zoneId != toZone }.forEach { target ->
                ListItem(
                    headlineContent = { Text(target.city.name) },
                    supportingContent = { Text(utcOffsetLabel(target.time.offsetSeconds)) },
                    trailingContent = {
                        Text(listOfNotNull(dayDifferenceLabel(target.dayDifference), target.time.dateTime.clockText(withSeconds = false)).joinToString(" "))
                    },
                    colors = ListItemDefaults.colors(containerColor = JarvisTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag(worldClockConvertCityTestTag(target.time.zoneId)),
                )
            }
        }
    }
}

@Composable
private fun ZonePicker(
    label: String,
    selected: String,
    options: List<City>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs)) {
        Text(text = label, style = JarvisTheme.typography.labelLarge, color = JarvisTheme.colorScheme.onSurfaceVariant)

        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = modifier) {
                Text(WorldCities.cityOf(selected).pickerLabel())
                Icon(imageVector = JarvisIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(JarvisTheme.dimens.iconSize.small))
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { city ->
                    DropdownMenuItem(
                        text = { Text(city.pickerLabel()) },
                        onClick = {
                            onSelect(city.zoneId)
                            expanded = false
                        },
                        modifier = Modifier.testTag(worldClockZoneOptionTestTag(city.zoneId)),
                    )
                }
            }
        }
    }
}

@Composable
private fun DatesTab(today: CivilDate, viewModel: WorldClockViewModel) {
    val todayText = remember { today.toString() }

    DateSection(title = "두 날짜 사이") {
        val from = rememberTextFieldState(todayText)
        val to = rememberTextFieldState(todayText)
        InputField(from, label = "시작 날짜", placeholder = todayText, testTag = WorldClockDiffFromTestTag)
        InputField(to, label = "끝 날짜", placeholder = todayText, testTag = WorldClockDiffToTestTag)

        val fromText = from.text.toString()
        val toText = to.text.toString()
        val difference = remember(fromText, toText) { viewModel.difference(fromText, toText) }
        ResultLines(
            lines = difference?.lines(),
            invalid = difference == null,
            testTag = WorldClockDiffResultTestTag,
        )
    }

    DateSection(title = "날짜 더하기·빼기") {
        val base = rememberTextFieldState(todayText)
        val days = rememberTextFieldState("100")
        InputField(base, label = "기준 날짜", placeholder = todayText, testTag = WorldClockAddBaseTestTag)
        InputField(days, label = "더할 날 수 (빼려면 −)", placeholder = "100", testTag = WorldClockAddDaysTestTag)

        val baseText = base.text.toString()
        val daysText = days.text.toString()
        val result = remember(baseText, daysText) { viewModel.addDays(baseText, daysText) }
        ResultLines(lines = result?.let { listOf(it.isoLabel()) }, invalid = result == null, testTag = WorldClockAddResultTestTag)
    }

    DateSection(title = "만 나이") {
        val birth = rememberTextFieldState()
        InputField(birth, label = "생년월일", placeholder = "2000-01-01", testTag = WorldClockBirthTestTag)

        val birthText = birth.text.toString()
        val age = remember(birthText, today) { viewModel.age(birthText, today) }
        when {
            birthText.isBlank() -> Unit
            age is AgeResult.Age -> ResultLines(
                lines = listOf(
                    "만 ${age.age.years}세",
                    "다음 생일 ${age.age.nextBirthday.isoLabel()} · ${dDayLabel(age.age.daysUntilNextBirthday)}",
                ),
                invalid = false,
                testTag = WorldClockAgeResultTestTag,
            )
            age == AgeResult.NotBornYet -> ErrorText("오늘보다 뒤의 날짜다.", Modifier.testTag(WorldClockAgeResultTestTag))
            else -> ResultLines(lines = null, invalid = true, testTag = WorldClockAgeResultTestTag)
        }
    }

    DateSection(title = "D-day") {
        val target = rememberTextFieldState()
        InputField(target, label = "목표 날짜", placeholder = "${today.year}-12-25", testTag = WorldClockTargetTestTag)

        val targetText = target.text.toString()
        val dDay = remember(targetText, today) { viewModel.daysUntil(targetText, today) }
        if (targetText.isNotBlank()) {
            ResultLines(
                lines = dDay?.let { (date, days) -> listOf(dDayLabel(days), date.isoLabel()) },
                invalid = dDay == null,
                testTag = WorldClockDDayResultTestTag,
            )
        }
    }
}

@Composable
private fun DateSection(title: String, content: @Composable () -> Unit) {
    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = JarvisTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun InputField(
    state: TextFieldState,
    label: String,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        state = state,
        modifier = modifier.testTag(testTag),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}

@Composable
private fun ResultLines(lines: List<String>?, invalid: Boolean, testTag: String) {
    if (invalid || lines == null) {
        ErrorText("2026-09-26 처럼 적는다.", Modifier.testTag(testTag))
        return
    }

    Column(modifier = Modifier.testTag(testTag)) {
        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                style = if (index == 0) JarvisTheme.typography.headlineSmall else JarvisTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = JarvisTheme.typography.bodyMedium, color = JarvisTheme.colorScheme.error)
}

@Composable
private fun NoteText(text: String) {
    Text(text = text, style = JarvisTheme.typography.bodyMedium, color = JarvisTheme.colorScheme.tertiary)
}

/** `2026-09-26 (토) 20:00` */
private fun ZonedTime.label(): String = "${dateTime.date.isoLabel()} ${dateTime.clockText(withSeconds = false)}"
