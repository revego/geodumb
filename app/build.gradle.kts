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

android {
    namespace = "com.code4you.geodumb"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.code4you.geodumb"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
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
    implementation(libs.google.material)
    //implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.facebook.android:facebook-login:[17.0.2]")
    implementation("com.facebook.android:facebook-android-sdk:[17.0.2]")
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

}