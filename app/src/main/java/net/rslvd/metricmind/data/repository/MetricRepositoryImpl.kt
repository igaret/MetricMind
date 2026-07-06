package net.rslvd.metricmind.data.repository

import net.rslvd.metricmind.data.local.dao.MetricDao
import net.rslvd.metricmind.data.local.entity.MetricEntryEntity
import net.rslvd.metricmind.domain.model.MetricEntry
import net.rslvd.metricmind.domain.model.MetricType
import net.rslvd.metricmind.domain.repository.MetricRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class MetricRepositoryImpl @Inject constructor(
    private val dao: MetricDao,
) : MetricRepository {

    override fun observe(type: MetricType, from: LocalDate, to: LocalDate): Flow<List<MetricEntry>> =
        dao.observe(type.name, from.toEpochDay(), to.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(entry: MetricEntry) =
        dao.upsert(
            MetricEntryEntity(
                id = entry.id,
                type = entry.type,
                value = entry.value,
                note = entry.note,
                day = entry.day.toEpochDay(),
                createdAt = System.currentTimeMillis(),
            ),
        )

    override suspend fun delete(type: MetricType, day: LocalDate) =
        dao.delete(type.name, day.toEpochDay())

    override suspend fun range(type: MetricType, from: LocalDate, to: LocalDate): List<MetricEntry> =
        dao.range(type.name, from.toEpochDay(), to.toEpochDay()).map { it.toDomain() }

    private fun MetricEntryEntity.toDomain() = MetricEntry(
        id = id, type = type, value = value, note = note, day = LocalDate.ofEpochDay(day),
    )
}
