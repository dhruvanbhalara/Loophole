package com.shubhang.loophole

import android.content.Context
import com.shubhang.loophole.settings.AndroidSecureSettingsSource
import com.shubhang.loophole.settings.DevSettingsRepository
import com.shubhang.loophole.settings.DeveloperOptionsLauncher
import com.shubhang.loophole.settings.QuickSettingsTileManager
import com.shubhang.loophole.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * The app's dependency graph, in one readable place. Everything here is
 * app-lifetime and created lazily on first use.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val secureSettings by lazy { AndroidSecureSettingsSource(appContext) }

    val widgetUpdater: WidgetUpdater by lazy { WidgetUpdater(appContext) }

    /**
     * The widget cannot subscribe to a Flow — it is only alive while being
     * rendered — so every successful write pushes the new value into it here,
     * rather than at each of the call sites that toggle.
     * In addition, an observer flow updates the widget on external setting changes.
     */
    val devSettings: DevSettingsRepository by lazy {
        DevSettingsRepository(
            source = secureSettings,
            onChanged = { widgetUpdater.refresh() },
        ).also { repo ->
            // Keep widgets in sync with changes made outside the app (Settings app, ADB, QS tiles)
            appScope.launch {
                combine(
                    repo.isEnabled,
                    repo.isUsbDebuggingEnabled,
                    repo.isWirelessDebuggingEnabled
                ) { _, _, _ -> }
                    .drop(1)
                    .collect {
                        widgetUpdater.refresh()
                    }
            }
        }
    }

    val developerOptionsLauncher by lazy { DeveloperOptionsLauncher(appContext) }

    val quickSettingsTileManager by lazy { QuickSettingsTileManager(appContext) }
}

/** Reaches the graph from any Context — activities, services, receivers, workers. */
val Context.appContainer: AppContainer
    get() = (applicationContext as LoopholeApplication).container
