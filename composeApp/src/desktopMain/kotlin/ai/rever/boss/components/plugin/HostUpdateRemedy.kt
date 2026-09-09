package ai.rever.boss.components.plugin

import ai.rever.boss.updater.UpdateInfo
import ai.rever.boss.updater.UpdateState

/** Re-read the updater at click time: a resolved offer can outlive its available state. */
internal fun applyHostUpdateRemedy(
    remedy: PluginLoadRemedy.UpdateHost,
    state: UpdateState,
    download: (UpdateInfo) -> Unit,
): Result<String> =
    when (state) {
        is UpdateState.Downloading ->
            Result.success("A BOSS update is already downloading. Check its progress in the download center.")
        is UpdateState.ReadyToInstall ->
            Result.success("A BOSS update is ready to install. Install it from the update banner, then restart BOSS.")
        is UpdateState.UpdateAvailable -> {
            if (state.updateInfo.latestVersion.toString() == remedy.availableVersion) {
                download(state.updateInfo)
                Result.success("Downloading BOSS ${remedy.availableVersion}. The plugin loads after the restart.")
            } else {
                Result.failure(IllegalStateException("The offered BOSS update changed. Check the update banner."))
            }
        }
        else ->
            Result.failure(IllegalStateException("The update to ${remedy.availableVersion} is no longer available."))
    }
