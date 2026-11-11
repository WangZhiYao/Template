plugins {
    id("template.android.library")
    id("template.hilt")
}

android {
    namespace = "com.example.template.data.main"
}

dependencies {
    // implementation, not api: :app depends on every module directly, so nothing needs to be
    // re-exported here (see README 2.4).
    implementation(project(":domain:main"))
    implementation(project(":core:database"))
}
