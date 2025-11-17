import java.util.Properties

plugins {
//    id("com.android.application")
    id("com.android.library")
    id("maven-publish")
    id("kotlin-kapt")
    alias(libs.plugins.jetbrains.kotlin.android)
}

val config =
    Properties().apply { load(project.rootProject.file("config.properties").inputStream()) }

android {
    namespace = "com.eka.conversation"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "IS_DEBUG", "true")
            buildConfigField("String", "MATRIX_URL", "\"${config["MATRIX_URL"]}\"")
        }
        create("staging") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "IS_DEBUG", "false")
            buildConfigField("String", "MATRIX_URL", "\"${config["MATRIX_URL_DEV"]}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "IS_DEBUG", "false")
            buildConfigField("String", "MATRIX_URL", "\"${config["MATRIX_URL"]}\"")
        }
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

//publishing {
//    publications {
//        create<MavenPublication>("release") {
//            groupId = "com.eka.conversation"
//            artifactId = "eka-conversation"
//            version = "1.0.6"
//
//            artifact("../app/build/outputs/aar/app-release.aar")
//        }
//    }
//}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.eka.conversation"
                artifactId = "eka-conversation"
                version = "1.0.5"
            }
        }
    }
    tasks.named("publishReleasePublicationToMavenLocal") {
        dependsOn(tasks.named("bundleReleaseAar"))
    }
}

dependencies {

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.gson)
    implementation(libs.squareup.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.markdown)
    implementation("com.github.mrmike:ok2curl:0.8.0")
    implementation(libs.eka.network.android)
}