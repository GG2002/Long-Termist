package com.cyc.yearlymemoir.ui.yearlycalendar

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cyc.yearlymemoir.utils.formatDateComponents
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 数据类保持不变
data class Event(val summary: String, val targetDate: LocalDate)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EveryDayCard(
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
    navController: NavController = rememberNavController()
) {
    val today = LocalDate.now()
    val (solar, weekday, lunar) = formatDateComponents(today)
    val nearestEvent = getNearestYearlyEvent()
    val metrics = getFavoriteMetrics()

    val daysUntilEvent = ChronoUnit.DAYS.between(today, nearestEvent.targetDate)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // 1. 日期区域
            DateHeader(solar = solar, weekday = weekday, lunar = lunar)

            Spacer(modifier = Modifier.height(24.dp))
            // 使用 colorScheme.outlineVariant 作为分隔线颜色，更柔和
            HorizontalDivider(color = colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // 2. 事件倒计时区域
            UpcomingEventSection(
                daysUntil = daysUntilEvent,
                eventSummary = nearestEvent.summary,
                navController = navController
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))

            val scrollState = rememberScrollState()
            // 3. 指标图表区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .weight(1f),
            ) {
                MetricsChartSection(
                    metrics = metrics,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedTransitionScope = sharedTransitionScope,
                    navController = navController
                )
//                Spacer(modifier = Modifier.height(24.dp))
//                MetricsChartSection(metrics = metrics, navController)
//                Spacer(modifier = Modifier.height(24.dp))
//                MetricsChartSection(metrics = metrics, navController)
//                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DateHeader(solar: String, weekday: String, lunar: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = solar, // 只显示“日”
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            // 使用 colorScheme.onSurface 作为表面上的主要文本颜色
            color = colorScheme.onSurface
        )
        Column {
            Text(
                text = weekday,
                style = typography.titleMedium,
                color = colorScheme.onSurface
            )
            Text(
                text = "农历 $lunar",
                style = typography.bodySmall,
                // 使用 colorScheme.onSurfaceVariant 作为次要文本颜色
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpcomingEventSection(daysUntil: Long, eventSummary: String, navController: NavController) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    navController.navigate("YearlyCalendar")
                }
            )
        }) {
        Text(
            text = "下一个期待",
            style = typography.titleMedium,
            // 次要标题，使用 onSurfaceVariant
            color = colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$daysUntil",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                // 倒计时天数是视觉焦点，使用主强调色 primary
                color = colorScheme.primary,
                lineHeight = 40.sp
            )
            Text(
                text = "天后",
                style = typography.headlineSmall,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = "是「$eventSummary」的日子",
            style = typography.bodyLarge,
            // 描述性文本，使用 onSurfaceVariant
            color = colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MetricsChartSection(
    metrics: Pair<String, List<Pair<String, Double>>>,
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
    navController: NavController,
) {

    with(sharedTransitionScope) {
        Column(modifier = Modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(key = "personal_balance_chart"),
                animatedVisibilityScope = animatedVisibilityScope
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        navController.navigate("PersonalBalanceScreen")
                    }
                )
            })
        {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metrics.first, // "余额变动"
                    style = typography.titleMedium,
                    color = colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 图表颜色现在完全由 colorScheme 控制
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                indicatorProperties = HorizontalIndicatorProperties(
                    textStyle = typography.bodySmall.copy(
                        color = colorScheme.onSurface
                    ),
                    contentBuilder = { it.format(0) }
                ),
                labelProperties = LabelProperties(
                    enabled = true,
                    textStyle = typography.bodyMedium.copy(
                        color = colorScheme.onSurface
                    ),
                    labels = metrics.second.map { it.first }
                        .slice(0 until metrics.second.size step 4)
                ),
                // 左上角的 legend 标，展示单个指标的时候当然要禁掉
                labelHelperProperties = LabelHelperProperties(
                    enabled = false,
                    textStyle = typography.bodyMedium.copy(
                        color = colorScheme.onSurface
                    ),
                ),
                minValue = metrics.second.map { it.second }.min() - 100,
                maxValue = metrics.second.map { it.second }.max() + 100,
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
                        strokeAnimationSpec = tween(1000, easing = FastOutSlowInEasing),
                        gradientAnimationDelay = 1000,
                    )
                ),
            )
        }
    }
}


// --- 模拟数据函数 (保持不变) ---
fun getNearestYearlyEvent(): Event {
    return Event("西瓜变甜了", LocalDate.now().plusDays(25))
}

fun getFavoriteMetrics(): Pair<String, List<Pair<String, Double>>> {
    return Pair(
        "余额变动",
        listOf(
            Pair("09-08", 49800.0),
            Pair("09-09", 50100.0),
            Pair("09-10", 50002.0),
            Pair("09-11", 49871.34),
            Pair("09-12", 50250.0),
            Pair("09-13", 50180.0),
            Pair("09-08", 49800.0),
            Pair("09-09", 50100.0),
            Pair("09-10", 50002.0),
            Pair("09-11", 49871.34),
            Pair("09-12", 50250.0),
            Pair("09-13", 50180.0),
            Pair("09-08", 49800.0),
            Pair("09-09", 50100.0),
            Pair("09-10", 50002.0),
            Pair("09-11", 49871.34),
            Pair("09-12", 50250.0),
            Pair("09-13", 50180.0),
            Pair("09-08", 49800.0),
            Pair("09-09", 50100.0),
            Pair("09-10", 50002.0),
            Pair("09-11", 49871.34),
            Pair("09-12", 50250.0),
            Pair("09-13", 50180.0)
        )
    )
}
