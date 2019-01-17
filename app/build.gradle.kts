plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.github.triplet.play") version Versions.com_github_triplet_play_gradle_plugin
    id("io.sentry.android.gradle")
}

android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "me.iberger.enq"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 2
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore.jks")
            storePassword = System.getenv("SIGNING_KEYSTORE_PW")
            keyAlias = "enq"
            keyPassword = System.getenv("SIGNING_KEY_PW")
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = " (debug)"
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    sourceSets {
        getByName("main").res.srcDirs("src/main/resources", " src/main/res")
    }
    lintOptions {
        isAbortOnError = false
    }
}

play {
    serviceAccountCredentials = file("play_credentials.json")
    track = "beta"
    defaultToAppBundles = true
    resolutionStrategy = "auto"
}

sentry {
    autoProguardConfig = true
    autoUpload = true
}

dependencies {
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_android)
    implementation(Libs.core_ktx)
    implementation(Libs.fragment_ktx)
    implementation(Libs.appcompat)
    implementation(Libs.constraintlayout)
    implementation(Libs.material)

//    implementation project(":jmusicbot")
    implementation(Libs.jmusicbotandroid)
    implementation(Libs.timbersentry)

    implementation(Libs.glide)
    implementation(Libs.moshi)

    implementation(Libs.fastadapter)
    implementation(Libs.fastadapter_commons)
    implementation(Libs.fastadapter_extensions)

    implementation(Libs.iconics_core)
    implementation(Libs.community_material_typeface)
    implementation(Libs.core_kt)
}
