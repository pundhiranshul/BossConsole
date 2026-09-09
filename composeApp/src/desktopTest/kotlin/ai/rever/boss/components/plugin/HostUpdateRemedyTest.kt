package ai.rever.boss.components.plugin

import ai.rever.boss.updater.UpdateInfo
import ai.rever.boss.updater.UpdateState
import ai.rever.boss.utils.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostUpdateRemedyTest {
    private val remedy = PluginLoadRemedy.UpdateHost("9.5.9")
    private val info =
        UpdateInfo(
            available = true,
            currentVersion = Version(9, 5, 8),
            latestVersion = Version(9, 5, 9),
            releaseNotes = "",
            downloadUrl = "https://example.invalid/BOSS.dmg",
        )

    @Test
    fun `available offer starts exactly its advertised download`() {
        val downloads = mutableListOf<UpdateInfo>()
        val result = applyHostUpdateRemedy(remedy, UpdateState.UpdateAvailable(info), downloads::add)
        assertTrue(result.isSuccess)
        assertEquals(listOf(info), downloads)
        assertTrue(result.getOrThrow().contains("9.5.9"))
    }

    @Test
    fun `in-flight states acknowledge progress without starting another download`() {
        val downloading = applyHostUpdateRemedy(remedy, UpdateState.Downloading(0.5f)) { error("duplicate download") }
        val ready = applyHostUpdateRemedy(remedy, UpdateState.ReadyToInstall("/tmp/BOSS.dmg")) { error("duplicate download") }
        assertTrue(downloading.getOrThrow().contains("already downloading"))
        assertTrue(ready.getOrThrow().contains("ready to install"))
    }

    @Test
    fun `changed offer cannot download a version that may fail the plugin floor`() {
        val changed = info.copy(latestVersion = Version(9, 5, 7))
        val result = applyHostUpdateRemedy(remedy, UpdateState.UpdateAvailable(changed)) { error("stale offer") }
        assertTrue(result.isFailure)
    }

    @Test
    fun `unavailable states remain failures and never start downloads`() {
        val states =
            listOf(
                UpdateState.Idle,
                UpdateState.CheckingForUpdates,
                UpdateState.UpToDate,
                UpdateState.Installing,
                UpdateState.RestartRequired,
                UpdateState.Error("network failure"),
            )
        states.forEach { state ->
            assertTrue(applyHostUpdateRemedy(remedy, state) { error("unexpected download") }.isFailure, "$state")
        }
    }
}
