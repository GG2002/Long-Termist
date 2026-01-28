package com.cyc.yearlymemoir.venassists

import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.ui.personalbanlance.getTodayString
import com.cyc.yearlymemoir.utils.LogRecorder
import com.ven.assists.AssistsCore
import com.ven.assists.AssistsCore.click
import com.ven.assists.AssistsCore.getBoundsInScreen
import com.ven.assists.AssistsCore.getChildren
import com.ven.assists.AssistsCore.txt
import com.ven.assists.stepper.Step
import com.ven.assists.stepper.StepCollector
import kotlinx.coroutines.delay

class AlipayFlow(flowCtx: FlowContext) : BaseBalanceFlow(flowCtx) {
    override fun register(collector: StepCollector, nextStepId: Int) {
        collector
            .next(GetBalanceStep.RESTART_ZFB.ordinal) { op ->
                flowCtx.tryUpdateDeferred(op.data)

                if (
                    !flowCtx.ds.shouldUpdateBalance(BalanceChannelType.ZFB) ||
                    !restartApp("com.eg.android.AlipayGphone")
                ) {
                    return@next Step.get(nextStepId, delay = 0)
                }
                Step.get(GetBalanceStep.ENTER_ZFB_理财_TAB.ordinal, delay = 1500)
            }
            .next(GetBalanceStep.ENTER_ZFB_理财_TAB.ordinal) {
                // 进入理财 Tab
                val tabs = AssistsCore.findById("android:id/tabs")[0].getChildren()
                    .sortedBy { it.getBoundsInScreen().left }
                tabs.getOrNull(1)?.click()
                Step.get(GetBalanceStep.GET_ZFB_BALANCE.ordinal)
            }
            .next(GetBalanceStep.GET_ZFB_BALANCE.ordinal) {
                var retryNum = 3
                while (true) {
                    try {
                        val getAmountText: () -> String = {
                            val nodes =
                                AssistsCore.findById("com.alipay.android.widget.fortunehome:id/fh_tv_assets_amount_num")
                            if (nodes.isEmpty()) throw IllegalStateException("无法获取支付宝余额")
                            nodes[0].txt()
                        }
                        var text = getAmountText()
                        // 余额被隐藏，要点击小眼睛
                        var hidden = text.startsWith("**")
                        if (hidden) {
                            AssistsCore.findById("com.alipay.android.widget.fortunehome:id/hide_layout")[0].click()
                            delay(2000)
                            text = getAmountText()
                        }

                        var balance: Double = Double.NaN
                        for (attempt in 1..5) {
                            var current = text.replace(",", "").toDouble()
                            current = "%.2f".format(current).toDouble()
                            LogRecorder.d(
                                "支付宝自动化",
                                "第 $attempt 次尝试：当前余额 = $current, 上次余额 = $balance"
                            )
                            if (current.isFinite() && current == balance) {
                                LogRecorder.i("支付宝自动化", "余额已稳定：$current")
                                balance = current
                                break
                            }
                            balance = current
                            delay(1000)
                            text = getAmountText()
                        }
                        if (hidden) {
                            AssistsCore.findById("com.alipay.android.widget.fortunehome:id/hide_layout")[0].click()
                        }
                        LogRecorder.i(
                            "支付宝自动化",
                            "支付宝余额：$balance，${UniversalDate.today()}"
                        )
                        flowCtx.ds.updateTodayBalance(BalanceChannelType.ZFB)
                        flowCtx.model.upsertBalance(
                            BalanceRecord(
                                sourceType = "支付宝",
                                recordDate = getTodayString(),
                                balance = balance,
                            )
                        )
                        LogRecorder.i("支付宝自动化", "获取支付宝余额完毕")
                        break
                    } catch (e: Exception) {
                        retryNum--
                        LogRecorder.e("支付宝自动化", e.toString() + "，剩余 $retryNum 次重试机会")
                        if (retryNum == 0) throw IllegalStateException("无法获取支付宝余额")
                        delay(2000)
                    }
                }
                Step.get(nextStepId)
            }
    }
}