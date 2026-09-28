plugins {
    id("template.android.library")
    id("template.hilt")
    id("template.room")
}

android {
    namespace = "com.example.template.core.database.impl"

    room {
        schemaDirectory("$projectDir/schemas")
    }

    ksp {
        arg("room.generateKotlin", "true")
    }
}

dependencies {
    implementation(project(":core:database-api"))
}
