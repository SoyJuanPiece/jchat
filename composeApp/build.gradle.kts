import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun Project.readConfig(name: String, defaultValue: String? = null): String {
    val value = providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: defaultValue

    return value ?: error("Missing required config: $name")
}

fun Project.readOptionalConfig(name: String): String? =
    providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
        }

        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.navigation)
            implementation(libs.compose.material.icons.extended)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.uuid)
            implementation(libs.coil.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.supabase.coil)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Datetime
            implementation(libs.kotlinx.datetime)

            // Koin
            implementation(libs.koin.core)

            // Supabase
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.storage)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // SQLDelight
            implementation(libs.sqldelight.coroutines.extensions)
        }

        nativeMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "com.jchat"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    val appVersionCode = readConfig("APP_VERSION_CODE", "10501").toInt()
    val appVersionName = readConfig("APP_VERSION_NAME", "1.5.1")
    val supabaseUrl = readConfig("SUPABASE_URL", "https://ppincerggnnauznalbjd.supabase.co")
    val supabaseAnonKey = readConfig("SUPABASE_ANON_KEY", "")
    val releaseKeystorePath = readOptionalConfig("ANDROID_KEYSTORE_PATH") ?: "$projectDir/key.jks"
    val releaseStorePassword = readOptionalConfig("STORE_PASSWORD")
    val releaseKeyAlias = readOptionalConfig("KEY_ALIAS")
    val releaseKeyPassword = readOptionalConfig("KEY_PASSWORD")
    val hasReleaseSigning = !releaseStorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.jchat"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "APP_ENV", "\"production\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "APP_ENV", "\"debug\"")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.resources {
    publicResClass = true
}

sqldelight {
    databases {
        create("JChatDatabase") {
            packageName.set("com.jchat.db")
        }
    }
}
