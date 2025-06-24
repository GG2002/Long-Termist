package com.cyc.yearlymemoir.ui.yearlycalendar

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.GlanceTheme.colors
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cyc.yearlymemoir.utils.formatDateComponents
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 这是我们用 Glance 重写的 UI
@Composable
fun EveryDayWidgetContent() {
    // 数据获取逻辑可以放在这里
    val today = LocalDate.now()
    val (solar, weekday, lunar) = formatDateComponents(today) // 复用你的数据函数
    val nearestEvent = getNearestYearlyEvent()
    val metrics = getFavoriteMetrics()
    val daysUntilEvent = ChronoUnit.DAYS.between(today, nearestEvent.targetDate)

    // Glance 没有 Card，我们用 Column + background + cornerRadius 模拟
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(12.dp) // 用圆角模拟卡片
                .padding(16.dp)
        ) {
            // 1. 日期区域 (和 Compose 非常像)
            DateHeaderGlance(solar = solar, weekday = weekday, lunar = lunar)

            Spacer(modifier = GlanceModifier.height(16.dp))
            // 分隔线用一个矮的 Box 模拟
            Box(
                modifier = GlanceModifier.height(1.dp).fillMaxWidth()
                    .background(GlanceTheme.colors.outline),
                contentAlignment = Alignment.TopStart,
                content = { }
            )
            Spacer(modifier = GlanceModifier.height(16.dp))

            // 2. 事件倒计时区域
            UpcomingEventSectionGlance(
                daysUntil = daysUntilEvent,
                eventSummary = nearestEvent.summary
            )

            Spacer(modifier = GlanceModifier.height(16.dp))
            Box(
                modifier = GlanceModifier.height(1.dp).fillMaxWidth()
                    .background(GlanceTheme.colors.outline),
                contentAlignment = Alignment.TopStart,
                content = { }
            )
            Spacer(modifier = GlanceModifier.height(16.dp))

            // 3. 指标图表区域 (可滑动)
            // 使用 LazyColumn 实现滚动！
//            LazyColumn(modifier = GlanceModifier.defaultWeight()) {
//                // 为了演示，我们放3个
//                items(3) { index ->
//                    MetricsChartSection(metrics = metrics)
//                    if (index < 2) {
//                        Spacer(modifier = GlanceModifier.height(16.dp))
//                    }
//                }
//            }
        }
    }
}

// --- 下面是子组件的 Glance 版本 ---

@Composable
fun DateHeaderGlance(solar: String, weekday: String, lunar: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = solar,
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        )
        Spacer(modifier = GlanceModifier.width(16.dp))
        Column {
            Text(
                text = weekday,
                style = TextStyle(
                    color = colors.onSurface
                )
//                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "农历 $lunar",
                style = TextStyle(
                    color = colors.onSurfaceVariant
                )
//                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun UpcomingEventSectionGlance(daysUntil: Long, eventSummary: String) {
    Column {
        Text(
            text = "下一个期待",
            style = TextStyle(
                color = colors.onSurfaceVariant
            )
//            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$daysUntil",
                style = TextStyle(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                )
//                lineHeight = 40.sp
            )
            Text(
                text = "天后",
                style = TextStyle(
                    color = colors.onSurface,
                ),
//                style = MaterialTheme.typography.headlineSmall,
                modifier = GlanceModifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = "是「$eventSummary」的日子",
            style = TextStyle(
                color = colors.onSurfaceVariant,
            ),
//            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// Glance 没有 LinearProgressIndicator，我们用 Box 来手动实现一个简版的
//@Composable
//fun MetricsChartSection(metrics: List<Metric>) {
//    Column {
//        Text("关键指标", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium))
//        Spacer(modifier = GlanceModifier.height(12.dp))
//        metrics.forEach { metric ->
//            Column(modifier = GlanceModifier.padding(bottom = 10.dp)) {
//                Row(
//                    modifier = GlanceModifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(metric.name, style = TextStyle(fontSize = 14.sp))
//                    Text("${(metric.value * 100).toInt()}%", style = TextStyle(fontSize = 12.sp))
//                }
//                Spacer(modifier = GlanceModifier.height(4.dp))
//                // 手动实现进度条
//                Box(
//                    modifier = GlanceModifier.fillMaxWidth().height(8.dp)
//                        .background(ColorProvider(R.color.progress_track_color)).cornerRadius(4.dp)
//                ) {
//                    Box(
//                        modifier = GlanceModifier
//                            .fillMaxHeight()
//                            .fillMaxWidth() // 按百分比填充宽度
//                            .background(ColorProvider(metric.color)) // 注意这里颜色用法
//                            .cornerRadius(4.dp),
//                        contentAlignment = Alignment.TopStart,
//                        content = {},
//                    )
//                }
//            }
//        }
//    }
//}

// "宿主"类，它告诉系统这个 Widget 的内容是什么
class EveryDayWidget : GlanceAppWidget() {
    // 这是唯一需要重写的方法。
    // 它是一个 suspend 函数，允许你在提供UI内容之前执行异步操作（如网络请求、数据库查询）。
    override suspend fun provideGlance(context: Context, id: GlanceId) {

        // provideContent 是一个高阶函数，它负责启动 Glance 的组合过程。
        // 你需要将你的 @Composable UI 内容作为 lambda 表达式传递给它。
        provideContent {
            // 在这里调用你的顶层 @Composable 函数
            EveryDayWidgetContent()
        }
    }
}

// "接收器"类，是系统与你的 Widget 交互的入口点
class EveryDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EveryDayWidget()
}

// 注意：Glance 中颜色不能直接用 Color(0xFF...)，需要定义在 colors.xml 中
// 比如在 res/values/colors.xml 添加:
// <color name="progress_track_color">#E0E0E0</color>
// 并且 Metric 数据类中的 Color 也需要改成 R.color.some_color 这样的资源ID