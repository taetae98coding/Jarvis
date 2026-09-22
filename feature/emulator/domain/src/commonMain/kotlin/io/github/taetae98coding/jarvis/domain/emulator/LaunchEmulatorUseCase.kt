package io.github.taetae98coding.jarvis.domain.emulator

/**
 * 켤 수 없는 기기는 여기서 걸러낸다. 화면이 이미 막고 있지만, 목록이 갱신되는 사이에 들어온
 * 요청이 기기까지 가지 않도록 규칙을 도메인에도 둔다.
 */
class LaunchEmulatorUseCase(
    private val repository: EmulatorRepository,
) {
    suspend operator fun invoke(device: EmulatorDevice) {
        if (!device.canLaunch) return

        repository.launch(device.id)
    }
}
