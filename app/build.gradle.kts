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
// https://mvnrepository.com/artifact/androidx.lifecycle/lifecycle-service
    runtimeOnly("androidx.lifecycle:lifecycle-service:2.8.3")
    // Optional Room dependencies
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("androidx.room:room-guava:$room_version")
    implementation("androidx.room:room-paging:$room_version")
    implementation("androidx.lifecycle:lifecycle-service:2.6.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

}
