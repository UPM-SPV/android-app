package gf.gab.upm.spv.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BatteryUnknown
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoiseAware
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import gf.gab.upm.spv.R
import gf.gab.upm.spv.ble.IBleClient
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ble.SpvBleManager
import gf.gab.upm.spv.ble.SpvDevice
import gf.gab.upm.spv.ble.spv.SpvDeviceSettings
import gf.gab.upm.spv.ble.spv.SpvEnvironmentalMeasurements
import gf.gab.upm.spv.db.SpvDeviceInfo
import gf.gab.upm.spv.ui.shader.drawWithFbmBackground
import gf.gab.upm.spv.ui.shader.drawWithFbmForeground
import gf.gab.upm.spv.ui.theme.HumidityGradient
import gf.gab.upm.spv.ui.theme.TemperatureGradient
import gf.gab.upm.spv.ui.theme.customColors
import gf.gab.upm.spv.ui.widget.MagicBall
import gf.gab.upm.spv.ui.widget.RingParameters
import gf.gab.upm.spv.ui.widget.WaveParameters
import gf.gab.upm.spv.ui.widget.WobbleIcon
import gf.gab.upm.spv.util.bleCheck

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onPairClicked: () -> Unit = {},
    onDeviceDetailsClicked: (MacAddress) -> Unit = {},
) {
    val devices by viewModel.devices.collectAsState()

    val dialogDevice = remember { mutableStateOf<DashboardSpvDevice?>(null) }
    val deviceSettings by viewModel.currentDeviceSettings.collectAsState()
    val deviceInfo by viewModel.currentDeviceInfo.collectAsState()

    bleCheck(viewModel.bleStatus()) { viewModel.startScan() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if(viewModel.bleStatus() == IBleClient.BleSupport.SUPPORTED) { viewModel.startScan() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.stopScan()
    }

    Scaffold(
        bottomBar = {
            BottomBar(
                suggestPairing = devices.isEmpty(),
                onPairClicked = onPairClicked,
            )
        }
    ) { contentPadding ->
        dialogDevice.value?.let {
            DeviceDialog(
                onDismiss = {
                    dialogDevice.value = null
                    viewModel.removeDeviceInfo()
                },
                friendlyName = deviceInfo?.friendlyName,
                settings = deviceSettings,
                onSave = { friendlyName, newSettings ->
                    deviceInfo?.macAddress?.let { address ->
                        viewModel.updateDeviceName(address, friendlyName)
                        newSettings?.let { newSettings ->
                            if(newSettings != deviceSettings) {
                                viewModel.updateDeviceSettings(address, newSettings)
                            }
                        }
                    }
                },
                onDelete = {
                    deviceInfo?.macAddress?.let { address ->
                        viewModel.deleteDevice(address)
                    }
                }
            )
        }
        if(devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.dashboard_no_devices),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(contentPadding)
            ) {
                items(items = devices, key = { it.device().info().macAddress.toLeBytes().toList() }) { device ->
                    DeviceCard(
                        device = device,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = tween(durationMillis = 200)
                            ),
                        onLongPress = {
                            dialogDevice.value = device
                            viewModel.fetchDeviceInfo(device)
                        },
                        onClick = {
                            onDeviceDetailsClicked(device.device().info().macAddress)
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun BottomBar(
    // Nudge the user to press the pair button if they have no saved devices.
    suggestPairing: Boolean = false,
    onPairClicked: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            ExtendedFloatingActionButton(
                modifier = Modifier.then(
                if(suggestPairing)
                        Modifier
                            .clip(CircleShape)
                            .drawWithFbmBackground(
                                colors = MaterialTheme.customColors.bluetoothGradient
                            )
                    else
                        Modifier
                ),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onPairClicked()
                },
                containerColor = if(suggestPairing) { Color.Transparent } else { MaterialTheme.colorScheme.primary },
                shape = CircleShape,
                icon = { Icon(Icons.Default.AddLink, stringResource(R.string.dashboard_bottom_bar_pair_text)) },
                text = { Text(stringResource(R.string.dashboard_bottom_bar_pair_text)) },
            )

        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DeviceDialog(
    onDismiss: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSave: (String, SpvDeviceSettings?) -> Unit = { _, _ -> },
    friendlyName: String? = null,
    settings: DashboardSpvDeviceSettings = DashboardSpvDeviceSettings.None
) {
    val haptics = LocalHapticFeedback.current
    val sensorSettings = remember(settings) { mutableStateOf(
        when(settings) {
            DashboardSpvDeviceSettings.Loading -> null
            DashboardSpvDeviceSettings.None -> null
            is DashboardSpvDeviceSettings.Settings -> settings.settings
        }
    )}

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.dashboard_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                )
                if(friendlyName == null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@Surface
                }

                val friendlyNameTextField = rememberTextFieldState(initialText = friendlyName)
                TextField(
                    state = friendlyNameTextField,
                    label = { Text(stringResource(R.string.dashboard_settings_friendly_name_description)) }
                )

                when(settings) {
                    DashboardSpvDeviceSettings.Loading -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    DashboardSpvDeviceSettings.None -> {}
                    is DashboardSpvDeviceSettings.Settings -> { sensorSettings.value?.let { settings ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.dashboard_settings_sensors_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SensorButton(
                                icon = Icons.Default.Thermostat,
                                isEnabled = settings.sensorSettings.temperature,
                                onClick = { sensorSettings.value = settings.toggleTemperature() }
                            )
                            SensorButton(
                                icon = Icons.Default.WaterDrop,
                                isEnabled = settings.sensorSettings.humidity,
                                onClick = { sensorSettings.value = settings.toggleHumidity() }
                            )
                            SensorButton(
                                icon = Icons.Default.LightMode,
                                isEnabled = settings.sensorSettings.luminosity,
                                onClick = { sensorSettings.value = settings.toggleLuminosity() }
                            )
                            SensorButton(
                                icon = Icons.Default.NoiseAware,
                                isEnabled = settings.sensorSettings.noise,
                                onClick = { sensorSettings.value = settings.toggleNoise() }
                            )
                            SensorButton(
                                icon = Icons.Default.BubbleChart,
                                isEnabled = settings.sensorSettings.voc,
                                onClick = { sensorSettings.value = settings.toggleVoc() }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = settings.samplingRateMinutes.toFloat(),
                                onValueChange = {
                                    sensorSettings.value =
                                        settings.copy(samplingRateMinutes = it.toInt())
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                },
                                valueRange = 1f..60f,
                                steps = 60,
                                // Sliders with too many steps render badly.
                                // Disable the individual ticks.
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        sliderState = sliderState,
                                        drawStopIndicator = null,
                                        drawTick = { _, _ -> }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Text(textAlign = TextAlign.Center, text = "${settings.samplingRateMinutes} min", modifier = Modifier.width(64.dp))
                        }
                    }}
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Reject)
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, stringResource(R.string.dashboard_settings_delete_button_text))
                        Text(stringResource(R.string.dashboard_settings_delete_button_text))
                    }

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSave(friendlyNameTextField.text.toString(), sensorSettings.value)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, stringResource(R.string.dashboard_settings_save_button_text))
                        Text(stringResource(R.string.dashboard_settings_save_button_text))
                    }
                }
            }
        }
    }
}

