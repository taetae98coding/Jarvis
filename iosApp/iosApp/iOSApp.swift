import SwiftUI
import Shared
import UIKit

// 회전잠금의 실체는 이 마스크다. requestGeometryUpdate 로 방향을 바꿔도 마스크가 넓으면 기기를
// 돌리는 순간 되돌아간다. SwiftUI 가 만든 루트 컨트롤러는 우리 것이 아니라서, 앱 델리게이트가
// 공유 코드의 값을 그대로 답한다.
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        UIInterfaceOrientationMask(rawValue: UInt(DeviceRotationGateKt.supportedInterfaceOrientationMask()))
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}
