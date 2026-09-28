plugins {
    id("template.android.library")
    id("template.compose")
}

android {
    namespace = "com.example.template.shared.ui"
}

dependencies {
    api(project(":shared:designsystem"))

    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.runtime.compose)
}
