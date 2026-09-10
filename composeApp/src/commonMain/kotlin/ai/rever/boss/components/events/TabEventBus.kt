package ai.rever.boss.components.events

import ai.rever.boss.ipc.IpcEventBridge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event emitted when a tab should be selected in a specific panel.
 *
 * @param targetWindowId The window that should handle this event
 * @param panelId The panel containing the tab
 * @param tabId The tab to select
 */
data class TabSelectEvent(
    val targetWindowId: String,
    val panelId: String,
    val tabId: String,
)

/**
 * Event bus for tab-related events.
 */
object TabEventBus {
    /** Optional IPC bridge for forwarding events cross-process in kernel mode. */
    @Volatile var ipcBridge: IpcEventBridge? = null

    private val _tabSelectEvents =
        MutableSharedFlow<TabSelectEvent>(
            replay = 0,
            extraBufferCapacity = 10,
        )
    val tabSelectEvents: SharedFlow<TabSelectEvent> = _tabSelectEvents.asSharedFlow()

    /**
     * Emit a tab select event.
     *
     * @param targetWindowId The window that should select the tab
     * @param panelId The panel containing the tab
     * @param tabId The tab to select
     */
    suspend fun selectTab(
        targetWindowId: String,
        panelId: String,
        tabId: String,
    ) {
        val event = TabSelectEvent(targetWindowId, panelId, tabId)
        _tabSelectEvents.emit(event)
        ipcBridge?.forward("TabSelectEvent", event, targetWindowId)
    }
}
