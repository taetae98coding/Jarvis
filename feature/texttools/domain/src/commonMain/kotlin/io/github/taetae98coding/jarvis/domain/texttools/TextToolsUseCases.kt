package io.github.taetae98coding.jarvis.domain.texttools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ObserveTextToolsSettingsUseCase(
    private val settings: TextToolsSettingsRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<TextToolsSettings> =
        combine(
            settings.observeSelectedTool(),
            settings.observeInput(),
            settings.observeLimit(),
            settings.observePasswordOptions(),
            ::TextToolsSettings,
        ).stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            TextToolsSettings(
                tool = settings.readSelectedTool(),
                input = settings.readInput(),
                limit = settings.readLimit(),
                passwordOptions = settings.readPasswordOptions(),
            ),
        )
}

class SelectTextToolUseCase(
    private val settings: TextToolsSettingsRepository,
) {
    operator fun invoke(tool: TextTool) {
        settings.setSelectedTool(tool)
    }
}

class SetTextInputUseCase(
    private val settings: TextToolsSettingsRepository,
) {
    /** 저장값만 자른다. 까닭은 docs/common/text-tools.html R12 와 그 결정에 있다. */
    operator fun invoke(input: String) {
        settings.setInput(truncateForStorage(input))
    }

    companion object {
        const val MaxStoredLength = 8_000

        fun truncateForStorage(input: String): String {
            if (input.length <= MaxStoredLength) return input
            // 잘린 자리가 서로게이트 쌍 사이면 짝 없는 high surrogate 가 남아 다시 읽을 때 깨진 글자가 된다.
            val end = if (input[MaxStoredLength - 1].isHighSurrogate()) MaxStoredLength - 1 else MaxStoredLength
            return input.substring(0, end)
        }
    }
}

class SetTextLimitUseCase(
    private val settings: TextToolsSettingsRepository,
) {
    operator fun invoke(target: Int?, basis: LimitBasis) {
        settings.setLimit(TextLimit.of(target, basis))
    }
}

class SetPasswordOptionsUseCase(
    private val settings: TextToolsSettingsRepository,
) {
    /** 정리한 옵션을 돌려준다. 켜진 종류를 모두 끄려는 변경은 받지 않고 [current] 를 그대로 돌려준다. */
    operator fun invoke(current: PasswordOptions, requested: PasswordOptions): PasswordOptions {
        val next = if (requested.classes.isEmpty()) current.normalized() else requested.normalized()
        settings.setPasswordOptions(next)
        return next
    }
}

class GeneratePasswordUseCase(
    private val generator: PasswordGenerator,
) {
    operator fun invoke(options: PasswordOptions): String = generator.generate(options)
}

class CountTextUseCase {
    operator fun invoke(text: String): TextStats = TextCounter.count(text)
}

class TransformTextUseCase {
    operator fun invoke(text: String): List<TextTransformOutput> = TextTransformer.transformAll(text)
}
