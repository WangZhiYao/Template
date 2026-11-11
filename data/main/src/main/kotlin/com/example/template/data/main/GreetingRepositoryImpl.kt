package com.example.template.data.main

import com.example.template.core.database.dao.GreetingDao
import com.example.template.core.database.entity.GreetingEntity
import com.example.template.domain.main.Greeting
import com.example.template.domain.main.GreetingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Public because Hilt generates the component in `:app` and must reference this type;
 * isolation comes from the dependency graph (`:feature:main` never depends on this module),
 * not from `internal`.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
class GreetingRepositoryImpl @Inject constructor(
    private val greetingDao: GreetingDao,
) : GreetingRepository {

    override fun observe(): Flow<Greeting> =
        greetingDao.observe().map { it.toGreeting() }

    override suspend fun save(text: String) =
        greetingDao.upsert(GreetingEntity(text = text))
}
