package com.cyc.yearlymemoir.ui.personalbanlance

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.repository.TimePeriod
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

class ChartCardViewModel(application: Application) : AndroidViewModel(application) {
    private val model = MainApplication.repository

    // 图表数据
    private val _balanceChartData = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val balanceChartData: StateFlow<List<Pair<String, Double>>> = _balanceChartData

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 昨日消费
    private val _yestedayCost = MutableStateFlow(0.0)
    val yesterdayCost: StateFlow<Double> = _yestedayCost

    // 上月消费
    private val _lastMonthCost = MutableStateFlow(0.0)
    val lastMonthCost: StateFlow<Double> = _lastMonthCost

    // 去年消费
    private val _lastYearCost = MutableStateFlow(0.0)
    val lastYearCost: StateFlow<Double> = _lastYearCost

    fun firstLoadBalanceDetails() {
        viewModelScope.launch {
            println(UniversalDate.today())
            _isLoading.value = true
            try {
                // 改为读取所有 BalanceRecord，并按天汇总总余额
                val allBalances = withContext(Dispatchers.IO) {
                    model.getAllBalances()
                }
                val dailyTotals = aggregateDailyTotals(allBalances)
                calculateSummaryDataFromDailyTotals(dailyTotals)
                _balanceChartData.value = dailyTotals
                    .sortedBy { it.first }
                    .takeLast(30)
                    .map { Pair(it.first.formatMonthDay(), it.second) }
            } catch (e: Exception) {
                // 处理错误
                Log.e("余额图表", "how do?", e)
                _balanceChartData.value = emptyList() // 或设置一个错误状态
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 以 BalanceRecord 聚合后的每日总余额来计算摘要指标
    private fun calculateSummaryDataFromDailyTotals(dailyTotals: List<Pair<LocalDate, Double>>) {
        if (dailyTotals.isEmpty()) return

        val sorted = dailyTotals.sortedBy { it.first }

        // 昨日变化：与上一天的差值
        val latest = sorted.lastOrNull()
        val prev = sorted.dropLast(1).lastOrNull()
        _yestedayCost.value =
            if (latest != null && prev != null) latest.second - prev.second else 0.0

        // 本月相对上月变化：当月第一天总额 - 上月第一天总额
        val latestDate = latest?.first ?: return
        val firstOfThisMonth =
            sorted.firstOrNull { it.first.year == latestDate.year && it.first.month == latestDate.month }?.second
        val lastMonthDate = latestDate.minusMonths(1)
        val firstOfLastMonth =
            sorted.firstOrNull { it.first.year == lastMonthDate.year && it.first.month == lastMonthDate.month }?.second
        _lastMonthCost.value =
            if (firstOfThisMonth != null && firstOfLastMonth != null) firstOfThisMonth - firstOfLastMonth else 0.0

        // 本年相对去年变化：当年第一天总额 - 去年第一天总额
        val firstOfThisYear = sorted.firstOrNull { it.first.year == latestDate.year }?.second
        val firstOfLastYear = sorted.firstOrNull { it.first.year == latestDate.year - 1 }?.second
        _lastYearCost.value =
            if (firstOfThisYear != null && firstOfLastYear != null) firstOfThisYear - firstOfLastYear else 0.0
    }

    // 公开一个方法给 UI 调用来触发数据加载
    fun loadBalanceDetails(
        timePeriod: TimePeriod,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 读取所有 BalanceRecord，聚合并根据周期切片
                val allBalances = withContext(Dispatchers.IO) { model.getAllBalances() }
                val dailyTotals = aggregateDailyTotals(allBalances).sortedBy { it.first }

                when (timePeriod) {
                    TimePeriod.DAY -> {
                        // 最近 30 天，按 MM-dd 显示
                        val last30 = dailyTotals.takeLast(30)
                        _balanceChartData.value =
                            last30.map { Pair(it.first.formatMonthDay(), it.second) }
                    }

                    TimePeriod.MONTH -> {
                        // 按月汇总（每月最后一天的总额），显示 yy-MM
                        val monthTotals = dailyTotals
                            .groupBy { Pair(it.first.year, it.first.month) }
                            .map { (ym, list) ->
                                // 用当月最后一天或最后一条记录代表该月余额
                                val lastOfMonth = list.maxByOrNull { it.first.dayOfMonth }!!
                                Pair(ym, lastOfMonth.second)
                            }
                            .sortedBy { it.first.first * 100 + it.first.second.value }
                        val last12 = monthTotals.takeLast(12)
                        _balanceChartData.value = last12.map { (ym, v) ->
                            Pair(
                                String.format(
                                    "%02d-%02d",
                                    ym.first % 100,
                                    ym.second.value
                                ), v
                            )
                        }
                    }

                    TimePeriod.YEAR -> {
                        // 按年汇总（每年最后一天的总额），显示 yyyy
                        val yearTotals = dailyTotals
                            .groupBy { it.first.year }
                            .map { (y, list) ->
                                val lastOfYear = list.maxByOrNull { it.first.dayOfYear }!!
                                Pair(y, lastOfYear.second)
                            }
                            .sortedBy { it.first }
                        val last10 = yearTotals.takeLast(10)
                        _balanceChartData.value = last10.map { (y, v) -> Pair(y.toString(), v) }
                    }
                }
            } catch (e: Exception) {
                // 处理错误
                _balanceChartData.value = emptyList() // 或设置一个错误状态
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 将所有 BalanceRecord 按日期聚合为每日总余额
    private fun aggregateDailyTotals(allBalances: List<BalanceRecord>): List<Pair<LocalDate, Double>> {
        return allBalances
            .groupBy { it.recordDate }
            .map { (date, records) -> Pair(LocalDate.parse(date), records.sumOf { it.balance }) }
    }

    // 辅助：将 LocalDate 格式化为 MM-dd
    private fun LocalDate.formatMonthDay(): String {
        return String.format("%02d-%02d", this.monthValue, this.dayOfMonth)
    }
}

@Composable
fun BanlanceChartCard(
    refreshTick: Int,
    viewModel: ChartCardViewModel = viewModel()
) {
    val balanceChartData by viewModel.balanceChartData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val yesterdayCost by viewModel.yesterdayCost.collectAsStateWithLifecycle()
    val lastMonthCost by viewModel.lastMonthCost.collectAsStateWithLifecycle()
    val lastYearCost by viewModel.lastYearCost.collectAsStateWithLifecycle()

    val metrics = Pair(
        "余额变动", balanceChartData
    )

    LaunchedEffect(Unit) {
        viewModel.firstLoadBalanceDetails()
    }
    // 父组件触发刷新：监听 refreshTick
    LaunchedEffect(refreshTick) {
        viewModel.firstLoadBalanceDetails()
    }

    Column {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metrics.first, // "余额变动"
                    style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface
                )
                TimePeriodSwitcher { selectIndex ->
                    viewModel.loadBalanceDetails(
                        when (selectIndex) {
                            0 -> TimePeriod.DAY
                            1 -> TimePeriod.MONTH
                            2 -> TimePeriod.YEAR
                            else -> TimePeriod.DAY
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val content = remember(metrics) {
                    movableContentOf {
                        // 图表颜色现在完全由 colorScheme 控制
                        LineChart(
                            modifier = Modifier
                                .width(500.dp)
                                .height(220.dp),
                            data = listOf(
                                Line(
                                    label = metrics.first,
                                    values = metrics.second.map { it.second },
                                    // 折线颜色使用主强调色
                                    color = SolidColor(colorScheme.primary),
                                    curvedEdges = false,
                                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                                    // 渐变填充的起始色使用带透明度的主强调色
                                    firstGradientFillColor = colorScheme.primary.copy(alpha = .4f),
                                    secondGradientFillColor = Color.Transparent,
                                    strokeAnimationSpec = tween(
                                        750,
                                        easing = FastOutSlowInEasing
                                    ),
                                    gradientAnimationDelay = 100,
                                )
                            ),
                            indicatorProperties = HorizontalIndicatorProperties(textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = colorScheme.onSurface
                            ), contentBuilder = { it.format(0) }),
                            labelProperties = LabelProperties(
                                enabled = true,
                                textStyle = MaterialTheme.typography.labelSmall.copy(
                                    color = colorScheme.onSurface
                                ),
                                labels = metrics.second.map { it.first }.sampleUpToN(7)
                            ),
                            // 左上角的 legend 标，展示单个指标的时候当然要禁掉
                            labelHelperProperties = LabelHelperProperties(
                                enabled = false,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurface
                                ),
                            ),
                            minValue = max(
                                metrics.second.minOfOrNull { it.second }?.minus(100)
                                    ?: 0.0, 0.0
                            ),
                            maxValue = metrics.second.maxOfOrNull { it.second }?.plus(100)
                                ?: 0.0,
                        )
                    }
                }

                content()
            }

            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CostColumn("去年", lastYearCost)
                CostColumn("上月", lastMonthCost)
                CostColumn("昨日", yesterdayCost)
            }

        }
    }
}


fun <T> List<T>.sampleUpToN(n: Int): List<T> {
    return when {
        isEmpty() -> emptyList()
        size <= n -> this // 不超过 6 个，直接返回全部
        else -> {
            val step = maxOf(1, (size) / (n - 1)) // 确保有 6 个点：0, step*1, ..., step*5
            List(n) { index ->
                this[(index * step).coerceAtMost(size - 1)]
            }
        }
    }
}

// 辅助函数：获取尺寸并转换为 Dp
@Composable
private fun Modifier.onSizeChangedWithDp(
    callback: (width: Dp, positionX: Dp) -> Unit
): Modifier {
    val density = LocalDensity.current
    return this.onGloballyPositioned { coordinates ->
        with(density) {
            callback(
                coordinates.size.width.toDp(), coordinates.positionInParent().x.toDp()
            )
        }
    }
}

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun TimePeriodSwitcher(onClick: (Int) -> Unit) {
    var selectedIndex by remember { mutableStateOf(0) }
    val options = listOf("日", "月", "年")

    // 计算每个选项的宽度
    val widths = remember { mutableStateListOf<Dp>(0.dp, 0.dp, 0.dp) }
    val positions = remember { mutableStateListOf<Dp>(0.dp, 0.dp, 0.dp) }

    // 滑块动画
    val sliderPosition by animateDpAsState(
        targetValue = positions.getOrElse(selectedIndex) { 0.dp }, label = "Slider animation"
    )
    val sliderWidth by animateDpAsState(
        targetValue = widths.getOrElse(selectedIndex) { 0.dp }, label = "Slider width animation"
    )

    val surfaceHeight = 28.dp
    val surfaceWidth = 75.dp

    Surface(
        color = Color.LightGray.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(top = 4.dp)
            .width(surfaceWidth)
            .height(surfaceHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // 悬浮滑块
            Box(
                modifier = Modifier
                    .width(sliderWidth)
                    .height(surfaceHeight - 4.dp)
                    .offset(x = sliderPosition)
                    .shadow(
                        elevation = 4.dp, shape = RoundedCornerShape(8.dp), clip = true
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )

            Row(
                modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, option ->
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChangedWithDp { width, positionX ->
                            widths[index] = width
                            positions[index] = positionX
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                selectedIndex = index
                                onClick(selectedIndex)
                            })
                        }, contentAlignment = Alignment.Center
                    ) {
                        val fontSize = 12.sp
                        Text(
                            text = option,
                            color = if (selectedIndex == index) {
                                colorScheme.primary
                            } else {
                                colorScheme.onSurfaceVariant
                            },
                            fontSize = fontSize,
                            lineHeight = fontSize,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CostColumn(name: String, cost: Double, width: Int = 90, fontSize: Int = 14) {
    val (colo: Color, ico: ImageVector) = when {
        cost < 0 -> Pair(Color.Red, Icons.Filled.ArrowDropDown)
        cost == 0.0 -> Pair(Color.Gray, Icons.Filled.HorizontalRule)
        else -> Pair(Color(0xFF18A656), Icons.Filled.ArrowDropUp)
    }
    Column(
        Modifier.width(width.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name, fontSize = fontSize.sp
            )
            Icon(
                imageVector = ico,
                contentDescription = name + "趋势",
                tint = colo,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            abs(cost).format(2),
            fontSize = (fontSize + 4).sp,
            color = colo,
        )
    }
}
