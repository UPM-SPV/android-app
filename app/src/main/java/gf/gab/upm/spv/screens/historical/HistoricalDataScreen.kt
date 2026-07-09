package gf.gab.upm.spv.screens.historical

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.commandiron.wheel_picker_compose.core.WheelTextPicker
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import gf.gab.upm.spv.R
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.db.SpvSensorData
import gf.gab.upm.spv.ota.OtaInfo
import gf.gab.upm.spv.ui.shader.drawWithFbmBackground
import gf.gab.upm.spv.ui.theme.customColors
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Composable
fun HistoricalDataScreen(
    device: MacAddress,
    viewModel: HistoricalDataViewModel = hiltViewModel(),
    onUpdateClicked: (MacAddress, OtaInfo) -> Unit = {_,_ -> }
) {

    val deviceData by viewModel.sensorData.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    var timeGranularity by remember { mutableStateOf(TimeGranularity.DAY) }
    var timeRange by remember { mutableStateOf<TimeFilter>(TimeFilter.Day.today()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(timeRange) {
        val (start, end) = timeRange.toInstantRange()
        viewModel.startDbFetch(address = device, start, end)
    }

    LaunchedEffect(Unit) {
        viewModel.startConnection(device)
    }

    Scaffold { contentPadding ->

        if(showDatePicker) {
            TimeFilterPickerDialog(
                type = timeGranularity,
                onSelected = { timeRange = it },
                onDismiss = { showDatePicker = false },
            )
        }

        Column(modifier = Modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.historical_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TimeGranularityPicker(
                    timeGranularity,
                ) {
                    timeGranularity = it
                    timeRange = it.toTodayFilter()
                }
                CalendarButton { showDatePicker = true }
                ExportDataButton {
                    val (start, end) = timeRange.toInstantRange()
                    viewModel.exportCsv(device, start, end)
                }
            }

            AnimatedVisibility(
                visible = updateStatus.isAvailable(),
                enter = fadeIn(animationSpec = tween(500)) +
                        expandVertically(animationSpec = tween(500))
            ) {
                UpdateInfoBox(
                    updateStatus,
                    onUpdateClicked = {
                        when(val status = updateStatus) {
                            is HistoricalDataUpdateStatus.Available -> onUpdateClicked(device, status.new)
                            else -> {}
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Chart(
                title = stringResource(R.string.historical_chart_title_temperature),
                data = deviceData,
                dataAccessor = { d -> d.data.temperatureCelsius?.let { t -> Pair(d.date, t) } },
                timeGranularity = timeGranularity,
                unitFormatter = { "$it ºC" },
            )
            Chart(
                title = stringResource(R.string.historical_chart_title_humidity),
                data = deviceData,
                dataAccessor = { d -> d.data.humidityRh?.let { t -> Pair(d.date, t) } },
                timeGranularity = timeGranularity,
                unitFormatter = { "$it %" },
            )
            Chart(
                title = stringResource(R.string.historical_chart_title_luminosity),
                data = deviceData,
                dataAccessor = { d -> d.data.lux?.let { t -> Pair(d.date, t) } },
                timeGranularity = timeGranularity,
                unitFormatter = { "$it lx" },
            )
            Chart(
                title = stringResource(R.string.historical_chart_title_noise),
                data = deviceData,
                dataAccessor = { d -> d.data.decibels?.let { t -> Pair(d.date, t) } },
                timeGranularity = timeGranularity,
                unitFormatter = { "$it dB" },
            )
            Chart(
                title = stringResource(R.string.historical_chart_title_voc),
                data = deviceData,
                dataAccessor = { d -> d.data.vocRaw?.let { t -> Pair(d.date, t.toDouble()) } },
                timeGranularity = timeGranularity,
                unitFormatter = { "$it" },
            )
        }
    }
}


@Composable
private fun Chart(
    title: String,
    data: List<SpvSensorData>,
    dataAccessor: (SpvSensorData) -> Pair<Instant, Double>?,
    timeGranularity: TimeGranularity,
    unitFormatter: (Double) -> String
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            val (dates, data) = data.mapNotNull { dataAccessor(it) }.unzip()
            if(dates.isEmpty()) {
                lineModel { series(
                    y = listOf(0.0),
                    x = listOf(0.0)
                )}
            } else {
                lineModel { series(
                    y = data,
                    x = dates.map { it.toEpochMilliseconds() }
                )}
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = title
    )
    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .height(256.dp)
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(
                                Fill(MaterialTheme.customColors.chartColor)
                            ),
                            areaFill = LineCartesianLayer.AreaFill.single(
                                Fill(
                                    MaterialTheme.customColors.chartColor.copy(alpha = 0.2f)
                                )
                            ),
                        )
                    )
                ),
                startAxis = VerticalAxis.rememberStart(
                    label = rememberAxisLabelComponent(
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                        padding = Insets(horizontal = 6.dp, vertical = 2.dp),
                    ),
                    line = rememberAxisLineComponent(
                        fill = Fill(MaterialTheme.colorScheme.outlineVariant),
                        thickness = 1.dp,
                    ),
                    tick = null,
                    guideline = rememberAxisGuidelineComponent(
                        fill = Fill(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                        thickness = 0.5.dp,
                    ),
                    itemPlacer = VerticalAxis.ItemPlacer.count({ 5 }),

                    valueFormatter = {_, yValue, _ -> unitFormatter(yValue) },

                ),

                bottomAxis = HorizontalAxis.rememberBottom(
                    label = rememberAxisLabelComponent(
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                        ),
                    ),
                    line = rememberAxisLineComponent(
                        fill = Fill(MaterialTheme.colorScheme.outlineVariant),
                    ),
                    guideline = null,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { 3 }),
                    valueFormatter = {_, xValue, _ -> timeGranularity.formatTime(xValue.toLong()) },
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxSize(),
            placeholder = {
                Text(text = stringResource(R.string.historical_chart_no_data))
            },
            zoomState = rememberVicoZoomState(
                zoomEnabled = true,
                initialZoom = Zoom.Content
            )
        )
    }
}

