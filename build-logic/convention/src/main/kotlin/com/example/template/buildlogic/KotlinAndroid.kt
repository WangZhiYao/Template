package com.example.template.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Applies the common Kotlin + Android options for an Android library module.
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
internal fun Project.configureKotlinAndroid() {
    extensions.configure<LibraryExtension> {
        compileSdk {
            version = release(AppConfig.COMPILE_SDK)
        }

        defaultConfig {
            minSdk = AppConfig.MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = AppConfig.JAVA_VERSION
            targetCompatibility = AppConfig.JAVA_VERSION
        }
    }

    configureTestDependencies()
}

/**
 * Applies the common Kotlin + Android options for the Android application module.
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
internal fun Project.configureKotlinAndroidApplication() {
    extensions.configure<ApplicationExtension> {
        namespace = AppConfig.APPLICATION_ID

        compileSdk {
            version = release(AppConfig.COMPILE_SDK)
        }

        defaultConfig {
            applicationId = AppConfig.APPLICATION_ID
            versionCode = AppConfig.VERSION_CODE
            versionName = AppConfig.VERSION_NAME
            minSdk = AppConfig.MIN_SDK
            targetSdk = AppConfig.TARGET_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                optimization {
                    enable = false
                }
            }
        }

        compileOptions {
            sourceCompatibility = AppConfig.JAVA_VERSION
            targetCompatibility = AppConfig.JAVA_VERSION
        }
    }

    configureTestDependencies()
}

/**
 * Adds the shared test dependencies so modules do not repeat them.
 */
private fun Project.configureTestDependencies() = dependencies {
    "testImplementation"(libs.findLibrary("junit").get())
    "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())

    "androidTestImplementation"(libs.findLibrary("androidx-junit").get())
    "androidTestImplementation"(libs.findLibrary("androidx-espresso-core").get())
}

/**
 * Version catalog accessor used by the convention plugins.
 */
internal val Project.libs
    get() = extensions.getByType(VersionCatalogsExtension::class.java)
        .named("libs")
