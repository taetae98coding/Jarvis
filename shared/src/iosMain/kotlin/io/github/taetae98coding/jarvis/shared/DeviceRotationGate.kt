package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.data.rotation.DeviceRotationGate

/**
 * iosApp 의 `AppDelegate` 가 `supportedInterfaceOrientationsFor` 에서 부른다.
 *
 * :data 는 프레임워크로 내보내지 않으므로 Swift 가 볼 수 있는 자리는 :shared 뿐이다.
 */
fun supportedInterfaceOrientationMask(): ULong = DeviceRotationGate.mask
