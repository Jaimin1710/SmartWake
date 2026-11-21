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
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    compileOnly("com.google.android.gms:play-services-ads:23.6.0")
    compileOnly("com.tradplusad:tradplus:14.1.20.1")
    compileOnly("com.tradplusad:tradplus-facebook:1.14.1.20.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
