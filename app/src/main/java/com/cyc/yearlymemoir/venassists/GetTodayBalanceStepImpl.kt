package com.cyc.yearlymemoir.venassists

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.model.BalanceChannelType
import com.cyc.yearlymemoir.model.DatastoreInit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.ven.assists.AssistsCore
import com.ven.assists.AssistsCore.click
import com.ven.assists.AssistsCore.getBoundsInScreen
import com.ven.assists.AssistsCore.getChildren
import com.ven.assists.AssistsCore.txt
import com.ven.assists.stepper.Step
import com.ven.assists.stepper.StepCollector
import com.ven.assists.stepper.StepImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException
import kotlin.math.min


enum class GetBalanceStep {
    RESTART_ZFB, ENTER_ZFB_理财_TAB, GET_ZFB_BALANCE, RESTART_WX, ENTER_WX_我的_TAB, ENTER_WX_服务_TAB, ENTER_WX_钱包_TAB, GET_WX_BALANCE, RESTART_YSF, GET_YSF_BALANCE, RETURN_APP
}

class GetTodayBalanceStepImpl : StepImpl() {
    companion object {
        private val recognizer: TextRecognizer by lazy {
            TextRecognition.getClient(
                ChineseTextRecognizerOptions.Builder().build()
            )
        }
    }

    private var context: Context = MainActivity.appContext
    private var ds: DatastoreInit = MainActivity.ds
    private var deferred: CompletableDeferred<String>? = null

