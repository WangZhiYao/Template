package com.example.template.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.template.core.database.entity.GreetingEntity
import kotlinx.coroutines.flow.Flow

/**
 * @author WangZhiYao
 * @since 2026/11/11
 */
@Dao
interface GreetingDao {

    @Query("SELECT * FROM greetings WHERE id = 0")
    fun observe(): Flow<GreetingEntity?>

    @Upsert
    suspend fun upsert(entity: GreetingEntity)
}
