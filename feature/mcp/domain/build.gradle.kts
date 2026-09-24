plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // 도구가 부르는 이음새. domain 의존 규칙의 예외다(docs/common/module-architecture.html R13).
            api(projects.core.automation)
        }
    }
}
