plugins {
    id("template.android.application")
}

dependencies {
    debugImplementation(libs.leakcanary.android)

    implementation(project(":shared:ui"))
    implementation(project(":feature:main"))
}
