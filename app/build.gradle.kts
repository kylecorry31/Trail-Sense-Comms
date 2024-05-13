import com.android.build.gradle.internal.tasks.databinding.DataBindingGenBaseClassesTask
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.time.LocalDate

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.kylecorry.trail_sense_comms"
    compileSdk = 34

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.kylecorry.trail_sense_comms"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    androidResources {
        // Support for auto-generated locales for per-app language settings
        generateLocaleConfig = true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".release"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("nightly") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-nightly-${LocalDate.now()}"
        }
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources.merges += "META-INF/LICENSE.md"
        resources.merges += "META-INF/LICENSE-notice.md"
        jniLibs {
            useLegacyPackaging = true
        }
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.flexbox)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Sol
    implementation(libs.sol)

    // Andromeda
    implementation(libs.andromeda.core)
    implementation(libs.andromeda.connection)
    implementation(libs.andromeda.fragments)
    implementation(libs.andromeda.exceptions)
    implementation(libs.andromeda.preferences)
    implementation(libs.andromeda.permissions)
    implementation(libs.andromeda.notify)
    implementation(libs.andromeda.alerts)
    implementation(libs.andromeda.pickers)
    implementation(libs.andromeda.list)
    implementation(libs.andromeda.files)
    implementation(libs.andromeda.views)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Hilt for Jetpack components
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.junit.platform.runner)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.kotlin)
}

// This is a workaround to get viewbinding to work with ksp + hilt (https://github.com/google/dagger/issues/4097)
androidComponents {
    onVariants(selector().all()) { variant ->
        afterEvaluate {
            project.tasks.getByName("ksp" + variant.name.capitalized() + "Kotlin") {
                val dataBindingTask =
                    project.tasks.getByName("dataBindingGenBaseClasses" + variant.name.capitalized()) as DataBindingGenBaseClassesTask
                (this as AbstractKotlinCompileTool<*>).setSource(dataBindingTask.sourceOutFolder)
            }
        }
    }
}