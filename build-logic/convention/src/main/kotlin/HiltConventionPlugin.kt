import com.example.template.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Hilt dependency injection.
 *
 * Configures KSP, the Hilt Android plugin, and the hilt-android / hilt-android-compiler
 * dependencies.
 *
 * Usage: `plugins { id("template.hilt") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")
            apply(plugin = "com.google.dagger.hilt.android")

            dependencies {
                "implementation"(libs.findLibrary("hilt-android").get())
                "ksp"(libs.findLibrary("hilt-android-compiler").get())
            }
        }
    }
}
