plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.steve.zapkest"
    compileSdk = 35
    defaultConfig { applicationId = "com.steve.zapkest"; minSdk = 23; targetSdk = 35; versionCode = 10; versionName = "8.0.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
