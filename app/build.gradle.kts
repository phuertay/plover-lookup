plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.phuertay.ploverlookup"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    val workflowRun = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()

    defaultConfig {
        applicationId = "com.phuertay.ploverlookup"
        minSdk = 26
        targetSdk = 35
        versionCode = workflowRun ?: 1
        versionName = if (workflowRun == null) "1.0" else "1.$workflowRun"
    }

    signingConfigs {
        create("sideload") {
            storeFile = file("sideload.keystore")
            storePassword = "ploverlookup"
            keyAlias = "ploverlookup"
            keyPassword = "ploverlookup"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.all {
            it.testLogging {
                events("passed", "failed", "skipped")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val compose_bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(compose_bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    testImplementation("junit:junit:4.13.2")
}
