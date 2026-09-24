package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.automation.DeviceAutomation
import io.github.taetae98coding.jarvis.domain.emulator.DevicePairingRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import org.koin.dsl.module

val emulatorDataModule = module {
    // 데이터 소스는 :data 의 프로세스 싱글턴이다. JVM 에서는 호스트 에이전트도 같은 Flow 를
    // 구독해야 해서 Koin 이 아니라 최상위 val 이 그 수명을 갖는다.
    single<EmulatorRepository> { DefaultEmulatorRepository(emulatorDataSource) }
    single<DevicePairingRepository> { DefaultDevicePairingRepository(devicePairingDataSource) }
    deviceAutomation?.let { automation -> single<DeviceAutomation> { automation } }
}
