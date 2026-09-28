plugins {
    id("template.android.library")
}

android {
    namespace = "com.example.template.core.database.api"
}

dependencies {
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    api(libs.androidx.paging.runtime)
}
