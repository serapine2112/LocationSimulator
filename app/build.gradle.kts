
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Membaca API key dari local.properties
val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")

    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { input ->
            load(input)
        }
    }
}

val maptilerKey = localProperties.getProperty(
    "MAPTILER_API_KEY",
    ""
)

android {
    namespace = "com.example.locationsimulator"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.locationsimulator"

        minSdk = 26
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // Membuat API key tersedia melalui BuildConfig
        buildConfigField(
            "String",
            "MAPTILER_API_KEY",
            "\"$maptilerKey\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
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
}

dependencies {

    // OpenStreetMap / osmdroid
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Unit testing
    testImplementation(libs.junit)

    // Android testing
    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
