import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.example.template.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Jetpack Compose.
 *
 * Configures the Compose compiler plugin, enables the compose build feature, and adds the
 * dependencies that every Compose module needs:
 * - the Compose BOM, so the versionless Compose artifacts declared in modules resolve
 * - instrumented test artifacts (BOM + ui-test-junit4)
 * - debug-only tooling (ui-tooling, ui-test-manifest)
 *
 * The actual UI artifacts (ui, material3, icons, ...) stay in the module that owns them,
 * because only the design system should expose them as `api`.
 *
 * Usage: `plugins { id("template.compose") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    buildFeatures {
                        compose = true
                    }
                }
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    buildFeatures {
                        compose = true
                    }
                }
            }

            dependencies {
                val bom = libs.findLibrary("androidx-compose-bom").get()

                "implementation"(platform(bom))

                "androidTestImplementation"(platform(bom))
                "androidTestImplementation"(
                    libs.findLibrary("androidx-compose-ui-test-junit4").get()
                )

                "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
    }
}
