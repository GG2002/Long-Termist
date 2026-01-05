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
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.UniversalMDDateType
import com.cyc.yearlymemoir.domain.model.YearlyDetail
import com.cyc.yearlymemoir.utils.formatDateComponents
import com.nlf.calendar.Lunar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 生成 `01-31` 格式的 `月 - 日` 字符串
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
            Triple("2 月 29 日", null, null)
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


// 将仓库返回的 Detail 列表映射为当年每个日子的 MemoryData（summaryList）
// 规则：
// - yearly=true：日期无关年份，按类型映射到当年 MonthDay；
//   * MonthDay：直接使用其月日
//   * MonthWeekday：换算该年该月第几周几到具体月日
//   * LunarDate：按当年农历映射到当年公历月日（已在 asMonthDayInYear 处理跨年问题）
// - yearly=false：仅接受本年、且为 MonthDay 的事件；其他类型忽略
// 输出键为 "MM-dd"
fun mapDetailsToMemoryDataForYear(
    yearlyDetails: List<YearlyDetail>,
    currentYearDetails: List<Detail>,
    year: Int
): Map<String, MemoryData> {
    val formatter = DateTimeFormatter.ofPattern("MM-dd")
    val grouped = mutableMapOf<String, MutableList<String>>()

    // 处理 yearly 事件
    yearlyDetails.forEach { yd ->
        val md = yd.mdDate
        val key: String? = when (val raw = md.getRawMDDate()) {
            is UniversalMDDateType.MonthDay -> {
                val ld = LocalDate.of(year, raw.month, raw.day)
                ld.format(formatter)
            }

            is UniversalMDDateType.MonthWeekday -> {
                val monthDay = md.asMonthDayInYear(year)
                val ld = LocalDate.of(year, monthDay.month, monthDay.day)
                ld.format(formatter)
            }

            is UniversalMDDateType.LunarDate -> {
                // 农历：需要同时考虑 year-1 与 year 两个映射，若都落在当年，则两个都保留
                val lunarMonth = if (raw.isLeap) -raw.month else raw.month
                val candThis = Lunar.fromYmd(year, lunarMonth, raw.day).solar
                val candPrev = Lunar.fromYmd(year - 1, lunarMonth, raw.day).solar

                // 返回主键用于后续添加；此分支返回一个主键，但在下方插入 grouped 时会扩展为可能的两个键
                val chosen = when {
                    candThis.year == year -> candThis
                    candPrev.year == year -> candPrev
                    else -> candThis
                }
                LocalDate.of(chosen.year, chosen.month, chosen.day).format(formatter)
            }
        }

        if (key != null) {
            // 特殊处理：若为农历且 candPrev 与 candThis 都映射到当年，则同时加入两个键
            val mdRaw = yd.mdDate.getRawMDDate()
            if (mdRaw is UniversalMDDateType.LunarDate) {
                val lunarMonth = if (mdRaw.isLeap) -mdRaw.month else mdRaw.month
                val candThis = Lunar.fromYmd(year, lunarMonth, mdRaw.day).solar
                val candPrev = Lunar.fromYmd(year - 1, lunarMonth, mdRaw.day).solar

                val keys = mutableSetOf<String>()
                if (candThis.year == year) {
                    keys.add(
                        LocalDate.of(candThis.year, candThis.month, candThis.day).format(formatter)
                    )
                }
                if (candPrev.year == year) {
                    keys.add(
                        LocalDate.of(candPrev.year, candPrev.month, candPrev.day).format(formatter)
                    )
                }

                if (keys.isEmpty()) {
                    // 兜底：使用已有计算出的 key
                    keys.add(key)
                }

                keys.forEach { k ->
                    val list = grouped.getOrPut(k) { mutableListOf() }
                    list.add(yd.detail)
                }
            } else {
                val list = grouped.getOrPut(key) { mutableListOf() }
                list.add(yd.detail)
            }
        }
    }

    // 处理本年记载事件：只接受当年、MonthDay
    currentYearDetails.forEach { d ->
        val md = d.mdDate
        val isSameYear = d.year == year
        val raw = md.getRawMDDate()
        val key: String? = if (isSameYear && raw is UniversalMDDateType.MonthDay) {
            val ld = LocalDate.of(year, raw.month, raw.day)
            ld.format(formatter)
        } else null

        if (key != null) {
            val list = grouped.getOrPut(key) { mutableListOf() }
            list.add(d.detail)
        }
    }

    // 转为 MemoryData 映射
    return grouped.mapValues { (_, v) -> MemoryData(summaryList = v) }
}


