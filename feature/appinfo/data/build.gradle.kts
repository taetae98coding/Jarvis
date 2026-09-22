plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

val generateAppVersion = tasks.register("generateAppVersion") {
    val appVersion = libs.versions.appVersion.get()
    val outputDirectory = layout.buildDirectory.dir("generated/appVersion/kotlin")

    inputs.property("appVersion", appVersion)
    outputs.dir(outputDirectory)

    doLast {
        val file = outputDirectory.get()
            .file("io/github/taetae98coding/jarvis/data/appinfo/AppVersion.kt")
            .asFile

        file.parentFile.mkdirs()
        file.writeText(
            """
            package io.github.taetae98coding.jarvis.data.appinfo

            internal const val APP_VERSION: String = "$appVersion"

            """.trimIndent(),
        )
    }
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppVersion)

            dependencies {
                // appInfoDataModule 이 도메인 인터페이스를 내보내므로 api 다.
                api(projects.feature.appinfo.domain)
            }
        }
    }
}
