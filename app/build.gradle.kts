plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.indirx.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.indirx.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.2.5"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    val localTestKey = rootProject.file("test-signing/indirx-debug.keystore")
    if (localTestKey.exists()) {
        signingConfigs { getByName("debug") { storeFile = localTestKey } }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
    packaging { jniLibs.useLegacyPackaging = true; resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*") }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
}
