package io.github.taetae98coding.jarvis.domain.mcp

import io.github.taetae98coding.jarvis.domain.mcp.McpParameterType.BOOLEAN
import io.github.taetae98coding.jarvis.domain.mcp.McpParameterType.INTEGER
import io.github.taetae98coding.jarvis.domain.mcp.McpParameterType.STRING

// 설명은 Claude 가 도구를 고를 때 읽는다. 좌표 규칙과 탭이 붙는다는 사실을 여기 적어야 Claude 가 스크린샷을 먼저 찍는다.

private val tabIdParameter = McpToolParameter(
    name = "tabId",
    type = INTEGER,
    description = "브라우저 탭 id. 빼면 이 세션이 마지막으로 쓴 탭, 없으면 이 패널의 첫 브라우저 탭.",
    required = false,
)

private val deviceIdParameter = McpToolParameter("deviceId", STRING, "device_list 가 준 기기 식별자.")

internal val BrowserTools: List<McpTool> = listOf(
    McpTool(
        name = "browser_open",
        description = "Jarvis 앱 안에 새 브라우저 탭(Chromium)을 열고 주소를 불러온다. 탭은 이 Claude 가 있는 패널에 선택되지 않은 채 붙어서 사용자가 눌러 볼 수 있다. 새 tabId 를 돌려준다.",
        parameters = listOf(McpToolParameter("url", STRING, "열 주소. 스킴이 없으면 https:// 를 붙인다.")),
    ),
    McpTool(
        name = "browser_tabs",
        description = "이 패널의 브라우저 탭 목록(tabId, 제목, 주소). 사용자가 연 탭도 포함한다.",
    ),
    McpTool(
        name = "browser_navigate",
        description = "브라우저 탭에서 주소를 열고 다 불러올 때까지 기다린다.",
        parameters = listOf(tabIdParameter, McpToolParameter("url", STRING, "열 주소.")),
    ),
    McpTool(
        name = "browser_back",
        description = "브라우저 탭에서 뒤로 간다.",
        parameters = listOf(tabIdParameter),
    ),
    McpTool(
        name = "browser_screenshot",
        description = "보이는 뷰포트를 JPEG 로 찍는다. 이미지 픽셀이 곧 CSS 픽셀이라 browser_click 의 좌표로 그대로 쓴다.",
        parameters = listOf(tabIdParameter),
    ),
    McpTool(
        name = "browser_snapshot",
        description = "주소·제목과, 보이는 링크·버튼·입력칸 등 상호작용 요소의 역할·이름·가운데 좌표(CSS 픽셀), 본문 글자 앞부분을 준다. 스크린샷보다 가볍다.",
        parameters = listOf(tabIdParameter),
    ),
    McpTool(
        name = "browser_click",
        description = "뷰포트 좌표(CSS 픽셀)를 마우스로 누르고 뗀다. 실제 입력(isTrusted)으로 들어간다.",
        parameters = listOf(
            tabIdParameter,
            McpToolParameter("x", INTEGER, "뷰포트 왼쪽에서의 x."),
            McpToolParameter("y", INTEGER, "뷰포트 위에서의 y."),
            McpToolParameter("doubleClick", BOOLEAN, "true 면 두 번 누른다.", required = false),
        ),
    ),
    McpTool(
        name = "browser_type",
        description = "포커스된 요소에 글자를 넣는다. 먼저 browser_click 으로 입력칸을 누른다.",
        parameters = listOf(tabIdParameter, McpToolParameter("text", STRING, "넣을 글자.")),
    ),
    McpTool(
        name = "browser_press_key",
        description = "키 하나를 누르고 뗀다. Enter, Tab, Backspace, Delete, Escape, ArrowUp, ArrowDown, ArrowLeft, ArrowRight, Home, End, PageUp, PageDown.",
        parameters = listOf(tabIdParameter, McpToolParameter("key", STRING, "키 이름.")),
    ),
    McpTool(
        name = "browser_scroll",
        description = "마우스 휠을 굴린다. deltaY 가 양수면 아래로.",
        parameters = listOf(
            tabIdParameter,
            McpToolParameter("deltaY", INTEGER, "굴릴 양(CSS 픽셀)."),
            McpToolParameter("x", INTEGER, "휠을 굴릴 x. 빼면 뷰포트 가운데.", required = false),
            McpToolParameter("y", INTEGER, "휠을 굴릴 y. 빼면 뷰포트 가운데.", required = false),
        ),
    ),
    McpTool(
        name = "browser_evaluate",
        description = "페이지에서 JavaScript 식을 실행하고 결과를 JSON 글자로 준다. Promise 는 기다린다.",
        parameters = listOf(tabIdParameter, McpToolParameter("expression", STRING, "실행할 식.")),
    ),
    McpTool(
        name = "browser_close",
        description = "이 패널의 브라우저 탭을 닫는다.",
        parameters = listOf(McpToolParameter("tabId", INTEGER, "닫을 탭 id.")),
    ),
)

