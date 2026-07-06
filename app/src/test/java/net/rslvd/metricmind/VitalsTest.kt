package net.rslvd.metricmind

import net.rslvd.metricmind.domain.model.VitalAccuracy
import net.rslvd.metricmind.domain.model.VitalType
import net.rslvd.metricmind.sensors.SleepHeuristic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepHeuristicTest {

    @Test
    fun `still device, screen off, at night is asleep`() {
        val estimate = SleepHeuristic.estimate(
            movementVariance = 0.01f,
            screenInteractive = false,
            hourOfDay = 2,
        )
        assertTrue(estimate.asleep)
        assertTrue(estimate.confidence > 0.9f)
    }

    @Test
    fun `moving device with screen on during the day is awake`() {
        val estimate = SleepHeuristic.estimate(
            movementVariance = 2.5f,
            screenInteractive = true,
            hourOfDay = 14,
        )
        assertFalse(estimate.asleep)
        assertTrue(estimate.confidence > 0.9f)
    }

    @Test
    fun `no accelerometer falls back to screen and time signals`() {
        val estimate = SleepHeuristic.estimate(
            movementVariance = null,
            screenInteractive = false,
            hourOfDay = 23,
        )
        assertTrue(estimate.asleep)
    }

    @Test
    fun `screen on at night is ambiguous but leans awake`() {
        val estimate = SleepHeuristic.estimate(
            movementVariance = 2.0f,
            screenInteractive = true,
            hourOfDay = 23,
        )
        assertFalse(estimate.asleep)
    }
}

class VitalAccuracyTest {

    @Test
    fun `unverified readings give null percent`() {
        val acc = VitalAccuracy(VitalType.HEART_RATE, total = 5, confirmed = 0, corrected = 0)
        assertNull(acc.percentCorrect)
        assertEquals(0, acc.verified)
    }

    @Test
    fun `percent correct is confirmed over verified`() {
        val acc = VitalAccuracy(VitalType.HEART_RATE, total = 10, confirmed = 8, corrected = 2)
        assertEquals(10, acc.verified)
        assertEquals(80, acc.percentCorrect)
    }
}
