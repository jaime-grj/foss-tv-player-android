plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.gaarx.iptvplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gaarx.iptvplayer"
        minSdk = 24
        targetSdk = 35
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
    implementation("com.amazon.android:media3-exoplayer:1.3.1")
    implementation("com.amazon.android:media3-exoplayer-dash:1.3.1")
    implementation("com.amazon.android:media3-exoplayer-hls:1.3.1")
    implementation("com.amazon.android:media3-ui:1.3.1")
    implementation("com.amazon.android:media3-datasource-rtmp:1.3.1")
    implementation("com.amazon.android:media3-datasource-cronet:1.3.1")
    implementation(files("../libs/lib-decoder-ffmpeg-release.aar"))
    implementation(files("../libs/lib-decoder-av1-release.aar"))

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
    implementation("org.chromium.net:cronet-embedded:108.5359.79")
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