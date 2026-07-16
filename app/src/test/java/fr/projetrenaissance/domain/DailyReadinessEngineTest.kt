package fr.projetrenaissance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyReadinessEngineTest {
    @Test fun `no data never invents a score`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs())
        assertEquals(null, result.score)
        assertEquals(ReadinessClassification.INSUFFICIENT, result.classification)
    }

    @Test fun `partial inputs are renormalized`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(pain = 0))
        assertEquals(100, result.score)
        assertTrue(result.confidence < 20)
        assertTrue(result.recommendation.startsWith("Données limitées"))
    }

    @Test fun `complete healthy inputs produce good readiness`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(
            sleep = SleepReadinessInput(480, 470, 20),
            cardiovascular = CardioReadinessInput(58.0, 59.0, 5.0),
            recentLoad = LoadReadinessInput(300.0, 300.0),
            pain = 0,
            manual = ManualReadinessInput(5, 1, 5),
        ))
        assertTrue(result.score!! >= 80)
        assertEquals(ReadinessClassification.GOOD, result.classification)
        assertEquals(5, result.factors.size)
    }

    @Test fun `weak sleep lowers sleep factor`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(sleep = SleepReadinessInput(180, 480)))
        assertTrue(result.score!! < 40)
    }

    @Test fun `unusual resting heart rate is detected`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(cardiovascular = CardioReadinessInput(72.0, 60.0, 2.0)))
        assertEquals(10, result.factors.single().score)
    }

    @Test fun `available rmssd contributes without replacing resting heart rate`() {
        val withoutHrv = DailyReadinessEngine.calculate(ReadinessInputs(cardiovascular = CardioReadinessInput(60.0, 60.0, 2.0)))
        val withLowHrv = DailyReadinessEngine.calculate(ReadinessInputs(cardiovascular = CardioReadinessInput(60.0, 60.0, 2.0, hrvRmssd = 20.0, personalAverageHrv = 40.0)))
        assertTrue(withLowHrv.score!! < withoutHrv.score!!)
    }

    @Test fun `recent rmssd can be used without inventing resting heart rate`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(cardiovascular = CardioReadinessInput(ageHours = 2.0, hrvRmssd = 38.0, personalAverageHrv = 40.0)))
        assertTrue(result.score!! >= 90)
    }

    @Test fun `old cardiovascular value is excluded`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(cardiovascular = CardioReadinessInput(60.0, 60.0, 37.0)))
        assertEquals(null, result.score)
    }

    @Test fun `strong recent load lowers load factor`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(recentLoad = LoadReadinessInput(500.0, 250.0)))
        assertTrue(result.factors.single().score <= 60)
    }

    @Test fun `high pain overrides recommendation`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(pain = 8, manual = ManualReadinessInput(5, 1, 5)))
        assertTrue(result.recommendation.startsWith("Douleur élevée"))
    }

    @Test fun `manual values are converted from one to five`() {
        val result = DailyReadinessEngine.calculate(ReadinessInputs(manual = ManualReadinessInput(5, 1, 3)))
        assertEquals(83, result.score)
    }

    @Test fun `classification thresholds remain stable`() {
        assertEquals(ReadinessClassification.WEAK, DailyReadinessEngine.classify(39))
        assertEquals(ReadinessClassification.PARTIAL, DailyReadinessEngine.classify(40))
        assertEquals(ReadinessClassification.PARTIAL, DailyReadinessEngine.classify(59))
        assertEquals(ReadinessClassification.CORRECT, DailyReadinessEngine.classify(60))
        assertEquals(ReadinessClassification.CORRECT, DailyReadinessEngine.classify(79))
        assertEquals(ReadinessClassification.GOOD, DailyReadinessEngine.classify(80))
        assertNotNull(DailyReadinessEngine.classify(100))
    }

    @Test fun `low confidence widens readiness estimate`() {
        assertEquals(86..100, readinessEstimateRange(100, 10))
        assertEquals("Fiabilité faible", readinessReliabilityLabel(10))
    }

    @Test fun `medium confidence avoids false precision`() {
        assertEquals(78..90, readinessEstimateRange(84, 66))
        assertEquals("Fiabilité moyenne", readinessReliabilityLabel(66))
    }

    @Test fun `full confidence preserves exact score`() {
        assertEquals(84..84, readinessEstimateRange(84, 100))
        assertEquals("Fiabilité élevée", readinessReliabilityLabel(100))
    }
}
