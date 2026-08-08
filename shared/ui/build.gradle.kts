import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.template.shared.ui"
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

    buildFeatures {
        viewBinding = true
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

    api(project(":shared:designsystem"))

    api(libs.androidx.constraintlayout)

    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)

    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)

}