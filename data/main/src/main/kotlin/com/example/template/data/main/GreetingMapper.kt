package com.example.template.data.main

import com.example.template.core.database.entity.GreetingEntity
import com.example.template.domain.main.Greeting

/**
 * Entity -> domain mapping. Mappers do not participate in DI, so they stay internal
 * (see README 2.2 `:data:*`).
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
internal fun GreetingEntity?.toGreeting(): Greeting = Greeting(
    text = this?.text.orEmpty(),
)
