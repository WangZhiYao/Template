import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for ViewBinding.
 *
 * Enables viewBinding for both application and library modules.
 *
 * Usage: `plugins { id("template.viewbinding") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
class ViewBindingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    buildFeatures {
                        viewBinding = true
                    }
                }
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    buildFeatures {
                        viewBinding = true
                    }
                }
            }
        }
    }
}
