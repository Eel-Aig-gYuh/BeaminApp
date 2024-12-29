plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.ubercus"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.ubercus"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    viewBinding {
        enable = true
    }
    buildFeatures {
        viewBinding = true
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
    lint {
        lint.checkReleaseBuilds = false
        lint.abortOnError = false
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Declare the dependencies for the desired Firebase products without specifying versions
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.firebaseui:firebase-ui-database:8.0.2")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.firebaseui:firebase-ui-storage:8.0.2")
    implementation("com.google.firebase:firebase-storage:21.0.1")

    // RxJava
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.10")

    // Material
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    // Circle Image
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")

    // Google Maps API
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:android-maps-utils-v3:2.0.3")

    // Google Places API
    implementation("com.google.android.libraries.places:places:4.1.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // Dexter
    implementation("com.karumi:dexter:6.1.2")

    // GeoFire
    implementation("com.firebase:geofire-android:3.2.0")
    implementation("com.firebase:geofire-android-common:3.2.0")

    // Sliding Up Panel
    implementation(libs.library)

    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.3") // Check for the latest version
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.3")

    // Route
    implementation ("com.github.jd-alexander:library:1.1.0")

    // paypal
    implementation("com.paypal.sdk:paypal-android-sdk:2.16.0")
}