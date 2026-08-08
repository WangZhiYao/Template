import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.template.shared.designsystem"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.fragment.ktx)

    api(libs.material)
}