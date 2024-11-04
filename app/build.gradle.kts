plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.gaarj.iptvplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gaarj.iptvplayer"
        minSdk = 24
        targetSdk = 34
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
    kotlinOptions {
        jvmTarget = "11"
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
    implementation("com.amazon.android:exoplayer:2.18.7")
    implementation(files("../libs/extension-ffmpeg-release.aar"))

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

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
    kapt(libs.hilt.compiler)

    //Room
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //DataStore
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    //implementation(libs.extension.okhttp) // OkHttp extension for ExoPlayer
    //implementation(libs.okhttp) // OkHttp library
    implementation("com.github.bumptech.glide:glide:4.11.0")
    kapt ("com.github.bumptech.glide:compiler:4.11.0")
    implementation("joda-time:joda-time:2.10.14")
    implementation("com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.5.6")
    api("com.jakewharton.threetenabp:threetenabp:1.4.7")

    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")
    implementation("io.reactivex.rxjava3:rxjava:3.0.6")

    implementation(project(":library"))
}

kapt {
    correctErrorTypes = true
}