package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.AndroidProject
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunChoice
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.IosProject
import io.github.taetae98coding.jarvis.domain.terminal.IosRunChoice
import io.github.taetae98coding.jarvis.domain.terminal.IosRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.ProjectLoad
import io.github.taetae98coding.jarvis.domain.terminal.RunDevice
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import io.github.taetae98coding.jarvis.ui.device.DeviceScreens
import kotlinx.coroutines.flow.Flow

const val TerminalRunDialogTestTag = "terminal:run-dialog"
const val TerminalRunDialogModuleTestTag = "terminal:run-dialog:module"
const val TerminalRunDialogVariantTestTag = "terminal:run-dialog:variant"
const val TerminalRunDialogDeviceTestTag = "terminal:run-dialog:device"
const val TerminalRunDialogLoadingTestTag = "terminal:run-dialog:loading"
const val TerminalRunDialogErrorTestTag = "terminal:run-dialog:error"
const val TerminalRunDialogRetryTestTag = "terminal:run-dialog:retry"
const val TerminalRunDialogConfirmTestTag = "terminal:run-dialog:confirm"

fun terminalRunDialogOptionTestTag(value: String): String = "terminal:run-dialog:option:$value"

private class Option(val key: String, val label: String, val detail: String? = null)

/** Android 앱 실행 창(docs/common/terminal-run.html R6–R8·R17). */
@Composable
internal fun AndroidRunDialog(
    directory: String,
    load: (String) -> Flow<ProjectLoad<AndroidProject>>,
    devices: DeviceScreens?,
    remembered: AndroidRunChoice?,
    onRun: (AndroidRunRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val project by remember(directory, attempt) { load(directory) }.collectAsStateWithLifecycle(ProjectLoad.Loading)
    val targets = runTargets(devices, DevicePlatform.Android)

    var moduleKey by remember { mutableStateOf(remembered?.modulePath) }
    var variantKey by remember { mutableStateOf(remembered?.variant) }
    var deviceKey by remember { mutableStateOf(remembered?.deviceId) }

    val apps = (project as? ProjectLoad.Loaded)?.value?.apps.orEmpty()
    val app = apps.firstOrNull { it.modulePath == moduleKey } ?: apps.firstOrNull()
    val variants = app?.variants.orEmpty()
    val variant = variants.firstOrNull { it.name == variantKey } ?: variants.firstOrNull { it.name == "debug" } ?: variants.firstOrNull()
    val device = pickDevice(targets, deviceKey)

    RunDialogScaffold(
        title = "Android 앱 실행",
        load = project,
        loadingText = "빌드 변형을 읽는 중…",
        errorText = "빌드 변형을 읽지 못했습니다.",
        emptyText = "Android 앱 모듈이 없습니다.".takeIf { project is ProjectLoad.Loaded && apps.isEmpty() },
        onRetry = { attempt++ },
        canRun = app != null && variant != null && device != null,
        onRun = { if (app != null && variant != null && device != null) onRun(AndroidRunRequest(directory, app.modulePath, variant, device)) },
        onDismiss = onDismiss,
    ) {
        if (apps.size > 1) {
            SelectField("모듈", app?.modulePath, apps.map { Option(it.modulePath, it.modulePath) }, { moduleKey = it }, TerminalRunDialogModuleTestTag)
        }
        SelectField("빌드 변형", variant?.name, variants.map { Option(it.name, it.name, it.applicationId) }, { variantKey = it }, TerminalRunDialogVariantTestTag)
        DeviceField(targets, device, onSelect = { deviceKey = it })
    }
}

/** iOS 앱 실행 창(docs/common/terminal-run.html R12·R13·R17). */
@Composable
internal fun IosRunDialog(
    directory: String,
    load: (String) -> Flow<ProjectLoad<IosProject>>,
    devices: DeviceScreens?,
    remembered: IosRunChoice?,
    onRun: (IosRunRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val project by remember(directory, attempt) { load(directory) }.collectAsStateWithLifecycle(ProjectLoad.Loading)
    val targets = runTargets(devices, DevicePlatform.IOS)

    var schemeKey by remember { mutableStateOf(remembered?.scheme) }
    var configurationKey by remember { mutableStateOf(remembered?.configuration) }
    var deviceKey by remember { mutableStateOf(remembered?.deviceId) }

    val loaded = (project as? ProjectLoad.Loaded)?.value
    val schemes = loaded?.schemes.orEmpty()
    val scheme = schemes.firstOrNull { it == schemeKey } ?: schemes.firstOrNull()
    val configurations = loaded?.configurations.orEmpty()
    val configuration = configurations.firstOrNull { it == configurationKey } ?: configurations.firstOrNull { it == "Debug" } ?: configurations.firstOrNull()
    val device = pickDevice(targets, deviceKey)

    RunDialogScaffold(
        title = "iOS 앱 실행",
        load = project,
        loadingText = "스킴을 읽는 중…",
        errorText = "스킴을 읽지 못했습니다.",
        emptyText = "스킴이 없습니다.".takeIf { loaded != null && schemes.isEmpty() },
        onRetry = { attempt++ },
        canRun = loaded != null && scheme != null && configuration != null && device != null,
        onRun = {
            if (loaded != null && scheme != null && configuration != null && device != null) onRun(IosRunRequest(loaded, scheme, configuration, device))
        },
        onDismiss = onDismiss,
    ) {
        if (schemes.size > 1) {
            SelectField("스킴", scheme, schemes.map { Option(it, it) }, { schemeKey = it }, TerminalRunDialogModuleTestTag)
        }
        SelectField("구성", configuration, configurations.map { Option(it, it) }, { configurationKey = it }, TerminalRunDialogVariantTestTag)
        DeviceField(targets, device, onSelect = { deviceKey = it })
    }
}

/** null 은 기기 목록을 아직 모르는 것이다. 기기 기능이 빠진 조립이면 빈 목록이다. */
@Composable
private fun runTargets(devices: DeviceScreens?, platform: DevicePlatform): List<Pair<RunDevice, String>>? {
    val choices = if (devices == null) emptyList() else devices.runTargets() ?: return null

    return choices.filter { it.devicePlatform == platform }.map { it.toRunDevice() to it.kind }
}

private fun DeviceChoice.toRunDevice(): RunDevice =
    RunDevice(id = id, name = name, platform = devicePlatform, isPhysical = isPhysical, isRunning = isRunning, canMirror = canMirror)

// 저장된 기기가 지금 없으면 켜진 첫 기기, 그것도 없으면 첫 기기다(R17).
private fun pickDevice(targets: List<Pair<RunDevice, String>>?, key: String?): RunDevice? {
    val devices = targets?.map { it.first }.orEmpty()

    return devices.firstOrNull { it.id == key } ?: devices.firstOrNull { it.isRunning } ?: devices.firstOrNull()
}

@Composable
private fun DeviceField(targets: List<Pair<RunDevice, String>>?, device: RunDevice?, onSelect: (String) -> Unit) {
    when {
        targets == null -> Text("기기를 찾는 중…", style = JarvisTheme.typography.bodyMedium, color = JarvisTheme.colorScheme.onSurfaceVariant)
        targets.isEmpty() -> Text("실행할 기기가 없습니다.", style = JarvisTheme.typography.bodyMedium, color = JarvisTheme.colorScheme.onSurfaceVariant)
        else -> SelectField(
            label = "기기",
            value = device?.id,
            options = targets.map { (target, kind) -> Option(target.id, target.name, kind) },
            onSelect = onSelect,
            testTag = TerminalRunDialogDeviceTestTag,
        )
    }
}

@Composable
private fun RunDialogScaffold(
    title: String,
    load: ProjectLoad<*>,
    loadingText: String,
    errorText: String,
    emptyText: String?,
    onRetry: () -> Unit,
    canRun: Boolean,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m)) {
                when {
                    load is ProjectLoad.Loading -> Text(loadingText, modifier = Modifier.testTag(TerminalRunDialogLoadingTestTag))
                    load is ProjectLoad.Failed -> {
                        Text(errorText, color = JarvisTheme.colorScheme.error, modifier = Modifier.testTag(TerminalRunDialogErrorTestTag))
                        load.message?.let {
                            Text(it, style = JarvisTheme.typography.bodySmall, color = JarvisTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onRetry, modifier = Modifier.testTag(TerminalRunDialogRetryTestTag)) { Text("다시 시도") }
                    }
                    emptyText != null -> Text(emptyText, modifier = Modifier.testTag(TerminalRunDialogErrorTestTag))
                    else -> content()
                }
            }
        },
        confirmButton = {
            Button(onClick = onRun, enabled = canRun, modifier = Modifier.testTag(TerminalRunDialogConfirmTestTag)) { Text("실행") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        modifier = Modifier.testTag(TerminalRunDialogTestTag),
    )
}

/** 라벨·지금 값·▼ 한 줄. 누르면 목록 메뉴가 뜬다. */
@Composable
private fun SelectField(label: String, value: String?, options: List<Option>, onSelect: (String) -> Unit, testTag: String) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.key == value }

    Box {
        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { expanded = true }.testTag(testTag)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(JarvisTheme.dimens.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = JarvisTheme.typography.labelMedium, color = JarvisTheme.colorScheme.onSurfaceVariant)
                    Text(selected?.label ?: "-", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    selected?.detail?.let {
                        Text(it, style = JarvisTheme.typography.bodySmall, color = JarvisTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(imageVector = JarvisIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(JarvisTheme.dimens.iconSize.small))
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option.label)
                            option.detail?.let { Text(it, style = JarvisTheme.typography.bodySmall, color = JarvisTheme.colorScheme.onSurfaceVariant) }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option.key)
                    },
                    modifier = Modifier.testTag(terminalRunDialogOptionTestTag(option.key)),
                )
            }
        }
    }
}
