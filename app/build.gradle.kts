plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * The release keystore, or null on any machine that has not been given one.
 *
 * It arrives as a path in the environment rather than a file in the tree or a
 * `keystore.properties`, because the repo is public and the only place the real
 * keystore exists is a GitHub secret that `release.yml` decodes to a temp file.
 * Absent — which is every local build — release falls back to debug signing, so
 * `assembleRelease` still works for a smoke test; it just produces an APK that
 * can never be installed over a real one.
 */
val releaseKeystore = System.getenv("SHABIT_KEYSTORE")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) }
    ?.takeIf { it.exists() }

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
        // Bumped by hand, in a commit, before the tag that ships it: `release.yml`
        // refuses to build a tag whose name does not match versionName. Two releases
        // must never share a versionCode — Android takes it as the same build.
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SHABIT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SHABIT_KEY_ALIAS")
                keyPassword = System.getenv("SHABIT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Falling back to debug signing keeps a local `assembleRelease` working.
            // CI must never take that branch — an APK signed with the debug key cannot
            // be installed over a real one, and the only way out of that on a phone is
            // uninstall, which takes every habit with it. `release.yml` checks the
            // signer of the APK it is about to publish for exactly this reason.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")

            isMinifyEnabled = true
            isShrinkResources = true
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
