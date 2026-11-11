package com.example.template.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the greeting shown by the main screen.
 *
 * This is a persistence type: `:data:main` maps it to the `Greeting` domain model.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
@Entity(tableName = "greetings")
data class GreetingEntity(
    @PrimaryKey val id: Int = 0,
    val text: String,
)
