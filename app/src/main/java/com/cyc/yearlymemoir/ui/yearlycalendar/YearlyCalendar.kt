package com.cyc.yearlymemoir.ui.yearlycalendar

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.WorkScheduler
import com.cyc.yearlymemoir.utils.formatDateComponents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 生成 `01-31` 格式的 `月-日` 字符串
// 直接生成 366 天
fun generateYearDateKeys(): List<String> {
    val formatter = DateTimeFormatter.ofPattern("MM-dd")
    val result = mutableListOf<String>()
    var currentDate = LocalDate.of(2024, 1, 1) // 使用闰年 2024 来确保有 366 天
    repeat(366) {
        result.add(currentDate.format(formatter))
        currentDate = currentDate.plusDays(1)
    }
    return result
}

// 新函数：预计算全年每个日期的显示数据 (基于固定 366 个 dateKey)
fun generateYearDisplayData(
    year: Int, customMemoryData: Map<String, MemoryData>
): Pair<List<DayDisplayData>, Int> {
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ofPattern("MM-dd"))
    val currentYearIsLeap = LocalDate.of(year, 1, 1).isLeapYear // 检查目标年份是否是闰年

    // 获取固定的 366 个日期键 ("MM-dd" 格式)
    val allDateKeys = generateYearDateKeys()
    var todayIdx = 0
    val allDisplayData: List<DayDisplayData> = allDateKeys.mapIndexed { index, dateKey ->
        val memory = customMemoryData[dateKey] ?: MemoryData()
        // 判断是否是“今天”，需要日期键和年份都匹配
        val isToday = dateKey == todayStr && year == today.year

        // 初始化显示文本和标志
        var showSolarDay = memory.showSolarDay // 默认使用 MemoryData 的设置
        var showWeekDay = memory.showWeekDay
        var showLunarDay = memory.showLunarDay

        val (solarCalc, weekdayCalc, lunarCalc) = if (!currentYearIsLeap && dateKey == "02-29") {
            showLunarDay = false
            showWeekDay = false
            showSolarDay = true
            Triple("2月29日", null, null)
        } else {
            val loDate =
                LocalDate.parse("$year-$dateKey", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            formatDateComponents(loDate)
        }

        // --- 如果是“今天”，覆盖显示标志，强制显示全部 ---
        if (isToday) {
            // 如果是今天，无论 MemoryData 如何设置，都显示公历、星期、农历
            showSolarDay = true
            showWeekDay = true
            showLunarDay = true
            todayIdx = index
        }

        DayDisplayData(
            dateKey = dateKey, // Preserve the original "MM-dd" key
            isToday = isToday,
            solarDayText = solarCalc,
            weekDayText = weekdayCalc,
            lunarDayText = lunarCalc,
            summaryList = memory.summaryList,
            showSolarDay = showSolarDay, // These are the *final* determined show flags
            showWeekDay = showWeekDay,
            showLunarDay = showLunarDay
        )
    }

    return Pair(allDisplayData, todayIdx)
}


// UI 状态
sealed class CalendarScreenUiState {
    object Loading : CalendarScreenUiState()
    data class Success(
        val precomputedData: PrecomputedData, val targetItemIndex: Int // 我们要定位到的目标项的索引 (0-365)
    ) : CalendarScreenUiState()
}

class CalendarViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow<CalendarScreenUiState>(CalendarScreenUiState.Loading)
    val uiState: StateFlow<CalendarScreenUiState> = _uiState.asStateFlow()

    fun onPrecomputationFinished(precomputedData: PrecomputedData) {
        // 计算完成，切换UI状态
        _uiState.value = CalendarScreenUiState.Success(
            precomputedData = precomputedData,
            targetItemIndex = precomputedData.targetItemIndex // 你的目标索引
        )
    }

}

@Composable
fun DataPrecalculator(
    onPrecomputationFinished: (PrecomputedData) -> Unit
) {
    var today: LocalDate
    var todayIdx by remember { mutableIntStateOf(0) }
    var groupedItems by remember { mutableStateOf<List<List<DayDisplayData>>?>(null) }
    var startCalculation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 加载原始数据
        today = LocalDate.now()
        val curYear = today.year

        val customMemoryData =
            mutableStateMapOf(
                "02-29" to MemoryData(summaryList = listOf("🍓变甜了"), showWeekDay = true),
                "06-01" to MemoryData(summaryList = listOf("🍉变甜了"))
            )

        val (allItems, todayIndex) = generateYearDisplayData(curYear, customMemoryData)
        todayIdx = todayIndex
        groupedItems = allItems.chunked(3)

        delay(1000)
        startCalculation = true
    }

    if (startCalculation) {
        val density = LocalDensity.current

        // SubcomposeLayout 允许我们在测量阶段组合和测量子元素
        SubcomposeLayout(
            modifier = Modifier
                .fillMaxWidth()
                .height(1000.dp)
        ) { constraints ->
            val rowHeights = mutableListOf<Float>()
            val itemWidth = with(density) { constraints.maxWidth.toDp() }

            groupedItems!!.forEachIndexed { index, rowItems ->
                val measurables = subcompose(index) {
                    ListItemRow(
                        items = rowItems,
                        modifier = Modifier.width(itemWidth)
                    )
                }

                if (measurables.isNotEmpty()) {
                    val placeable =
                        measurables[0].measure(constraints.copy(maxHeight = 300)) // 使用新的约束
                    val heightPx = placeable.height.toFloat()
                    rowHeights.add(heightPx)
                }
            }

            val precomputedData =
                PrecomputedData(groupedItems!!, rowHeights, todayIdx)
            onPrecomputationFinished(precomputedData)

            // 这个 layout 让它在 UI 上不占任何空间，纯粹用于后台计算
            layout(0, 0) {}
        }
    }
}