    fun restartApp(context: Context, packageName: String) {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: run {
            // 应用未安装
            val displayName = when (packageName) {
                "com.eg.android.AlipayGphone" -> "支付宝"
                "com.tencent.mm" -> "微信"
                "com.unionpay" -> "云闪付"
                else -> packageName  // 如果没有匹配项，则返回原包名
            }
            Log.i("自动化获取余额", "$displayName 未安装")
            Toast.makeText(context, "$displayName 未安装", Toast.LENGTH_SHORT).show()
            return
        }

        // 清除旧任务栈，模拟重启
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "无法启动 $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (cont.isActive) {
                        cont.resume(task.result, null)
                    }
                } else {
                    if (cont.isActive) {
                        cont.resumeWithException(task.exception ?: UnknownError("Unknown error"))
                    }
                }
            }
        }
    }

    private suspend fun findAndClickText(
        cropped: Bitmap, targetText: String, screenRegion: Rect
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

                        // 转换为原始屏幕坐标
                        val globalX = screenRegion.left + centerX
                        val globalY = screenRegion.top + centerY

                        // 模拟点击
                        withContext(Dispatchers.Main) {
                            AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                            Log.i(
                                "自动化获取余额",
                                "找到目标：“$targetText”，点击坐标：($globalX, $globalY)"
                            )
                        }
                        return Pair(globalX, globalY)
                    }
                }
            }
        }
        Log.i("自动化获取余额", "未在区域找到目标：“$targetText”")
        return Pair(0, 0)
    }

    override fun onImpl(collector: StepCollector) {
        var screenshot: Bitmap
        var width: Int
        var height: Int

        collector.
            // 获取支付宝余额
        next(GetBalanceStep.RESTART_ZFB.ordinal) { op ->
            if (op.data != null) {
                deferred = op.data as? CompletableDeferred<String>
            }
            if (!ds.shouldUpdateBalance(BalanceChannelType.ZFB)) {
                return@next Step.get(GetBalanceStep.RESTART_WX.ordinal, delay = 0)
            }
            Log.i("支付宝自动化", "获取支付宝余额...")
            restartApp(context, "com.eg.android.AlipayGphone")
            return@next Step.get(GetBalanceStep.ENTER_ZFB_理财_TAB.ordinal)
        }.next(GetBalanceStep.ENTER_ZFB_理财_TAB.ordinal) { op ->
            // 进入理财 Tab
            val tabs = AssistsCore.findById("android:id/tabs")[0].getChildren()
                .sortedBy { it.getBoundsInScreen().left }
            tabs.getOrNull(1)?.click()
            return@next Step.get(GetBalanceStep.GET_ZFB_BALANCE.ordinal)
        }.next(GetBalanceStep.GET_ZFB_BALANCE.ordinal) {
            // 获取余额
            val 获取余额文字: () -> String = {
                val 余额 =
                    AssistsCore.findById("com.alipay.android.widget.fortunehome:id/fh_tv_assets_amount_num")
                if (余额.isEmpty()) {
                    throw IllegalStateException("无法获取支付宝余额")
                }
                val 余额文字 = 余额[0].txt()
                余额文字
            }
            var 余额文字 = 获取余额文字()
            // 余额被隐藏，要点击小眼睛
            var hidden = 余额文字.startsWith("**")
            if (hidden) {
                AssistsCore.findById("com.alipay.android.widget.fortunehome:id/hide_layout")[0].click()
                delay(2 * 1000)
                余额文字 = 获取余额文字()
            }
            var balance = 余额文字.replace(",", "").toDouble()
            balance = "%.2f".format(balance).toDouble()
            // 假如之前余额被隐藏，获取完余额后恢复原状
            if (hidden) {
                AssistsCore.findById("com.alipay.android.widget.fortunehome:id/hide_layout")[0].click()
            }

            Log.i("支付宝自动化", "支付宝余额：$balance")
            ds.setTodayBalance(BalanceChannelType.ZFB, balance)
            Log.i("支付宝自动化", "获取支付宝余额完毕")
            return@next Step.get(GetBalanceStep.RESTART_WX.ordinal)
        }.
            // 获取微信余额
        next(GetBalanceStep.RESTART_WX.ordinal) { op ->
            if (op.data != null) {
                deferred = op.data as? CompletableDeferred<String>
            }
            if (!ds.shouldUpdateBalance(BalanceChannelType.WX)) {
                return@next Step.get(GetBalanceStep.RESTART_YSF.ordinal, delay = 0)
            }
            Log.i("微信自动化", "获取微信余额...")
            restartApp(context, "com.tencent.mm")
            return@next Step.get(GetBalanceStep.ENTER_WX_我的_TAB.ordinal, delay = 2 * 1000)
        }.next(GetBalanceStep.ENTER_WX_我的_TAB.ordinal) {
            // 1. 处理右下角区域 -> 寻找“我”
            if (ds.getInt("wx_我_x") > 0) {
                withContext(Dispatchers.Main) {
                    val globalX = ds.getInt("wx_我_x")
                    val globalY = ds.getInt("wx_我_y")
                    Log.i("微信自动化", "获取缓存“我”坐标：($globalX, $globalY)")
                    AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                }
            } else {
                screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                width = screenshot.width
                height = screenshot.height

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
            return@next Step.get(GetBalanceStep.ENTER_WX_服务_TAB.ordinal)
        }.next(GetBalanceStep.ENTER_WX_服务_TAB.ordinal) {
            // 2. 处理左上角区域 -> 寻找“服务”
            if (ds.getInt("wx_服务_x") > 0) {
                withContext(Dispatchers.Main) {
                    val globalX = ds.getInt("wx_服务_x")
                    val globalY = ds.getInt("wx_服务_y")
                    Log.i("微信自动化", "获取缓存“服务”坐标：($globalX, $globalY)")
                    AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                }
            } else {
                screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                width = screenshot.width
                height = screenshot.height

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
            return@next Step.get(GetBalanceStep.ENTER_WX_钱包_TAB.ordinal)
        }.next(GetBalanceStep.ENTER_WX_钱包_TAB.ordinal) {
            // 3. 处理右上角区域 -> 寻找“钱包”
            if (ds.getInt("wx_钱包_x") > 0) {
                withContext(Dispatchers.Main) {
                    val globalX = ds.getInt("wx_钱包_x")
                    val globalY = ds.getInt("wx_钱包_y")
                    Log.i("微信自动化", "获取缓存“钱包”坐标：($globalX, $globalY)")
                    AssistsCore.gestureClick(globalX.toFloat(), globalY.toFloat())
                }
            } else {
                screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
                width = screenshot.width
                height = screenshot.height

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
            return@next Step.get(GetBalanceStep.GET_WX_BALANCE.ordinal)
        }.next(GetBalanceStep.GET_WX_BALANCE.ordinal) {
            screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none

            // 上半部分图片坐标
            val topHalfRect =
                Rect(screenshot.width / 10, 0, screenshot.width, screenshot.height / 2)

            // 截图确保“零钱”与“零钱通”显示完全，防止其他应用的通知/弹窗导致截到的图余额数字不全
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
                val 零钱通文字区域图片 = if (ds.getInt("wx_零钱_left") == 0) {
                    topHalfBitmap
                } else {
                    val tmp = Rect(
                        screenshot.width / 10,
                        ds.getInt("wx_零钱_top"),
                        screenshot.width * 3 / 10,
                        ds.getInt("wx_零钱_bottom")
                    )
//                    context.startFloatingService(
//                        Rect(
//                            tmp.left,
//                            tmp.top,
//                            tmp.right,
//                            tmp.bottom
//                        )
//                    )
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
                                    Log.i("微信自动化", "tongPosition: $tongPosition")
                                    // “零钱通”也包含“零钱”，要跳过
                                    continue
                                }

                                element.text.contains("零钱") -> {
                                    qianPosition = element.boundingBox
                                    Log.i("微信自动化", "qianPosition: $qianPosition")
                                }
                            }
                        }
                    }
                }

                if (qianPosition != null && tongPosition != null) {
                    break
                }
                retryNumber--
                Log.i("微信自动化", "未找到“零钱”或“零钱通”，剩余 $retryNumber 次重试")
                delay(3 * 1000)
                screenshot = AssistsCore.takeScreenshot() ?: return@next Step.none
            } while (retryNumber > 0)

            if (qianPosition == null || tongPosition == null) {
                Log.i("微信自动化", "无法找到“零钱”或“零钱通”，请检查代码逻辑或微信版本")
                return@next Step.get(GetBalanceStep.RESTART_YSF.ordinal)
            }

            // Step 3: 截取该区域的右半部分
            // 零钱余额图片坐标
            val rightPartRect: Rect = if (ds.getInt("wx_零钱_left") == 0) {
                val tmp = Rect(
                    (topHalfRect.width() / 2f).toInt(),
                    qianPosition.top,
                    topHalfRect.width() * 19 / 20 - (5 * MainActivity.metrics.density).toInt(),
                    tongPosition.bottom
                )
                if (Settings.canDrawOverlays(context)) {
                    context.startFloatingService(
                        Rect(
                            tmp.left + screenshot.width / 10,
                            tmp.top,
                            tmp.right + screenshot.width / 10,
                            tmp.bottom
                        )
                    )
                    Log.d("微信自动化", "余额位置弹窗确认。")
                    // 挂起函数，等待用户在任何界面上做出选择
                    val confirmed = OverlayConfirmManager.show(
                        context = context.applicationContext,
                        title = "零钱与零钱通位置确认",
                        message = "请确认识别的区域是否完整覆盖零钱与零钱通的余额数字，完整覆盖则点击确认，否则点击取消进行重试"
                    )
                    if (confirmed) {
                        Log.d("ConfirmTest", "用户已确认，执行下一步！")
                        ds.putInt("wx_零钱_left", tmp.left)
                        ds.putInt("wx_零钱_top", tmp.top)
                        ds.putInt("wx_零钱_right", tmp.right)
                        ds.putInt("wx_零钱_bottom", tmp.bottom)
                    } else {
                        Log.d("ConfirmTest", "用户已取消，重新识图。")
                        context.stopFloatingService()
                        return@next Step.repeat
                    }
                }
                tmp
            } else {
                Rect(
                    ds.getInt("wx_零钱_left"),
                    ds.getInt("wx_零钱_top"),
                    ds.getInt("wx_零钱_right"),
                    ds.getInt("wx_零钱_bottom"),
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
                val pattern = Regex("\\b(\\d+\\.?\\d*)\\b") // 匹配独立的数字或小数
                val match = pattern.find(input)
                return match?.groupValues?.getOrNull(1)?.toDouble()
            }

            val extractedNumbers =
                finalResult.textBlocks.flatMap { it.lines }.flatMap { it.elements }
                    .mapNotNull { element -> extractBalance(element.text) }


            var balance = when (extractedNumbers.isEmpty()) {
                false -> {
                    Log.i("微信自动化", "微信识别到余额：${extractedNumbers.joinToString(", ")}")
                    extractedNumbers.sum()
                }

                true -> {
                    Log.i("微信自动化", "未识别到余额数字")
                    0.0
                }
            }
            balance = "%.2f".format(balance).toDouble()

            Log.i("微信自动化", "微信余额：$balance")
            ds.setTodayBalance(BalanceChannelType.WX, balance)
            Log.i("微信自动化", "获取微信余额完毕")
            return@next Step.get(GetBalanceStep.RESTART_YSF.ordinal)
        }.
            // 获取云闪付余额
        next(GetBalanceStep.RESTART_YSF.ordinal) { op ->
            if (op.data != null) {
                deferred = op.data as? CompletableDeferred<String>
            }
            if (!ds.shouldUpdateBalance(BalanceChannelType.YSF)) {
                return@next Step.get(GetBalanceStep.RETURN_APP.ordinal, delay = 0)
            }
            Log.i("云闪付自动化", "获取云闪付余额...")
            restartApp(context, "com.unionpay")
            return@next Step.get(GetBalanceStep.GET_YSF_BALANCE.ordinal)
        }.next(GetBalanceStep.GET_YSF_BALANCE.ordinal) {
            var tmp = AssistsCore.findById("com.unionpay:id/tablayout")
            var retryNum = 3
            while (tmp.isEmpty()) {
                delay(1000)
                tmp = AssistsCore.findById("com.unionpay:id/tablayout")
                retryNum--
                if (retryNum == 0) {
                    return@next Step.get(GetBalanceStep.RESTART_YSF.ordinal, delay = 0)
                }
            }
            // 获取余额
            val 获取余额文字: () -> String = {
                val 余额 = AssistsCore.findById("com.unionpay:id/tv_fortune_balance_value")
                if (余额.isEmpty()) {
                    throw IllegalStateException("无法获取云闪付余额")
                }
                val 余额文字 = 余额[0].txt()
                余额文字
            }
            var 余额文字 = 获取余额文字()
            // 余额被隐藏，要点击小眼睛
            if (余额文字.startsWith("**")) {
                AssistsCore.findById("com.unionpay:id/iv_fortune_eye")[0].click()
                delay(2000)
                余额文字 = 获取余额文字()
            }
            // 余额需要从不同卡里汇总加载
            while (余额文字.startsWith("--")) {
                delay(2000)
                余额文字 = 获取余额文字()
            }
            var lastBalance: Double? = null
            var currentBalance = 余额文字.replace(",", "").toDouble()
            while (true) {
                if (lastBalance != null && lastBalance == currentBalance) break
                lastBalance = currentBalance
                delay(1000)
                余额文字 = 获取余额文字()
                currentBalance = 余额文字.replace(",", "").toDouble()
            }

            Log.i("云闪付自动化", 余额文字)
            var balance = 余额文字.replace(",", "").toDouble()
            balance = "%.2f".format(balance).toDouble()

            Log.i("云闪付自动化", "云闪付余额：$balance")
            ds.setTodayBalance(BalanceChannelType.YSF, balance)
            Log.i("云闪付自动化", "获取云闪付余额完毕")
            return@next Step.get(GetBalanceStep.RETURN_APP.ordinal, delay = 0)
        }.next(GetBalanceStep.RETURN_APP.ordinal) {
            deferred?.complete("fianl complete")
                ?: Log.e("自动化", "op.data 不是 CompletableDeferred<String> 类型")
            val selfPackageName = context.packageName
            // 直接回到 PersonalBalanceScreen 界面
            if (AssistsCore.getPackageName() != selfPackageName) {
                val launchIntent = Intent("$selfPackageName.PersonalBalanceScreen").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchIntent.setClassName(
                    "com.cyc.yearlymemoir", "com.cyc.yearlymemoir.MainActivity"
                )
                context.startActivity(launchIntent)
            }
            return@next Step.none
        }
    }
}

// 扩展函数，方便调用
private fun Context.startFloatingService(rect: Rect) {
    if (Settings.canDrawOverlays(this)) {
        val intent = Intent(this, WXFloatingWindowService::class.java).apply {
            // 将 Rect 对象放入 Intent
            putExtra(WXFloatingWindowService.KEY_RECT, rect)
        }
        startService(intent)
    }
}

private fun Context.stopFloatingService() {
    stopService(Intent(this, WXFloatingWindowService::class.java))
}