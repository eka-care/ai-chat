plugins {
    id("com.android.library")
    id("maven-publish")
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization")
}

/**
 * Matrix (MedAssist) client: token exchange, sessions, WebSocket / HTTP-stream chat,
 * tool calls (elicitations), file upload, voice notes, history, feedback and recovery.
 * Native Kotlin port of the synapse-sdk core. No UI.
 */
android {
    namespace = "com.eka.conversation.matrix"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release { isMinifyEnabled = false }
        create("staging") { isMinifyEnabled = false }
    }
    kotlin {
        jvmToolchain(17)
    }
    publishing {
        singleVariant("release") { withSourcesJar() }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                // Same coordinates JitPack gives this module: com.github.eka-care.ai-chat:matrix:<tag>
                groupId = "com.github.eka-care.ai-chat"
                artifactId = "matrix"
                version = "2.1.0"
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // api: callers receive JsonObject payloads (tool inputs / tool_result) from the client.
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
