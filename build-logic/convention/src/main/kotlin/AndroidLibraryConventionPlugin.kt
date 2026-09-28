import com.example.template.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Convention plugin for Android library modules.
 *
 * Configures only the Android basics: com.android.library, compileSdk/minSdk, Java 21 and the
 * shared test dependencies.
 *
 * Design note: capabilities that not every library needs (Hilt, Compose) are intentionally not
 * applied here. Modules opt in with `template.hilt` / `template.compose`, so a library that is
 * neither DI-aware nor UI-related is not forced to pull in a DI framework, a KSP processor or
 * the Compose compiler.
 *
 * Usage: `plugins { id("template.android.library") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            configureKotlinAndroid()
        }
    }
}
