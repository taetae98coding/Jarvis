plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    compilerOptions {
        // PlatformContext 가 expect class 다. Beta 기능이라는 안내일 뿐이라 경고를 끈다.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
