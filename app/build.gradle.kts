plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sahith.shabit"

    // AGP 9 takes SDK levels as blocks rather than scalars.
    //
    // compileSdk is 37 rather than the 36 named in #2: Compose 1.12 (from the
    // BOM below) refuses to be consumed by anything compiling against 36.
    // compileSdk only decides which APIs are visible at compile time —
    // targetSdk, which opts into new runtime behaviour, stays at 36.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sahith.shabit"
        minSdk {
            version = release(26)
        }
        targetSdk {
            version = release(36)
        }
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Minification stays off until #8, which owns release signing and
            // is responsible for verifying Room and Glance keep rules. Enabling
            // R8 here would ship config that no CI job actually exercises.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric reads the merged manifest and resources through the config
            // file AGP only writes when this is on. Without it the data-layer tests
            // cannot open a Room database at all.
            isIncludeAndroidResources = true
        }
    }

    // Java/Kotlin target levels are deliberately left at AGP's defaults. With
    // built-in Kotlin, AGP keeps the Java and Kotlin targets in step; setting
    // only one of them is how you get "Inconsistent JVM-target compatibility".
    // minSdk 26 means java.time is available natively, so no desugaring.
}

// Room's schema JSON is the only record of what version 1 looked like, and the first
// migration test will need it. Exporting it into a tracked directory keeps that record
// under review rather than inside build/.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Glance renders the widget through RemoteViews. It is a separate Compose-like
    // runtime, not the Compose in the app above — the two share no composables, which is
    // why the widget redraws the same grid rather than reusing HabitGrid.
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // The data layer is tested against a real in-memory SQLite database, which needs a
    // Context. Robolectric supplies one on the JVM so `testDebugUnitTest` — and therefore
    // CI — covers the toggle transaction and the delete cascade without an emulator.
    testImplementation(libs.robolectric)
    testImplementation(composeBom)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
