import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.android.gms.oss-licenses-plugin")
    id("dagger.hilt.android.plugin")
    id("com.github.triplet.play")
    id("kotlin-android")
}

kapt {
    javacOptions {
        // Increase the max count of errors from annotation processors.
        // Default is 100.
        option("-Xmaxerrs", 500)
    }
}

play {
    serviceAccountCredentials.set(file("../service_account.json"))
    defaultToAppBundles.set(true)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
// Initialize a new Properties() object called keystoreProperties.
val keystoreProperties = Properties()
// Load your keystore.properties file into the keystoreProperties object.
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = file("../toggles_keystore.jks")
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        val composeVersion: String by rootProject.extra
        kotlinCompilerExtensionVersion = "1.0.2"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileSdk = 31

    defaultConfig {
        applicationId = "se.eelde.toggles"
        minSdk = 21
        targetSdk = 31
        versionCode = 6
        versionName = "1.01.02"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val wrenchAuthority = "com.izettle.wrench.configprovider"
        val wrenchPermission = "com.izettle.wrench.permission"
        val togglesAuthority = "se.eelde.toggles.configprovider"
        val togglesPermission = "se.eelde.toggles.provider_permission"

        manifestPlaceholders["wrenchAuthority"] = wrenchAuthority
        manifestPlaceholders["wrenchPermission"] = wrenchPermission
        manifestPlaceholders["togglesAuthority"] = togglesAuthority
        manifestPlaceholders["togglesPermission"] = togglesPermission

        buildConfigField("String", "CONFIG_AUTHORITY", "\"$togglesAuthority\"")
    }
    packagingOptions {
        exclude("META-INF/main.kotlin_module")
        exclude("META-INF/atomicfu.kotlin_module")
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOfNotNull(
            "-Xopt-in=kotlin.RequiresOptIn"
        )
        useIR = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            signingConfig = signingConfigs["release"]
        }
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            versionNameSuffix = " debug"
//            applicationIdSuffix = ".debug"
        }
    }
    lint {
        baselineFile = file("lint-baseline.xml")
        isCheckReleaseBuilds = true
        isAbortOnError = true
        isWarningsAsErrors = true
        lintConfig = File("../lint.xml")
    }
    sourceSets {
        // debug.assets.srcDirs => https://github.com/robolectric/robolectric/issues/3928
        // debug.assets.srcDirs += files("$projectDir/schemas".toString())
        getByName("debug") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

dependencies {
    val roomVersion ="2.4.0-alpha04"
    val composeVersion = "1.0.2"
    val lifecycleVersion = "2.3.1"
    val daggerVersion = "2.38.1"

    implementation("androidx.ui:ui-tooling:1.0.0-alpha07")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.foundation:foundation-layout:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.animation:animation:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07")
    implementation("androidx.navigation:navigation-compose:2.4.0-alpha08")

    testImplementation("junit:junit:4.13.2")

    testImplementation("androidx.test:core-ktx:1.4.0")
    testImplementation("androidx.test.ext:truth:1.4.0")
    testImplementation("androidx.test:rules:1.4.0")
    testImplementation("androidx.test:runner:1.4.0")
    testImplementation("androidx.test.ext:junit:1.1.3")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.robolectric:robolectric:4.6.1")
    testImplementation("androidx.test.espresso:espresso-core:3.4.0")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("androidx.work:work-testing:2.6.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation(platform("com.google.firebase:firebase-bom:28.4.0"))

    implementation("com.google.dagger:hilt-android:$daggerVersion")
    kapt("com.google.dagger:hilt-android-compiler:$daggerVersion")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-work:1.0.0")

    testImplementation("com.google.dagger:hilt-android-testing:$daggerVersion")
    kaptTest("com.google.dagger:hilt-android-compiler:$daggerVersion")

    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("com.google.dagger:dagger:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:2.38.1")

    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-core-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.paging:paging-runtime-ktx:3.0.1")

    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")

    implementation("com.izettle.wrench:wrench-core:0.3")
//    implementation(project(":toggles-core"))
//    implementation(project(":toggles-prefs"))
//    implementation(project(":toggles-flow"))
    implementation("se.eelde.toggles:toggles-core:0.0.2")
    implementation("se.eelde.toggles:toggles-flow:0.0.1")
    implementation("se.eelde.toggles:toggles-prefs:0.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.work:work-runtime-ktx:2.6.0")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")
}
