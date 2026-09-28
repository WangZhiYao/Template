import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for feature modules.
 *
 * Configures Android Library + Hilt + ViewBinding, plus the dependencies shared by every
 * feature module (core:common, shared:ui).
 *
 * Design note: feature modules are numerous, uniform in shape, and always need DI
 * (@AndroidEntryPoint / @HiltViewModel) and ViewBinding (XML layouts), so both are bundled
 * here. `template.android.library` stays minimal and core/shared libraries opt in per module.
 *
 * Usage: `plugins { id("template.android.feature") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "template.android.library")
            apply(plugin = "template.hilt")
            apply(plugin = "template.viewbinding")

            dependencies {
                "implementation"(project(":core:common"))
                "implementation"(project(":shared:ui"))
            }
        }
    }
}
