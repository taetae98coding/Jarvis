package io.github.taetae98coding.jarvis.browser

import io.github.taetae98coding.jarvis.automation.BrowserAutomation
import org.koin.core.module.Module
import org.koin.dsl.module

actual val browserModule: Module = module {
    if (BrowserEngine.isSupported) single<BrowserAutomation> { CefBrowserAutomation() }
}
