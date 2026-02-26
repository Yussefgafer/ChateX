plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.kai.ghostmesh"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kai.ghostmesh"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.8.7")
    
    // 🚀 Fix Lint Error: InvalidFragmentVersionForActivityResult
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    
    // 🔥 Material 3 Expressive
    implementation("androidx.compose.material3:material3:1.5.0-alpha14")
    implementation("androidx.compose.material3.adaptive:adaptive:1.3.0-alpha09")
    implementation("androidx.compose.material3.adaptive:adaptive-layout:1.3.0-alpha09")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.3.0-alpha09")
    
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Google Nearby Connections
    implementation("com.google.android.gms:play-services-nearby:19.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 🚀 Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // 🚀 Room 2.8.4 - The 2026 stable for Kotlin 2.3
    val roomVersion = "2.8.4" 
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// ⚡ Force KSP2 for the K2 compiler
ksp {
    arg("useKsp2", "true")
}
