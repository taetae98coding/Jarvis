package io.github.taetae98coding.jarvis.ui.rotation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 홈 타일이 여는 기능 화면. 앱 셸은 이 타입을 모르고, 등록은 [rotationUiModule] 이 한다. */
@Serializable
internal data object DeviceRotationRoute : NavKey
