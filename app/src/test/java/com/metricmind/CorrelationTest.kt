package com.metricmind

import com.metricmind.domain.usecase.ComputeCorrelation
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrelationTest {

    @Test
    fun perfectPositiveCorrelation() {
        val points = listOf(1f to 2f, 2f to 4f, 3f to 6f, 4f to 8f)
        assertEquals(1.0f, ComputeCorrelation.pearson(points), 1e-4f)
    }

    @Test
    fun perfectNegativeCorrelation() {
        val points = listOf(1f to 8f, 2f to 6f, 3f to 4f, 4f to 2f)
        assertEquals(-1.0f, ComputeCorrelation.pearson(points), 1e-4f)
    }

    @Test
    fun tooFewPointsIsZero() {
        assertEquals(0f, ComputeCorrelation.pearson(listOf(1f to 1f)), 0f)
    }
}
