plugins {
    id("template.kotlin.library")
}

dependencies {
    // api: :feature:main collects the Flows returned by UseCases.
    api(libs.kotlinx.coroutines.core)
    // @Inject constructor on UseCases only; Dagger reads the javax namespace (see README 2.4).
    implementation(libs.javax.inject)
}
