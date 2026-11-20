plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.sololine.smartwake"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.Jaimin1710"
                artifactId = "smartwake"
            }
            create<MavenPublication>("releaseRepo") {
                from(components["release"])
                groupId = "com.github.Jaimin1710.SmartWake"
                artifactId = "smartwake"
            }
            create<MavenPublication>("releaseCapital") {
                from(components["release"])
                groupId = "com.github.Jaimin1710"
                artifactId = "SmartWake"
            }
            create<MavenPublication>("releaseRepoCapital") {
                from(components["release"])
                groupId = "com.github.Jaimin1710.SmartWake"
                artifactId = "SmartWake"
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
