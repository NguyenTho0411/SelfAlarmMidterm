plugins {
    alias(libs.plugins.android.application) // Android application plugin
}

android {
    namespace = "hcmute.edu.vn.selfalarm"
    compileSdk = 35

    defaultConfig {
        applicationId = "hcmute.edu.vn.selfalarm"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    viewBinding {
        enable = true
    }

    buildFeatures {
        viewBinding= true
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

val room_version = "2.6.1"

dependencies {
    // AndroidX and other libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // AndroidX Room Database
    implementation("androidx.room:room-runtime:$room_version")
    androidTestImplementation("androidx.room:room-testing:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Unit Test Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Kotlin Symbol Processing for Room
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // Optional Room dependencies
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("androidx.room:room-guava:$room_version")
    implementation("androidx.room:room-paging:$room_version")

    // Lifecycle dependencies
    implementation("androidx.lifecycle:lifecycle-service:2.6.0")

    // Navigation dependencies
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    // Logging dependencies
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.2.6")

    // ExoPlayer dependencies
    implementation("com.google.android.exoplayer:exoplayer-hls:2.X.X")
    implementation("com.google.android.exoplayer:exoplayer:2.18.0")
    implementation("com.google.android.exoplayer:extension-okhttp:2.18.0")

    // OkHttp dependencies
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    // Image loading
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.11.0")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.4.0")

    // CircleImageView
    implementation("de.hdodenhof:circleimageview:2.2.0")
}
