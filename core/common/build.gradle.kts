plugins {
    id("template.android.library")
    id("template.hilt")
}

android {
    namespace = "com.example.template.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)

    api(libs.androidx.datastore.preferences)
}
