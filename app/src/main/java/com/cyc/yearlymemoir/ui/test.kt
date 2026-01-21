package com.cyc.yearlymemoir.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

// 基础点：x, y 都是 0.0f ~ 1.0f 之间的小数
data class NormPoint(
    val xRatio: Float,
    val yRatio: Float
)

data class VoronoiBlock(
    val id: String,             // 点击事件回传这个 ID
    val colorHex: String,    // 例如 "#FF5733"
    val center: NormPoint,   // 归一化的重心，用于做缩放锚点
    val path: List<NormPoint> // 归一化的路径点列表
)

// 解析颜色十六进制字符串到 Color
private fun parseColor(hex: String): Color {
    // 支持 #RRGGBB 或 #AARRGGBB
    val clean = hex.removePrefix("#")
    return when (clean.length) {
        6 -> {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            Color(r, g, b)
        }

        8 -> {
            val a = clean.substring(0, 2).toInt(16)
            val r = clean.substring(2, 4).toInt(16)
            val g = clean.substring(4, 6).toInt(16)
            val b = clean.substring(6, 8).toInt(16)
            Color(r, g, b, a)
        }

        else -> Color.LightGray
    }
}

// 点是否在多边形内（射线法）
private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var intersectCount = 0
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        val minY = minOf(a.y, b.y)
        val maxY = maxOf(a.y, b.y)
        val xIntersection: Float
        if (point.y > minY && point.y <= maxY) {
            if (a.y != b.y) {
                xIntersection = a.x + (point.y - a.y) * (b.x - a.x) / (b.y - a.y)
                if (xIntersection > point.x) intersectCount++
            }
        }
    }
    return intersectCount % 2 == 1
}

// 将归一化点映射到当前 Canvas 尺寸
private fun mapToCanvas(norm: NormPoint, size: Size, inset: Float = 0f): Offset {
    val contentW = (size.width - 2f * inset).coerceAtLeast(0f)
    val contentH = (size.height - 2f * inset).coerceAtLeast(0f)
    return Offset(
        inset + norm.xRatio * contentW,
        inset + norm.yRatio * contentH
    )
}

// 构造路径
private fun buildPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isNotEmpty()) {
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val p = points[i]
            path.lineTo(p.x, p.y)
        }
        path.close()
    }
    return path
}

