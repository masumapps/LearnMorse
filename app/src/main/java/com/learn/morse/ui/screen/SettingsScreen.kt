package com.learn.morse.ui.screen

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.learn.morse.R
import com.learn.morse.audio.Haptics
import com.learn.morse.audio.MorsePlayer
import com.learn.morse.audio.SoundSettings
import com.learn.morse.audio.Torch
import com.learn.morse.language.AppLanguage
import com.learn.morse.monetization.Ads
import com.learn.morse.monetization.Billing
import com.learn.morse.monetization.Entitlements
import com.learn.morse.reminder.DailyReminder
import com.learn.morse.ui.theme.AppTheme
import com.learn.morse.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.Calendar

// "MORSE" — played by the test button.
private const val TEST_MORSE = "-- --- .-. ... ."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onPrivacyPolicyClick: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(SoundSettings.load(context)) }
    fun update(new: SoundSettings) {
        settings = new
        new.save(context)
    }

    val scope = rememberCoroutineScope()
    var testJob by remember { mutableStateOf<Job?>(null) }
    val isPlaying = testJob?.isActive == true

    Scaffold(
        // Shows the app's Morse pattern background, drawn in MainActivity.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PremiumSection()

            Text(stringResource(R.string.settings_sound), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            SettingSlider(
                label = stringResource(R.string.settings_tone),
                valueText = stringResource(R.string.settings_hz, settings.frequency),
                value = settings.frequency.toFloat(),
                range = SoundSettings.FREQUENCY_RANGE.first.toFloat()..SoundSettings.FREQUENCY_RANGE.last.toFloat(),
                steps = (SoundSettings.FREQUENCY_RANGE.last - SoundSettings.FREQUENCY_RANGE.first) / 50 - 1,
                onChange = { update(settings.copy(frequency = it.roundToInt())) }
            )
            SettingSlider(
                label = stringResource(R.string.settings_speed),
                valueText = stringResource(R.string.wpm_value, settings.wpm),
                value = settings.wpm.toFloat(),
                range = SoundSettings.WPM_RANGE.first.toFloat()..SoundSettings.WPM_RANGE.last.toFloat(),
                steps = SoundSettings.WPM_RANGE.last - SoundSettings.WPM_RANGE.first - 1,
                onChange = {
                    val wpm = it.roundToInt()
                    // Farnsworth can't be faster than the letters themselves; equal means off.
                    update(settings.copy(wpm = wpm, farnsworthWpm = settings.farnsworthWpm?.takeIf { f -> f < wpm }))
                }
            )
            SettingSlider(
                label = stringResource(R.string.settings_farnsworth),
                valueText = settings.farnsworthWpm?.let { stringResource(R.string.settings_farnsworth_value, it) }
                    ?: stringResource(R.string.settings_off),
                description = stringResource(R.string.settings_farnsworth_desc),
                value = (settings.farnsworthWpm ?: settings.wpm).toFloat(),
                // At the slowest speed there's no slower overall speed; keep a valid range while disabled.
                range = SoundSettings.WPM_RANGE.first.toFloat()..maxOf(settings.wpm, SoundSettings.WPM_RANGE.first + 1).toFloat(),
                steps = (settings.wpm - SoundSettings.WPM_RANGE.first - 1).coerceAtLeast(0),
                enabled = settings.wpm > SoundSettings.WPM_RANGE.first,
                onChange = { value ->
                    val wpm = value.roundToInt()
                    update(settings.copy(farnsworthWpm = wpm.takeIf { it < settings.wpm }))
                }
            )
            SettingSlider(
                label = stringResource(R.string.settings_volume),
                valueText = "${(settings.volume * 100).roundToInt()}%",
                value = settings.volume,
                range = 0.1f..1f,
                onChange = { update(settings.copy(volume = it)) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { update(SoundSettings()) }) { Text(stringResource(R.string.settings_reset)) }
                Button(
                    onClick = {
                        testJob?.cancel()
                        if (!isPlaying) {
                            testJob = scope.launch {
                                MorsePlayer.play(context, TEST_MORSE, settings)
                            }.also { job -> job.invokeOnCompletion { if (testJob == job) testJob = null } }
                        } else {
                            testJob = null
                        }
                    }
                ) {
                    Icon(if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (isPlaying) R.string.stop else R.string.settings_test))
                }
            }

            SignalOutputs(settings, onChange = ::update)

            ReminderSection()

            ThemeSection()

            LanguageSection()

            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onPrivacyPolicyClick) { Text(stringResource(R.string.privacy_policy)) }
        }
    }
}

