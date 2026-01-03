import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Preview
@Composable
fun PullOutCardScreen() {
    // 使用一个Box来容纳屏幕内容和底部卡片
    Box(modifier = Modifier.fillMaxSize()) {
        // 这里是你的主屏幕内容，例如地图、列表等
        // ...

        // 将 PullOutCard 放在Box的底部
        PullOutCard(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // 在这里填充卡片内容
            // ColumnScope 已经提供，所以可以直接放置组件
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "这里是第一行描述，它在折叠时也应该部分可见。",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "这里是更详细的内容，只有在卡片完全展开后才能看到。".repeat(10),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// 内部使用的，用于提示可拖动的横条
@Composable
private fun DragHandle() {
    Spacer(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .width(40.dp)
            .height(4.dp)
            .background(
                color = colorScheme.onSurface.copy(alpha = 0.4f),
                shape = CircleShape
            )
    )
}

/**
 * 可从底部拉出的卡片组件（优化版）
 *
 * @param modifier 修饰符
 * @param collapsedHeight 折叠时的起始可见高度。这个高度应足以容纳DragHandle和你希望初始显示的内容。
 * @param expandedHeight 展开时的完整高度。
 * @param minExpandedVisibleHeight 展开后，可被拖动到的最小可见高度。
 * @param content 卡片内部的 Composable 内容。
 */
@Composable
fun PullOutCard(
    modifier: Modifier = Modifier,
    collapsedHeight: Dp = 90.dp,
    expandedHeight: Dp = 500.dp,
    minExpandedVisibleHeight: Dp = 150.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    val hiddenOffset = 200

    val density = LocalDensity.current
    val collapsedHeightPx = with(density) {
        collapsedHeight.toPx() + hiddenOffset
    }
    val expandedHeightPx = with(density) {
        expandedHeight.toPx() + hiddenOffset
    }
    val minExpandedVisibleHeightPx = with(density) { minExpandedVisibleHeight.toPx() }
    val dragLimitY = expandedHeightPx - minExpandedVisibleHeightPx // 可拖动的Y轴范围（展开后）

    val transition = updateTransition(targetState = isExpanded, label = "CardExpandCollapse")
    // 右上角图标选择
    val iconRotation by transition.animateFloat(
        label = "IconRotation",
        transitionSpec = { tween(durationMillis = 300) }
    ) { expanded ->
        if (expanded) 45f else 0f
    }
    // 外部容器的高度
    val containerHeight by transition.animateFloat(
        label = "ContainerHeight",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        }
    ) { expanded ->
        if (expanded) expandedHeightPx else collapsedHeightPx
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { (containerHeight).toDp() })
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY =
                        if (isExpanded) 0f else hiddenOffset.toFloat() + dragOffsetY.value
                }
                .pointerInput(isExpanded) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            // 拖动结束，如果是折叠状态，用弹簧动画回弹
                            if (!isExpanded) {
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val newOffsetY = if (isExpanded) {
                            // 展开状态：自由拖动，有边界
                            (dragOffsetY.value + dragAmount).coerceIn(0f, dragLimitY)
                        } else {
                            // 折叠状态：带阻尼的回弹拖动
                            dragOffsetY.value + dragAmount * 0.08f
                        }
                        coroutineScope.launch {
                            dragOffsetY.snapTo(newOffsetY)
                        }
                    }
                },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors()
                .copy(containerColor = colorScheme.surface, contentColor = colorScheme.onSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DragHandle()
                    this.content()
                }

                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    onClick = {
                        coroutineScope.launch {
                            // 如果卡片当前因拖动而有位移，先让它动画归位
                            if (dragOffsetY.value != 0f) {
                                dragOffsetY.animateTo(0f, animationSpec = tween(150))
                            }
                            // 切换展开/折叠状态
                            isExpanded = !isExpanded
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isExpanded) "收起卡片" else "展开卡片",
                        modifier = Modifier.rotate(iconRotation),
                        tint = colorScheme.onSurface
                    )
                }
            }
        }
    }
}