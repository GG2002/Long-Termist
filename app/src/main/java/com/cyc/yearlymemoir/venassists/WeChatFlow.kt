package com.cyc.yearlymemoir.venassists

import android.graphics.Bitmap
import android.graphics.Rect
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.ui.personalbanlance.getTodayString
import com.cyc.yearlymemoir.utils.LogRecorder
import com.google.mlkit.vision.common.InputImage
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.Step
import com.ven.assists.stepper.StepCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.lang.Thread.sleep

class WeChatFlow(flowCtx: FlowContext) : BaseBalanceFlow(flowCtx) {
    override fun register(collector: StepCollector, nextStepId: Int) {
        collector
            .next(GetBalanceStep.RESTART_WX.ordinal) { op ->
                flowCtx.tryUpdateDeferred(op.data)

                if (
                    !flowCtx.ds.shouldUpdateBalance(BalanceChannelType.WX) ||
                    !restartApp("com.tencent.mm")
                ) {
                    return@next Step.get(nextStepId, delay = 0)
                }
                Step.get(GetBalanceStep.ENTER_WX_我的_TAB.ordinal, delay = 2000)
            }
            .next(GetBalanceStep.ENTER_WX_我的_TAB.ordinal) {
                // 1. 处理右下角区域 -> 寻找“我”
                val ds = flowCtx.ds
                val screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                var centerXY = Pair(0, 0)
                if (ds.getInt("wx_我_x") > 0) {
                    withContext(Dispatchers.Main) {
                        val globalX = ds.getInt("wx_我_x")
                        val globalY = ds.getInt("wx_我_y")
                        LogRecorder.i("微信自动化", "获取缓存“我”坐标：($globalX, $globalY)")
                        centerXY = Pair(globalX, globalY)
                    }
                } else {
                    val width = screenshot.width
                    val height = screenshot.height

                    val bottomRightRect = Rect(width / 2, height * 4 / 5, width, height)
                    val bottomRightCropped = Bitmap.createBitmap(
                        screenshot,
                        bottomRightRect.left,
                        bottomRightRect.top,
                        bottomRightRect.width(),
                        bottomRightRect.height()
                    )

                    val (regions, _) = ImageProcessing.getNormalizedRegions(bottomRightCropped)
                    val ptNorm =
                        ImageProcessing.strategyLastTwoCenter(regions) ?: return@next Step.get(
                            nextStepId,
                            delay = 0
                        )
                    centerXY = ImageProcessing.mapToGlobal(ptNorm, bottomRightRect)
                    ds.putInt("wx_我_x", centerXY.first)
                    ds.putInt("wx_我_y", centerXY.second)
                }

                // 比对点击前后的区域是否变化
                val halfW = 50
                val halfH = 30
                val beforeRegion = cropAroundCenter(screenshot, centerXY, halfW, halfH)
                var changed = false
                for (i in 0 until 4) {
                    val finalXY = ImageProcessing.randomClickXY(centerXY, halfW to halfH)
                    val (x, y) = finalXY
                    val tt = withContext(Dispatchers.Main) {
                        AssistsCore.gestureClick(x, y)
                    }
                    LogRecorder.i(
                        "自动化获取余额",
                        "点击目标：“我”，坐标：($x, $y) $tt"
                    )

                    delay(500)
                    val nextScreenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                    val afterRegion = cropAroundCenter(nextScreenshot, centerXY, 50, 30)
                    if (!ImageProcessing.areBitmapsSame(beforeRegion, afterRegion)) {
                        changed = true
                        break
                    }
                }

                Step.get(GetBalanceStep.ENTER_WX_服务_TAB.ordinal)
            }
            .next(GetBalanceStep.ENTER_WX_服务_TAB.ordinal) {
                // 2. 处理左上角区域 -> 寻找“服务”
                val ds = flowCtx.ds
                val screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                var centerXY = Pair(0, 0)
                if (ds.getInt("wx_服务_x") > 0) {
                    withContext(Dispatchers.Main) {
                        val globalX = ds.getInt("wx_服务_x")
                        val globalY = ds.getInt("wx_服务_y")
                        LogRecorder.i("微信自动化", "获取缓存“服务”坐标：($globalX, $globalY)")
                        centerXY = Pair(globalX, globalY)
                    }
                } else {
                    val width = screenshot.width
                    val height = screenshot.height

                    val topLeftRect = Rect(0, height / 5, width / 2, height * 3 / 5)
                    val topLeftCropped = Bitmap.createBitmap(
                        screenshot,
                        topLeftRect.left,
                        topLeftRect.top,
                        topLeftRect.width(),
                        topLeftRect.height()
                    )

                    val (regions, _) = ImageProcessing.getNormalizedRegions(topLeftCropped)
                    val ptNorm =
                        ImageProcessing.strategyFirstColRight(regions) ?: return@next Step.get(
                            nextStepId,
                            delay = 0
                        )
                    centerXY = ImageProcessing.mapToGlobal(ptNorm, topLeftRect)
                    ds.putInt("wx_服务_x", centerXY.first)
                    ds.putInt("wx_服务_y", centerXY.second)
                }

                // 比对点击前后的区域是否变化
                val halfW = 50
                val halfH = 30
                val beforeRegion = cropAroundCenter(screenshot, centerXY, halfW, halfH)
                var changed = false
                for (i in 0 until 4) {
                    val finalXY = ImageProcessing.randomClickXY(
                        centerXY,
                        halfW to halfH
                    )
                    val (x, y) = finalXY
                    val tt = withContext(Dispatchers.Main) {
                        AssistsCore.gestureClick(x, y)
                    }
                    LogRecorder.i(
                        "自动化获取余额",
                        "点击目标：“服务”，坐标：($x, $y) $tt"
                    )

                    delay(500)
                    val nextScreenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                    val afterRegion = cropAroundCenter(nextScreenshot, centerXY, halfW, halfH)
                    if (!ImageProcessing.areBitmapsSame(beforeRegion, afterRegion)) {
                        changed = true
                        break
                    }
                }
                Step.get(GetBalanceStep.ENTER_WX_钱包_TAB.ordinal)
            }
            .next(GetBalanceStep.ENTER_WX_钱包_TAB.ordinal) {
                // 3. 处理右上角区域 -> 寻找“钱包”
                val ds = flowCtx.ds
                val screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                var centerXY = Pair(0, 0)
                if (ds.getInt("wx_钱包_x") > 0) {
                    withContext(Dispatchers.Main) {
                        val globalX = ds.getInt("wx_钱包_x")
                        val globalY = ds.getInt("wx_钱包_y")
                        LogRecorder.i("微信自动化", "获取缓存“钱包”坐标：($globalX, $globalY)")
                        centerXY = Pair(globalX, globalY)
                    }
                } else {
                    val width = screenshot.width
                    val height = screenshot.height

                    val topRightRect = Rect(width / 2, height / 10, width, height / 4)
                    val topRightCropped = Bitmap.createBitmap(
                        screenshot,
                        topRightRect.left,
                        topRightRect.top,
                        topRightRect.width(),
                        topRightRect.height()
                    )

                    val (regions, _) = ImageProcessing.getNormalizedRegions(topRightCropped)
                    val ptNorm =
                        ImageProcessing.strategyFirstColRight(regions) ?: return@next Step.get(
                            nextStepId,
                            delay = 0
                        )
                    centerXY = ImageProcessing.mapToGlobal(ptNorm, topRightRect)
                    ds.putInt("wx_钱包_x", centerXY.first)
                    ds.putInt("wx_钱包_y", centerXY.second)
                }

                // 比对点击前后的区域是否变化
                val halfW = 60
                val halfH = 40
                val beforeRegion = cropAroundCenter(screenshot, centerXY, halfW, halfH)
                var changed = false
                for (i in 0 until 4) {
                    val finalXY = ImageProcessing.randomClickXY(
                        centerXY,
                        halfW to halfH
                    )
                    val (x, y) = finalXY
                    val tt = withContext(Dispatchers.Main) {
                        AssistsCore.gestureClick(x, y)
                    }
                    LogRecorder.i(
                        "自动化获取余额",
                        "点击目标：“钱包”，坐标：($x, $y) $tt"
                    )

                    delay(500)
                    val nextScreenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                    val afterRegion = cropAroundCenter(nextScreenshot, centerXY, halfW, halfH)
                    if (!ImageProcessing.areBitmapsSame(beforeRegion, afterRegion)) {
                        changed = true
                        break
                    }
                }
                Step.get(GetBalanceStep.GET_WX_BALANCE.ordinal)
            }
            .next(GetBalanceStep.GET_WX_BALANCE.ordinal) {
                var screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none

                // 上 1/3 部分，右 1/2 部分图片坐标
                val width = screenshot.width
                val height = screenshot.height
                val topHalfRect = Rect(width / 2, height / 10, width, height / 3)

                // 截图确保“零钱”与“零钱通”的余额数字显示完全
                val ds = flowCtx.ds
                var regions: List<ImageProcessing.NormRect>
                var retryNumber = 3
                var topHalfBitmap: Bitmap
                do {
                    topHalfBitmap = Bitmap.createBitmap(
                        screenshot,
                        topHalfRect.left,
                        topHalfRect.top,
                        topHalfRect.width() - (10..100).random(),
                        topHalfRect.height() + (10..100).random()
                    )

                    val (tmpRegions, _) = ImageProcessing.getNormalizedRegions(topHalfBitmap, saveIntermediates = true)
                    regions = tmpRegions
                    if (regions.size == 2) {
                        break
                    }
                    retryNumber--
                    LogRecorder.i(
                        "微信自动化",
                        "“零钱”、“零钱通”未显示完全，剩余 $retryNumber 次重试"
                    )
                    delay(5 * 1000)
                    screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                } while (retryNumber > 0)

                var balance = 0.0
                for (region in regions) {
                    // 处理每个区域
                    val cropRect = Rect(
                        (region.x * topHalfBitmap.width).toInt(),
                        (region.y * topHalfBitmap.height).toInt(),
                        ((region.x + region.w) * topHalfBitmap.width).toInt(),
                        ((region.y + region.h) * topHalfBitmap.height).toInt()
                    )
                    // 进行裁剪
                    val croppedBitmap = Bitmap.createBitmap(
                        topHalfBitmap,
                        cropRect.left,
                        cropRect.top,
                        cropRect.width(),
                        cropRect.height()
                    )
                    // OCR 提取数字
                    val finalImage = InputImage.fromBitmap(croppedBitmap, 0)
                    val finalResult = recognizer.process(finalImage).await()
                    val extractedNumbers = finalResult.textBlocks
                        .flatMap { it.lines }
                        .flatMap { it.elements }
                        .mapNotNull { element ->
                            val pattern = Regex("\\b(\\d+\\.?\\d*)\\b")
                            val match = pattern.find(element.text)
                            match?.groupValues?.getOrNull(1)?.toDouble()
                        }
                    balance += extractedNumbers.sum()
                }

                LogRecorder.i("微信自动化", "微信余额：$balance")
                ds.updateTodayBalance(BalanceChannelType.WX)
                flowCtx.model.upsertBalance(
                    BalanceRecord(
                        sourceType = "微信",
                        recordDate = getTodayString(),
                        balance = balance,
                    )
                )
                LogRecorder.i("微信自动化", "获取微信余额完毕")
                Step.get(nextStepId)
            }
    }

    private fun cropAroundCenter(
        bitmap: Bitmap,
        center: Pair<Int, Int>,
        halfWidth: Int,
        halfHeight: Int
    ): Bitmap {
        val (cx, cy) = center
        val left = (cx - halfWidth).coerceAtLeast(0)
        val top = (cy - halfHeight).coerceAtLeast(0)
        val right = (cx + halfWidth).coerceAtMost(bitmap.width)
        val bottom = (cy + halfHeight).coerceAtMost(bitmap.height)
        val width = right - left
        val height = bottom - top
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}