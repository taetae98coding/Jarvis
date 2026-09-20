package io.github.taetae98coding.jarvis.shared.platform

actual val platformName: String = "JVM ${System.getProperty("java.version")}"
