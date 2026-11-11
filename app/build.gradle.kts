plugins {
    id("template.android.application")
}

dependencies {
    debugImplementation(libs.leakcanary.android)

    // Features
    implementation(project(":feature:main"))

    // Data implementations
    implementation(project(":data:main"))

    // Domain contracts — Hilt's generated component references these interfaces
    implementation(project(":domain:main"))

    // Infrastructure
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // Shared UI
    implementation(project(":shared:designsystem"))
    implementation(project(":shared:ui"))
}
