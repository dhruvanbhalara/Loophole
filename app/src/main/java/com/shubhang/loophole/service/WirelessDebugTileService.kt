package com.shubhang.loophole.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.shubhang.loophole.R
import com.shubhang.loophole.appContainer
import com.shubhang.loophole.settings.SecureSetting
import com.shubhang.loophole.settings.SettingsWriteResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile that toggles Wireless Debugging (Android 11+).
 */
class WirelessDebugTileService : TileService() {

    private val devSettings by lazy { appContainer.devSettings }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        if (!SecureSetting.WIRELESS_DEBUGGING.isSupportedOnCurrentSdk) {
            renderUnsupported()
            return
        }
        listeningJob = serviceScope.launch {
            devSettings.isWirelessDebuggingEnabled.collect(::renderTile)
        }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        if (!SecureSetting.WIRELESS_DEBUGGING.isSupportedOnCurrentSdk) {
            return
        }
        serviceScope.launch {
            if (devSettings.toggle(SecureSetting.WIRELESS_DEBUGGING) is SettingsWriteResult.PermissionDenied) {
                openApp()
            }
        }
    }

    private fun renderUnsupported() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_UNAVAILABLE
        tile.label = getString(R.string.tile_wireless_label)
        tile.updateTile()
    }

    private fun renderTile(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(if (enabled) R.string.tile_wireless_label_on else R.string.tile_wireless_label_off)
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this, 0, launch, PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launch)
        }
    }
}