internal val DeviceTools: List<McpTool> = listOf(
    McpTool(
        name = "device_list",
        description = "개발자 머신의 Android 에뮬레이터·iOS 시뮬레이터와 연결된 실물 기기 목록. 줄마다 식별자, 이름, 플랫폼, 실물 여부, 실행 중, 조작 가능 여부.",
    ),
    McpTool(
        name = "device_boot",
        description = "꺼진 Android 에뮬레이터·iOS 시뮬레이터를 켠다. 켜진 뒤의 식별자는 device_list 로 다시 본다.",
        parameters = listOf(deviceIdParameter),
    ),
    McpTool(
        name = "device_screenshot",
        description = "기기 화면을 JPEG 로 찍는다(긴 변 최대 1280px, iOS 는 포인트 크기). 다른 기기 도구의 좌표는 이 이미지의 픽셀이다. 기기는 이 Claude 가 있는 패널에 기기 탭으로 붙어 사용자가 볼 수 있다.",
        parameters = listOf(deviceIdParameter),
    ),
    McpTool(
        name = "device_tap",
        description = "화면을 누르고 뗀다. durationMs 를 주면 그만큼 누르고 있는다(길게 누르기).",
        parameters = listOf(
            deviceIdParameter,
            McpToolParameter("x", INTEGER, "device_screenshot 이미지의 x."),
            McpToolParameter("y", INTEGER, "device_screenshot 이미지의 y."),
            McpToolParameter("durationMs", INTEGER, "누르고 있을 시간.", required = false),
        ),
    ),
    McpTool(
        name = "device_swipe",
        description = "한 점에서 다른 점으로 끈다. 스크롤은 위로 끌어 아래 내용을 본다.",
        parameters = listOf(
            deviceIdParameter,
            McpToolParameter("fromX", INTEGER, "시작 x."),
            McpToolParameter("fromY", INTEGER, "시작 y."),
            McpToolParameter("toX", INTEGER, "끝 x."),
            McpToolParameter("toY", INTEGER, "끝 y."),
            McpToolParameter("durationMs", INTEGER, "끄는 시간. 기본 300.", required = false),
        ),
    ),
    McpTool(
        name = "device_type",
        description = "포커스된 입력칸에 글자를 넣는다. 한글 등 ASCII 밖 글자도 된다. 먼저 device_tap 으로 입력칸을 누른다.",
        parameters = listOf(deviceIdParameter, McpToolParameter("text", STRING, "넣을 글자.")),
    ),
    McpTool(
        name = "device_press",
        description = "하드웨어·시스템 키. back, home, app_switch, enter, delete, power, volume_up, volume_down. iOS 에는 back·app_switch·power 가 없다.",
        parameters = listOf(deviceIdParameter, McpToolParameter("key", STRING, "키 이름.")),
    ),
    McpTool(
        name = "device_ui_tree",
        description = "화면의 UI 요소를 한 줄씩(종류, 글자, 식별자, 가운데 좌표). 좌표는 device_screenshot 이미지의 픽셀이다.",
        parameters = listOf(deviceIdParameter),
    ),
    McpTool(
        name = "device_launch_app",
        description = "앱을 앞으로 띄운다. Android 는 패키지 이름, iOS 는 번들 식별자.",
        parameters = listOf(deviceIdParameter, McpToolParameter("appId", STRING, "패키지 이름 또는 번들 식별자.")),
    ),
)
