package com.cyc.yearlymemoir.venassists

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.unaryMinus

/**
 * 从 Bitmap 进行文字/区域粗分割：
 * 1) 缩放到指定宽度（默认 320）
 * 2) 灰度化
 * 3) 大津法 (Otsu) 自适应二值化（保证黑底白字）
 * 4) 12x3 膨胀
 * 5) 4-连通域提取，输出归一化矩形区域
 */
object ImageProcessing {

    data class NormRect(
        val x: Float, // 相对宽度归一化 [0,1]
        val y: Float, // 相对高度归一化 [0,1]
        val w: Float, // 相对宽度
        val h: Float  // 相对高度
    )

    /**
     * 主入口：从 Bitmap 获取归一化连通域区域与二值化膨胀图（0/255）
     */
    fun getNormalizedRegions(
        bitmap: Bitmap,
        standardWidth: Int = 320,
        saveIntermediates: Boolean = false
    ): Pair<List<NormRect>, Array<IntArray>> {
        val origW = bitmap.width
        val origH = bitmap.height
        if (origW <= 0 || origH <= 0) return emptyList<NormRect>() to arrayOf()

        // 检查是否是硬件位图并转换
        val readAbleBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        // 读取原图所有像素（ARGB）
        val origPixels = IntArray(origW * origH)
        readAbleBitmap.getPixels(origPixels, 0, origW, 0, 0, origW, origH)

        // 最近邻缩放 + 灰度化
        val scale = standardWidth.toFloat() / origW.toFloat()
        val newH = max(1, (origH * scale).roundToInt())
        val newW = standardWidth
        val gray = Array(newH) { IntArray(newW) }
        for (r in 0 until newH) {
            val origR = (r / scale).toInt()
            val baseR = origR * origW
            for (c in 0 until newW) {
                val origC = (c / scale).toInt()
                val color = origPixels[baseR + origC]
                // 经典灰度：0.299R + 0.587G + 0.114B
                val rr = Color.red(color)
                val gg = Color.green(color)
                val bb = Color.blue(color)
                val g = ((0.299f * rr) + (0.587f * gg) + (0.114f * bb)).toInt()
                gray[r][c] = g
            }
        }

        // 大津法阈值 + 黑底白字
        val binary = adaptiveThreshold(gray)

        // 膨胀（12x3）
        val kw = 12
        val kh = 3
        val hx = kw / 2
        val hy = kh / 2
        val dilated = Array(newH) { IntArray(newW) }
        for (r in 0 until newH) {
            for (c in 0 until newW) {
                if (binary[r][c] == 255) {
                    val r0 = max(0, r - hy)
                    val r1 = min(newH - 1, r + hy)
                    val c0 = max(0, c - hx)
                    val c1 = min(newW - 1, c + hx)
                    for (yy in r0..r1) {
                        for (xx in c0..c1) {
                            dilated[yy][xx] = 255
                        }
                    }
                }
            }
        }

        // 连通域（4-连通）
        val visited = Array(newH) { BooleanArray(newW) }
        val regions = mutableListOf<NormRect>()

        fun idxOf(y: Int, x: Int) = y * newW + x

        for (r in 0 until newH) {
            for (c in 0 until newW) {
                if (dilated[r][c] == 255 && !visited[r][c]) {
                    var minX = c
                    var maxX = c
                    var minY = r
                    var maxY = r

                    val q: ArrayDeque<Int> = ArrayDeque()
                    q.add(idxOf(r, c))
                    visited[r][c] = true

                    while (q.isNotEmpty()) {
                        val id = q.removeFirst()
                        val y = id / newW
                        val x = id % newW

                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y

                        // 4 邻域
                        if (x + 1 < newW && dilated[y][x + 1] == 255 && !visited[y][x + 1]) {
                            visited[y][x + 1] = true
                            q.add(idxOf(y, x + 1))
                        }
                        if (x - 1 >= 0 && dilated[y][x - 1] == 255 && !visited[y][x - 1]) {
                            visited[y][x - 1] = true
                            q.add(idxOf(y, x - 1))
                        }
                        if (y + 1 < newH && dilated[y + 1][x] == 255 && !visited[y + 1][x]) {
                            visited[y + 1][x] = true
                            q.add(idxOf(y + 1, x))
                        }
                        if (y - 1 >= 0 && dilated[y - 1][x] == 255 && !visited[y - 1][x]) {
                            visited[y - 1][x] = true
                            q.add(idxOf(y - 1, x))
                        }
                    }

                    val cw = maxX - minX + 1
                    val ch = maxY - minY + 1
                    val area = cw * ch
                    if (area < 625) continue
                    if (area > 10000) continue

                    if (saveIntermediates) {
                        for (xx in minX until maxX) {
                            for (yy in minY until maxY) {
                                dilated[yy][xx] = 125
                            }
                        }
                    }

                    val nx = minX.toFloat() / newW
                    val ny = minY.toFloat() / newH
                    val nw = cw.toFloat() / newW
                    val nh = ch.toFloat() / newH
                    regions.add(NormRect(nx, ny, nw, nh))
                }
            }
        }

        // 可选：保存中间结果到 Downloads
        if (saveIntermediates) {
            try {
                val grayBmp = arrayToBitmap(gray)
                val binaryBmp = arrayToBitmap(binary)
                val dilatedBmp = arrayToBitmap(dilated)
                saveBitmapToDownloads(grayBmp, "YM_${'$'}ts_gray.png")
                saveBitmapToDownloads(binaryBmp, "YM_${'$'}ts_binary.png")
                saveBitmapToDownloads(dilatedBmp, "YM_${'$'}ts_dilated.png")
                grayBmp.recycle()
                binaryBmp.recycle()
                dilatedBmp.recycle()
            } catch (e: Exception) {
                Log.w("ImageProcessing", "Save intermediates failed: ${'$'}e")
            }
        }

        return regions to dilated
    }

