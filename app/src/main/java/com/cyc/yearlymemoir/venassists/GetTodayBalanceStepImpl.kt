package com.cyc.yearlymemoir.venassists

import android.content.Intent
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.utils.LogRecorder
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.Step
import com.ven.assists.stepper.StepCollector
import com.ven.assists.stepper.StepImpl


enum class GetBalanceStep {
    // 支付宝步骤
    RESTART_ZFB, ENTER_ZFB_理财_TAB, GET_ZFB_BALANCE,

    // 微信步骤
    RESTART_WX, ENTER_WX_我的_TAB, ENTER_WX_服务_TAB, ENTER_WX_钱包_TAB, GET_WX_BALANCE,

    // 云闪付步骤
    RESTART_YSF, GET_YSF_BALANCE,

    // 结束
    RETURN_APP
}

class GetTodayBalanceStepImpl : StepImpl() {
    private val flowContext = FlowContext()

    override fun onImpl(collector: StepCollector) {
        val alipay = AlipayFlow(flowContext)
        val wechat = WeChatFlow(flowContext)
        val unionpay = UnionPayFlow(flowContext)

        // 串联逻辑：Alipay -> WeChat -> UnionPay -> ReturnApp
        alipay.register(collector, nextStepId = GetBalanceStep.RESTART_WX.ordinal)
        wechat.register(collector, nextStepId = GetBalanceStep.RESTART_YSF.ordinal)
        unionpay.register(collector, nextStepId = GetBalanceStep.RETURN_APP.ordinal)

        // 注册最后的收尾步骤
        collector.next(GetBalanceStep.RETURN_APP.ordinal) {
            // 记录更新时间
            flowContext.ds.putLong("last_update_time", System.currentTimeMillis())

            // 返回 APP 界面
            val context = flowContext.context
            if (AssistsCore.getPackageName() != context.packageName) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = "${context.packageName}.PersonalBalanceScreen"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

            // 【关键点】通知任务完成
            // 无论从哪里开始，deferred 都应该已经被对应的入口 Step 捕获到了 flowContext 中
            flowContext.deferred?.complete("final complete")
                ?: LogRecorder.e("自动化", "Deferred 未捕获，流程可能异常")

            Step.none
        }
    }
}
