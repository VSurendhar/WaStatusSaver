plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dagger)
    kotlin("kapt")
    id("kotlin-parcelize")
    id("com.google.protobuf") version "0.9.5"
}

/*val secrets = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secrets.load(FileInputStream(secretsFile))
}*/

android {
    lint {
        baseline = file("lint-baseline.xml")
    }
    namespace = "com.voidDeveloper.wastatussaver"
    compileSdk = 36
    android.buildFeatures.buildConfig = true
    defaultConfig {
        applicationId = "com.voidDeveloper.wastatussaver"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "2.0"
//        buildConfigField("String", "BOT_TOKEN", "\"${secrets.getProperty("botToken")}\"")
//        buildConfigField("String", "CHAT_ID", "\"${secrets.getProperty("chatId")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.7"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins.create("java") {
                option("lite")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Tab Layout
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    // Dagger
    implementation(libs.dagger.hilt)
    implementation(libs.hilt.compose.navigation)
    implementation(libs.androidx.hilt.work)
    kapt(libs.dagger.kapt)
    kapt(libs.androidx.hilt.compiler)
    //  Data Store
    implementation(libs.androidx.datastore.preferences)

    // Gson
    implementation(libs.gson)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Ok Http
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Media3
    implementation(libs.androidx.media3.exoplayer.v100beta02)
    implementation(libs.androidx.media3.ui.v100beta02)

    // Material Icons
    implementation(libs.androidx.compose.material.icons.extended)

    // ProtoBuf Data Store
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.animation:animation:1.6.8")

}