plugins {
    id("template.android.feature")
}

android {
    namespace = "com.example.template.feature.main"
}

dependencies {
    // Contracts only — never :data:main (see README 2.3).
    implementation(project(":domain:main"))
}
