package com.cyc.yearlymemoir.venassists

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.provider.Settings
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.services.OverlayConfirmManager
import com.cyc.yearlymemoir.services.WXFloatingWindowService
import com.cyc.yearlymemoir.utils.LogRecorder
import com.google.mlkit.vision.common.InputImage
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.Step
import com.ven.assists.stepper.StepCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min

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
                if (ds.getInt("wx_我_x") > 0) {
                    withContext(Dispatchers.Main) {
                        val globalX = ds.getInt("wx_我_x")
                        val globalY = ds.getInt("wx_我_y")
                        LogRecorder.i("微信自动化", "获取缓存“我”坐标：($globalX, $globalY)")
                        AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                    }
                } else {
                    val screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                    val width = screenshot.width
                    val height = screenshot.height

                    val bottomRightRect = Rect(width / 2, height / 2, width, height)
                    val bottomRightCropped = Bitmap.createBitmap(
                        screenshot,
                        bottomRightRect.left,
                        bottomRightRect.top,
                        bottomRightRect.width(),
                        bottomRightRect.height()
                    )

                    val (x, y) = findAndClickText(bottomRightCropped, "我", bottomRightRect)
                    ds.putInt("wx_我_x", x)
                    ds.putInt("wx_我_y", y)
                }
                Step.get(GetBalanceStep.ENTER_WX_服务_TAB.ordinal)
            }
            .next(GetBalanceStep.ENTER_WX_服务_TAB.ordinal) {
                // 2. 处理左上角区域 -> 寻找“服务”
                val ds = flowCtx.ds
                if (ds.getInt("wx_服务_x") > 0) {
                    withContext(Dispatchers.Main) {
                        val globalX = ds.getInt("wx_服务_x")
                        val globalY = ds.getInt("wx_服务_y")
                        LogRecorder.i("微信自动化", "获取缓存“服务”坐标：($globalX, $globalY)")
                        AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                    }
                } else {
                    val screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                    val width = screenshot.width
                    val height = screenshot.height

                    val topLeftRect = Rect(0, 0, width / 2, height / 2)
                    val topLeftCropped = Bitmap.createBitmap(
                        screenshot,
                        topLeftRect.left,
                        topLeftRect.top,
                        topLeftRect.width(),
                        topLeftRect.height()
                    )

                    val (x, y) = findAndClickText(topLeftCropped, "服务", topLeftRect)
                    ds.putInt("wx_服务_x", x)
                    ds.putInt("wx_服务_y", y)
                }
                Step.get(GetBalanceStep.ENTER_WX_钱包_TAB.ordinal)
            }
            .next(GetBalanceStep.ENTER_WX_钱包_TAB.ordinal) {
                // 3. 处理右上角区域 -> 寻找“钱包”
                val ds = flowCtx.ds
                if (ds.getInt("wx_钱包_x") > 0) {
                    withContext(Dispatchers.Main) {
                        val globalX = ds.getInt("wx_钱包_x")
                        val globalY = ds.getInt("wx_钱包_y")
                        LogRecorder.i("微信自动化", "获取缓存“钱包”坐标：($globalX, $globalY)")
                        AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                    }
                } else {
                    val screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                    val width = screenshot.width
                    val height = screenshot.height

                    val topRightRect = Rect(width / 2, 0, width, height / 2)
                    val topRightCropped = Bitmap.createBitmap(
                        screenshot,
                        topRightRect.left,
                        topRightRect.top,
                        topRightRect.width(),
                        topRightRect.height()
                    )

                    val (x, y) = findAndClickText(topRightCropped, "钱包", topRightRect)
                    ds.putInt("wx_钱包_x", x)
                    ds.putInt("wx_钱包_y", y)
                }
                Step.get(GetBalanceStep.GET_WX_BALANCE.ordinal)
            }
            .next(GetBalanceStep.GET_WX_BALANCE.ordinal) {
                var screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none

                // 上半部分图片坐标
                val topHalfRect =
                    Rect(screenshot.width / 10, 0, screenshot.width, screenshot.height / 2)

                // 截图确保“零钱”与“零钱通”显示完全
                var qianPosition: Rect? = null
                var tongPosition: Rect? = null
                var topHalfBitmap: Bitmap
                var retryNumber = 3
                do {
                    // Step 1: 截取上半部分图片
                    topHalfBitmap = Bitmap.createBitmap(
                        screenshot,
                        topHalfRect.left,
                        topHalfRect.top,
                        topHalfRect.width() - (10..150).random(),
                        topHalfRect.height() + (10..100).random()
                    )

                    // Step 2: OCR 上半部分，寻找 "零钱" 和 "零钱通"
                    val 零钱通文字区域图片 = if (flowCtx.ds.getInt("wx_零钱_left") == 0) {
                        topHalfBitmap
                    } else {
                        val tmp = Rect(
                            screenshot.width / 10,
                            flowCtx.ds.getInt("wx_零钱_top"),
                            screenshot.width * 3 / 10,
                            flowCtx.ds.getInt("wx_零钱_bottom")
                        )
                        Bitmap.createBitmap(
                            screenshot,
                            tmp.left,
                            tmp.top,
                            tmp.width() - (10..20).random(),
                            tmp.height() + (10..100).random()
                        )
                    }
                    val image = InputImage.fromBitmap(零钱通文字区域图片, 0)
                    val result = recognizer.process(image).await()

                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                println(element.text)
                                when {
                                    element.text.contains("零钱通") -> {
                                        tongPosition = element.boundingBox
                                        LogRecorder.i("微信自动化", "tongPosition: $tongPosition")
                                        continue
                                    }

                                    element.text.contains("零钱") -> {
                                        qianPosition = element.boundingBox
                                        LogRecorder.i("微信自动化", "qianPosition: $qianPosition")
                                    }
                                }
                            }
                        }
                    }

                    if (qianPosition != null && tongPosition != null) {
                        break
                    }
                    retryNumber--
                    LogRecorder.i("微信自动化", "未找到“零钱”或“零钱通”，剩余 $retryNumber 次重试")
                    delay(3 * 1000)
                    screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                } while (retryNumber > 0)

                if (qianPosition == null || tongPosition == null) {
                    LogRecorder.i("微信自动化", "无法找到“零钱”或“零钱通”，请检查代码逻辑或微信版本")
                    return@next Step.get(nextStepId)
                }

                // Step 3: 截取该区域的右半部分 -> 零钱余额图片坐标
                val rightPartRect: Rect = if (flowCtx.ds.getInt("wx_零钱_left") == 0) {
                    val tmp = Rect(
                        (topHalfRect.width() / 2f).toInt(),
                        qianPosition!!.top,
                        topHalfRect.width() * 19 / 20 - (5 * MainActivity.metrics.density).toInt(),
                        tongPosition!!.bottom
                    )
                    if (Settings.canDrawOverlays(flowCtx.context)) {
                        startFloatingService(
                            flowCtx.context, Rect(
                                tmp.left + screenshot.width / 10,
                                tmp.top,
                                tmp.right + screenshot.width / 10,
                                tmp.bottom
                            )
                        )
                        LogRecorder.d("微信自动化", "余额位置弹窗确认。")
                        val confirmed = OverlayConfirmManager.show(
                            context = flowCtx.context.applicationContext,
                            title = "零钱与零钱通位置确认",
                            message = "请确认识别的区域是否完整覆盖零钱与零钱通的余额数字，完整覆盖则点击确认，否则点击取消进行重试"
                        )
                        if (confirmed) {
                            LogRecorder.d("ConfirmTest", "用户已确认，执行下一步！")
                            flowCtx.ds.putInt("wx_零钱_left", tmp.left)
                            flowCtx.ds.putInt("wx_零钱_top", tmp.top)
                            flowCtx.ds.putInt("wx_零钱_right", tmp.right)
                            flowCtx.ds.putInt("wx_零钱_bottom", tmp.bottom)
                        } else {
                            LogRecorder.d("ConfirmTest", "用户已取消，重新识图。")
                            stopFloatingService(flowCtx.context)
                            return@next Step.repeat
                        }
                        stopFloatingService(flowCtx.context)
                    }
                    tmp
                } else {
                    Rect(
                        flowCtx.ds.getInt("wx_零钱_left"),
                        flowCtx.ds.getInt("wx_零钱_top"),
                        flowCtx.ds.getInt("wx_零钱_right"),
                        flowCtx.ds.getInt("wx_零钱_bottom"),
                    )
                }

                // Ocr Test Time Augmentation
                val leftRandomOffset = (100..150).random()
                val topRandomOffset = -(0..30).random()
                println(rightPartRect)
                println(
                    min(
                        rightPartRect.right,
                        topHalfBitmap.width
                    ) - (rightPartRect.left + leftRandomOffset) - 1
                )
                val processedBitmap = Bitmap.createBitmap(
                    topHalfBitmap,
                    rightPartRect.left + leftRandomOffset,
                    rightPartRect.top + topRandomOffset,
                    min(
                        rightPartRect.right,
                        topHalfBitmap.width
                    ) - (rightPartRect.left + leftRandomOffset) - 1,
                    rightPartRect.height() - topRandomOffset
                )

                // Step 4: OCR 提取数字
                val finalImage = InputImage.fromBitmap(processedBitmap, 0)
                val finalResult = recognizer.process(finalImage).await()

                fun extractBalance(input: String): Double? {
                    val pattern = Regex("\\b(\\d+\\.?\\d*)\\b")
                    val match = pattern.find(input)
                    return match?.groupValues?.getOrNull(1)?.toDouble()
                }

                val extractedNumbers = finalResult.textBlocks
                    .flatMap { it.lines }
                    .flatMap { it.elements }
                    .mapNotNull { element -> extractBalance(element.text) }

                var balance = when (extractedNumbers.isEmpty()) {
                    false -> {
                        LogRecorder.i(
                            "微信自动化",
                            "微信识别到余额：${extractedNumbers.joinToString(", ")}"
                        )
                        extractedNumbers.sum()
                    }

                    true -> {
                        LogRecorder.i("微信自动化", "未识别到余额数字")
                        0.0
                    }
                }
                balance = "%.2f".format(balance).toDouble()

                LogRecorder.i("微信自动化", "微信余额：$balance")
                flowCtx.ds.updateTodayBalance(BalanceChannelType.WX)
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

    // OCR 查找点击逻辑（迁移自 GetTodayBalanceStepImpl）
    private suspend fun findAndClickText(
        cropped: Bitmap,
        targetText: String,
        screenRegion: Rect
    ): Pair<Int, Int> {
        val image = InputImage.fromBitmap(cropped, 0)
        val result = withContext(Dispatchers.IO) {
            recognizer.process(image).await()
        }
        for (block in result.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    if (element.text.contains(targetText, ignoreCase = true)) {
                        val bounds = element.boundingBox ?: continue
                        val centerX = bounds.centerX()
                        val centerY = bounds.centerY()

                        val globalX = screenRegion.left + centerX
                        val globalY = screenRegion.top + centerY

                        withContext(Dispatchers.Main) {
                            AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                            LogRecorder.i(
                                "自动化获取余额",
                                "找到目标：“$targetText”，点击坐标：($globalX, $globalY)"
                            )
                        }
                        return Pair(globalX, globalY)
                    }
                }
            }
        }
        LogRecorder.i("自动化获取余额", "未在区域找到目标：“$targetText”")
        return Pair(0, 0)
    }

    private fun startFloatingService(ctx: android.content.Context, rect: Rect) {
        if (Settings.canDrawOverlays(ctx)) {
            val intent = Intent(ctx, WXFloatingWindowService::class.java).apply {
                putExtra(WXFloatingWindowService.KEY_RECT, rect)
            }
            ctx.startService(intent)
        }
    }

    private fun stopFloatingService(ctx: android.content.Context) {
        ctx.stopService(Intent(ctx, WXFloatingWindowService::class.java))
    }
}