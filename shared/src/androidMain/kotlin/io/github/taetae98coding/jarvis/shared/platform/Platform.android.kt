package io.github.taetae98coding.jarvis.shared.platform

import android.os.Build

actual val platformName: String = "Android ${Build.VERSION.SDK_INT}"
