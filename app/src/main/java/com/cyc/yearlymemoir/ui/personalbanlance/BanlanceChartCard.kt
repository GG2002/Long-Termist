package com.cyc.yearlymemoir.ui.personalbanlance

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import kotlin.math.abs

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BanlanceChartCard(
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    val metrics = Pair(
        "余额变动", listOf(
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

    val yestedayCost: Double = -10230.34
    val lastMonthCost: Double = 1900.18
    val lastYearCost: Double = 0.0

//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(IntrinsicSize.Max)
//            .background(color = Color.Transparent)
//    ) {
    Column(
//        modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        with(sharedTransitionScope) {
            Column(
                modifier = Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "personal_balance_chart"),
                    animatedVisibilityScope = animatedVisibilityScope
                ), horizontalAlignment = Alignment.CenterHorizontally
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
                    TimePeriodSwitcher()
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 图表颜色现在完全由 colorScheme 控制
                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    indicatorProperties = HorizontalIndicatorProperties(textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = colorScheme.onSurface
                    ), contentBuilder = { it.format(0) }),
                    labelProperties = LabelProperties(enabled = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurface
                        ),
                        labels = metrics.second.map { it.first }
                            .slice(0 until metrics.second.size step 4)),
                    // 左上角的 legend 标，展示单个指标的时候当然要禁掉
                    labelHelperProperties = LabelHelperProperties(
                        enabled = false,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
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
                            strokeAnimationSpec = tween(2000, easing = FastOutSlowInEasing),
                            gradientAnimationDelay = 1000,
                        )
                    ),
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CostColumn("去年", lastYearCost)
                    CostColumn("上月", lastMonthCost)
                    CostColumn("昨日", yestedayCost)
                }
            }
        }
    }
//    }
}

// 辅助函数：获取尺寸并转换为Dp
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
fun TimePeriodSwitcher() {
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
            .padding(0.dp, 4.dp, 8.dp, 0.dp)
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
                            detectTapGestures(onTap = { selectedIndex = index })
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
    Column(Modifier.width(width.dp)) {
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
            "${abs(cost)}",
            fontSize = (fontSize + 4).sp,
            color = colo,
        )
    }
}
