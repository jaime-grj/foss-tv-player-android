import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.gaarx.tvplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gaarx.iptvplayer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ExoPlayer & FFmpeg
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.rtmp)
    implementation(libs.media3.datasource.cronet)
    implementation(files("../libs/lib-decoder-ffmpeg-release.aar"))
    implementation(files("../libs/lib-decoder-av1-release.aar"))

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.retrofit.scalars)

    // Jsoup
    implementation(libs.jsoup)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // JsonPath
    implementation(libs.jsonpathkt)

    // Json
    implementation(libs.json)

    //viewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    //LiveData
    implementation(libs.androidx.lifecycle.livedata)

    //Hilt
    implementation(libs.hilt.android)
    ksp (libs.hilt.compiler)

    //Cronet
    implementation(libs.cronet.embedded)

    //Room
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //DataStore
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Glide
    implementation(libs.glide)
    ksp (libs.glide.compiler)

    implementation(libs.joda.time)
    api(libs.threetenabp)

    // RxJava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)

    implementation(project(":library"))
}

kapt {
    correctErrorTypes = true
}