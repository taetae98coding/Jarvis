rootProject.name = "Jarvis"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // 타깃 선언을 모듈 16개가 반복하지 않게 컨벤션 플러그인을 여기서 들여온다.
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core:automation")
include(":core:browser")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:widget")

include(":feature:appinfo:domain")
include(":feature:appinfo:data")
include(":feature:appinfo:ui")

include(":feature:emulator:domain")
include(":feature:emulator:data")
include(":feature:emulator:ui")

include(":feature:screen:domain")
include(":feature:screen:data")
include(":feature:screen:ui")
include(":feature:screen:widget")

include(":feature:rotation:domain")
include(":feature:rotation:data")
include(":feature:rotation:ui")
include(":feature:rotation:widget")

include(":feature:terminal:domain")
include(":feature:terminal:data")
include(":feature:terminal:ui")

include(":feature:theme:domain")
include(":feature:theme:data")
include(":feature:theme:ui")

include(":feature:profiling:domain")
include(":feature:profiling:data")
include(":feature:profiling:ui")

include(":feature:battery:domain")
include(":feature:battery:data")
include(":feature:battery:ui")
include(":feature:focus:domain")
include(":feature:focus:data")
include(":feature:focus:ui")
include(":feature:devtools:domain")
include(":feature:devtools:data")
include(":feature:devtools:ui")

include(":feature:mcp:domain")
include(":feature:mcp:data")

include(":app:ui")
include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")
