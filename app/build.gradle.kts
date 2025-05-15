import java.util.Properties
import java.io.FileInputStream

val localProps = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProps.load(it) }
}

val googleApiKey = localProps.getProperty("google.api.key")
    ?: throw GradleException("google.api.key is missing from local.properties")


plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.2"
}
android {
    namespace = "com.example.planitout"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.planitout"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_API_KEY", "\"$googleApiKey\"")
        manifestPlaceholders["GOOGLE_API_KEY"] = googleApiKey

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

    buildFeatures {
        buildConfig = true
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}


dependencies {
    // AndroidX and Material Design
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation (libs.android.gif.drawable)
    implementation (libs.core.splashscreen)
    implementation(libs.places)
    implementation(libs.firebase.storage)
    implementation (libs.glide)
    annotationProcessor (libs.compiler)
    implementation(libs.flexbox)


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Lifecycle (ViewModel/LiveData and Swipe Refresh)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.swiperefreshlayout)

    // Retrofit for API calls
    implementation(libs.retrofit2.retrofit)

    // Google Play Services (Maps and Location)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.analytics)


    // Calendar
    implementation("com.kizitonwose.calendar:view:2.6.2")
    implementation("com.kizitonwose.calendar:compose:2.6.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")


}