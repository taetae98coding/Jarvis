package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskAll
import platform.UIKit.UIInterfaceOrientationMaskLandscapeLeft
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationMaskPortraitUpsideDown
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS

internal actual fun createDeviceRotationDataSource(context: PlatformContext): DeviceRotationDataSource =
    InterfaceOrientationRotation

/**
 * 앱 창의 방향을 바꾼다. 기기 전역 회전잠금은 읽지도 쓰지도 못한다.
 *
 * 방향을 바꾸는 것과 그 방향을 유지하는 것이 다른 API 라서 둘을 함께 쓴다. 허용 마스크를
 * [DeviceRotationGate] 에 좁혀 두고, `requestGeometryUpdate` 로 지금 그 방향으로 가라고 요청한다.
 */
private object InterfaceOrientationRotation : DeviceRotationDataSource {
    // 우리가 방향을 바꿨을 때는 기기가 움직이지 않아 시스템 알림이 오지 않는다.
    private val ownChanges = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun readStatus(): DeviceRotationStatus =
        DeviceRotationStatus(
            supported = true,
            locked = DeviceRotationGate.mask != UIInterfaceOrientationMaskAll,
            angle = windowScene()?.interfaceOrientation?.let(::angleOf),
        )

    override fun observeStatus(): Flow<DeviceRotationStatus> =
        observeOnSignals(signals = merge(deviceOrientationChanges(), ownChanges)) { readStatus() }

    override fun setAngle(angle: RotationAngle) {
        apply(maskOf(angle))
    }

    override fun setLocked(locked: Boolean) {
        val current = windowScene()?.interfaceOrientation?.let(::angleOf) ?: RotationAngle.Degrees0

        apply(if (locked) maskOf(current) else UIInterfaceOrientationMaskAll)
    }

    // 시스템 회전잠금을 사용자에게 요구할 수 있는 창구가 없다.
    override fun requestPermission() = Unit

    private fun apply(mask: UIInterfaceOrientationMask) {
        DeviceRotationGate.mask = mask

        val preferences = UIWindowSceneGeometryPreferencesIOS(interfaceOrientations = mask)

        // 실패(예: 노치 iPhone 의 거꾸로 방향)는 무시한다. 다음 방출에 실제 방향이 그대로 실려 와서
        // 카드가 저절로 사실을 보여준다.
        windowScene()?.requestGeometryUpdateWithPreferences(preferences) { }

        ownChanges.tryEmit(Unit)
    }

    private fun windowScene(): UIWindowScene? =
        UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>().firstOrNull()

    private fun deviceOrientationChanges(): Flow<Unit> =
        callbackFlow {
            val device = UIDevice.currentDevice

            // 알림은 생성이 켜져 있는 동안에만 온다. 기본값은 꺼짐이다.
            device.beginGeneratingDeviceOrientationNotifications()

            val observer = NSNotificationCenter.defaultCenter.addObserverForName(
                UIDeviceOrientationDidChangeNotification,
                null,
                NSOperationQueue.mainQueue,
            ) { _ -> trySend(Unit) }

            awaitClose {
                NSNotificationCenter.defaultCenter.removeObserver(observer)
                device.endGeneratingDeviceOrientationNotifications()
            }
        }
}

// 각도 기준은 docs/common/device-rotation.html#angle 에 있다. 90도가 LandscapeLeft 인 것이
// 뒤바뀌기 쉬워서 한곳에 모은다.
private fun maskOf(angle: RotationAngle): UIInterfaceOrientationMask =
    when (angle) {
        RotationAngle.Degrees0 -> UIInterfaceOrientationMaskPortrait
        RotationAngle.Degrees90 -> UIInterfaceOrientationMaskLandscapeLeft
        RotationAngle.Degrees180 -> UIInterfaceOrientationMaskPortraitUpsideDown
        RotationAngle.Degrees270 -> UIInterfaceOrientationMaskLandscapeRight
    }

private fun angleOf(orientation: UIInterfaceOrientation): RotationAngle? =
    when (orientation) {
        UIInterfaceOrientationPortrait -> RotationAngle.Degrees0
        UIInterfaceOrientationLandscapeLeft -> RotationAngle.Degrees90
        UIInterfaceOrientationPortraitUpsideDown -> RotationAngle.Degrees180
        UIInterfaceOrientationLandscapeRight -> RotationAngle.Degrees270
        else -> null
    }
