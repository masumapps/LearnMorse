plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.learn.morse"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.learn.morse"
        minSdk = 25
        targetSdk = 37
        versionCode = 6
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Real AdMob ids; debug overrides them with Google's sample ids, so we never load real ads ourselves.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-1066129502643553~2685007033"
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-1066129502643553/7725580724\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-1066129502643553/2493435342\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"ca-app-pub-1066129502643553/3383011031\"")
        buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-1066129502643553/7298205313\"")
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    bundle {
        language {
            // Ship every language to every device: the app can switch language itself (onboarding,
            // Settings), and Play would otherwise install only the device language's strings.
            enableSplit = false
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.services.ads)
    implementation(libs.billing.ktx)
    implementation(libs.ump)
    implementation(libs.review.ktx)
    // play-services-ads pulls WorkManager 2.7.0 (Room 2.2.5), whose keep rules R8 full mode breaks.
    implementation(libs.androidx.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}
