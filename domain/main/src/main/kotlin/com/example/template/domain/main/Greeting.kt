package com.example.template.domain.main

/**
 * Domain model of the main screen's greeting.
 *
 * This is NOT a Room entity: `:data:main` owns the mapping from `GreetingEntity`.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
data class Greeting(
    val text: String,
)
