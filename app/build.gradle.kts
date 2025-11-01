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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
            buildConfigField("String", "PIN_MODE", "\"bad\"")
        }
        create("vuln") {
            dimension = "securityProfile"
            applicationIdSuffix = ".vuln"
            versionNameSuffix = "-vuln"
        }
        create("pinning") {
            dimension = "topic"
            applicationIdSuffix = ".pinning"
            versionNameSuffix = "-pinning"
        }
        create("e2e") {
            dimension = "topic"
            applicationIdSuffix = ".e2e"
            versionNameSuffix = "-e2e"
        }
        create("re") {
            dimension = "topic"
            applicationIdSuffix = ".re"
            versionNameSuffix = "-re"
        }
        create("perm") {
            dimension = "topic"
            applicationIdSuffix = ".perm"
            versionNameSuffix = "-perm"
        }
        create("links") {
            dimension = "topic"
            applicationIdSuffix = ".links"
            versionNameSuffix = "-links"
        }
        create("storage") {
            dimension = "topic"
            applicationIdSuffix = ".storage"
            versionNameSuffix = "-storage"
        }
        create("root") {
            dimension = "topic"
            applicationIdSuffix = ".root"
            versionNameSuffix = "-root"
        }
        create("web") {
            dimension = "topic"
            applicationIdSuffix = ".web"
            versionNameSuffix = "-web"
        }
        create("users") {
            dimension = "topic"
            applicationIdSuffix = ".users"
            versionNameSuffix = "-users"
        }
        create("risks") {
            dimension = "topic"
            applicationIdSuffix = ".risks"
            versionNameSuffix = "-risks"
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}