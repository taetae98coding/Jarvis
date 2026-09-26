package io.github.taetae98coding.jarvis.domain.devtools

/** UUID 는 입력이 아니라 [GenerateUuidsUseCase] 로 만들므로 결과 줄이 없다. */
class ConvertDevToolInputUseCase {
    operator fun invoke(tool: DevTool, input: String, base64UrlSafe: Boolean): List<DevToolOutput> =
        when (tool) {
            DevTool.TIMESTAMP -> TimestampConverter.convert(input)
            DevTool.BASE64 -> Base64Converter.convert(input, base64UrlSafe)
            DevTool.URL -> UrlConverter.convert(input)
            DevTool.JSON -> JsonFormatter.convert(input)
            DevTool.UUID -> emptyList()
            DevTool.HASH -> HashCalculator.convert(input)
            DevTool.COLOR -> ColorConverter.convert(input)
        }

    /** 색 도구의 견본. 색으로 읽히지 않으면 null. */
    fun swatch(tool: DevTool, input: String): RgbColor? = if (tool == DevTool.COLOR) ColorConverter.parse(input) else null
}