    /**
     * 大津法阈值，返回二值化图（0/255），并在白像素占比 > 50% 时整体翻转，确保黑底白字
     */
    private fun adaptiveThreshold(gray: Array<IntArray>): Array<IntArray> {
        val rows = gray.size
        if (rows == 0) return arrayOf()
        val cols = gray[0].size
        val total = rows * cols

        val hist = IntArray(256)
        var sumAll = 0L
        for (r in 0 until rows) {
            val row = gray[r]
            for (c in 0 until cols) {
                val p = row[c]
                hist[p]++
                sumAll += (p * 1L)
            }
        }

        var wB = 0L
        var sumB = 0L
        var maxVar = -1.0
        var bestT = 0
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0L) continue
            val wF = total - wB
            if (wF == 0L) break

            sumB += t.toLong() * hist[t]
            val mB = sumB.toDouble() / wB
            val mF = (sumAll - sumB).toDouble() / wF
            val variance = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (variance > maxVar) {
                maxVar = variance
                bestT = t
            }
        }

        val binary = Array(rows) { IntArray(cols) }
        var whiteCount = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (gray[r][c] > bestT) {
                    binary[r][c] = 255
                    whiteCount++
                } else {
                    binary[r][c] = 0
                }
            }
        }

        // 黑底白字：如果白色像素过半，则翻转
        if (whiteCount > total / 2) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    binary[r][c] = 255 - binary[r][c]
                }
            }
        }
        return binary
    }

    // 将 0/255 的二维数组转为灰度 Bitmap
    private fun arrayToBitmap(arr: Array<IntArray>): Bitmap {
        val h = arr.size
        val w = if (h > 0) arr[0].size else 1
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        var idx = 0
        for (r in 0 until h) {
            val row = arr[r]
            for (c in 0 until w) {
                val v = row[c].coerceIn(0, 255)
                pixels[idx++] = Color.argb(255, v, v, v)
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    // 将 Bitmap 保存到外部存储的 Downloads 目录
    @Suppress("DEPRECATION")
    private fun saveBitmapToDownloads(
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Boolean {
        return try {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val outFile = File(downloadsDir, fileName)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(format, quality, fos)
                fos.flush()
            }
            true
        } catch (e: Exception) {
            Log.w("ImageProcessing", "Failed writing to Downloads: ${'$'}e")
            false
        }
    }

    private fun toReadableBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    }

    fun areBitmapsSame(first: Bitmap, second: Bitmap): Boolean {
        if (first.width != second.width || first.height != second.height) return false
        val b1 = toReadableBitmap(first)
        val b2 = toReadableBitmap(second)
        val width = b1.width
        val height = b1.height
        val size = width * height
        val p1 = IntArray(size)
        val p2 = IntArray(size)
        b1.getPixels(p1, 0, width, 0, 0, width, height)
        b2.getPixels(p2, 0, width, 0, 0, width, height)
        return p1.contentEquals(p2)
    }

    /**
     * 归一化点 -> 原图绝对坐标（考虑抖动）
     */
    fun mapToGlobal(
        normPt: Pair<Float, Float>,
        cropRect: Rect,
    ): Pair<Int, Int> {
        val (nx, ny) = normPt
        val gx = cropRect.left
        val gy = cropRect.top
        val gw = cropRect.width()
        val gh = cropRect.height()

        val fx = gx + (nx * gw).toInt()
        val fy = gy + (ny * gh).toInt()
        return fx to fy
    }

    fun randomClickXY(
        clickXY: Pair<Int, Int>,
        jitter: Pair<Int, Int> = 0 to 0
    ): Pair<Float, Float> {
        val (cx, cy) = clickXY
        val (jx, jy) = jitter
        val jxVal = if (jx == 0) 0f else Random.nextFloat() * 2 * jx - jx
        val jyVal = if (jy == 0) 0f else Random.nextFloat() * 2 * jy - jy
        val rx = cx.toFloat() + jxVal
        val ry = cy.toFloat() + jyVal
        return rx to ry
    }

    // ================= 策略 =================

    /** 取 x 轴最右侧两个区域，计算特定中心点 */
    fun strategyLastTwoCenter(regions: List<NormRect>): Pair<Float, Float>? {
        if (regions.size < 2) return null
        val sorted = regions.sortedBy { it.x }
        var r1 = sorted[sorted.size - 2]
        var r2 = sorted[sorted.size - 1]
        if (r1.y > r2.y) {
            val tmp = r1
            r1 = r2
            r2 = tmp
        }
        val targetX = r1.x + r1.w / 2f
        val targetY = (r1.y + r2.y + r2.h) / 2f
        return targetX to targetY
    }

    /** 取第一列 (x 差异小) 的第一个 (y 最小)，取其右侧区域 */
    fun strategyFirstColRight(regions: List<NormRect>): Pair<Float, Float>? {
        if (regions.isEmpty()) return null
        val sorted = regions.sortedBy { it.x }
        val baseX = sorted.first().x
        val col = sorted.filter { kotlin.math.abs(it.x - baseX) < 0.005f }
        if (col.isEmpty()) return null
        val top = col.minByOrNull { it.y } ?: return null
        val targetX = 0.5f
        val targetY = top.y + top.h / 2f
        return targetX to targetY
    }

    /** 简单示例：按 y 排序后取第一个的中心点 */
    fun strategyFindWallet(regions: List<NormRect>): Pair<Float, Float>? {
        if (regions.isEmpty()) return null
        val top = regions.sortedBy { it.y }.first()
        val midX = top.x + top.w / 2f
        val midY = top.y + top.h / 2f
        return midX to midY
    }
}
