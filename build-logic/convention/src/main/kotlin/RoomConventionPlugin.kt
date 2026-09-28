import com.example.template.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Room.
 *
 * Configures KSP, the androidx.room plugin, the room-runtime / room-ktx dependencies and
 * room-compiler via KSP.
 *
 * Module specific options (schemaDirectory, generateKotlin, ...) stay in the module's
 * `android { room { ... } }` block.
 *
 * Usage: `plugins { id("template.room") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")
            apply(plugin = "androidx.room")

            dependencies {
                "implementation"(libs.findLibrary("androidx-room-runtime").get())
                "implementation"(libs.findLibrary("androidx-room-ktx").get())
                "ksp"(libs.findLibrary("androidx-room-compiler").get())
            }
        }
    }
}
