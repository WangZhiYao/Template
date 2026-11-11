plugins {
    id("template.android.library")
    id("template.hilt")
    id("template.room")
}

android {
    namespace = "com.example.template.core.database"

    room {
        schemaDirectory("$projectDir/schemas")
    }

    ksp {
        arg("room.generateKotlin", "true")
    }
}

dependencies {
    // Exposed to :data:* modules, which reference DAOs and entities without applying Room
    // themselves. `template.room` already adds the same artifacts as `implementation`; the
    // `api` declarations here lift them onto the consumer classpath.
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    api(libs.androidx.paging.runtime)
}
