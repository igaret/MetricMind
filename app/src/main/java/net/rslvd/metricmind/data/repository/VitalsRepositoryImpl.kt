package net.rslvd.metricmind.data.repository

import net.rslvd.metricmind.data.local.dao.VitalDao
import net.rslvd.metricmind.data.local.entity.VitalReadingEntity
import net.rslvd.metricmind.domain.model.VitalAccuracy
import net.rslvd.metricmind.domain.model.VitalReading
import net.rslvd.metricmind.domain.model.VitalType
import net.rslvd.metricmind.domain.model.VitalVerification
import net.rslvd.metricmind.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VitalsRepositoryImpl @Inject constructor(
    private val dao: VitalDao,
) : VitalsRepository {

    override fun observeRecent(type: VitalType, limit: Int): Flow<List<VitalReading>> =
        dao.observeRecent(type.name, limit).map { list -> list.map { it.toDomain() } }

    override fun observeAllRecent(limit: Int): Flow<List<VitalReading>> =
        dao.observeAllRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun record(type: VitalType, value: Float): VitalReading {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            VitalReadingEntity(
                type = type,
                value = value,
                timestamp = now,
                verification = VitalVerification.UNVERIFIED,
                correctedValue = null,
            ),
        )
        return VitalReading(id = id, type = type, value = value, timestamp = now)
    }

    override suspend fun verify(id: Long, verification: VitalVerification, correctedValue: Float?) =
        dao.setVerification(id, verification.name, correctedValue)

    override suspend fun accuracy(type: VitalType): VitalAccuracy = VitalAccuracy(
        type = type,
        total = dao.countAll(type.name),
        confirmed = dao.countByVerification(type.name, VitalVerification.CONFIRMED.name),
        corrected = dao.countByVerification(type.name, VitalVerification.CORRECTED.name),
    )

    private fun VitalReadingEntity.toDomain() = VitalReading(
        id = id,
        type = type,
        value = value,
        timestamp = timestamp,
        verification = verification,
        correctedValue = correctedValue,
    )
}