@Composable
private fun CalendarButton(
    onClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        }
    ) {
        Icon(
            imageVector = Icons.Default.EditCalendar,
            contentDescription = stringResource(R.string.historical_granularity_date_pick_button_description)
        )
    }
}

@Composable
private fun ExportDataButton(
    onClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        }
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = stringResource(R.string.historical_export_data_button_description)
        )
    }
}

@Composable
private fun TimeGranularityPicker(
    selected: TimeGranularity,
    modifier: Modifier = Modifier,
    onSelected: (TimeGranularity) -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val options = listOf(TimeGranularity.DAY, TimeGranularity.MONTH, TimeGranularity.YEAR)

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = {
                    onSelected(type)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                },
                selected = selected == type,
                label = { Text(type.localize()) }

            )
        }
    }
}

private enum class TimeGranularity {
    DAY, MONTH, YEAR;

    @Composable
    fun localize(): String = when(this) {
            DAY -> stringResource(R.string.historical_granularity_picker_day)
            MONTH -> stringResource(R.string.historical_granularity_picker_month)
            YEAR -> stringResource(R.string.historical_granularity_picker_year)
        }

    fun toTodayFilter(): TimeFilter = when(this) {
        DAY -> TimeFilter.Day.today()
        MONTH -> TimeFilter.Month.today()
        YEAR -> TimeFilter.Year.today()
    }

    fun formatTime(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        return when (this) {
            DAY -> {
                DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(instant.toJavaInstant())
            }

            MONTH -> {
                DateTimeFormatter.ofPattern("EEE d")
                    .withZone(ZoneId.systemDefault())
                    .format(instant.toJavaInstant())
            }
            YEAR -> {
                DateTimeFormatter.ofPattern("MMMM")
                    .withZone(ZoneId.systemDefault())
                    .format(instant.toJavaInstant())
            }
        }
    }

}

private sealed interface TimeFilter {
    data class Day(val date: LocalDate) : TimeFilter {
        companion object {
            fun today(): Day = Day(LocalDate.now())
        }

        override fun toInstantRange(): Pair<Instant, Instant> {
            return Pair(
                Instant.fromEpochSeconds(date.atStartOfDay().toUnix()),
                Instant.fromEpochSeconds(date.atLastMomentOfDay().toUnix()),
            )
        }
    }

    data class Month(val date: LocalDate) : TimeFilter {
        companion object {
            fun today(): Month = Month(Day.today().date.withDayOfMonth(1))
            fun fromYearMonth(year: Int, month: Int): Month = Month(LocalDate.of(year, month, 1))
        }

        override fun toInstantRange(): Pair<Instant, Instant> {
            val monthStart = date.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay()
            val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth()).atLastMomentOfDay()