private fun SpvDeviceSettings.toggleTemperature(): SpvDeviceSettings = copy(
    sensorSettings = this.sensorSettings.copy(
        temperature = !this.sensorSettings.temperature,
        humidity = !this.sensorSettings.temperature,
        voc = !this.sensorSettings.temperature && this.sensorSettings.voc
    )
)

private fun SpvDeviceSettings.toggleHumidity() = copy(
    sensorSettings = this.sensorSettings.copy(
        humidity = !this.sensorSettings.humidity,
        temperature = !this.sensorSettings.humidity,
        voc = !this.sensorSettings.humidity && this.sensorSettings.voc
    )
)

private fun SpvDeviceSettings.toggleLuminosity() = copy(
    sensorSettings = this.sensorSettings.copy(
        luminosity = !this.sensorSettings.luminosity
    )
)

private fun SpvDeviceSettings.toggleNoise() = copy(
    sensorSettings = this.sensorSettings.copy(
        noise = !this.sensorSettings.noise
    )
)

private fun SpvDeviceSettings.toggleVoc() = copy(
    sensorSettings = this.sensorSettings.copy(
        temperature = !this.sensorSettings.voc || this.sensorSettings.temperature,
        humidity = !this.sensorSettings.voc || this.sensorSettings.humidity,
        voc = !this.sensorSettings.voc
    )
)

