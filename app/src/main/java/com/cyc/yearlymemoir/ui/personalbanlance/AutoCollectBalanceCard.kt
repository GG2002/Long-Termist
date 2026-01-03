import android.app.Application
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.venassists.GetBalanceStep
import com.cyc.yearlymemoir.venassists.GetTodayBalanceStepImpl
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.StepManager
import ir.ehsannarmani.compose_charts.extensions.format
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.WX_ENABLED
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.YSF_ENABLED
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.ZFB_ENABLED
import kotlinx.coroutines.launch


class AutoCollectBalanceViewModel(application: Application) : AndroidViewModel(application) {
    // ViewModel implementation
    private val model = MainApplication.repository
    private val ds = MainActivity.ds

    // 当前余额
    private val _allBalance = MutableStateFlow(0.0)
    val allBalance: StateFlow<Double> = _allBalance

    fun getSupportedApp(): List<BalanceChannelType>{
        // 创建支持的应用列表
        var supportedApps = listOf<BalanceChannelType>()
        if (ds.getString(YSF_ENABLED)?.toBoolean() != true){
            supportedApps = supportedApps + listOf(BalanceChannelType.YSF)
        }
        if (ds.getString(WX_ENABLED)?.toBoolean() != true){
            supportedApps = supportedApps + listOf(BalanceChannelType.WX)
        }
        if (ds.getString(ZFB_ENABLED)?.toBoolean() != true){
            supportedApps = supportedApps + listOf(BalanceChannelType.ZFB)
        }
        return supportedApps
    }

    fun loadAllCacheBalance() {

    }

    fun resetAllCacheBalance() {

    }
}

fun autoCollectBalanceViewModelFactory(application: Application): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AutoCollectBalanceViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AutoCollectBalanceViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

@Composable
fun AutoCollectBalanceCard(refreshAllBalanceCallBack: () -> Unit) {
    val viewModel: AutoCollectBalanceViewModel = viewModel(
        factory = autoCollectBalanceViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    // 获取支持的余额渠道类型
    val supportedBalanceChannelTypes = viewModel.getSupportedApp()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(supportedBalanceChannelTypes) { balanceChannelType ->
            BalanceItem(balanceChannelType)
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
            text = "* 长按应用图标即可重新统计该应用余额",
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                if (!AssistsCore.isAccessibilityServiceEnabled()) {
                    AssistsCore.openAccessibilitySetting()
                    return@Button
                }
                viewModel.resetAllCacheBalance()

                val deferred = CompletableDeferred<String>()
                StepManager.execute(
                    GetTodayBalanceStepImpl::class.java,
                    GetBalanceStep.RESTART_ZFB.ordinal,
                    0, data = deferred
                )
                deferred.invokeOnCompletion { throwable ->
                    if (throwable == null) {
                        val result = deferred.getCompleted()
                        viewModel.loadAllCacheBalance()
                        refreshAllBalanceCallBack()
                        Log.i("自动化完成", "Result: $result")
                    } else {
                        Log.e("自动化出错", "Error: $throwable")
                    }
                }
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

class BalanceItemViewModel(application: Application, private val balanceType: BalanceChannelType) :
    AndroidViewModel(application) {
    private val model = MainApplication.repository

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance
    private val _spend = MutableStateFlow(0.0)
    val spend: StateFlow<Double> = _spend
    private val _history = MutableStateFlow(emptyList<Detail>())
    val history: StateFlow<List<Detail>> = _history

    fun resetCacheBalance() {
        viewModelScope.launch {
            _balance.value = 0.0
            _spend.value = 0.0
            _history.value = emptyList()
        }
    }


    fun loadCacheBalance() {
        viewModelScope.launch {
            try {
                val detail = coroutineScope {
                    val balanceHistory = async {
                        withContext(Dispatchers.IO) {
                            // 获取余额历史记录
                            model.getBalanceHistory(balanceType)
                        }
                    }
                    balanceHistory.await()
                }
                _history.value = detail
                _balance.value = detail.lastOrNull()?.detail?.toDouble() ?: 0.0
                _spend.value = if (detail.size >= 2) {
                    detail[detail.size - 2].detail.toDouble() - detail[detail.size - 1].detail.toDouble()
                } else {
                    0.0
                }
            } catch (e: Exception) {
                // 处理错误
                Log.e("余额图表", "how do?", e)
            }
        }
    }

    fun getTodayBalance(): Double {
        return balance.value
    }
}

fun balanceItemViewModelFactory(
    application: Application,
    balanceType: BalanceChannelType
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BalanceItemViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BalanceItemViewModel(application, balanceType) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

@Composable
fun BalanceItem(balanceType: BalanceChannelType) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val haptic = LocalHapticFeedback.current

    // 获取对应的 ViewModel
    val viewModel: BalanceItemViewModel = viewModel(
        factory = balanceItemViewModelFactory(application, balanceType)
    )
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val spend by viewModel.spend.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadCacheBalance()
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    // 单击逻辑
                }, onDoubleTap = { offset ->
                    // 双击逻辑
                }, onLongPress = { offset ->
                    // 长按逻辑
                    balanceItemLongPressHandler(
                        balanceType,
                        haptic,
                        context,
                        viewModel
                    )
                })
            },
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = balanceType.iconRes),
                contentDescription = balanceType.displayName,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )

            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy((-7).dp)
            ) {
                val fontSize = 10
                val (colo: Color, ico: ImageVector) = when {
                    spend < 0 -> Pair(Color.Red, Icons.Filled.ArrowDropDown)
                    spend == 0.0 -> Pair(Color.Gray, Icons.Filled.HorizontalRule)
                    else -> Pair(Color(0xFF18A656), Icons.Filled.ArrowDropUp)
                }
                Text(
                    text = "¥${"%.2f".format(balance)}",
                    style = typography.bodyLarge,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ico,
                        contentDescription = balanceType.displayName + "趋势",
                        tint = colo,
                        modifier = Modifier.size((fontSize + 4).dp)
                    )
                    Text(
                        abs(spend).format(2) + " ",
                        fontSize = fontSize.sp,
                        color = colo,
                    )
                }
            }
        }
    }
}

// BalanceItem 长按触发函数
fun balanceItemLongPressHandler(
    channelType: BalanceChannelType,
    haptic: HapticFeedback,
    context: Context,
    viewModel: BalanceItemViewModel,
) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

    Log.d(
        "获取余额是否需要更新",
        "${channelType.displayName}：${viewModel.getTodayBalance()} 需要更新：" + MainActivity.ds.shouldUpdateBalance(
            channelType
        )
    )

    // 特殊处理微信的悬浮窗权限
    if (channelType == BalanceChannelType.WX) {
        if (!Settings.canDrawOverlays(context)) {
            MainActivity.navController.navigate("PermissionScreen")
            return
        }
    }

    // 通用的无障碍权限检查
    if (!AssistsCore.isAccessibilityServiceEnabled()) {
        AssistsCore.openAccessibilitySetting()
        return
    }

    viewModel.resetCacheBalance()
    val deferred = CompletableDeferred<String>()
    StepManager.execute(
        GetTodayBalanceStepImpl::class.java,
        GetBalanceStep.RESTART_ZFB.ordinal,
        0,
        data = deferred
    )
    deferred.invokeOnCompletion { throwable ->
        if (throwable == null) {
            val result = deferred.getCompleted()
            // 调用 suspend 函数
            viewModel.loadCacheBalance()
            Log.i("自动化完成", "Result: $result")
        } else {
            Log.e("自动化出错", "Error: $throwable")
        }
    }
}