plugins {
    id("com.android.application")
}

android {
    namespace = "com.readbook.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.readbook.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
}
