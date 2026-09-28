plugins {
    id("template.android.feature")
    id("template.serialization")
}

android {
    namespace = "com.example.template.feature.main"
}

dependencies {
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.paging.compose)
}
