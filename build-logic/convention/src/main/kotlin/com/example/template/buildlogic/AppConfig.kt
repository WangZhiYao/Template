package com.example.template.buildlogic

import org.gradle.api.JavaVersion

/**
 * Global build configuration: the single source of truth for app id, version and SDK levels.
 *
 * Modules no longer declare compileSdk, minSdk or versionCode themselves; the convention
 * plugins read the values from here.
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
object AppConfig {

    // --- Application identity and version ---

    const val APPLICATION_ID = "com.example.template"

    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0"

    // --- SDK levels ---

    const val COMPILE_SDK = 37
    const val MIN_SDK = 28
    const val TARGET_SDK = 37

    // --- Compiler options ---

    /** Java / Kotlin target. AGP 9 has built-in Kotlin, so jvmTarget follows compileOptions. */
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_21
}
