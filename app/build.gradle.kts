import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}

android {
    namespace = "com.georgearn.showtracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.georgearn.showtracker"
        minSdk = 31 // Android 12 (required floor for Monet/dynamic color anyway)
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OMDB_API_KEY", "\"${localProps.getProperty("OMDB_API_KEY", "")}\"")
        buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"${localProps.getProperty("TMDB_READ_ACCESS_TOKEN", "")}\"")
    }

    signingConfigs {
        create("release") {
            val path = localProps.getProperty("RELEASE_KEYSTORE_PATH")
                ?: System.getenv("RELEASE_KEYSTORE_PATH")
                ?: "C:/Users/GeorgeArn/.android-keystores/android-release.jks"
            val ksFile = File(path)
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = localProps.getProperty("RELEASE_KEYSTORE_PASSWORD")
                    ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: System.getenv("FLET_ANDROID_SIGNING_KEY_STORE_PASSWORD")
                    ?: ""
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                    ?: System.getenv("FLET_ANDROID_SIGNING_KEY_ALIAS")
                    ?: "androidrelease"
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("FLET_ANDROID_SIGNING_KEY_PASSWORD")
                    ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val relConfig = signingConfigs.getByName("release")
            if (relConfig.storeFile?.exists() == true) {
                signingConfig = relConfig
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.8")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Room (local DB - watchlist)
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // Retrofit / Moshi (TMDB + OMDb)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil (posters/backdrops)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // WorkManager (background periodic release-status checks -> notifications)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // DataStore (theme + prefs)
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