/** Premium status, or the offer to buy it, plus restoring a purchase and changing ad consent. */
@Composable
private fun PremiumSection() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isPremium by Entitlements.isPremium.collectAsState()
    val price by Billing.premiumPrice.collectAsState()

    Text(stringResource(R.string.settings_premium), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    if (isPremium) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = StarColor)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.settings_premium_active), style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        PremiumBenefits()
        PremiumButton(price = price, onClick = { activity?.let { Billing.buyPremium(it) } })
        TextButton(onClick = { Billing.refreshPurchases() }) { Text(stringResource(R.string.settings_restore)) }
    }
    if (activity != null && Ads.isPrivacyOptionsRequired(context)) {
        TextButton(onClick = { Ads.showPrivacyOptions(activity) }) { Text(stringResource(R.string.settings_ad_privacy)) }
    }
}

/**
 * Which outputs carry the morse: sound, vibration, screen flash and the flashlight (Premium). At least
 * one stays on, so lessons always have a signal to recognize.
 */
@Composable
private fun SignalOutputs(settings: SoundSettings, onChange: (SoundSettings) -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isPremium by Entitlements.isPremium.collectAsState()
    val hasVibrator = remember { Haptics.isAvailable(context) }
    val hasTorch = remember { Torch.isAvailable(context) }
    fun change(new: SoundSettings) {
        val torchOn = new.flashlight && isPremium
        if (new.sound || new.vibrate || new.screenFlash || torchOn) onChange(new)
    }

    Text(stringResource(R.string.settings_signal), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(
        stringResource(R.string.settings_signal_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column {
        OutputSwitch(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.settings_sound), null, settings.sound) {
            change(settings.copy(sound = it))
        }
        if (hasVibrator) {
            OutputSwitch(Icons.Filled.Vibration, stringResource(R.string.settings_vibration), stringResource(R.string.settings_vibration_desc), settings.vibrate) {
                change(settings.copy(vibrate = it))
            }
        }
        OutputSwitch(
            Icons.Filled.LightMode,
            stringResource(R.string.settings_screen_flash),
            stringResource(R.string.settings_screen_flash_desc),
            settings.screenFlash,
        ) { change(settings.copy(screenFlash = it)) }
        if (hasTorch) {
            OutputSwitch(
                Icons.Filled.FlashlightOn,
                stringResource(R.string.settings_flashlight),
                stringResource(if (isPremium) R.string.settings_flashlight_desc else R.string.settings_premium),
                settings.flashlight && isPremium,
                locked = !isPremium,
            ) {
                if (isPremium) change(settings.copy(flashlight = it))
                else activity?.let { a -> Billing.buyPremium(a) }
            }
        }
    }
}

/** The daily reminder: on or off, and at what time. Turning it on asks for notifications on Android 13+. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(DailyReminder.isEnabled(context) && DailyReminder.canNotify(context)) }
    var hour by remember { mutableIntStateOf(DailyReminder.hour(context)) }
    var minute by remember { mutableIntStateOf(DailyReminder.minute(context)) }
    var pickingTime by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            DailyReminder.enable(context, hour, minute)
            enabled = true
        }
    }
    val time = remember(hour, minute) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        DateFormat.getTimeFormat(context).format(calendar.time)
    }

    Text(stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Column {
        OutputSwitch(
            Icons.Filled.NotificationsActive,
            stringResource(R.string.settings_daily_reminder),
            stringResource(R.string.settings_reminder_desc),
            enabled,
        ) { on ->
            when {
                !on -> {
                    DailyReminder.disable(context)
                    enabled = false
                }
                DailyReminder.canNotify(context) -> {
                    DailyReminder.enable(context, hour, minute)
                    enabled = true
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickingTime = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.settings_time), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(time, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (pickingTime) {
        val state = rememberTimePickerState(hour, minute, is24Hour = DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    DailyReminder.enable(context, hour, minute)
                    pickingTime = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state = state) },
        )
    }
}

/** Light, dark, or following the phone; the app redraws in it right away. */
@Composable
private fun ThemeSection() {
    val context = LocalContext.current
    val current by AppTheme.mode.collectAsState()
    val options = listOf(
        ThemeMode.SYSTEM to R.string.settings_theme_system,
        ThemeMode.LIGHT to R.string.settings_theme_light,
        ThemeMode.DARK to R.string.settings_theme_dark,
    )
    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = mode == current,
                onClick = { AppTheme.set(context, mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

/** The app's language; picking one redraws the app in it. */
@Composable
private fun LanguageSection() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val current = remember { AppLanguage.current(context) }
    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AppLanguage.supported.forEachIndexed { index, (tag, name) ->
            SegmentedButton(
                selected = tag == current,
                onClick = { activity?.let { AppLanguage.set(it, tag) } },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AppLanguage.supported.size),
            ) { Text(name) }
        }
    }
}

@Composable
private fun OutputSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    locked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (locked) {
            Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.settings_premium), tint = MaterialTheme.colorScheme.primary)
        } else {
            // The row handles the toggle, so the switch itself isn't separately focusable.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    description: String? = null,
    enabled: Boolean = true,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps, enabled = enabled)
    }
}
