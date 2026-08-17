package com.shubhang.loophole.settings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Single source of truth for Developer Options and debugging toggles. Every entry point —
 * the app UI, the Quick Settings tiles, and the home-screen widget — reads and writes
 * through this, so behaviour stays identical everywhere.
 *
 * [onChanged] runs after each successful write. It is how the widget gets
 * refreshed without every caller having to remember to do it; a plain lambda
 * rather than an interface, wired up in
 * [com.shubhang.loophole.AppContainer].
 */
class DevSettingsRepository(
    private val source: SecureSettingsSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onChanged: suspend (Boolean) -> Unit = {},
) {

    /** Current value, then every later change, including changes made outside the app. */
    val isEnabled: Flow<Boolean> = observe(SecureSetting.DEV_OPTIONS)

    val isUsbDebuggingEnabled: Flow<Boolean> = observe(SecureSetting.USB_DEBUGGING)

    val isWirelessDebuggingEnabled: Flow<Boolean> = observe(SecureSetting.WIRELESS_DEBUGGING)

    fun observe(setting: SecureSetting): Flow<Boolean> =
        source.observe(setting)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    suspend fun currentValue(): Boolean = currentValue(SecureSetting.DEV_OPTIONS)

    suspend fun currentValue(setting: SecureSetting): Boolean = withContext(ioDispatcher) {
        source.read(setting)
    }

    suspend fun setEnabled(enabled: Boolean): SettingsWriteResult =
        setEnabled(SecureSetting.DEV_OPTIONS, enabled)

    suspend fun setEnabled(setting: SecureSetting, enabled: Boolean): SettingsWriteResult = withContext(ioDispatcher) {
        if (setting == SecureSetting.DEV_OPTIONS && enabled) {
            // Clear the debugging flags first, so switching Developer Options on
            // never also restores a USB or wireless debugging session that was
            // left enabled from last time.
            for (debugSetting in SecureSetting.DEBUGGING) {
                if (!source.write(debugSetting, false)) {
                    return@withContext SettingsWriteResult.PermissionDenied
                }
            }
        }

        if (!source.write(setting, enabled)) {
            return@withContext SettingsWriteResult.PermissionDenied
        }
        // Read back rather than trusting the requested value, so callers always
        // reflect what the system actually stored.
        val stored = source.read(setting)
        onChanged(stored)
        SettingsWriteResult.Success(stored)
    }

    suspend fun toggle(): SettingsWriteResult = toggle(SecureSetting.DEV_OPTIONS)

    suspend fun toggle(setting: SecureSetting): SettingsWriteResult =
        setEnabled(setting, !currentValue(setting))
}