@Composable
private fun SensorButton(
    icon: ImageVector,
    isEnabled: Boolean = true,
    contentDescription: String? = null,
    onClick: () -> Unit = {},
) {
    val containerColor by animateColorAsState(
        targetValue = if(isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(500)
    )
    val contentColor by animateColorAsState(
        targetValue = if(isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(500)
    )
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            if(isEnabled)
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
            else
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onClick()
        },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

private val TEMPERATURE_COLOR_MAPPING = listOf(
    -10.0f,
    10.0f,
    20.0f,
    30.0f,
    35.0f,
)
private val HUMIDITY_COLOR_MAPPING = listOf(
    0f,
    25f,
    75f
)
@Composable
private fun DeviceCard(
    device: DashboardSpvDevice,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val (deviceInfo, isConnecting, isConnected) = when (device) {
        is DashboardSpvDevice.Connected -> Triple(device.spvDevice.info, false, true)
        is DashboardSpvDevice.Connecting -> Triple(device.spvDevice.info, true, false)
        is DashboardSpvDevice.Disconnected -> Triple(device.spvDevice.info, false, false)
    }

    val measurements = when (device) {
        is DashboardSpvDevice.Connected -> device.measurements
        else -> null
    }
    val batteryLevel = when (device) {
        is DashboardSpvDevice.Connected -> device.batteryLevel
        else -> null
    }

    val cardBorderColor by animateColorAsState(
        targetValue = if (isConnected) MaterialTheme.customColors.bluetoothLight.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(500)
    )
    val cardBackgroundColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF0A1128) else MaterialTheme.colorScheme.onBackground.copy(
            alpha = 0.2f
        ),
        animationSpec = tween(500)
    )
    val onBackgroundColor by animateColorAsState(
        targetValue =
            if (isConnected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        animationSpec = tween(500)
    )
    val sensorValuesColor by animateColorAsState(
        targetValue =
            if (isConnected) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(500)
    )
    val sensorValuesColorFull = MaterialTheme.colorScheme.onBackground

    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .then(modifier)
            .combinedClickable(
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onLongPress()
                },
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        border = BorderStroke(1.dp, cardBorderColor),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.height(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = deviceInfo.friendlyName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = onBackgroundColor,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.size(8.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isConnected || isConnecting) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val animationProgress by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        val batteryLevelIcons = listOf(
                            Icons.Default.Battery0Bar,
                            Icons.Default.Battery1Bar,
                            Icons.Default.Battery2Bar,
                            Icons.Default.Battery3Bar,
                            Icons.Default.Battery4Bar,
                            Icons.Default.Battery5Bar,
                            Icons.Default.Battery6Bar,
                            Icons.Default.BatteryFull,
                        )
                        val batteryIcon = when (batteryLevel) {
                            is Int -> {
                                val percentage = batteryLevel.toDouble() / 100
                                val index = (percentage * batteryLevelIcons.size).toInt()
                                val indexClamped = index.coerceIn(0, batteryLevelIcons.size - 1)
                                batteryLevelIcons[indexClamped]
                            }

                            else -> Icons.AutoMirrored.Filled.BatteryUnknown
                        }
                        AnimatedContent(
                            targetState = batteryLevel,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                            }
                        ) { batteryLevel ->
                            Text(
                                text = when (batteryLevel) {
                                    null -> "--%"
                                    else -> String.format("%d%%", batteryLevel)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = if (batteryLevel == null) sensorValuesColor else sensorValuesColorFull
                                ),
                            )
                        }
                        AnimatedContent(
                            targetState = batteryIcon,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                            }
                        ) { batteryIcon ->
                            Icon(
                                imageVector = batteryIcon,
                                contentDescription = null,
                                tint = if (batteryLevel == null) sensorValuesColor else sensorValuesColorFull,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint =
                                if (isConnecting)
                                    lerp(
                                        MaterialTheme.customColors.bluetoothLight,
                                        MaterialTheme.customColors.bluetoothDark,
                                        animationProgress
                                    )
                                else MaterialTheme.customColors.bluetoothLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(4.dp))

            AnimatedContent(
                targetState = measurements,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
            ) { measurements ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text =
                                when (measurements?.temperatureCelsius) {
                                    null -> "--°C"
                                    else -> String.format("%.1f°C", measurements.temperatureCelsius)
                                },
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (measurements?.temperatureCelsius == null) sensorValuesColor else sensorValuesColorFull
                            ),
                            modifier = Modifier.then(
                                if(measurements?.temperatureCelsius != null) {
                                    val (colorStart, colorEnd) = calculateFbmColorPair(
                                        measurements.temperatureCelsius.toFloat(),
                                        5f,
                                        MaterialTheme.customColors.temperatureGradient,
                                        TEMPERATURE_COLOR_MAPPING
                                    )
                                    modifier.drawWithFbmForeground(
                                        listOf(colorStart, colorEnd)
                                    )
                                }
                                else Modifier
                            )
                        )
                        Text(
                            text =
                                when (measurements?.humidityRh) {
                                    null -> "-- %"
                                    else -> String.format("%.1f %%", measurements.humidityRh)
                                },
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (measurements?.humidityRh == null) sensorValuesColor else sensorValuesColorFull
                            ),
                            modifier = Modifier.then(
                                if (measurements?.humidityRh != null) {
                                    val (colorStart, colorEnd) = calculateFbmColorPair(
                                        measurements.humidityRh.toFloat(),
                                        25f,
                                        MaterialTheme.customColors.humidityGradient,
                                        HUMIDITY_COLOR_MAPPING
                                    )
                                    modifier.drawWithFbmForeground(
                                        listOf(colorStart, colorEnd)
                                    )
                                }
                                else Modifier
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (measurements?.decibels) {
                                null -> "-- dB"
                                else -> String.format("%.1f dB", measurements.decibels)
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (measurements?.decibels == null) sensorValuesColor else sensorValuesColorFull
                            ),
                        )

                        Text(
                            text = when (measurements?.lux) {
                                null -> "-- lx"
                                else -> String.format("%.1f lx", measurements.lux)
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (measurements?.lux == null) sensorValuesColor else sensorValuesColorFull
                            ),
                        )

                        Text(
                            text = when (measurements?.vocRaw) {
                                null -> "-- VOC"
                                else -> "${"%,d".format(measurements.vocRaw)} VOC"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                color = if (measurements?.vocRaw == null) sensorValuesColor else sensorValuesColorFull
                            ),
                        )
                    }
                }
            }
        }
    }
}


