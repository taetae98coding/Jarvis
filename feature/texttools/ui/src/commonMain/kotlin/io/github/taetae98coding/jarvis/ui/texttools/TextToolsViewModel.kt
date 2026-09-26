package io.github.taetae98coding.jarvis.ui.texttools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.texttools.CountTextUseCase
import io.github.taetae98coding.jarvis.domain.texttools.GeneratePasswordUseCase
import io.github.taetae98coding.jarvis.domain.texttools.LimitBasis
import io.github.taetae98coding.jarvis.domain.texttools.LimitProgress
import io.github.taetae98coding.jarvis.domain.texttools.ObserveTextToolsSettingsUseCase
import io.github.taetae98coding.jarvis.domain.texttools.PasswordOptions
import io.github.taetae98coding.jarvis.domain.texttools.SelectTextToolUseCase
import io.github.taetae98coding.jarvis.domain.texttools.SetPasswordOptionsUseCase
import io.github.taetae98coding.jarvis.domain.texttools.SetTextInputUseCase
import io.github.taetae98coding.jarvis.domain.texttools.SetTextLimitUseCase
import io.github.taetae98coding.jarvis.domain.texttools.TextLimit
import io.github.taetae98coding.jarvis.domain.texttools.TextStats
import io.github.taetae98coding.jarvis.domain.texttools.TextTool
import io.github.taetae98coding.jarvis.domain.texttools.TextToolsSettings
import io.github.taetae98coding.jarvis.domain.texttools.TextTransformOutput
import io.github.taetae98coding.jarvis.domain.texttools.TransformTextUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class TextToolsViewModel(
    observeSettings: ObserveTextToolsSettingsUseCase,
    private val selectTool: SelectTextToolUseCase,
    private val setInput: SetTextInputUseCase,
    private val setLimit: SetTextLimitUseCase,
    private val setPasswordOptions: SetPasswordOptionsUseCase,
    private val generatePassword: GeneratePasswordUseCase,
    private val countText: CountTextUseCase,
    private val transformText: TransformTextUseCase,
) : ViewModel() {
    val settings: StateFlow<TextToolsSettings> = observeSettings(viewModelScope)

    // 저장이 신호로 돌아오기 전에도 결과가 따라가도록 화면이 마지막으로 넘긴 값을 든다(docs/common/text-tools.html#implementation).
    private val inputDraft = MutableStateFlow<String?>(null)
    private val limitDraft = MutableStateFlow<TextLimit?>(null)
    private val optionsDraft = MutableStateFlow<PasswordOptions?>(null)

    val limit: StateFlow<TextLimit> =
        combine(settings, limitDraft) { saved, draft -> draft ?: saved.limit }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), settings.value.limit)

    val passwordOptions: StateFlow<PasswordOptions> =
        combine(settings, optionsDraft) { saved, draft -> draft ?: saved.passwordOptions }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), settings.value.passwordOptions)

    private val currentInput = combine(settings, inputDraft) { saved, draft -> draft ?: saved.input }

    val stats: StateFlow<TextStats> =
        currentInput.map(countText::invoke)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), countText(inputOf(settings.value)))

    val progress: StateFlow<LimitProgress?> =
        combine(stats, limit) { stats, limit -> limit.progress(stats) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), limit.value.progress(stats.value))

    val transforms: StateFlow<List<TextTransformOutput>> =
        currentInput.map(transformText::invoke)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), transformText(inputOf(settings.value)))

    // 비밀번호는 메모리에만 둔다. 저장하지 않는 까닭은 docs/common/text-tools.html#decision-no-password-persist.
    private val _password = MutableStateFlow(generatePassword(passwordOptions.value))
    val password: StateFlow<String> = _password.asStateFlow()

    /** 입력 칸을 처음 채울 값. 탭을 오가도 방금 친 글자가 저장값보다 앞선다. */
    fun inputOf(settings: TextToolsSettings): String = inputDraft.value ?: settings.input

    fun limitTextOf(limit: TextLimit): String = limit.target?.toString().orEmpty()

    fun onSelectTool(tool: TextTool) {
        selectTool(tool)
    }

    fun onInputChange(input: String) {
        inputDraft.value = input
        setInput(input)
    }

    fun onLimitTextChange(text: String) {
        val next = TextLimit.of(text.filter(Char::isDigit).take(MaxLimitDigits).toIntOrNull(), limit.value.basis)
        limitDraft.value = next
        setLimit(next.target, next.basis)
    }

    fun onLimitBasisChange(basis: LimitBasis) {
        val next = limit.value.copy(basis = basis)
        limitDraft.value = next
        setLimit(next.target, next.basis)
    }

    fun onPasswordOptionsChange(requested: PasswordOptions) {
        val current = passwordOptions.value
        val next = setPasswordOptions(current, requested)
        optionsDraft.value = next
        if (next != current) _password.value = generatePassword(next)
    }

    fun onRegeneratePassword() {
        _password.value = generatePassword(passwordOptions.value)
    }

    private companion object {
        // Int 를 넘지 않는 자릿수. 목표 글자 수로 10억 자가 필요할 일은 없다.
        const val MaxLimitDigits = 9
    }
}
