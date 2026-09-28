import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.example.template.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    // Compile-time only: at runtime these Gradle plugins are provided by the root project's
    // `plugins { ... apply false }` block.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "template.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "template.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "template.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("compose") {
            id = "template.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("hilt") {
            id = "template.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("room") {
            id = "template.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("serialization") {
            id = "template.serialization"
            implementationClass = "SerializationConventionPlugin"
        }
    }
}
