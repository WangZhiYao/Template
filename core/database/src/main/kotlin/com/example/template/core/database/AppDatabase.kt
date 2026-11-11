package com.example.template.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.template.core.database.dao.GreetingDao
import com.example.template.core.database.entity.GreetingEntity

/**
 * The single Room database of the app. Every entity and DAO lives in this module, so
 * `@Database` can reference them in one compilation unit (see README 2.2 / 2.4).
 *
 * Adding a capability means: add the entity to [entities], add a DAO accessor here, and
 * provide the DAO in `DatabaseModule`. Bump [androidx.room.Database.version] and write a
 * Migration whenever an existing schema changes.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
@Database(
    entities = [GreetingEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun greetingDao(): GreetingDao
}
