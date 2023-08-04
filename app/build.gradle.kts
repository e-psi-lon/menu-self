plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android.buildFeatures.buildConfig = true
// On importe ByteOutputStream pour pouvoir récupérer le hash de la dernière version de l'application

android {
    namespace = "fr.e_psi_lon.menuself"
    compileSdk = 33

    defaultConfig {
        applicationId = "fr.e_psi_lon.menuself"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "0.5"
        if (project.hasProperty("args")) {
            versionName += " (build ${project.property("args")})"
        }
        buildConfigField("String", "GIT_COMMIT_HASH", "\"${if (project.hasProperty("args")) project.property("args") else "unknown"}\"")



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

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.jsoup:jsoup:1.14.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}