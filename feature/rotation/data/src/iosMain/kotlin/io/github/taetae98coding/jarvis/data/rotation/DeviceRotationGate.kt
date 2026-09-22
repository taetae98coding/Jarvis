package io.github.taetae98coding.jarvis.data.rotation

import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskAll

/**
 * 앱이 허용하는 화면 방향. 회전잠금의 실체다.
 *
 * `requestGeometryUpdate` 로 방향을 바꿔도 이 마스크가 넓으면 기기를 돌리는 순간 되돌아간다.
 * SwiftUI 가 만든 루트 컨트롤러는 우리 것이 아니라서, 마스크는 앱 델리게이트의
 * `supportedInterfaceOrientationsFor` 가 답한다. iosApp 이 그 답을 여기서 읽어 간다.
 */
public object DeviceRotationGate {
    public var mask: UIInterfaceOrientationMask = UIInterfaceOrientationMaskAll
        internal set
}
