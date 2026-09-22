package io.github.taetae98coding.jarvis.data

import android.content.Context

// Context 에 그대로 별칭을 걸 수는 없다. Context 가 abstract 여서 expect 클래스의 modality 와
// 어긋난다(EXPECT_ACTUAL_INCOMPATIBLE_MODALITY). 그래서 한 겹 감싼다.
actual class PlatformContext(val context: Context)
