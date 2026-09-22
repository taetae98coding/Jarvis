package io.github.taetae98coding.jarvis.ui.appinfo

import androidx.lifecycle.ViewModel
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase

internal class AppInfoViewModel(
    getAppInfo: GetAppInfoUseCase,
) : ViewModel() {
    val appInfo: AppInfo = getAppInfo()
}
