plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.haval.m6launcher"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.haval.m6launcher"
        minSdk = 28
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // НАСТРОЙКА РАЗДЕЛЬНЫХ APK
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64") // Машина и BlueStacks
            isUniversalApk = true          // Соберет еще и один общий (на всякий случай)
        }
    }

    buildTypes {
        release {
            // ВКЛЮЧАЕМ R8 (СЖАТИЕ КОДА)
            isMinifyEnabled = true
            // УДАЛЯЕМ НЕИСПОЛЬЗУЕМЫЕ РЕСУРСЫ (иконки, строки)
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // ВАЖНО: Подписываем debug-ключом, чтобы можно было сразу установить и протестировать!
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.yandex.android:maps.mobile:4.45.0-full")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
}