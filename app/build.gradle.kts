plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
}

android {
    namespace = "gf.gab.upm.spv"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "gf.gab.upm.spv"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../config/local/keystore.jks")
            keyAlias = "key"
            // Java keystores MUST have a password, even if the use case does not require it.
            // Since the keystore lives in the repository's secret section, it is only accessible
            // through a pipeline, so the password does not matter.
            storePassword = "keypassword"
            keyPassword = "keypassword"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }

    defaultConfig {
        val caCertPemFile = project.rootProject.file("config/local/cacert.pem")
        if(!caCertPemFile.exists()) {
            throw GradleException(
                """
                CA Root certificate file not found in `config/local/cacert.pem`.
                If compiling locally, make sure to copy your CA certificate.
                If you are seeing this error from a GitHub Actions pipeline, make sure
                that your script calls `config/caimportgh.sh`.
            """.trimIndent()
            )
        }
        val caCertPem = caCertPemFile
            .readText()
            .replace("\n", "\\n")
            .replace("\r", "\\r")


        val otaUrl = providers.environmentVariable("OTA_ENDPOINT_URL")
            .orElse("https://api.github.com/repos/UPM-SPV/firmware/")
            .get()

        buildConfigField("String", "CA_CRT_PEM", "\"$caCertPem\"")
        buildConfigField("String", "OTA_ENDPOINT_URL", "\"$otaUrl\"")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.4"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    implementation(libs.bcprov.jdk18on)
    implementation(libs.bcpkix.jdk18on)

    implementation(libs.androidx.accompanist.permissions)
    implementation(libs.nordicsemi.android.ble)
    implementation(libs.nordicsemi.android.ble.ktx)
    implementation(libs.nordicsemi.android.ble.scanner)
    implementation(libs.nordicsemi.android.ble.mcumgr)
    implementation(libs.gson)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)

    ksp(libs.androidx.room.compiler)

    implementation(libs.vico.core)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.views)

    implementation(libs.retrofit2)

    implementation(libs.wheelpickercompose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
