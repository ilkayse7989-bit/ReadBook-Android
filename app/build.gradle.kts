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
        versionCode = 1
        versionName = "1.0.0-test"
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
    implementation("com.google.android.gms:play-services-ads:25.4.0")
}
