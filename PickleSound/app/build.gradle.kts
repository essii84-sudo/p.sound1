plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.picklesound"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.picklesound"
        minSdk = 30          // Wear OS 3 이상 (갤럭시 워치4 이후 모델)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // 개인 사용 목적: 고정 키로 서명 → 새 버전을 덮어써서 설치(업데이트)할 수 있음
    signingConfigs {
        create("personal") {
            storeFile = file("picklesound.jks")
            storePassword = "picklesound"
            keyAlias = "picklesound"
            keyPassword = "picklesound"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("personal")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
}
