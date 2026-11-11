import com.example.template.buildlogic.AppConfig
import com.example.template.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Convention plugin for pure Kotlin JVM modules (`:domain:*`).
 *
 * Configures `org.jetbrains.kotlin.jvm` with the Java 21 target and the shared JVM test
 * dependencies. No Android artifacts, no Hilt, no Room, no Compose: domain modules declare
 * business contracts only and must stay framework-free (see README 2.1 / 2.2).
 *
 * Design note: unlike the Android plugins, no `androidTestImplementation` dependencies are
 * added because a JVM module has no instrumented-test source set.
 *
 * Usage: `plugins { id("template.kotlin.library") }`
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = AppConfig.JAVA_VERSION
                targetCompatibility = AppConfig.JAVA_VERSION
            }

            extensions.configure<KotlinJvmProjectExtension>("kotlin") {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }

            dependencies {
                "testImplementation"(libs.findLibrary("junit").get())
                "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