            return Pair(
                Instant.fromEpochSeconds(monthStart.toUnix()),
                Instant.fromEpochSeconds(monthEnd.toUnix())
            )
        }
    }

    data class Year(val date: LocalDate) : TimeFilter {
        companion object {
            fun today(): Year = Year(Day.today().date.with(TemporalAdjusters.firstDayOfYear()))
            fun fromYear(year: Int): Year = Year(LocalDate.of(year, 1, 1))
        }

        override fun toInstantRange(): Pair<Instant, Instant> {
            val yearStart = date.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay()
            val yearEnd = date.with(TemporalAdjusters.lastDayOfYear()).atLastMomentOfDay()

            return Pair(
                Instant.fromEpochSeconds(yearStart.toUnix()),
                Instant.fromEpochSeconds(yearEnd.toUnix())
            )
        }
    }

    fun toInstantRange(): Pair<Instant, Instant>
}

private fun LocalDate.atLastMomentOfDay(): LocalDateTime =
    LocalDateTime.of(this, LocalTime.MAX)

private fun LocalDateTime.toUnix(): Long {
    val zoneOffset = ZoneId.systemDefault().rules.getOffset(this)
    return this.toEpochSecond(zoneOffset)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFilterPickerDialog(
    type: TimeGranularity,
    onDismiss: () -> Unit = {},
    onSelected: (TimeFilter) -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {}
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = DatePickerDefaults.colors().containerColor
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val filter = when (type) {
                    TimeGranularity.DAY -> {
                        val datePickerState = rememberDatePickerState(TimeFilter.Day.today().date)
                        DatePicker(
                            title = null,
                            headline = null,
                            showModeToggle = false,
                            state = datePickerState
                        )
                        TimeFilter.Day(
                            datePickerState.getSelectedDate() ?: TimeFilter.Day.today().date
                        )
                    }

                    TimeGranularity.MONTH -> {
                        var month by remember { mutableIntStateOf(LocalDate.now().monthValue) }
                        var year by remember { mutableIntStateOf(LocalDate.now().year) }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.historical_time_filter_picker_title_month),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            YearPicker(onYearSelected = { year = it })
                            MonthPicker(onMonthSelected = { month = it })
                        }
                        TimeFilter.Month.fromYearMonth(year, month)
                    }

                    TimeGranularity.YEAR -> {
                        var year by remember { mutableIntStateOf(LocalDate.now().year) }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.historical_time_filter_picker_title_year),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            YearPicker(onYearSelected = { year = it })
                        }
                        TimeFilter.Year.fromYear(year)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSelected(filter)
                        onDismiss()
                    },
                ) {
                    Icon(Icons.Default.Check, stringResource(R.string.dashboard_settings_delete_button_text))
                    Text(stringResource(R.string.historical_time_filter_picker_confirm_button))
                }
            }
        }
    }
}

@Composable
private fun MonthPicker(
    modifier: Modifier = Modifier,
    onMonthSelected: (Int) -> Unit
) {
    var monthIndex by remember { mutableIntStateOf(LocalDate.now().monthValue - 1) }
    val monthRange = DateFormatSymbols().shortMonths.filterNotNull()

    WheelTextPicker(
        modifier = modifier,
        startIndex = monthIndex,
        texts = monthRange,
        rowCount = 5,
        onScrollFinished = { index ->
            onMonthSelected(index + 1)
            monthIndex = index
            monthIndex
        }
    )
}

@Composable
private fun YearPicker(
    modifier: Modifier = Modifier,
    onYearSelected: (Int) -> Unit
) {
    var year by remember { mutableIntStateOf(LocalDate.now().year)}
    val yearRange = (1..(LocalDate.now().year)).toList()

    WheelTextPicker(
        modifier = modifier,
        startIndex = year,
        texts = yearRange.map { it.toString() },
        rowCount = 5,
        onScrollFinished = { index ->
            onYearSelected(yearRange[index])
            year = index
            year
        }
    )
}

@Composable
private fun UpdateInfoBox(
    updateInfo: HistoricalDataUpdateStatus,
    onUpdateClicked: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(32.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.historical_update_available))
        Spacer(modifier = Modifier.height(8.dp))
        ExtendedFloatingActionButton(
            modifier = Modifier
                        .clip(CircleShape)
                        .drawWithFbmBackground(
                            colors = MaterialTheme.customColors.bluetoothGradient
                        ),
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onUpdateClicked()
            },
            containerColor = Color.Transparent,
            shape = CircleShape,
            icon = { Icon(Icons.Default.FileDownload, stringResource(R.string.historical_update_button)) },
            text = { Text(stringResource(R.string.historical_update_button)) },
        )
    }
}
