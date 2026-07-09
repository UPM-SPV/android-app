package gf.gab.upm.spv.screens.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import gf.gab.upm.spv.R
import gf.gab.upm.spv.ble.IBleClient
import gf.gab.upm.spv.nfc.AndroidNfcScanner
import gf.gab.upm.spv.nfc.INfcScanner
import gf.gab.upm.spv.ui.shader.drawWithFbmForeground
import gf.gab.upm.spv.ui.shader.drawWithFbmBackground
import gf.gab.upm.spv.ui.theme.BluetoothDark
import gf.gab.upm.spv.ui.theme.BluetoothLight
import gf.gab.upm.spv.ui.theme.customColors
import gf.gab.upm.spv.ui.widget.WaveCircles
import gf.gab.upm.spv.ui.widget.WobbleIcon
import gf.gab.upm.spv.util.AnimatedCheckmarkState
import gf.gab.upm.spv.util.AnimatedSpinnerCheckmark
import gf.gab.upm.spv.util.getActivity
import kotlin.random.Random


@OptIn(ExperimentalPermissionsApi::class)
@Composable
@Preview
fun PairingScreen(
    viewModel: PairingViewModel = hiltViewModel(),
    onDone: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.getActivity()
    val nfcScanner = remember(activity) {
        activity?.let { AndroidNfcScanner(activity) }
    }
    LaunchedEffect(nfcScanner) {
        nfcScanner?.let { viewModel.scanNfc(it) }
    }

    Scaffold { _ ->
        AnimatedContent(
            targetState = uiState,
            contentKey = { state ->
                when(state) {
                    is PairingUiState.BleScanning,
                    is PairingUiState.BleConnecting,
                    is PairingUiState.BlePairing,
                    is PairingUiState.BleAttesting -> "BleScreen"

                    is PairingUiState.NfcWaiting,
                    is PairingUiState.NfcReading -> "NfcScreen"

                    else -> state::class.simpleName
                }
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
        ) { state ->
            when(state) {
                is PairingUiState.NfcSupportError -> {
                    NfcErrorScreen(state) {
                        nfcScanner?.let { viewModel.scanNfc(it) }
                    }
                }

                is PairingUiState.NfcWaiting, is PairingUiState.NfcReading -> {
                    NfcScanScreen(state)
                }

                is PairingUiState.BleSupportError -> {
                    BleSupportErrorScreen(state) {
                        viewModel.startBleScan()
                    }
                }

                is PairingUiState.BleScanning, is PairingUiState.BleConnecting,
                is PairingUiState.BlePairing, is PairingUiState.BleAttesting-> {
                    BleScanScreen(state)
                }

                is PairingUiState.BleAttestationError -> {
                    BleAttestationErrorScreen { viewModel.startBleScan() }
                }
                is PairingUiState.BleConnectionError -> {
                    BleConnectionErrorScreen { viewModel.startBleScan() }
                }
                PairingUiState.Done -> {
                    DoneScreen(onDone)
                }
            }
        }
    }
}


@Composable
@Preview
private fun ErrorScreenTemplate(
    titleText: String = "ERROR TITLE",
    bodyText: String = "ERROR_BODY",
    buttonText: String = "BUTTON_TEXT",
    onButtonPressed: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            WobbleIcon(
                modifier = Modifier.size(96.dp),
                imageVector = Icons.Default.Error,
                tint = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.size(16.dp))

        Button(
            onClick = onButtonPressed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp, end = 16.dp),
            colors = ButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                disabledContainerColor = MaterialTheme.colorScheme.onErrorContainer,
                disabledContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
private fun DoneScreen(
    onButtonPressed: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(192.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            WobbleIcon(
                modifier = Modifier.size(164.dp),
                imageVector = Icons.Default.Stars,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.pairing_screen_done_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.size(16.dp))

        Button(
            onClick = onButtonPressed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp, end = 16.dp),
            colors = ButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.pairing_screen_done_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
private fun NfcErrorScreen(
    error: PairingUiState.NfcSupportError =
        PairingUiState.NfcSupportError(INfcScanner.NfcSupport.NOT_SUPPORTED),
    onButtonPressed: () -> Unit = {}
) {
    val bodyText = when(error.errorReason) {
        INfcScanner.NfcSupport.SUPPORTED -> ""
        INfcScanner.NfcSupport.DISABLED -> stringResource(R.string.pairing_screen_nfc_error_disabled)
        INfcScanner.NfcSupport.NO_PERMISSION -> stringResource(R.string.pairing_screen_nfc_error_no_permission)
        INfcScanner.NfcSupport.NOT_SUPPORTED -> stringResource(R.string.pairing_screen_nfc_error_not_supported)
    }

    ErrorScreenTemplate(
        titleText = stringResource(R.string.pairing_screen_nfc_error_title),
        bodyText = bodyText,
        buttonText = stringResource(R.string.pairing_screen_nfc_error_retry),
        onButtonPressed = onButtonPressed
    )
}

@Composable
private fun BleSupportErrorScreen(
    error: PairingUiState.BleSupportError,
    onButtonPressed: () -> Unit = {}
) {
    val bodyText = when(error.errorReason) {
        IBleClient.BleSupport.SUPPORTED -> ""
        IBleClient.BleSupport.DISABLED -> stringResource(R.string.pairing_screen_ble_support_error_disabled)
        IBleClient.BleSupport.NO_PERMISSION -> stringResource(R.string.pairing_screen_ble_support_error_no_permission)
        IBleClient.BleSupport.NOT_SUPPORTED -> stringResource(R.string.pairing_screen_ble_support_error_not_supported)
    }
    ErrorScreenTemplate(
        titleText = stringResource(R.string.pairing_screen_ble_support_error_title),
        bodyText = bodyText,
        buttonText = stringResource(R.string.pairing_screen_ble_support_error_retry),
        onButtonPressed = onButtonPressed
    )
}


@Composable
private fun BleConnectionErrorScreen(
    onButtonPressed: () -> Unit = {}
) {
    ErrorScreenTemplate(
        titleText = stringResource(R.string.pairing_screen_ble_connect_error_title),
        bodyText = stringResource(R.string.pairing_screen_ble_connect_error_body),
        buttonText = stringResource(R.string.pairing_screen_ble_connect_error_retry),
        onButtonPressed = onButtonPressed
    )
}

@Composable
private fun BleAttestationErrorScreen(
    onButtonPressed: () -> Unit = {}
) {
    ErrorScreenTemplate(
        titleText = stringResource(R.string.pairing_screen_ble_attest_error_title),
        bodyText = stringResource(R.string.pairing_screen_ble_attest_error_body),
        buttonText = stringResource(R.string.pairing_screen_ble_attest_error_retry),
        onButtonPressed = onButtonPressed
    )
}



@Composable
@Preview
private fun NfcScanScreen(
    state: PairingUiState = PairingUiState.NfcReading
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            WobbleIcon(
                modifier = Modifier.size(96.dp),
                imageVector = Icons.Default.Nfc
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.pairing_screen_nfc_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.pairing_screen_nfc_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.height(16.dp)
        ) {
            if(state is PairingUiState.NfcWaiting) {
                state.error?.let { error ->
                    val text = when(error) {
                        PairingUiState.NfcTagError.TagReadingError -> stringResource(R.string.pairing_screen_nfc_error_reading_tag)
                        PairingUiState.NfcTagError.UnknownTag -> stringResource(R.string.pairing_screen_nfc_error_unknown_tag)
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.height(32.dp)
        ) {
            if(state is PairingUiState.NfcReading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
private fun BleScanScreen(
    state: PairingUiState = PairingUiState.NfcReading
) {
    // Synchronize the icon's shader and the wave's.
    val shaderRandom by remember { mutableDoubleStateOf(Random.nextDouble() * 10000f) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(256.dp),
            contentAlignment = Alignment.Center
        )
        {
            WaveCircles(
                modifier = Modifier
                    .wrapContentSize(Alignment.Center, unbounded = true)
                    .drawWithFbmForeground(
                        timeStart = shaderRandom,
                        colors = MaterialTheme.customColors.bluetoothGradient
                    ),
                maxRadius = 128.dp,
                minRadius = 64.dp,
                waveCount = 3
            )
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .drawWithFbmBackground(
                        timeStart = shaderRandom,
                        colors = MaterialTheme.customColors.bluetoothGradient
                    ),
                contentAlignment = Alignment.Center
            ) {
                WobbleIcon(
                    modifier = Modifier.size(96.dp),
                    imageVector = Icons.Default.Bluetooth,
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.pairing_screen_ble_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.pairing_screen_ble_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.size(16.dp))

        Column {
            ProgressStep(
                text = stringResource(R.string.pairing_screen_ble_step_scan),
                state = when(state) {
                    is PairingUiState.BleScanning -> AnimatedCheckmarkState.Spinning
                    else -> AnimatedCheckmarkState.Complete
                },
            )
            ProgressStep(
                text = stringResource(R.string.pairing_screen_ble_step_connect),
                state = when(state) {
                    is PairingUiState.BleScanning -> AnimatedCheckmarkState.Inactive
                    is PairingUiState.BleConnecting -> AnimatedCheckmarkState.Spinning
                    else -> AnimatedCheckmarkState.Complete
                },
            )
            ProgressStep(
                text = stringResource(R.string.pairing_screen_ble_step_pair),
                state = when(state) {
                    is PairingUiState.BleScanning -> AnimatedCheckmarkState.Inactive
                    is PairingUiState.BleConnecting -> AnimatedCheckmarkState.Inactive
                    is PairingUiState.BlePairing -> AnimatedCheckmarkState.Spinning
                    else -> AnimatedCheckmarkState.Complete
                },
            )
            ProgressStep(
                text = stringResource(R.string.pairing_screen_ble_step_attest),
                state = when(state) {
                    is PairingUiState.BleScanning -> AnimatedCheckmarkState.Inactive
                    is PairingUiState.BleConnecting -> AnimatedCheckmarkState.Inactive
                    is PairingUiState.BlePairing -> AnimatedCheckmarkState.Inactive
                    is PairingUiState.BleAttesting -> AnimatedCheckmarkState.Spinning
                    else -> AnimatedCheckmarkState.Complete
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
private fun ProgressStep(
    state: AnimatedCheckmarkState = AnimatedCheckmarkState.Inactive,
    text: String = "Test string",
) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedSpinnerCheckmark(modifier = Modifier.size(32.dp), state = state)
        Spacer(modifier = Modifier.size(16.dp))

        val textColor = if(state == AnimatedCheckmarkState.Inactive) {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.33f)
        } else {
            MaterialTheme.colorScheme.onBackground
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
