package io.github.taetae98coding.jarvis.domain.worldclock

/** 고를 수 있는 도시 하나. [zoneId] 는 IANA 시간대 이름이고 저장값이다. */
data class City(
    val zoneId: String,
    val name: String,
    val englishName: String,
)

/**
 * 고를 수 있는 도시 목록. 시간대마다 사람이 가장 많이 찾는 도시 하나를 둔다(같은 시간대의 두 도시는 서로 다른
 * IANA 이름이 있을 때만 — 예: 토론토 America/Toronto 와 뉴욕 America/New_York).
 *
 * 목록에는 IANA 정식 이름만 쓴다. 기기 시간대는 옛 별칭으로 올 수 있어(Chrome·Safari 의 Intl 은 ICU 가 쓰는
 * `Asia/Calcutta`·`Asia/Saigon` 을, JVM 은 설정에 따라 `Etc/UTC` 를 준다) [find] 가 [Aliases] 로 먼저 바꾼다.
 */
object WorldCities {
    const val UtcZoneId: String = "UTC"

    val all: List<City> = listOf(
        City("Asia/Seoul", "서울", "Seoul"),
        City("Asia/Tokyo", "도쿄", "Tokyo"),
        City("Asia/Shanghai", "베이징", "Beijing"),
        City("Asia/Hong_Kong", "홍콩", "Hong Kong"),
        City("Asia/Taipei", "타이베이", "Taipei"),
        City("Asia/Manila", "마닐라", "Manila"),
        City("Asia/Singapore", "싱가포르", "Singapore"),
        City("Asia/Bangkok", "방콕", "Bangkok"),
        City("Asia/Ho_Chi_Minh", "호찌민", "Ho Chi Minh City"),
        City("Asia/Jakarta", "자카르타", "Jakarta"),
        City("Asia/Kolkata", "뭄바이·델리", "Mumbai Delhi Kolkata"),
        City("Asia/Kathmandu", "카트만두", "Kathmandu"),
        City("Asia/Dubai", "두바이", "Dubai"),
        City("Asia/Tehran", "테헤란", "Tehran"),
        City("Europe/Moscow", "모스크바", "Moscow"),
        City("Europe/Istanbul", "이스탄불", "Istanbul"),
        City("Europe/Athens", "아테네", "Athens"),
        City("Europe/Berlin", "베를린", "Berlin"),
        City("Europe/Paris", "파리", "Paris"),
        City("Europe/Rome", "로마", "Rome"),
        City("Europe/Madrid", "마드리드", "Madrid"),
        City("Europe/Amsterdam", "암스테르담", "Amsterdam"),
        City("Europe/London", "런던", "London"),
        City("Africa/Cairo", "카이로", "Cairo"),
        City("Africa/Lagos", "라고스", "Lagos"),
        City("Africa/Johannesburg", "요하네스버그", "Johannesburg"),
        City("Africa/Nairobi", "나이로비", "Nairobi"),
        City("America/Sao_Paulo", "상파울루", "Sao Paulo"),
        City("America/Argentina/Buenos_Aires", "부에노스아이레스", "Buenos Aires"),
        City("America/Mexico_City", "멕시코시티", "Mexico City"),
        City("America/New_York", "뉴욕", "New York"),
        City("America/Toronto", "토론토", "Toronto"),
        City("America/Chicago", "시카고", "Chicago"),
        City("America/Denver", "덴버", "Denver"),
        City("America/Phoenix", "피닉스", "Phoenix"),
        City("America/Los_Angeles", "로스앤젤레스", "Los Angeles San Francisco"),
        City("America/Vancouver", "밴쿠버", "Vancouver"),
        City("America/Anchorage", "앵커리지", "Anchorage"),
        City("Pacific/Honolulu", "호놀룰루", "Honolulu"),
        City("Australia/Sydney", "시드니", "Sydney"),
        City("Australia/Perth", "퍼스", "Perth"),
        City("Pacific/Auckland", "오클랜드", "Auckland"),
        City(UtcZoneId, "협정 세계시", "UTC GMT"),
    )

    /** 처음 설치했을 때 세계 시계에 놓는 도시. */
    val defaultZoneIds: List<String> = listOf("America/New_York", "Europe/London", "Asia/Tokyo")

    private val byZone = all.associateBy(City::zoneId)

    private val Aliases = mapOf(
        "Asia/Calcutta" to "Asia/Kolkata",
        "Asia/Saigon" to "Asia/Ho_Chi_Minh",
        "Asia/Katmandu" to "Asia/Kathmandu",
        "America/Buenos_Aires" to "America/Argentina/Buenos_Aires",
        "Etc/UTC" to UtcZoneId,
        "Etc/GMT" to UtcZoneId,
        "Etc/Universal" to UtcZoneId,
        "Etc/Zulu" to UtcZoneId,
        "GMT" to UtcZoneId,
        "Universal" to UtcZoneId,
        "Zulu" to UtcZoneId,
    )

    fun find(zoneId: String): City? = byZone[Aliases[zoneId] ?: zoneId]

    /**
     * 목록에 없는 시간대(기기 시간대가 `Asia/Yangon` 같은 곳일 때)는 IANA 이름의 마지막 조각을 이름으로 쓴다.
     */
    fun cityOf(zoneId: String): City =
        find(zoneId) ?: City(zoneId, zoneId.substringAfterLast('/').replace('_', ' '), zoneId)

    /** 한국어 이름·영어 이름·IANA 이름 어디에든 [query] 가 들어 있으면 고른다. 대소문자와 공백·밑줄을 가리지 않는다. */
    fun search(query: String): List<City> {
        val needle = query.normalized()
        if (needle.isEmpty()) return all

        return all.filter { city ->
            city.name.normalized().contains(needle) ||
                city.englishName.normalized().contains(needle) ||
                city.zoneId.normalized().contains(needle)
        }
    }

    private fun String.normalized(): String = lowercase().filterNot { it.isWhitespace() || it == '_' || it == '·' }
}
