plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

buildscript {
    dependencies {
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    }
}

fun getCurrentGitBranch(): String {
    return try {
        val process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD")
        process.waitFor()
        val branch = process.inputStream.bufferedReader().readText().trim()
        if (branch.isNotEmpty()) branch else "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}

android {
    namespace = "com.code4you.geodumb"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.code4you.geodumb"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // ⬇️ Questo ora funzionerà
        buildConfigField("String", "GIT_BRANCH", "\"${getCurrentGitBranch()}\"")
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("String", "BUILD_TYPE", "\"${buildTypes}\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.ktx)
    implementation(libs.cronet.embedded)
    implementation("com.google.android.material:material:1.11.0")
    //implementation(libs.google.material)
    //implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //implementation("com.facebook.android:facebook-login:[18.1.3]")
    //implementation("com.facebook.android:facebook-android-sdk:[17.0.2]")
    implementation("com.squareup.picasso:picasso:2.8")
    //implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("com.google.android.material:material:1.8.0")  // o una versione più recente

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation("com.squareup.okhttp3:okhttp:4.9.0")

    implementation(libs.play.services.location)
    implementation(libs.material.v130)
    implementation(libs.androidx.constraintlayout.v204)
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation(libs.play.services.location.v2101)
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation("com.google.code.gson:gson:2.8.6")

    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // ViewBinding (opzionale ma utile)
    implementation("androidx.activity:activity-ktx:1.7.2")
    // Facebook SDK (se usi Facebook login)
    implementation("com.facebook.android:facebook-login:18.1.3")
    // Lifecycl
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // Retrofit
    //implementation("com.squareup.retrofit2:retrofit:2.9.0")
    //implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp con logging
    //implementation("com.squareup.okhttp3:okhttp:4.11.0")
    //implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    // Coroutines
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

}
