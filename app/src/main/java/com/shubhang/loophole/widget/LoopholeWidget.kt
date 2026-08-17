package com.shubhang.loophole.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.shubhang.loophole.R
import com.shubhang.loophole.appContainer
import com.shubhang.loophole.settings.SecureSetting
import com.shubhang.loophole.ui.DevOptionsActivity

val DevModeEnabledKey = booleanPreferencesKey("dev_mode_enabled")
val UsbDebugEnabledKey = booleanPreferencesKey("usb_debugging_enabled")
val WirelessDebugEnabledKey = booleanPreferencesKey("wireless_debugging_enabled")

val EnabledKey = DevModeEnabledKey

class LoopholeWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL_SIZE,
            MEDIUM_SIZE,
            LARGE_SIZE,
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val devSettings = context.appContainer.devSettings
        val liveDev = devSettings.currentValue(SecureSetting.DEV_OPTIONS)
        val liveUsb = devSettings.currentValue(SecureSetting.USB_DEBUGGING)
        val liveWireless = devSettings.currentValue(SecureSetting.WIRELESS_DEBUGGING)

        provideContent {
            val devEnabled = currentState(DevModeEnabledKey) ?: liveDev
            val usbEnabled = currentState(UsbDebugEnabledKey) ?: liveUsb
            val wirelessEnabled = currentState(WirelessDebugEnabledKey) ?: liveWireless

            GlanceTheme {
                AdaptiveWidgetBody(
                    devEnabled = devEnabled,
                    usbEnabled = usbEnabled,
                    wirelessEnabled = wirelessEnabled,
                )
            }
        }
    }

    companion object {
        val SMALL_SIZE = DpSize(120.dp, 48.dp)
        val MEDIUM_SIZE = DpSize(200.dp, 48.dp)
        val LARGE_SIZE = DpSize(280.dp, 48.dp)
    }
}

@Composable
private fun AdaptiveWidgetBody(
    devEnabled: Boolean,
    usbEnabled: Boolean,
    wirelessEnabled: Boolean,
) {
    val size = LocalSize.current
    val isExpanded = size.width >= 195.dp

    if (isExpanded) {
        ExpandedWidgetContent(devEnabled, usbEnabled, wirelessEnabled)
    } else {
        CompactWidgetContent(devEnabled)
    }
}

@Composable
private fun CompactWidgetContent(enabled: Boolean) {
    val chipBackground = if (enabled) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant
    val chipForeground = if (enabled) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant
    val gearBackground = if (enabled) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surface
    val gearForeground = if (enabled) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurfaceVariant

    val context = LocalContext.current
    val toggleDevIntent = Intent(context, ToggleReceiver::class.java).apply {
        action = ToggleReceiver.ACTION_TOGGLE_DEV_MODE
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(chipBackground)
            .cornerRadius(28.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .clickable(actionSendBroadcast(toggleDevIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(12.dp)
                    .cornerRadius(6.dp)
                    .background(chipForeground)
            ) {}
            Spacer(GlanceModifier.width(10.dp))
            Text(
                text = if (enabled) "ON" else "OFF",
                style = TextStyle(
                    color = chipForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
        }

        Spacer(GlanceModifier.width(8.dp))

        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .cornerRadius(22.dp)
                .background(gearBackground)
                .clickable(actionStartActivity<DevOptionsActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_settings_gear),
                contentDescription = "Open Developer Options",
                colorFilter = ColorFilter.tint(gearForeground),
                modifier = GlanceModifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ExpandedWidgetContent(devEnabled: Boolean, usbEnabled: Boolean, wirelessEnabled: Boolean) {
    val devBackground = if (devEnabled) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant
    val devForeground = if (devEnabled) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant
    val usbBackground = if (usbEnabled) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant
    val usbForeground = if (usbEnabled) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant
    val wirelessBackground = if (wirelessEnabled) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant
    val wirelessForeground = if (wirelessEnabled) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant
    val gearBackground = GlanceTheme.colors.surfaceVariant
    val gearForeground = GlanceTheme.colors.onSurfaceVariant

    val context = LocalContext.current
    val toggleDevIntent = Intent(context, ToggleReceiver::class.java).apply {
        action = ToggleReceiver.ACTION_TOGGLE_DEV_MODE
    }
    val toggleUsbIntent = Intent(context, ToggleReceiver::class.java).apply {
        action = ToggleReceiver.ACTION_TOGGLE_USB_DEBUG
    }
    val toggleWirelessIntent = Intent(context, ToggleReceiver::class.java).apply {
        action = ToggleReceiver.ACTION_TOGGLE_WIRELESS_DEBUG
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(28.dp)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dev Mode toggle chip
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .background(devBackground)
                .cornerRadius(22.dp)
                .padding(horizontal = 14.dp)
                .clickable(actionSendBroadcast(toggleDevIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(10.dp)
                    .cornerRadius(5.dp)
                    .background(devForeground)
            ) {}
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = if (devEnabled) "ON" else "OFF",
                style = TextStyle(
                    color = devForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        }

        Spacer(GlanceModifier.width(6.dp))

        // USB Debug toggle chip
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .cornerRadius(22.dp)
                .background(usbBackground)
                .clickable(actionSendBroadcast(toggleUsbIntent)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_usb_tile),
                contentDescription = "Toggle USB Debugging",
                colorFilter = ColorFilter.tint(usbForeground),
                modifier = GlanceModifier.size(22.dp)
            )
        }

        Spacer(GlanceModifier.width(6.dp))

        // Wireless Debug toggle chip
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .cornerRadius(22.dp)
                .background(wirelessBackground)
                .clickable(actionSendBroadcast(toggleWirelessIntent)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_wireless_tile),
                contentDescription = "Toggle Wireless Debugging",
                colorFilter = ColorFilter.tint(wirelessForeground),
                modifier = GlanceModifier.size(22.dp)
            )
        }

        Spacer(GlanceModifier.width(6.dp))

        // Gear icon
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .cornerRadius(22.dp)
                .background(gearBackground)
                .clickable(actionStartActivity<DevOptionsActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_settings_gear),
                contentDescription = "Open Developer Options",
                colorFilter = ColorFilter.tint(gearForeground),
                modifier = GlanceModifier.size(22.dp)
            )
        }
    }
}

class LoopholeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LoopholeWidget()
}
