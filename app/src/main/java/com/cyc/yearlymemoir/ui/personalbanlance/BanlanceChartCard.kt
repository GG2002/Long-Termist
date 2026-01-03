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
import com.cyc.yearlymemoir.domain.model.Detail
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
import java.time.Month
import kotlin.math.abs

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
                // 在后台调用 suspend 函数
                val (details, _) = withContext(Dispatchers.IO) {
                    model.getFirstDayFieldDataByName("总余额", TimePeriod.DAY)
                }
                calculateSummaryData(details.reversed())
                _balanceChartData.value = details.map {
                    val date = it.mdDate.substringAfter("MD-")
                    val balance = it.detail.toDouble()
                    Pair(date, balance)
                }.takeLast(30)
            } catch (e: Exception) {
                // 处理错误
                Log.e("余额图表", "how do?", e)
                _balanceChartData.value = emptyList() // 或设置一个错误状态
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateSummaryData(details: List<Detail>) {
        if (details.isEmpty()) {
            return
        }

        // 为了高效查找，我们将数据转换为带 LocalDate 的 Map
        // 同时处理 'MD-MM-dd' 格式
        val dateMap = details.associateBy { detail ->
            try {
                // 从 "MD-MM-dd" 解析月和日
                val parts = detail.mdDate.split('-').drop(1)
                LocalDate.of(detail.year, parts[0].toInt(), parts[1].toInt())
            } catch (e: Exception) {
                // 如果格式错误，返回一个不可能的日期以忽略此条目
                LocalDate.MIN
            }
        }.filterKeys { it != LocalDate.MIN }

        if (dateMap.isEmpty()) {
            return
        }

        // 1. 计算昨日变化
        // 注意：这是与上一条记录的对比，不一定是严格的“昨天”
        val latestEntry = dateMap.entries.first()
        val secondLatestEntry = dateMap.entries.elementAtOrNull(1)
        val yesterdayChange = if (secondLatestEntry != null) {
            val latestValue = latestEntry.value.detail.toDoubleOrNull() ?: 0.0
            val secondLatestValue = secondLatestEntry.value.detail.toDoubleOrNull() ?: 0.0
            // 变化 = 最新余额 - 上次余额
            latestValue - secondLatestValue
        } else {
            null
        }
        _yestedayCost.value = yesterdayChange ?: 0.0

        // 3. 计算本月相对上月变化
        val latestDate = latestEntry.key
        val firstOfThisMonth = findFirstEntryForMonth(latestDate.year, latestDate.month, dateMap)
        val firstOfLastMonth = findFirstEntryForMonth(
            latestDate.minusMonths(1).year,
            latestDate.minusMonths(1).month,
            dateMap
        )

        val monthChange = if (firstOfThisMonth != null && firstOfLastMonth != null) {
            val thisMonthValue = firstOfThisMonth.detail.toDoubleOrNull() ?: 0.0
            val lastMonthValue = firstOfLastMonth.detail.toDoubleOrNull() ?: 0.0
            thisMonthValue - lastMonthValue
        } else {
            null
        }
        _lastMonthCost.value = monthChange ?: 0.0

        // 4. 计算本年相对去年变化
        val firstOfThisYear = findFirstEntryForYear(latestDate.year, dateMap)
        val firstOfLastYear = findFirstEntryForYear(latestDate.year - 1, dateMap)
        val yearChange = if (firstOfThisYear != null && firstOfLastYear != null) {
            val thisYearValue = firstOfThisYear.detail.toDoubleOrNull() ?: 0.0
            val lastYearValue = firstOfLastYear.detail.toDoubleOrNull() ?: 0.0
            thisYearValue - lastYearValue
        } else {
            null
        }
        _lastYearCost.value = yearChange ?: 0.0
    }

    /**
     * 查找某年某月的第一条记录。
     */
    private fun findFirstEntryForMonth(
        year: Int,
        month: Month,
        data: Map<LocalDate, Detail>
    ): Detail? {
        return data.entries
            .filter { it.key.year == year && it.key.month == month }
            .minByOrNull { it.key.dayOfMonth } // 找到这个月里日期最早的那天
            ?.value
    }

    /**
     * 查找某年的第一条记录。
     */
    private fun findFirstEntryForYear(year: Int, data: Map<LocalDate, Detail>): Detail? {
        return data.entries
            .filter { it.key.year == year }
            .minByOrNull { it.key.dayOfYear } // 找到这年里日期最早的那天
            ?.value
    }

    // 公开一个方法给 UI 调用来触发数据加载
    fun loadBalanceDetails(
        timePeriod: TimePeriod,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 在后台调用 suspend 函数
                val (details, _) = withContext(Dispatchers.IO) {
                    model.getFirstDayFieldDataByName("总余额", timePeriod)
                }
                _balanceChartData.value = details.map {
                    val date = when (timePeriod) {
                        TimePeriod.DAY -> it.mdDate.substringAfter("MD-")
                        TimePeriod.MONTH -> it.year.toString()
                            .substring(2) + "-" + it.mdDate.substring(3, 5)

                        TimePeriod.YEAR -> it.year.toString()
                    }
                    val balance = it.detail.toDouble()
                    Pair(date, balance)
                }
                _balanceChartData.value = when (timePeriod) {
                    TimePeriod.DAY -> _balanceChartData.value.takeLast(30)
                    TimePeriod.MONTH -> _balanceChartData.value.takeLast(12)
                    TimePeriod.YEAR -> _balanceChartData.value.takeLast(10)
                }
            } catch (e: Exception) {
                // 处理错误
                _balanceChartData.value = emptyList() // 或设置一个错误状态
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@Composable
fun BanlanceChartCard(
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
                            minValue = metrics.second.minOfOrNull { it.second }?.minus(100)
                                ?: 0.0,
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
