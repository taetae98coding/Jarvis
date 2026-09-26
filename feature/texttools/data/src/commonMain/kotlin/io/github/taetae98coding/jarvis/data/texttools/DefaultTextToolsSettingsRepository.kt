package io.github.taetae98coding.jarvis.data.texttools

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.texttools.LimitBasis
import io.github.taetae98coding.jarvis.domain.texttools.PasswordOptions
import io.github.taetae98coding.jarvis.domain.texttools.TextLimit
import io.github.taetae98coding.jarvis.domain.texttools.TextTool
import io.github.taetae98coding.jarvis.domain.texttools.TextToolsSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DefaultTextToolsSettingsRepository(
    private val store: SettingsStore,
) : TextToolsSettingsRepository {
    override fun observeSelectedTool(): Flow<TextTool> =
        store.observeString(SelectedToolKey, TextTool.COUNT.storedValue).map(TextTool::fromStored)

    override fun readSelectedTool(): TextTool =
        TextTool.fromStored(store.getString(SelectedToolKey, TextTool.COUNT.storedValue))

    override fun setSelectedTool(tool: TextTool) {
        store.putString(SelectedToolKey, tool.storedValue)
    }

    override fun observeInput(): Flow<String> = store.observeString(InputKey, "")

    override fun readInput(): String = store.getString(InputKey, "")

    override fun setInput(input: String) {
        store.putString(InputKey, input)
    }

    override fun observeLimit(): Flow<TextLimit> =
        combine(store.observeString(LimitKey, ""), store.observeString(LimitBasisKey, "")) { target, basis ->
            limitOf(target, basis)
        }.distinctUntilChanged()

    override fun readLimit(): TextLimit = limitOf(store.getString(LimitKey, ""), store.getString(LimitBasisKey, ""))

    override fun setLimit(limit: TextLimit) {
        store.putString(LimitKey, limit.target?.toString().orEmpty())
        store.putString(LimitBasisKey, limit.basis.storedValue)
    }

    override fun observePasswordOptions(): Flow<PasswordOptions> =
        combine(
            store.observeString(PasswordLengthKey, ""),
            store.observeBoolean(PasswordUppercaseKey, Defaults.uppercase),
            store.observeBoolean(PasswordLowercaseKey, Defaults.lowercase),
            store.observeBoolean(PasswordDigitsKey, Defaults.digits),
            store.observeBoolean(PasswordSymbolsKey, Defaults.symbols),
        ) { length, uppercase, lowercase, digits, symbols ->
            PasswordFlags(length, uppercase, lowercase, digits, symbols)
        }.combine(store.observeBoolean(PasswordExcludeAmbiguousKey, Defaults.excludeAmbiguous)) { flags, excludeAmbiguous ->
            flags.toOptions(excludeAmbiguous)
        }.distinctUntilChanged()

    override fun readPasswordOptions(): PasswordOptions =
        PasswordFlags(
            length = store.getString(PasswordLengthKey, ""),
            uppercase = store.getBoolean(PasswordUppercaseKey, Defaults.uppercase),
            lowercase = store.getBoolean(PasswordLowercaseKey, Defaults.lowercase),
            digits = store.getBoolean(PasswordDigitsKey, Defaults.digits),
            symbols = store.getBoolean(PasswordSymbolsKey, Defaults.symbols),
        ).toOptions(store.getBoolean(PasswordExcludeAmbiguousKey, Defaults.excludeAmbiguous))

    override fun setPasswordOptions(options: PasswordOptions) {
        store.putString(PasswordLengthKey, options.length.toString())
        store.putBoolean(PasswordUppercaseKey, options.uppercase)
        store.putBoolean(PasswordLowercaseKey, options.lowercase)
        store.putBoolean(PasswordDigitsKey, options.digits)
        store.putBoolean(PasswordSymbolsKey, options.symbols)
        store.putBoolean(PasswordExcludeAmbiguousKey, options.excludeAmbiguous)
    }

    private fun limitOf(target: String, basis: String): TextLimit =
        TextLimit.of(target.toIntOrNull(), LimitBasis.fromStored(basis))

    // combine 이 인자 여섯부터는 배열 람다만 받아서, 다섯을 먼저 묶는다.
    private data class PasswordFlags(
        val length: String,
        val uppercase: Boolean,
        val lowercase: Boolean,
        val digits: Boolean,
        val symbols: Boolean,
    ) {
        fun toOptions(excludeAmbiguous: Boolean): PasswordOptions =
            PasswordOptions(
                length = length.toIntOrNull() ?: PasswordOptions.DefaultLength,
                uppercase = uppercase,
                lowercase = lowercase,
                digits = digits,
                symbols = symbols,
                excludeAmbiguous = excludeAmbiguous,
            ).normalized()
    }

    internal companion object {
        const val SelectedToolKey = "texttools_selected_tool"
        const val InputKey = "texttools_input"
        const val LimitKey = "texttools_limit"
        const val LimitBasisKey = "texttools_limit_basis"
        const val PasswordLengthKey = "texttools_password_length"
        const val PasswordUppercaseKey = "texttools_password_uppercase"
        const val PasswordLowercaseKey = "texttools_password_lowercase"
        const val PasswordDigitsKey = "texttools_password_digits"
        const val PasswordSymbolsKey = "texttools_password_symbols"
        const val PasswordExcludeAmbiguousKey = "texttools_password_exclude_ambiguous"

        private val Defaults = PasswordOptions()
    }
}
