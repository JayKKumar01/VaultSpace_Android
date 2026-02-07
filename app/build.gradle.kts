plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.github.jaykkumar01.vaultspace"

    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.github.jaykkumar01.vaultspace"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ✅ FIX FOR META-INF/DEPENDENCIES CONFLICT
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.browser:browser:1.9.0")
    // Google Play Services (account + auth)
    implementation("com.google.android.gms:play-services-auth:21.5.0")
// Google API client (Android)
    implementation("com.google.api-client:google-api-client-android:2.8.1")
// Google Drive API v3
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
//    implementation(libs.swiperefreshlayout)
    // Room (Java)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)


    implementation("androidx.media3:media3-exoplayer:1.9.1")
    implementation("androidx.media3:media3-ui:1.9.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
