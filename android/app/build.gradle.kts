plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val safenetApiCertPin = providers.gradleProperty("SAFENET_API_CERT_PIN_SHA256")
    .orElse(providers.environmentVariable("SAFENET_API_CERT_PIN_SHA256"))
    .orElse("")
    .get()

fun quotedBuildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.safenet.vpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zinsafenet.app"
        minSdk = 26  // Android 8.0+
        targetSdk = 35
        versionCode = 17
        versionName = "1.0.15"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Build config fields for runtime configuration
        buildConfigField("String", "API_BASE_URL", "\"https://safenetapp.truehand.top/api/v1\"")
        buildConfigField("String", "WS_BASE_URL", "\"wss://safenetapp.truehand.top\"")
        buildConfigField("String", "VERCEL_API_URL", "\"https://hy2onvercelvpn.vercel.app/api/\"")
        buildConfigField("String", "API_CERT_PIN_SHA256", quotedBuildConfigString(safenetApiCertPin))
        buildConfigField("String", "APP_NAME", "\"SafeNet VPN\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "safenet-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "API_BASE_URL", "\"https://safenetapp.truehand.top/api/v1\"")
            buildConfigField("String", "WS_BASE_URL", "\"wss://safenetapp.truehand.top\"")
            buildConfigField("String", "VERCEL_API_URL", "\"https://hy2onvercelvpn.vercel.app/api/\"")
            buildConfigField("String", "API_CERT_PIN_SHA256", "\"\"")
            buildConfigField("Boolean", "DEBUG_MODE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            ndk {
                debugSymbolLevel = "FULL"
            }
            // Optional hardening: pass SAFENET_API_CERT_PIN_SHA256=sha256/... for TLS pinning.
            // Android Studio release builds stay usable when the pin is not configured.
            buildConfigField("String", "API_CERT_PIN_SHA256", quotedBuildConfigString(safenetApiCertPin))
            buildConfigField("String", "VERCEL_API_URL", "\"https://hy2onvercelvpn.vercel.app/api/\"")
            buildConfigField("Boolean", "DEBUG_MODE", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-Xskip-metadata-version-check")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

configurations.all {
    resolutionStrategy.force(
        "androidx.core:core:1.13.1",
        "androidx.core:core-ktx:1.13.1",
        "org.jetbrains.kotlin:kotlin-stdlib:2.0.0",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.0",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0",
    )
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Sing-box Native Engine
    implementation(files("libs/libbox.aar"))

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Security (Encrypted SharedPreferences)
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // Image
    implementation(libs.coil.compose)

    // QR Code
    implementation(libs.zxing.core)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation(libs.androidx.ui.tooling.preview)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xskip-metadata-version-check")
    }
}
