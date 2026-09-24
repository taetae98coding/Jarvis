package io.github.taetae98coding.jarvis.domain.emulator

/**
 * 기기의 "페어링 코드로 기기 페어링" 은 늘 6자리 숫자를 보여 준다. 그 밖의 값은 틀린 것이 확실하므로
 * 기기까지 보내지 않는다. 화면이 이미 막지만 규칙을 도메인에도 둔다.
 */
class PairWithCodeUseCase(
    private val repository: DevicePairingRepository,
) {
    suspend operator fun invoke(service: PairingService, code: String): PairingResult {
        if (!isValidPairingCode(code)) return PairingResult.Failed("페어링 코드는 6자리 숫자입니다.")

        return repository.pair(service, code)
    }
}

const val PairingCodeLength: Int = 6

// Char.isDigit 는 전각 숫자(１２３)도 숫자로 본다. 기기가 보여 주는 것은 ASCII 숫자뿐이다.
fun isValidPairingCode(code: String): Boolean = code.length == PairingCodeLength && code.all { it in '0'..'9' }
