import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Convention plugin for Kotlin serialization.
 *
 * Applies org.jetbrains.kotlin.plugin.serialization. Needed by modules that declare
 * `@Serializable` types, for example type-safe Navigation routes.
 *
 * Usage: `plugins { id("template.serialization") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
class SerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
        }
    }
}
