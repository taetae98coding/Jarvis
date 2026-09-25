plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// 서명 키는 저장소 밖에 둔다. 워크트리마다 없는 keystore.properties 대신 모든 워크트리가 함께 보는
// ~/.gradle/gradle.properties 에서 읽는다(docs/platform/android.html#release-build).
val releaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .associateWith { providers.gradleProperty("jarvis.signing.$it").orNull?.takeIf(String::isNotBlank) }
    .takeIf { properties -> properties.values.all { it != null } }
    ?.mapValues { it.value!! }

android {
    namespace = "io.github.taetae98coding.jarvis"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.taetae98coding.jarvis"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode(libs.versions.appVersion.get())
        versionName = libs.versions.appVersion.get()
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }
}

if (releaseSigning == null) {
    logger.warn("jarvis.signing.* 이 없어 release APK·AAB 를 서명하지 않는다. docs/platform/android.html#release-build")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
}
