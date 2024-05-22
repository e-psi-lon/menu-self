plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android.buildFeatures.buildConfig = true

android {
    namespace = "fr.e_psi_lon.menuself"
    compileSdk = 34

    defaultConfig {
        applicationId = "fr.e_psi_lon.menuself"
        minSdk = 24
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
        versionName = "1.2.2"
        versionName += if (System.getenv("GITHUB_SHA") != null) {
            when (project.properties["channel"]?.toString()) {
                "stable" -> ""
                "alpha" -> " (alpha build ${System.getenv("GITHUB_SHA")?.substring(0, 7)})"
                "beta" -> " (beta build ${System.getenv("GITHUB_SHA")?.substring(0, 7)})"
                else -> " (build ${System.getenv("GITHUB_SHA")?.substring(0, 7)})"
            }
        } else {
            " (unknown build)"
        }
        buildConfigField(
            "String",
            "GIT_COMMIT_HASH",
            "\"${if (System.getenv("GITHUB_SHA") != null) System.getenv("GITHUB_SHA") else "unknown"}\""
        )



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            keyAlias = "menu-self"
            keyPassword = System.getenv("GPLAY_KEYSTORE_PASSWORD")
            storeFile = file("../menu-self.jks")
            storePassword = System.getenv("GPLAY_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.circleimageview)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
}