@Preview
@Composable
fun CalendarItemCard(
    memory: DayDisplayData =
        DayDisplayData(
            "02-29", true,
            "2月29号", "周日", "初一",
            listOf(),
            true, true, true
        )
) {
    // Background color logic: bright purple for today, transparent otherwise
    val (backGroundColor, textColor) = if (!memory.isToday) {
        Pair(colorScheme.surface, colorScheme.onSurface)
    } else {
        Pair(colorScheme.primary, colorScheme.onPrimary)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight() // Use fillMaxWidth to allow text alignment
            .background(color = backGroundColor)
    ) {
        // Summary List or Placeholder (-)
        if (memory.summaryList.isEmpty() && !memory.isToday) {
            Icon(
                imageVector = Icons.Filled.HorizontalRule,
                contentDescription = "历史无事",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            return@Column
        }

        if (memory.showSolarDay) {
            // Current day: Always show solar, weekday, lunar
            // Solar date (bigger text)
            Text(
                text = memory.solarDayText,
                style = MaterialTheme.typography.titleMedium, // Larger text for date
                color = textColor
            )
        }

        if (memory.showLunarDay || memory.showWeekDay) {
            // Weekday and Lunar (smaller text below)
            Row {
                if (memory.showWeekDay) {
                    Text(
                        text = memory.weekDayText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                if (memory.showSolarDay) {
                    Text(
                        text = memory.lunarDayText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
        }

        if (memory.summaryList.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 0.5.dp,
                color = textColor.copy(alpha = 0.5f)
            )
            // Show summaries if they exist (for both current and non-current days)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (summary in memory.summaryList) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ListItemRow(
    items: List<DayDisplayData> = listOf(
        DayDisplayData(
            "02-29", true,
            "2月29号", "周日", "初一",
            listOf(),
            true, true, true
        )
    ), modifier: Modifier = Modifier
) {
    val colNum = 3
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { dateData ->
            Box(
                modifier = Modifier
                    .weight(1F / colNum)
                    .fillMaxHeight()
                    .border(0.2.dp, Color.LightGray.copy(alpha = 0.6f))
                    .padding(4.dp), // Increased padding
                contentAlignment = Alignment.Center
            ) {
                CalendarItemCard(dateData)
            }
        }
    }
}

@Preview
@Composable
fun YearlyCalendar(
    viewModel: CalendarViewModel = CalendarViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 使用 colorScheme.background 作为屏幕背景色
        containerColor = colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.offset(y = -100.dp),
                onClick = { WorkScheduler.scheduleNowForTest(MainActivity.appContext) },
                // 使用 colorScheme.primary 作为 FAB 的背景色（主强调色）
                containerColor = colorScheme.primary,
                // 使用 colorScheme.onPrimary 作为 FAB 内容的颜色，确保与背景对比度
                contentColor = colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item"
                )
            }
        }
    ) { innerpadding ->
        Card(
            modifier = Modifier.padding(innerpadding),
            colors = CardColors(
                containerColor = colorScheme.surface,
                contentColor = Color.Black,
                disabledContainerColor = Color.White,
                disabledContentColor = Color.LightGray,
            )
        ) {
            val currentYear = remember { LocalDate.now().year }
            val 屏幕像素高度 = MainActivity.metrics.bounds.height()
            println("$屏幕像素高度, width: ${MainActivity.metrics.bounds.width()}")
            var 标题栏像素高度 by remember { mutableIntStateOf(0) }

            when (val state = uiState) {
                is CalendarScreenUiState.Loading -> {
                    // 显示加载动画
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorScheme.primary)
                        Text(
                            "正在准备数据...",
                            modifier = Modifier.padding(top = 60.dp),
                            color = colorScheme.onSurfaceVariant,
                            style = typography.bodySmall
                        )
                    }

                    DataPrecalculator { precomputeData ->
                        viewModel.onPrecomputationFinished(
                            precomputeData
                        )
                    }
                }

                is CalendarScreenUiState.Success -> {
                    val (groupedItems, rowHeights, targetItemIndex) = state.precomputedData

                    val density = LocalDensity.current
                    val targetRowIndex = targetItemIndex / 3
                    val initialOffset = -屏幕像素高度 / 3 + 标题栏像素高度 +
                            (rowHeights[targetRowIndex] / 2).toInt()
                    Log.d(
                        "YearlyCalendar",
                        "屏幕高度：$屏幕像素高度，" +
                                "标题栏高度：$标题栏像素高度，" +
                                "当天 Item 高度 / 2：${rowHeights[targetRowIndex] / 2}，" +
                                "最终 Offset：$initialOffset"
                    )

                    val lazyListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = targetRowIndex,
                        initialFirstVisibleItemScrollOffset = initialOffset
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(groupedItems) { index, rowItems ->
                            // 获取预计算好的高度
                            val heightInPx = rowHeights[index]
                            val heightInDp = with(density) { heightInPx.toDp() }

                            ListItemRow(
                                items = rowItems,
                                // *** 关键：应用预计算的高度 ***
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(heightInDp)
                            )
                        }
                    }
                }
            }
        }
    }
}
