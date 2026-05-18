plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    buildFeatures {
        viewBinding = true
    }

    namespace = "com.example.greenhousesystem"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.greenhousesystem"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
        // ── Firebase ──────────────────────────────────────
        implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-database")

        // ── UI Components ─────────────────────────────────
        // Material Design 3 — button, textfield, card, snackbar...
        implementation("com.google.android.material:material:1.12.0")

        // ConstraintLayout — dàn layout linh hoạt
        implementation("androidx.constraintlayout:constraintlayout:2.2.1")

        // ── Navigation ────────────────────────────────────
        // Điều hướng giữa các màn hình (Login → Register → Dashboard)
        implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
        implementation("androidx.navigation:navigation-ui-ktx:2.8.9")

        // RecyclerView
        implementation("androidx.recyclerview:recyclerview:1.3.2")

        // Circle ImageView — avatar drawer
        implementation("de.hdodenhof:circleimageview:3.1.0")
        // CardView
        implementation("androidx.cardview:cardview:1.0.0")
        // ── Image / Icon ──────────────────────────────────
        // Load ảnh avatar, icon
        implementation("com.github.bumptech.glide:glide:4.16.0")

        // Lottie — animation loading, success tick đẹp
        implementation("com.airbnb.android:lottie:6.6.6")

        // ── Lifecycle / ViewModel ─────────────────────────
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
        implementation("androidx.activity:activity-ktx:1.10.1")
        implementation("androidx.fragment:fragment-ktx:1.8.6")

        // ── Coroutines — xử lý bất đồng bộ Firebase ──────
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

        // ── SharedPreferences dễ dùng hơn ─────────────────
        implementation("androidx.datastore:datastore-preferences:1.1.7")

        // ── Phone number format ───────────────────────────
        // Validate và format số điện thoại Việt Nam
        implementation("com.googlecode.libphonenumber:libphonenumber:8.13.52")
        // MPAndroidChart
        implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

        // DrawerLayout
        implementation("androidx.drawerlayout:drawerlayout:1.2.0")

        // SwipeRefreshLayout
        implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")

        // FCM
        implementation("com.google.firebase:firebase-messaging")

        // WorkManager — chạy background task ổn định
        implementation("androidx.work:work-runtime-ktx:2.9.0")

        // Shimmer of Facebook
        implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}