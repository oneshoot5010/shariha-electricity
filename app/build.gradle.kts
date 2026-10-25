plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.oneshoot.shariha"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.oneshoot.shariha"
        minSdk = 24
        targetSdk = 34
        versionCode = (project.findProperty("verCode") as String?)?.toInt() ?: 1
        versionName = "1.0.$versionCode"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false; checkReleaseBuilds = false }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.mlkit:text-recognition:16.0.0")
}
