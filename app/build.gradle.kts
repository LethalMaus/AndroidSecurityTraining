plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.jamescullimore.android_security_training"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.jamescullimore.android_security_training"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("./../seminar.jks")
            storePassword = "changeit"
            keyAlias = "key0"
            keyPassword = "changeit"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Keep debug defaults
        }
    }

    flavorDimensions += listOf("securityProfile", "topic")

    productFlavors {
        create("secure") {
            dimension = "securityProfile"
            applicationIdSuffix = ".secure"
            versionNameSuffix = "-secure"
            buildConfigField("boolean", "MANUAL_PIN", "false")
            // Can be good, bad, ct or mitm
            buildConfigField("String", "PIN_MODE", "\"bad\"")
        }
        create("vuln") {
            dimension = "securityProfile"
            applicationIdSuffix = ".vuln"
            versionNameSuffix = "-vuln"
        }
        create("pinning") {
            dimension = "topic"
        }
        create("e2e") {
            dimension = "topic"
        }
        create("re") {
            dimension = "topic"
        }
        create("perm") {
            dimension = "topic"
        }
        create("links") {
            dimension = "topic"
        }
        create("storage") {
            dimension = "topic"
        }
        create("root") {
            dimension = "topic"
        }
        create("web") {
            dimension = "topic"
        }
        create("users") {
            dimension = "topic"
        }
        create("risks") {
            dimension = "topic"
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
        compose = true
        buildConfig = true
    }
}

// Disable release build variants for all 'vuln' flavors; keep secure variants unchanged
androidComponents {
    beforeVariants(selector().withBuildType("release").withFlavor("securityProfile" to "vuln")) { variantBuilder ->
        variantBuilder.enable = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.rootbeer.lib)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}