@Composable
fun DataPrecalculator(
    onPrecomputationFinished: (PrecomputedData) -> Unit,
    yearlyDetailsProvider: suspend () -> List<YearlyDetail> = { emptyList() },
    currentYearDetailsProvider: suspend () -> List<Detail> = { emptyList() }
) {
    var today: LocalDate
    var todayIdx by remember { mutableIntStateOf(0) }
    var groupedItems by remember { mutableStateOf<List<List<DayDisplayData>>?>(null) }
    var startCalculation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 放入 Default 线程运行，防止阻塞渲染主线程
        withContext(Dispatchers.Default) {
            // 加载原始数据
            today = LocalDate.now()
            val curYear = today.year
            // 拉取仓库数据并映射到当年 MemoryData（分开 yearly 与本年事件）
            val yearlyDetails: List<YearlyDetail> = yearlyDetailsProvider()
            val currentYearDetails: List<Detail> = currentYearDetailsProvider()
            val mappedMemory =
                mapDetailsToMemoryDataForYear(yearlyDetails, currentYearDetails, curYear)

            val (allItems, todayIndex) = generateYearDisplayData(curYear, mappedMemory)
            todayIdx = todayIndex
            groupedItems = allItems.chunked(3)
            delay(500)
        }

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
        // 计算完成，切换 UI 状态
        _uiState.value = CalendarScreenUiState.Success(
            precomputedData = precomputedData,
            targetItemIndex = precomputedData.targetItemIndex // 你的目标索引
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun YearlyCalendar(
    viewModel: CalendarViewModel = viewModel()
) {
    // 导航控制器（通过 MainActivity 暴露的 navController 使用）
    val navController = com.cyc.yearlymemoir.MainActivity.navController
    val uiState by viewModel.uiState.collectAsState()


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer,
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 左侧：年份
                        Text(
                            text = LocalDate.now().year.toString() + " 年",
                            style = typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        // 右侧：管理按钮改为图标按钮
                        IconButton(onClick = { navController.navigate("DetailsManagement") }) {
                            Icon(
                                imageVector = Icons.Filled.SentimentSatisfiedAlt,
                                contentDescription = "管理详情",
                                tint = colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
            )
        },
        // 使用 colorScheme.background 作为屏幕背景色
        containerColor = colorScheme.background,
        floatingActionButton = {
            // FloatingActionButton(
            //     modifier = Modifier.offset(x = (-30).dp, y = (-100).dp),
            //     onClick = { /* */ },
            //     // 使用 colorScheme.primary 作为 FAB 的背景色（主强调色）
            //     containerColor = colorScheme.primary,
            //     // 使用 colorScheme.onPrimary 作为 FAB 内容的颜色，确保与背景对比度
            //     contentColor = colorScheme.onPrimary
            // ) {
            //     Icon(
            //         imageVector = Icons.Default.Add,
            //         contentDescription = "Add Item"
            //     )
            // }
        }
    ) { innerPadding ->
        Card(
            modifier = Modifier.padding(innerPadding),
            colors = CardColors(
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface,
                disabledContainerColor = colorScheme.surfaceVariant,
                disabledContentColor = colorScheme.onSurfaceVariant,
            )
        ) {
            val 屏幕像素高度 = MainActivity.metrics.bounds.height()
            var 标题栏像素高度 by remember { mutableIntStateOf(0) }
            Log.d("YearlyCalendar", "$屏幕像素高度，width: ${MainActivity.metrics.bounds.width()}")

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

                    DataPrecalculator(
                        onPrecomputationFinished = { precomputeData ->
                            viewModel.onPrecomputationFinished(precomputeData)
                        },
                        yearlyDetailsProvider = {
                            MainApplication.repository.getAllYearlyDetails()
                        },
                        currentYearDetailsProvider = {
                            MainApplication.repository.getAllDetails()
                        }
                    )
                }

                is CalendarScreenUiState.Success -> {
                    val (groupedItems, rowHeights, targetItemIndex) = state.precomputedData

                    val density = LocalDensity.current
                    val targetRowIndex = targetItemIndex / 3
                    val initialOffset = -屏幕像素高度 / 3 + 标题栏像素高度 +
                            (rowHeights[targetRowIndex] / 2).toInt()
                    // Log.d(
                    //     "YearlyCalendar",
                    //     rowHeights.toString()
                    // )
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

@Preview
@Composable
fun CalendarItemCard(
    memory: DayDisplayData =
        DayDisplayData(
            "02-29", true,
            "2 月 29 号", "周日", "初一",
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
            "2 月 29 号", "周日", "初一",
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
