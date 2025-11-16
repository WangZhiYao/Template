// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.room) apply false
}

tasks.register("renameProject") {
    description = "Renames the project and package. Usage: ./gradlew renameProject -PnewProjectName=MyCoolApp -PnewPackageName=com.mycompany.coolapp"
    group = "maintenance"

    doLast {
        // --- 获取并验证输入 ---
        val newProjectName = project.findProperty("newProjectName") as? String
        val newPackageName = project.findProperty("newPackageName") as? String

        if (newProjectName.isNullOrBlank() || newPackageName.isNullOrBlank() || !newPackageName.contains(".")) {
            throw GradleException("用法: ./gradlew renameProject -PnewProjectName=\"YourAppName\" -PnewPackageName=\"com.yourcompany.app\"")
        }

        // --- 配置 ---
        val oldProjectName = "Template"
        val oldPackageName = "com.example.template"

        println("--- 开始重命名项目 ---")
        println("  新项目名: $newProjectName")
        println("  新包名  : $newPackageName")
        println("-----------------------------")

        // --- 阶段 1: 内容替换 ---
        println("阶段 1: 替换文件内容...")
        val filesToScan = rootProject.fileTree(".") {
            exclude(".git", ".gradle", "build", ".idea", "**/build/**")
        }

        filesToScan.forEach { file ->
            if (!file.isDirectory) {
                runCatching {
                    var content = file.readText(Charsets.UTF_8)
                    var modified = false
                    if (content.contains(oldPackageName)) {
                        content = content.replace(oldPackageName, newPackageName)
                        modified = true
                    }
                    if (content.contains(oldProjectName)) {
                        content = content.replace(oldProjectName, newProjectName)
                        modified = true
                    }
                    if (modified) {
                        println("  -> 正在修改: ${rootProject.projectDir.toURI().relativize(file.toURI()).path}")
                        file.writeText(content, Charsets.UTF_8)
                    }
                }.onFailure {
                    // 忽略读取二进制文件等导致的异常
                }
            }
        }
        println("阶段 1 完成。")

        // --- 阶段 2: 目录重命名 ---
        println("\n阶段 2: 迁移包名目录...")
        val oldPackagePath = oldPackageName.replace('.', '/')
        val newPackagePath = newPackageName.replace('.', '/')

        // 查找所有可能的源码根目录 (例如 src/main/java, src/test/kotlin 等)
        val potentialSrcRoots = rootProject.allprojects.flatMap { p ->
            listOf("java", "kotlin").flatMap { lang ->
                listOf("main", "test", "androidTest").map { sourceSet ->
                    File(p.projectDir, "src/$sourceSet/$lang")
                }
            }
        }.filter { it.exists() }

        potentialSrcRoots.forEach { srcRoot ->
            val oldDir = File(srcRoot, oldPackagePath)
            if (oldDir.exists() && oldDir.isDirectory) {
                val newDir = File(srcRoot, newPackagePath)
                println("  -> 正在迁移目录: ${oldDir.relativeTo(rootDir)}")
                
                // 复制所有内容到新目录
                oldDir.copyRecursively(newDir, overwrite = true)
                
                // 删除旧目录
                oldDir.deleteRecursively()

                // 清理旧结构中可能残留的空父目录
                var parent = oldDir.parentFile
                while (parent != null && parent.isDirectory && parent.listFiles()?.isEmpty() == true && !parent.absolutePath.equals(srcRoot.absolutePath, ignoreCase = true)) {
                    println("  -> 正在清理空目录: ${parent.relativeTo(rootDir)}")
                    val toDelete = parent
                    parent = parent.parentFile
                    toDelete.delete()
                }
            }
        }
        println("阶段 2 完成。")

        // --- 阶段 3: 更新 settings.gradle.kts ---
        println("\n阶段 3: 更新 settings.gradle.kts...")
        val settingsFile = File(rootDir, "settings.gradle.kts")
        if (settingsFile.exists()) {
            var content = settingsFile.readText()
            content = content.replace("rootProject.name = \"$oldProjectName\"", "rootProject.name = \"$newProjectName\"")
            settingsFile.writeText(content)
            println("  -> settings.gradle.kts 已更新。")
        }
        println("阶段 3 完成。")


        println("\n--- 重命名成功！ ---")
        println("建议: 请同步您的 Gradle 项目，并考虑重启 IDE。")
    }
}