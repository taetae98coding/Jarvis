package io.github.taetae98coding.jarvis.domain.emulator

/**
 * 깨우는 것도 입력을 주입하는 일이라 제스처와 같은 조건에서만 된다. 화면이 이미 막고 있지만,
 * 목록이 갱신되는 사이에 들어온 요청이 기기까지 가지 않도록 규칙을 도메인에도 둔다.
 *
 * 화면이 꺼져 있는지는 보지 않는다. 목록이 최대 5초 늦어서 이미 켜진 기기에 요청이 갈 수 있는데,
 * 그 명령은 켜진 화면을 다시 켤 뿐이라 해가 없다.
 */
class WakeDeviceUseCase(
    private val repository: EmulatorRepository,
) {
    suspend operator fun invoke(device: EmulatorDevice) {
        if (!device.canControl) return

        repository.wake(device.id)
    }
}
