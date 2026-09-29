plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.cs2.tracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cs2.tracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.compose.ui:ui:1.11.1")
    implementation("androidx.compose.material3:material3:1.5.0-alpha19")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("com.squareup.okhttp3:okhttp-android:5.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
