package ai.rever.boss.plugin

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Exercises the production readiness wait without starting manifest network traffic. */
class SystemPluginManifestSyncOrderingTest {
    @Test
    fun `a coroutine awaiting isInitialized does not complete before initialize is called`() =
        runTest {
            val initialized = MutableStateFlow(false)
            assertFalse(initialized.value, "precondition: not yet initialized")

            val waiter = async { SystemPluginManifestService.awaitSupabaseInitialized(initialized) }
            // Let the waiter actually start and register as a collector on the still-false flow,
            // rather than asserting on a coroutine that has merely been scheduled but never run -
            // that would pass unconditionally, proving nothing about suspension.
            runCurrent()

            assertFalse(waiter.isCompleted, "the waiter must still be suspended before initialize() runs")

            advanceTimeBy(60_000)
            runCurrent()
            assertFalse(waiter.isCompleted, "a slow initialization must not permanently abandon live sync")

            initialized.value = true
            runCurrent()

            assertTrue(waiter.isCompleted, "the waiter must resolve once initialize() runs")
            waiter.await()
        }
    @Test
    fun `readiness wait completes immediately for an initialized client`() = runTest {
        val initialized = MutableStateFlow(true)
        val before = testScheduler.currentTime
        SystemPluginManifestService.awaitSupabaseInitialized(initialized)
        assertEquals(before, testScheduler.currentTime)
    }

    @Test
    fun `readiness wait is cancellable before initialization`() = runTest {
        val initialized = MutableStateFlow(false)
        val waiter = async { SystemPluginManifestService.awaitSupabaseInitialized(initialized) }
        runCurrent()
        waiter.cancel()
        runCurrent()
        assertTrue(waiter.isCancelled)
    }
}
