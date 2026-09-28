plugins {
    id("template.android.application")
    id("template.serialization")
}

dependencies {
    debugImplementation(libs.leakcanary.android)

    implementation(project(":shared:ui"))
    implementation(project(":feature:main"))

    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