// 主组件：教堂彩玻璃效果
@Composable
fun VoronoiGlass(
    modifier: Modifier = Modifier,
    blocks: List<VoronoiBlock>,
    onBlockClick: (String) -> Unit = {},
    backgroundColor: Color = Color.Transparent,
    animationSafeInset: androidx.compose.ui.unit.Dp = 12.dp
) {
    val scope = rememberCoroutineScope()
    // 为每个块维护独立的缩放动画（果冻效果）
    val scaleMap = remember {
        mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>()
    }
    // 初始化/回收动画项
    LaunchedEffect(blocks) {
        blocks.forEach { block ->
            if (scaleMap[block.id] == null) scaleMap[block.id] = Animatable(1f)
        }
        val ids = blocks.map { it.id }.toSet()
        val toRemove = scaleMap.keys.filter { it !in ids }
        toRemove.forEach { scaleMap.remove(it) }
    }
    val density = LocalDensity.current
    val safeInsetPx = remember(density, animationSafeInset) {
        with(density) { animationSafeInset.toPx() }
    }
    Canvas(
        modifier = modifier
            .pointerInput(blocks, safeInsetPx) {
                // 简单点击事件识别：按下后计算点击点命中的块
                awaitPointerEventScope {
                    // 使用低门槛：仅处理按下事件
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed && change.previousPressed.not()) {
                            val s = size.toSize()
                            val drawInset = safeInsetPx
                                .coerceAtMost((minOf(s.width, s.height) / 1.5f) - 1f)
                                .coerceAtLeast(0f)
                            val clickPoint = change.position
                            // 遍历块，找出命中的第一个
                            val hit = blocks.firstOrNull { block ->
                                val polygon = block.path.map { mapToCanvas(it, s, drawInset) }
                                pointInPolygon(clickPoint, polygon)
                            }
                            if (hit != null) {
                                // 触发果冻动画
                                val anim = scaleMap[hit.id] ?: Animatable(1f).also {
                                    scaleMap[hit.id] = it
                                }
                                scope.launch {
                                    try {
                                        // 先快速弹到 1.08 再弹回 1.0，使用弹簧增加“duang”感
                                        anim.animateTo(
                                            1.03f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        anim.animateTo(
                                            1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    } catch (_: Throwable) {
                                        // ignore
                                    }
                                }
                                onBlockClick(hit.id)
                            }
                        }
                    }
                }
            }
    ) {
        val canvasSize = this.size
        // 先清背景，避免在缩放动画过程中产生残影
        drawRect(color = backgroundColor)
        val drawInset = safeInsetPx
            .coerceAtMost((minOf(canvasSize.width, canvasSize.height) / 2f) - 1f)
            .coerceAtLeast(0f)
        blocks.forEach { block ->
            val color = parseColor(block.colorHex)
            val polygonPoints = block.path.map { mapToCanvas(it, canvasSize, drawInset) }
            val path = buildPath(polygonPoints)
            val centerPx = mapToCanvas(block.center, canvasSize, drawInset)
            val scale = scaleMap[block.id]?.value ?: 1f

            // 在重心处进行缩放，避免漂移
            withTransform({
                translate(centerPx.x, centerPx.y)
                scale(scale)
                translate(-centerPx.x, -centerPx.y)
            }) {
                // 玻璃填充
                drawPath(path = path, color = color)
                // 玻璃描边（轻微白色 + 透明，增加质感）
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }
    }
}

// 预览
@Composable
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 200
)
fun TestPreview(modifier: Modifier = Modifier) {
    val sample = remember {
        listOf(
            VoronoiBlock(
                id = "A",
                colorHex = "#E0F8D0",
                center = NormPoint(0.1303f, 0.0585f),
                path = listOf(
                    NormPoint(0.248f, 0.704f),
                    NormPoint(0.196f, 0.985f),
                    NormPoint(0.006f, 0.986f),
                    NormPoint(0.006f, 0.444f),
                    NormPoint(0.161f, 0.425f),
                    NormPoint(0.296f, 0.463f),
                    NormPoint(0.248f, 0.704f),
                )
            ),
            VoronoiBlock(
                id = "B",
                colorHex = "#98FB98",
                center = NormPoint(0.1736f, 0.2479f),
                path = listOf(
                    NormPoint(0.360f, 0.145f),
                    NormPoint(0.334f, 0.259f),
                    NormPoint(0.254f, 0.296f),
                    NormPoint(0.126f, 0.296f),
                    NormPoint(0.011f, 0.211f),
                    NormPoint(0.010f, 0.011f),
                    NormPoint(0.390f, 0.010f),
                    NormPoint(0.360f, 0.145f),
                )
            ),
            VoronoiBlock(
                id = "C",
                colorHex = "#90EE90",
                center = NormPoint(0.1321f, 0.6660f),
                path = listOf(
                    NormPoint(0.112f, 0.344f),
                    NormPoint(0.104f, 0.397f),
                    NormPoint(0.007f, 0.416f),
                    NormPoint(0.006f, 0.234f),
                    NormPoint(0.120f, 0.318f),
                    NormPoint(0.112f, 0.344f),
                )
            ),
            VoronoiBlock(
                id = "D",
                colorHex = "#32CD32",
                center = NormPoint(0.0842f, 0.9828f),
                path = listOf(
                    NormPoint(0.244f, 0.374f),
                    NormPoint(0.250f, 0.419f),
                    NormPoint(0.123f, 0.401f),
                    NormPoint(0.141f, 0.322f),
                    NormPoint(0.239f, 0.322f),
                    NormPoint(0.244f, 0.374f),
                )
            ),
            VoronoiBlock(
                id = "E",
                colorHex = "#3CB371",
                center = NormPoint(0.1640f, 0.0390f),
                path = listOf(
                    NormPoint(0.316f, 0.363f),
                    NormPoint(0.305f, 0.445f),
                    NormPoint(0.266f, 0.416f),
                    NormPoint(0.257f, 0.322f),
                    NormPoint(0.341f, 0.274f),
                    NormPoint(0.316f, 0.363f),
                )
            ),
            VoronoiBlock(
                id = "G",
                colorHex = "#FA8072",
                center = NormPoint(0.6824f, 0.6505f),
                path = listOf(
                    NormPoint(0.985f, 0.983f),
                    NormPoint(0.216f, 0.984f),
                    NormPoint(0.358f, 0.258f),
                    NormPoint(0.984f, 0.337f),
                    NormPoint(0.985f, 0.983f),
                )
            ),
            VoronoiBlock(
                id = "H",
                colorHex = "#FF4500",
                center = NormPoint(0.7050f, 0.1425f),
                path = listOf(
                    NormPoint(0.983f, 0.305f),
                    NormPoint(0.378f, 0.227f),
                    NormPoint(0.409f, 0.014f),
                    NormPoint(0.983f, 0.014f),
                    NormPoint(0.983f, 0.305f),
                )
            )
        )
    }

    Box(
        modifier
    ) {
        VoronoiGlass(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            blocks = sample,
            animationSafeInset = 16.dp
        )
    }
}