import com.android.build.api.variant.impl.VariantOutputImpl
import java.util.Properties

// AGP 9 applies Kotlin itself, so there is no org.jetbrains.kotlin.android plugin here.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appVersionName = "1.0.0"

/**
 * Release signing details, from keystore.properties locally or from the environment in CI.
 *
 * Both are absent on a plain clone, and that is deliberate: `assembleRelease` still has to
 * work for anyone building from source, it just produces an unsigned APK. F-Droid builds and
 * reproducibility checks depend on that, so a missing keystore must never fail the build.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val keystorePath = signingValue("storeFile", "KEYSTORE_FILE")
val hasSigningConfig = keystorePath != null && rootProject.file(keystorePath).exists()

android {
    namespace = "io.github.splashieee7.ios2droidkeyboard"
    // API 37 (Android 17) is the current stable SDK, and the 2026 AndroidX releases
    // refuse to compile against anything older.
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.splashieee7.ios2droidkeyboard"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = appVersionName
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystorePath!!)
                storePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
                // v2 covers everything from minSdk 26 up. v3 is what makes key rotation
                // possible later, so it is worth having from the first release rather than
                // discovering it is missing when the key needs replacing.
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Release APKs ship as iOS2DroidKeyBoard-<version>.apk, per CLAUDE.md.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            (output as? VariantOutputImpl)?.outputFileName?.set("iOS2DroidKeyBoard-$appVersionName.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

/**
 * Hard rule from CLAUDE.md: this app must never request INTERNET, and no dependency may
 * pull it in transitively. Scans every merged manifest AGP produces, so it catches a
 * permission added by a library as well as one added by hand.
 */
tasks.register("checkNoInternetPermission") {
    group = "verification"
    description = "Fails if any merged manifest requests the INTERNET permission."

    val manifests = layout.buildDirectory.dir("intermediates/merged_manifests")
    outputs.upToDateWhen { false }

    doLast {
        val dir = manifests.get().asFile
        if (!dir.exists()) {
            throw GradleException(
                "No merged manifests found at $dir - run a build (e.g. assembleDebug) first.",
            )
        }

        val offenders = dir.walkTopDown()
            .filter { it.name == "AndroidManifest.xml" }
            .filter { it.readText().contains("android.permission.INTERNET") }
            .toList()

        if (offenders.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("INTERNET permission found in the merged manifest:")
                    offenders.forEach { appendLine("  $it") }
                    appendLine("This app must never request INTERNET. Find the dependency that added it.")
                },
            )
        }

        logger.lifecycle("No INTERNET permission in any merged manifest.")
    }
}
