plugins {
    id("template.android.library")
    id("template.viewbinding")
}

android {
    namespace = "com.example.template.shared.ui"
}

dependencies {
    api(project(":shared:designsystem"))

    api(libs.androidx.constraintlayout)

    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)

    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
}
