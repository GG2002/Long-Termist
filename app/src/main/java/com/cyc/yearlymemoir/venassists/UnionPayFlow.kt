package com.cyc.yearlymemoir.venassists

import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.utils.LogRecorder
import com.ven.assists.AssistsCore
import com.ven.assists.AssistsCore.click
import com.ven.assists.AssistsCore.txt
import com.ven.assists.stepper.Step
import com.ven.assists.stepper.StepCollector
import kotlinx.coroutines.delay

class UnionPayFlow(flowCtx: FlowContext) : BaseBalanceFlow(flowCtx) {
    override fun register(collector: StepCollector, nextStepId: Int) {
        collector
            .next(GetBalanceStep.RESTART_YSF.ordinal) { op ->
                flowCtx.tryUpdateDeferred(op.data)

                if (
                    !flowCtx.ds.shouldUpdateBalance(BalanceChannelType.YSF) ||
                    !restartApp("com.unionpay")
                ) {
                    return@next Step.get(nextStepId, delay = 0)
                }
                LogRecorder.i("云闪付自动化", "获取云闪付余额...")

                Step.get(GetBalanceStep.GET_YSF_BALANCE.ordinal, delay = 1500)
            }
            .next(GetBalanceStep.GET_YSF_BALANCE.ordinal) {
                // 两个版本的云闪付：
                // 新版：com.unionpay:id/tv_tab_name -> com.unionpay:id/card_info_balance_bill_amount，点击 com.unionpay:id/card_title_eyes
                // 旧版：com.unionpay:id/tablayout -> com.unionpay:id/tv_fortune_balance_value，点击 com.unionpay:id/iv_fortune_eye

                var retryNum = 5
                var isNewVersionUI = false
                while (retryNum > 0) {
                    val newRoot = AssistsCore.findById("com.unionpay:id/tv_tab_name")
                    val oldRoot = AssistsCore.findById("com.unionpay:id/tablayout")
                    if (newRoot.isNotEmpty()) {
                        isNewVersionUI = true; break
                    }
                    if (oldRoot.isNotEmpty()) {
                        isNewVersionUI = false; break
                    }
                    delay(1000)
                    retryNum--
                }
                if (retryNum == 0) {
                    return@next Step.get(nextStepId, delay = 0)
                }

                var balanceStr = ""
                if (isNewVersionUI) {
                    // 新版完整流程
                    val getText: () -> String = {
                        val nodes =
                            AssistsCore.findById("com.unionpay:id/card_info_balance_bill_amount")
                        if (nodes.isEmpty()) throw IllegalStateException("无法获取云闪付余额")
                        nodes[0].txt()
                    }
                    balanceStr = getText()
                    // 余额被隐藏，要点击小眼睛
                    if (balanceStr.startsWith("**")) {
                        AssistsCore.findById("com.unionpay:id/card_title_eyes")[0].click()
                        delay(2000)
                        balanceStr = getText()
                    }
                    while (balanceStr.startsWith("--")) {
                        delay(2000)
                        balanceStr = getText()
                    }
                    var lastBalance: Double? = null
                    var currentBalance = balanceStr.replace(",", "").toDouble()
                    while (true) {
                        if (lastBalance != null && lastBalance == currentBalance) break
                        lastBalance = currentBalance
                        delay(1000)
                        balanceStr = getText()
                        currentBalance = balanceStr.replace(",", "").toDouble()
                    }
                } else {
                    // 旧版完整流程
                    val getText: () -> String = {
                        val nodes = AssistsCore.findById("com.unionpay:id/tv_fortune_balance_value")
                        if (nodes.isEmpty()) throw IllegalStateException("无法获取云闪付余额")
                        nodes[0].txt()
                    }
                    balanceStr = getText()
                    // 余额被隐藏，要点击小眼睛
                    if (balanceStr.startsWith("**")) {
                        AssistsCore.findById("com.unionpay:id/iv_fortune_eye")[0].click()
                        delay(2000)
                        balanceStr = getText()
                    }
                    while (balanceStr.startsWith("--")) {
                        delay(2000)
                        balanceStr = getText()
                    }
                    var lastBalance: Double? = null
                    var currentBalance = balanceStr.replace(",", "").toDouble()
                    while (true) {
                        if (lastBalance != null && lastBalance == currentBalance) break
                        lastBalance = currentBalance
                        delay(1000)
                        balanceStr = getText()
                        currentBalance = balanceStr.replace(",", "").toDouble()
                    }
                }

                LogRecorder.i("云闪付自动化", balanceStr)
                var balance = balanceStr.replace(",", "").toDouble()
                balance = "%.2f".format(balance).toDouble()

                LogRecorder.i("云闪付自动化", "云闪付余额：$balance")
                flowCtx.ds.updateTodayBalance(BalanceChannelType.YSF)
                flowCtx.model.upsertBalance(
                    BalanceRecord(
                        sourceType = "云闪付",
                        recordDate = getTodayString(),
                        balance = balance,
                    )
                )
                LogRecorder.i("云闪付自动化", "获取云闪付余额完毕")
                Step.get(nextStepId)
            }
    }
}