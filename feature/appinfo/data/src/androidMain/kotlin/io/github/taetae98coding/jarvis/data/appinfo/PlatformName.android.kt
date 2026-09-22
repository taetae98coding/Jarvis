package io.github.taetae98coding.jarvis.data.appinfo

import android.os.Build

internal actual val platformName: String = "Android ${Build.VERSION.SDK_INT}"
