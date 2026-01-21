package com.cyc.yearlymemoir.ui.yearlycalendar

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cyc.yearlymemoir.ui.personalbanlance.sampleUpToN
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import java.time.LocalDate

// 数据类保持不变
data class Event(val summary: String, val targetDate: LocalDate)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MetricsChartSection(
    metrics: Pair<String, List<Pair<String, Double>>>,
    navController: NavController,
) {
    Card(
        modifier = Modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            Text(
                text = metrics.first, // "余额变动"
                style = typography.titleMedium,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LineChart(
                modifier = Modifier
                    .size(width = 500.dp, height = 200.dp),
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
                        strokeAnimationSpec = tween(500, easing = FastOutSlowInEasing),
                        gradientAnimationDelay = 100,
                    )
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                    textStyle = typography.bodySmall.copy(
                        color = colorScheme.onSurfaceVariant
                    ),
                    contentBuilder = { it.format(0) }
                ),
                labelProperties = LabelProperties(
                    enabled = true,
                    textStyle = typography.labelSmall.copy(
                        color = colorScheme.onSurfaceVariant
                    ),
                    labels = metrics.second.map { it.first }.sampleUpToN(7)
                ),
                // 左上角的 legend 标，展示单个指标的时候当然要禁掉
                labelHelperProperties = LabelHelperProperties(
                    enabled = false,
                    textStyle = typography.bodyMedium.copy(
                        color = colorScheme.onSurfaceVariant
                    ),
                ),
                minValue = metrics.second.minOfOrNull { it.second }?.minus(100) ?: 0.0,
                maxValue = metrics.second.maxOfOrNull { it.second }?.plus(100) ?: 0.0,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


// --- 模拟数据函数 (保持不变) ---
fun getNearestYearlyEvent(): Event {
    return Event("西瓜变甜了", LocalDate.now().plusDays(25))
}
