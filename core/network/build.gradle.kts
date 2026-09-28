plugins {
    id("template.android.library")
    id("template.hilt")
}

android {
    namespace = "com.example.template.core.network"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.retrofit)
    api(libs.retrofit.converter.kotlinx.serialization)
    api(libs.okhttp)
    api(libs.okhttp.logging.interceptor)
}