private fun calculateFbmColorPair(value: Float, drift: Float, colors: List<Color>, mappings: List<Float>): Pair<Color, Color> {
    // Find where the value falls in the color values and return a weighted interpolated pair
    // of colors:
    //
    // The idea is, if `value` is closer to V2 than V3, return:
    // V1                V2                V3                V4
    // | --------------- | --------------- | --------------- |
    //               ^------^------^
    //               C1     value  C2
    //
    // Where the breadth of the range is given by `drift`.
    //
    //
    // If the range falls out of bounds, use the limit itself:
    // V1                V2                V3                V4
    // | --------------- | --------------- | --------------- |
    // ^--^------^
    // C1 value  C2

    // Find which range the current value is in:
    val (leftIndex, leftWeight) = getValueRange(value - drift, mappings)
    val (rightIndex, rightWeight) = getValueRange(value + drift, mappings)

    // Get each edge's colors.
    val (leftStartColor, leftEndColor) = Pair(colors[leftIndex], colors[leftIndex + 1])
    val (rightStartColor, rightEndColor) = Pair(colors[rightIndex], colors[rightIndex + 1])

    // Interpolate the colors.
    val leftColor = lerp(leftStartColor, leftEndColor, leftWeight)
    val rightColor = lerp(rightStartColor, rightEndColor, rightWeight)

    return Pair(leftColor, rightColor)
}

