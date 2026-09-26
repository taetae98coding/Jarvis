package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GenerateUuidsUseCase(
    private val random: () -> String = ::randomUuid,
) {
    operator fun invoke(count: Int, uppercase: Boolean): List<String> =
        List(count.coerceIn(1, MaxUuidCount)) { random().let { if (uppercase) it.uppercase() else it.lowercase() } }

    companion object {
        const val MaxUuidCount = 100
    }
}

// Uuid.random() 은 버전 4, 변형 IETF 다.
@OptIn(ExperimentalUuidApi::class)
private fun randomUuid(): String = Uuid.random().toString()
