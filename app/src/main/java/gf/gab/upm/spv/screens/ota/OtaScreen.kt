package gf.gab.upm.spv.screens.ota

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import gf.gab.upm.spv.R
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ota.OtaInfo
import gf.gab.upm.spv.ui.theme.customColors
import gf.gab.upm.spv.ui.widget.MagicBall
import gf.gab.upm.spv.ui.widget.RingParameters
import gf.gab.upm.spv.ui.widget.WaveParameters
import gf.gab.upm.spv.ui.widget.WobbleIcon

@Composable
fun OtaScreen(
    viewModel: OtaViewModel = hiltViewModel(),
    device: MacAddress,
    otaInfo: OtaInfo,
    onDismiss: () -> Unit = {},
) {
    val otaStatus by viewModel.otaStatus.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.startOta(device, otaInfo)
    }

    val downloadProgress = remember { mutableFloatStateOf(0.0f) }
    when(otaStatus) {
        is OtaStatus.Downloading -> { downloadProgress.floatValue = (otaStatus as OtaStatus.Downloading).progress }
        OtaStatus.Connecting -> { downloadProgress.floatValue = 0.0f }
        else -> { downloadProgress.floatValue = 1.0f }
    }

    val dfuProgress = remember { mutableFloatStateOf(0.0f) }
    when(otaStatus) {
        is OtaStatus.DfuUpdating -> { dfuProgress.floatValue = (otaStatus as OtaStatus.DfuUpdating).progress }
        OtaStatus.Connecting -> { dfuProgress.floatValue = 0.0f }
        OtaStatus.DfuValidating -> { dfuProgress.floatValue = 1.0f }
        OtaStatus.Done -> { dfuProgress.floatValue = 1.0f }

        else -> {}
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(Modifier.size(64.dp))

            ProgressIndicator(
                text = otaStatus.getProgressText(),
                outerProgress = downloadProgress.floatValue,
                innerProgress = dfuProgress.floatValue,
                status = otaStatus
            )

            Spacer(modifier = Modifier.size(16.dp))

            StatusText(otaStatus)

            Spacer(modifier = Modifier.weight(1f))

            InteractionButton(
                status = otaStatus,
                onDismiss = onDismiss,
                onRetry = { viewModel.startOta(device, otaInfo) }
            )

            Spacer(modifier = Modifier.size(128.dp))
        }
    }
}

@Composable
private fun ProgressIndicator(
    text: String,
    outerProgress: Float,
    innerProgress: Float,
    status: OtaStatus,
) {

    val foregroundWaveParameters = if(!status.isError()) {
        WaveParameters(
            palette = status.progressColor(),
            amplitude = 20f,
            frequency = 1f,
            speed = 2f,
        )
    } else {
        WaveParameters(
            palette = status.progressColor(),
            amplitude = 0f,
            frequency = 0f,
            speed = 0f,
        )
    }

    val backgroundWaveParameters = if(!status.isError()) {
        WaveParameters(
            palette = status.progressColorSolid(),
            amplitude = 20f,
            frequency = 1f,
            speed = -2f,
        )
    } else {
        WaveParameters(
            palette = status.progressColorSolid(),
            amplitude = 0f,
            frequency = 0f,
            speed = 0f,
        )
    }

    val fontSize = if(status.isError()) {
        MaterialTheme.typography.headlineLarge.copy(fontSize = 20.em)
    } else {
        MaterialTheme.typography.headlineLarge.copy(fontSize = 10.em)
    }

    MagicBall(
        modifier = Modifier.size(300.dp),
        ringProgress = outerProgress,
        ballProgress = innerProgress,
        ringParameters = RingParameters(
            palette = status.progressColor(),
            thickness = 10.dp
        ),
        foregroundWave = foregroundWaveParameters,
        backgroundWave = backgroundWaveParameters
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when(status) {
                OtaStatus.Done -> {
                    WobbleIcon(
                        modifier = Modifier.size(128.dp),
                        tint = Color.White,
                        imageVector = Icons.Default.Check
                    )
                }
                else -> {
                    Text(
                        textAlign = TextAlign.Center,
                        text = text,
                        style = fontSize,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusText(
    status: OtaStatus,
) {
    val string = when(status) {
        OtaStatus.Connecting -> stringResource(R.string.ota_status_connecting)
        OtaStatus.ConnectionError -> stringResource(R.string.ota_status_connection_error)

        is OtaStatus.Downloading -> stringResource(R.string.ota_status_downloading)
        OtaStatus.DownloadError -> stringResource(R.string.ota_status_download_error)

        OtaStatus.DfuStarting -> stringResource(R.string.ota_status_dfu_starting)
        is OtaStatus.DfuUpdating -> stringResource(R.string.ota_status_dfu_updating)
        OtaStatus.DfuValidating -> stringResource(R.string.ota_status_dfu_validating)
        OtaStatus.DfuError -> stringResource(R.string.ota_status_dfu_error)

        OtaStatus.Done -> stringResource(R.string.ota_status_done)
    }

    AnimatedContent(
        targetState = string,
        label = "StatusText"
    ) { text ->
        Text(
            textAlign = TextAlign.Center,
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InteractionButton(
    status: OtaStatus,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {

    val isVisible = when {
        status is OtaStatus.Done -> true
        status.isError() -> true
        else -> false
    }

    val containerColor = when {
        status.isError() -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        status.isError() -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val text = when {
        status.isError() -> stringResource(R.string.ota_button_retry)
        else -> stringResource(R.string.ota_button_dismiss)
    }

    AnimatedVisibility(isVisible) {
        Button(
            onClick = {
                if(status.isError()) {
                    onRetry()
                } else {
                    onDismiss()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
        ) {
            Text(text)
        }
    }
}

@Composable
private fun OtaStatus.progressColor(): List<Color> = when {
    this.isError() -> MaterialTheme.customColors.magicBallErrorGradient
    else -> MaterialTheme.customColors.magicBallProgressGradient
}


@Composable
private fun OtaStatus.progressColorSolid(): List<Color> = listOf(when {
    this.isError() ->  MaterialTheme.customColors.magicBallErrorDark
    else -> MaterialTheme.customColors.magicBallProgressDark
})

private fun OtaStatus.getProgressText(): String = when(this) {
    OtaStatus.Connecting, OtaStatus.DfuStarting -> "..."
    OtaStatus.DfuError, OtaStatus.DownloadError, OtaStatus.ConnectionError -> "!"

    is OtaStatus.DfuUpdating -> "%.1f%%".format(this.progress * 100)
    is OtaStatus.Downloading -> "%.1f%%".format(this.progress * 100)
    OtaStatus.DfuValidating -> "100%"

    OtaStatus.Done -> "✓"
}

private fun OtaStatus.isError(): Boolean = when(this) {
    OtaStatus.DfuError, OtaStatus.DownloadError, OtaStatus.ConnectionError -> true
    else -> false
}
