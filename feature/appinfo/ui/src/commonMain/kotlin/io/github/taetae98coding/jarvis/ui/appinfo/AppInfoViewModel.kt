package io.github.taetae98coding.jarvis.ui.appinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase
import io.github.taetae98coding.jarvis.domain.appinfo.InstallAppUpdateUseCase
import io.github.taetae98coding.jarvis.domain.appinfo.ObserveAppUpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class AppInfoViewModel(
    getAppInfo: GetAppInfoUseCase,
    observeAppUpdate: ObserveAppUpdateUseCase,
    private val installAppUpdate: InstallAppUpdateUseCase,
) : ViewModel() {
    val appInfo: AppInfo = getAppInfo()

    private val release = observeAppUpdate(viewModelScope)

    private val install = MutableStateFlow<InstallProgress>(InstallProgress.Idle)

    val update: StateFlow<AppUpdateUiState> =
        combine(release, install) { release, install ->
            when {
                release == null -> AppUpdateUiState.None
                install is InstallProgress.Running -> AppUpdateUiState.Installing(release.version)
                install is InstallProgress.Failed -> AppUpdateUiState.Failed(release.version, install.reason)
                else -> AppUpdateUiState.Available(release.version)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AppUpdateUiState.None)

    fun onUpdateClick() {
        val release = release.value ?: return
        if (install.value is InstallProgress.Running) return

        install.value = InstallProgress.Running
        viewModelScope.launch {
            // 성공하면 앱이 곧 종료되므로 "설치 중…" 그대로 둔다.
            installAppUpdate(release).onFailure { error ->
                install.value = InstallProgress.Failed(error.message ?: error.toString())
            }
        }
    }

    private sealed interface InstallProgress {
        data object Idle : InstallProgress

        data object Running : InstallProgress

        data class Failed(val reason: String) : InstallProgress
    }
}

internal sealed interface AppUpdateUiState {
    data object None : AppUpdateUiState

    data class Available(val version: String) : AppUpdateUiState

    data class Installing(val version: String) : AppUpdateUiState

    data class Failed(val version: String, val reason: String) : AppUpdateUiState
}
