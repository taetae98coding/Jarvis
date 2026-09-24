import org.gradle.api.artifacts.VersionCatalogsExtension

// Android 전용 라이브러리. 홈 화면 위젯처럼 다른 타깃에 넣을 코드가 한 줄도 없는 모듈이 쓴다.
// KMP 타깃을 선언하지 않고 AGP 9 의 내장 Kotlin 으로 컴파일한다(:androidApp 과 같다).
plugins {
    id("com.android.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    // jarvis.kmp.library 와 같은 규칙으로 경로에서 namespace 를 만든다. 패키지와 다르므로 매니페스트의
    // 클래스 이름은 상대 이름(.Foo)이 아니라 전부 적어야 한다.
    namespace = "io.github.taetae98coding.jarvis" +
        path.removePrefix(":").split(":").joinToString("") { ".$it" }
    compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()

    defaultConfig {
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
    }
}
