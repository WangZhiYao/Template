plugins {
    id("template.android.library")
}

android {
    namespace = "com.example.template.shared.designsystem"
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.fragment.ktx)

    api(libs.material)
}
