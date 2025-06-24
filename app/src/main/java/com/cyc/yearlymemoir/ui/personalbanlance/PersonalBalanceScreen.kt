package com.cyc.yearlymemoir.ui.personalbanlance

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.R
import com.cyc.yearlymemoir.model.BalanceChannelType
import com.cyc.yearlymemoir.venassists.GetBalanceStep
import com.cyc.yearlymemoir.venassists.GetTodayBalanceStepImpl
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.StepManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PersonalBalanceScreen(
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
    navController: NavController
) {
    val ds = MainActivity.ds
    val context = MainActivity.appContext

    val wxBalance = remember { mutableDoubleStateOf(0.0) }
    val zfbBalance = remember { mutableDoubleStateOf(0.0) }
    val ysfBalance = remember { mutableDoubleStateOf(0.0) }
    val allBalance = remember(zfbBalance, wxBalance, ysfBalance) {
        mutableDoubleStateOf(
            ds.getTodayBalance(BalanceChannelType.ALL)
        )
    }

    LaunchedEffect(Unit) {
        println(1121)
        zfbBalance.value = ds.getTodayBalance(BalanceChannelType.ZFB)
        wxBalance.value = ds.getTodayBalance(BalanceChannelType.WX)
        ysfBalance.value = ds.getTodayBalance(BalanceChannelType.YSF)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
//                containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.primary,
                ),
                title = {
                    Text(
                        "总余额：${allBalance.doubleValue}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = 28.dp,
                    end = 28.dp
                ) // 避免内容被 AppBar 遮挡
        ) {
            Spacer(Modifier.height(10.dp))

            BanlanceChartCard(
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope,
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))

//            Card(modifier = Modifier.wrapContentHeight()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    BalanceItem(name = "支付宝",
                        iconRes = R.drawable.ic_zfb_gradient,
                        balance = zfbBalance.doubleValue,
                        onDoubleClick = {
                            println(
                                "支付宝：$zfbBalance 需要更新：" + ds.shouldUpdateBalance(
                                    BalanceChannelType.ZFB
                                )
                            )
                            if (!AssistsCore.isAccessibilityServiceEnabled()) {
                                AssistsCore.openAccessibilitySetting()
                                return@BalanceItem
                            }
                            ds.resetTodayBalance(BalanceChannelType.ZFB)
                            zfbBalance.value = 0.0
                            val deferred = CompletableDeferred<String>()
                            StepManager.execute(
                                GetTodayBalanceStepImpl::class.java,
                                GetBalanceStep.RESTART_ZFB.ordinal,
                                0, data = deferred
                            )
                            deferred.invokeOnCompletion { throwable ->
                                if (throwable == null) {
                                    val result = deferred.getCompleted()
                                    zfbBalance.value = ds.getTodayBalance(BalanceChannelType.ZFB)
                                    Log.i("自动化完成", "Result: $result")
                                } else {
                                    Log.e("自动化出错", "Error: $throwable")
                                }
                            }
                        })
                }

                item {
                    BalanceItem(name = "微信",
                        iconRes = R.drawable.ic_wx,
                        balance = wxBalance.doubleValue,
                        onDoubleClick = {
                            println(
                                "微信：$wxBalance 需要更新：" + ds.shouldUpdateBalance(
                                    BalanceChannelType.WX
                                )
                            )
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                return@BalanceItem
                            }
                            if (!AssistsCore.isAccessibilityServiceEnabled()) {
                                AssistsCore.openAccessibilitySetting()
                                return@BalanceItem
                            }
                            ds.resetTodayBalance(BalanceChannelType.WX)
                            wxBalance.value = 0.0
                            val deferred = CompletableDeferred<String>()
                            StepManager.execute(
                                GetTodayBalanceStepImpl::class.java,
                                GetBalanceStep.RESTART_WX.ordinal,
                                0, data = deferred
                            )
                            deferred.invokeOnCompletion { throwable ->
                                if (throwable == null) {
                                    val result = deferred.getCompleted()
                                    wxBalance.value = ds.getTodayBalance(BalanceChannelType.WX)
                                    Log.i("自动化完成", "Result: $result")
                                } else {
                                    Log.e("自动化出错", "Error: $throwable")
                                }
                            }
                        })
                }

                item {
                    BalanceItem(name = "云闪付",
                        iconRes = R.drawable.ic_ysf,
                        balance = ysfBalance.doubleValue,
                        onDoubleClick = {
                            println(
                                "云闪付：$ysfBalance 需要更新：" + ds.shouldUpdateBalance(
                                    BalanceChannelType.YSF
                                )
                            )
                            if (!AssistsCore.isAccessibilityServiceEnabled()) {
                                AssistsCore.openAccessibilitySetting()
                                return@BalanceItem
                            }
                            ds.resetTodayBalance(BalanceChannelType.YSF)
                            ysfBalance.doubleValue = 0.0
                            val deferred = CompletableDeferred<String>()
                            StepManager.execute(
                                GetTodayBalanceStepImpl::class.java,
                                GetBalanceStep.RESTART_YSF.ordinal,
                                0, data = deferred
                            )
                            deferred.invokeOnCompletion { throwable ->
                                if (throwable == null) {
                                    val result = deferred.getCompleted()
                                    ysfBalance.value = ds.getTodayBalance(BalanceChannelType.YSF)
                                    Log.i("自动化完成", "Result: $result")
                                } else {
                                    Log.e("自动化出错", "Error: $throwable")
                                }
                            }
                        })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    text = "* 双击应用图标即可重新统计该应用余额",
                    style = typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        if (!AssistsCore.isAccessibilityServiceEnabled()) {
                            AssistsCore.openAccessibilitySetting()
                            return@Button
                        }
                        ds.resetTodayBalance(BalanceChannelType.ALL)
                        zfbBalance.value = 0.0
                        wxBalance.value = 0.0
                        ysfBalance.value = 0.0
                        val deferred = CompletableDeferred<String>()
                        StepManager.execute(
                            GetTodayBalanceStepImpl::class.java,
                            GetBalanceStep.RESTART_ZFB.ordinal,
                            0, data = deferred
                        )
                        zfbBalance.value = ds.getTodayBalance(BalanceChannelType.ZFB)
                        wxBalance.value = ds.getTodayBalance(BalanceChannelType.WX)
                        ysfBalance.value = ds.getTodayBalance(BalanceChannelType.YSF)
//                        deferred.invokeOnCompletion { throwable ->
//                            if (throwable == null) {
//                                val result = deferred.getCompleted()
//                                refreshManager.triggerRefresh("ZFB")
//                                refreshManager.triggerRefresh("WX")
//                                refreshManager.triggerRefresh("YSF")
//                                refreshManager.triggerRefresh("ALL")
//                                Log.i("自动化完成", "Result: $result")
//                            } else {
//                                Log.e("自动化出错", "Error: $throwable")
//                            }
//                        }
                    },
                    colors = ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = colorScheme.primary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = colorScheme.onSurfaceVariant
                    ),
                ) {
                    Text("获取所有余额")
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新" // 用于无障碍服务
                    )
                }
            }
        }
    }
