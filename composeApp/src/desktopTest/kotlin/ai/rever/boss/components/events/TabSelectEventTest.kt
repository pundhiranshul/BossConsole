package ai.rever.boss.components.events

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Cross-window tab selection reaches the target window through [TabEventBus.tabSelectEvents],
 * so the bus carries the three properties the window's effect relies on: it is addressed to ONE
 * window (targetWindowId + panelId + tabId), and it never replays.
 */
class TabSelectEventTest {
    @Test
    fun `only the addressed window sees the tab selection`() =
        runTest {
            val mine = "window-addressed"
            val theirs = "window-bystander"

            val received =
                async {
                    withTimeoutOrNull(TIMEOUT_MS) {
                        TabEventBus.tabSelectEvents.first { it.targetWindowId == mine }
                    }
                }
            val bystander =
                async {
                    withTimeoutOrNull(TIMEOUT_MS) {
                        TabEventBus.tabSelectEvents.first { it.targetWindowId == theirs }
                    }
                }
            yield()

            TabEventBus.selectTab(mine, "panel-1", "tab-42")

            val event = assertNotNull(received.await())
            assertEquals("panel-1", event.panelId)
            assertEquals("tab-42", event.tabId)
            assertNull(bystander.await(), "a selection for one window must not select a tab in another")
        }

    /**
     * The flow has no replay - and must not gain one. A replayed selection would re-select the
     * tab in every window opened afterwards, yanking focus on unrelated windows at startup.
     */
    @Test
    fun `a window that opens later does not replay someone else's selection`() =
        runTest {
            val window = "window-late-collector"

            TabEventBus.selectTab(window, "panel-1", "tab-42")
            yield()

            val late =
                async {
                    withTimeoutOrNull(TIMEOUT_MS) {
                        TabEventBus.tabSelectEvents.first { it.targetWindowId == window }
                    }
                }

            assertNull(late.await(), "the selection was already delivered - a late window must not repeat it")
        }

    private companion object {
        const val TIMEOUT_MS = 2_000L
    }
}
