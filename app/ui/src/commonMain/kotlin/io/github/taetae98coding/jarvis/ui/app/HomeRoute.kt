package io.github.taetae98coding.jarvis.ui.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 앱 셸이 가진 유일한 라우트. 기능의 라우트는 각 기능의 Koin 모듈이 등록한다. */
@Serializable
internal data object HomeRoute : NavKey
