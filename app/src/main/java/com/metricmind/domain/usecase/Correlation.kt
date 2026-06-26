package com.metricmind.domain.usecase

import com.metricmind.domain.model.CorrelationResult
import com.metricmind.domain.model.MetricEntry
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.repository.MetricRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Computes Pearson correlation between two metrics over a date range, fully on-device.
 * Pairs are matched by day (inner join on the date both metrics were logged).
 */
class ComputeCorrelation @Inject constructor(
    private val metrics: MetricRepository,
) {
    suspend operator fun invoke(
        a: MetricType,
        b: MetricType,
        from: LocalDate,
        to: LocalDate,
    ): CorrelationResult {
        val byDayA = metrics.range(a, from, to).associateBy { it.day }
        val byDayB = metrics.range(b, from, to).associateBy { it.day }
        val points = byDayA.keys.intersect(byDayB.keys).mapNotNull { day ->
            val x = byDayA[day]?.value ?: return@mapNotNull null
            val y = byDayB[day]?.value ?: return@mapNotNull null
            x to y
        }
        return CorrelationResult(points.size, pearson(points), points)
    }

    companion object {
        fun pearson(points: List<Pair<Float, Float>>): Float {
            val n = points.size
            if (n < 2) return 0f
            val sx = points.sumOf { it.first.toDouble() }
            val sy = points.sumOf { it.second.toDouble() }
            val sxx = points.sumOf { it.first.toDouble() * it.first }
            val syy = points.sumOf { it.second.toDouble() * it.second }
            val sxy = points.sumOf { it.first.toDouble() * it.second }
            val num = n * sxy - sx * sy
            val den = sqrt((n * sxx - sx * sx) * (n * syy - sy * sy))
            return if (den == 0.0) 0f else (num / den).toFloat()
        }
    }
}

/** Pure helper so it is unit-testable without any Android deps. */
fun summarize(entries: List<MetricEntry>): Pair<Float, Float> {
    if (entries.isEmpty()) return 0f to 0f
    val avg = entries.map { it.value }.average().toFloat()
    val last = entries.maxByOrNull { it.day }?.value ?: 0f
    return avg to last
}