//    }
}

@Composable
fun BalanceItem(
    name: String, iconRes: Int, balance: Double, onDoubleClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    // 单击逻辑
                }, onDoubleTap = { offset ->
                    // 双击逻辑
                    onDoubleClick()
                }, onLongPress = { offset ->
                    // 长按逻辑
                })
            },
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = name,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )

            Text(
                text = "¥${"%.2f".format(balance)}",
                style = typography.bodyLarge,
            )
        }
    }
}

// 配置参数
data class RefreshConfig(
    val refreshInterval: Long = 1000L, // 每次刷新间隔
    val extendTimes: Int = 40          // 每次触发增加多少次
)

// 每个支付渠道的刷新状态
class BalanceRefreshState(initialBalance: Double = 0.0) {
    var balance by mutableDoubleStateOf(initialBalance)
    var remainingTimes by mutableIntStateOf(0)
}

// 通用刷新管理器
class BalanceRefreshManager(
    private val scope: CoroutineScope,
    private val config: RefreshConfig = RefreshConfig(),
    private val fetcher: (String) -> Double // 渠道标识符 → 获取余额的方法
) {
    private val states = mutableMapOf<String, BalanceRefreshState>()
    private val jobs = mutableMapOf<String, Job>()

    fun getOrCreateState(channel: String): BalanceRefreshState {
        return states.getOrPut(channel) { BalanceRefreshState() }
    }

    fun triggerRefresh(channel: String) {
        val state = getOrCreateState(channel)
        val existingJob = jobs[channel]

        var prevBalance = state.balance
        val newBalance = fetcher(channel)
        if (newBalance != prevBalance) {
            state.balance = newBalance
            state.remainingTimes = 0
            return
        }

        state.remainingTimes = config.extendTimes
        Log.d(
            "RefreshBalance",
            "1 $channel 剩余刷新次数：${state.remainingTimes} 任务状态：${existingJob?.isActive}"
        )

        if (existingJob?.isActive != true) {
            val initialRemainingTime = state.remainingTimes
            Log.d("RefreshBalance", "2 $channel 剩余刷新次数：$initialRemainingTime")
            scope.launch {
                state.remainingTimes = initialRemainingTime
                try {
                    var prevBalance = state.balance
                    Log.d(
                        "RefreshBalance",
                        "3 $channel 当前显示余额：$prevBalance 剩余刷新次数：${state.remainingTimes}"
                    )

                    while (state.remainingTimes > 0) {
                        val newBalance = fetcher(channel)
                        if (newBalance != prevBalance) {
                            state.balance = newBalance
                            state.remainingTimes = 0
                            break
                        }

                        delay(config.refreshInterval)
                        state.remainingTimes -= 1
                    }
                } finally {
                    jobs.remove(channel) // 确保无论是否异常都移除 job
                    Log.d("RefreshBalance", "$channel 任务已被移除 任务状态：${jobs[channel]}")
                }
            }.also { jobs[channel] = it }
        }
    }
}