/**
 * Returns which range the value is in, as well as how far inside the range it is.
 */
private fun getValueRange(value: Float, mappings: List<Float>): Pair<Int, Float> {
    if(mappings.first() > value) {
        return Pair(0, 0.0f)
    }
    if(mappings.last() < value) {
        return Pair(mappings.size - 2, 1.0f)
    }

    val rangeIndex = mappings.indexOfFirst { rangeStart -> rangeStart > value } - 1
    val rangeStart = mappings[rangeIndex]
    val rangeEnd = mappings[rangeIndex + 1]

    val weight = (value - rangeStart) / (rangeEnd - rangeStart)

    return Pair(rangeIndex, weight)
}

@Preview
@Composable
private fun DeviceCardP1() {
    DeviceCard(
        device = DashboardSpvDevice.Disconnected(
            spvDevice = SpvDevice.Disconnected(
                info = SpvDeviceInfo(
                    macAddress = MacAddress.fromString("FF:FF:FF:FF:FF:FF"),
                    friendlyName = "TestName",
                    serialNumber = "",
                )
            )
        )
    )
}

@Preview
@Composable
private fun DeviceCardP2() {
    DeviceCard(
        device = DashboardSpvDevice.Connecting(
            spvDevice = SpvDevice.Connecting(
                info = SpvDeviceInfo(
                    macAddress = MacAddress.fromString("FF:FF:FF:FF:FF:FF"),
                    friendlyName = "TestName",
                    serialNumber = "",
                ),
                manager = SpvBleManager(LocalContext.current)
            )
        )
    )
}

@Preview
@Composable
private fun DeviceCardP3() {
    DeviceCard(
        device = DashboardSpvDevice.Connected(
            spvDevice = SpvDevice.Connected(
                info = SpvDeviceInfo(
                    macAddress = MacAddress.fromString("FF:FF:FF:FF:FF:FF"),
                    friendlyName = "TestName",
                    serialNumber = "",
                ),
                manager = SpvBleManager(LocalContext.current)
            ),
            measurements = null,
            batteryLevel = null
        )
    )
}

@Preview
@Composable
private fun DeviceCardP4() {
    DeviceCard(
        device = DashboardSpvDevice.Connected(
            spvDevice = SpvDevice.Connected(
                info = SpvDeviceInfo(
                    macAddress = MacAddress.fromString("FF:FF:FF:FF:FF:FF"),
                    friendlyName = "TestName",
                    serialNumber = "",
                ),
                manager = SpvBleManager(LocalContext.current)
            ),
            measurements = SpvEnvironmentalMeasurements(
                temperatureCelsius = 25.0,
                humidityRh = 0.23,
                lux = 200.0,
                decibels = 70.0,
                vocRaw = 400
            ),
            batteryLevel = 50
        )
    )
}
