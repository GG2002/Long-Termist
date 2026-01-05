import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.WorkScheduler
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.WX_ENABLED
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.YSF_ENABLED
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.ZFB_ENABLED
import com.cyc.yearlymemoir.venassists.GetBalanceStep
import com.cyc.yearlymemoir.venassists.GetTodayBalanceStepImpl
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.StepManager
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


class AutoCollectBalanceViewModel(application: Application) : AndroidViewModel(application) {
    // ViewModel implementation
    private val model = MainApplication.repository
    private val ds = MainActivity.ds

    // 定时任务开关
    private val _isScheduled = MutableStateFlow(false)
    val isScheduled: StateFlow<Boolean> = _isScheduled

    // 环形图数据（由启用渠道与今日余额组成）
    private val _chartData = MutableStateFlow<List<Pie>>(emptyList())
    val chartData: StateFlow<List<Pie>> = _chartData

    // 每个渠道的余额/支出/历史缓存
    private val _channelBalances = MutableStateFlow<Map<BalanceChannelType, Double>>(emptyMap())
    val channelBalances: StateFlow<Map<BalanceChannelType, Double>> = _channelBalances
    private val _channelSpends = MutableStateFlow<Map<BalanceChannelType, Double>>(emptyMap())
    val channelSpends: StateFlow<Map<BalanceChannelType, Double>> = _channelSpends
    private val _channelHistories =
        MutableStateFlow<Map<BalanceChannelType, List<BalanceRecord>>>(emptyMap())
    val channelHistories: StateFlow<Map<BalanceChannelType, List<BalanceRecord>>> =
        _channelHistories

    @SuppressLint("RestrictedApi")
    fun loadIsScheduled() {
        viewModelScope.launch {
            val workManager =
                WorkManager.getInstance(getApplication<Application>().applicationContext)
            val workInfos =
                workManager.getWorkInfosForUniqueWork(WorkScheduler.UNIQUE_WORK_NAME).await()
            _isScheduled.value =
                workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }

    fun setDailyTaskEnabled(enabled: Boolean) {
        val ctx = getApplication<Application>().applicationContext
        if (enabled) {
            WorkScheduler.enableGetPersonalBalance8amWork(ctx)
        } else {
            WorkScheduler.cancelGetPersonalBalance8amWork(ctx)
        }
        _isScheduled.value = enabled
    }

    fun getSupportedApp(): List<BalanceChannelType> {
        // 创建支持的应用列表
        var supportedApps = listOf<BalanceChannelType>()
        if (ds.getString(YSF_ENABLED)?.toBoolean() == true) {
            supportedApps = supportedApps + listOf(BalanceChannelType.YSF)
        }
        if (ds.getString(WX_ENABLED)?.toBoolean() == true) {
            supportedApps = supportedApps + listOf(BalanceChannelType.WX)
        }
        if (ds.getString(ZFB_ENABLED)?.toBoolean() == true) {
            supportedApps = supportedApps + listOf(BalanceChannelType.ZFB)
        }
        return supportedApps
    }

    fun refreshChartData() {
        val segments = computeChartData(_channelBalances.value, getSupportedApp())
        _chartData.value = segments.map { seg ->
            Pie(label = seg.label, data = seg.value, color = seg.color)
        }
    }

    fun setSelectedPie(index: Int) {
        _chartData.value = _chartData.value.mapIndexed { i, pie -> pie.copy(selected = i == index) }
    }

    fun loadAllCacheBalance() {
        viewModelScope.launch {
            val supported = getSupportedApp()
            val balances = mutableMapOf<BalanceChannelType, Double>()
            val spends = mutableMapOf<BalanceChannelType, Double>()
            val histories = mutableMapOf<BalanceChannelType, List<BalanceRecord>>()
            try {
                for (ch in supported) {
                    val list =
                        withContext(Dispatchers.IO) { model.getBalancesBySource(ch.displayName) }
                    histories[ch] = list
                    val last = list.lastOrNull()?.balance ?: 0.0
                    balances[ch] = last
                    spends[ch] = if (list.size >= 2) {
                        list[list.size - 2].balance - list[list.size - 1].balance
                    } else 0.0
                }
                _channelHistories.value = histories
                _channelBalances.value = balances
                _channelSpends.value = spends
                refreshChartData()
            } catch (e: Exception) {
                Log.e("余额图表", "loadAllCacheBalance error", e)
            }
        }
    }

    fun resetCacheBalance(balanceType: BalanceChannelType) {
        viewModelScope.launch {
            when(balanceType) {
                BalanceChannelType.ZFB -> {
                    _channelHistories.value = _channelHistories.value.toMutableMap().apply {
                        this[BalanceChannelType.ZFB] = emptyList()
                    }
                    _channelBalances.value = _channelBalances.value.toMutableMap().apply {
                        this[BalanceChannelType.ZFB] = 0.0
                    }
                    _channelSpends.value = _channelSpends.value.toMutableMap().apply {
                        this[BalanceChannelType.ZFB] = 0.0
                    }
                    MainActivity.ds.resetTodayBalance(BalanceChannelType.ZFB)
                }
                BalanceChannelType.WX -> {
                    _channelHistories.value = _channelHistories.value.toMutableMap().apply {
                        this[BalanceChannelType.WX] = emptyList()
                    }
                    _channelBalances.value = _channelBalances.value.toMutableMap().apply {
                        this[BalanceChannelType.WX] = 0.0
                    }
                    _channelSpends.value = _channelSpends.value.toMutableMap().apply {
                        this[BalanceChannelType.WX] = 0.0
                    }
                    MainActivity.ds.resetTodayBalance(BalanceChannelType.WX)
                }
                BalanceChannelType.YSF -> {
                    _channelHistories.value = _channelHistories.value.toMutableMap().apply {
                        this[BalanceChannelType.YSF] = emptyList()
                    }
                    _channelBalances.value = _channelBalances.value.toMutableMap().apply {
                        this[BalanceChannelType.YSF] = 0.0
                    }
                    _channelSpends.value = _channelSpends.value.toMutableMap().apply {
                        this[BalanceChannelType.YSF] = 0.0
                    }
                    MainActivity.ds.resetTodayBalance(BalanceChannelType.YSF)
                }
                BalanceChannelType.ALL -> {
                    _channelHistories.value = emptyMap()
                    _channelBalances.value = emptyMap()
                    _channelSpends.value = emptyMap()
                    MainActivity.ds.resetTodayBalance(BalanceChannelType.ALL)
                }
            }
            refreshChartData()
        }
    }

    private fun loadBalancesBySource(sourceType: BalanceChannelType) {
        viewModelScope.launch {
            val source = sourceType.displayName
            val list = withContext(Dispatchers.IO) { model.getBalancesBySource(source) }

            val newHistories = _channelHistories.value.toMutableMap()
            newHistories[sourceType] = list
            _channelHistories.value = newHistories

            val newBalances = _channelBalances.value.toMutableMap()
            newBalances[sourceType] = list.lastOrNull()?.balance ?: 0.0
            _channelBalances.value = newBalances

            val newSpends = _channelSpends.value.toMutableMap()
            newSpends[sourceType] = if (list.size >= 2) {
                list[list.size - 2].balance - list[list.size - 1].balance
            } else 0.0
            _channelSpends.value = newSpends

            refreshChartData()
        }
    }

    suspend fun ensureTodayForAllSupported() {
        val supported = getSupportedApp()
        for (ch in supported) {
            val source = ch.displayName
            val list = withContext(Dispatchers.IO) { model.getBalancesBySource(source) }
            val today = getTodayString()
            var ensured = list
            if (list.none { it.recordDate == today }) {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val nearest =
                    list.maxByOrNull { java.time.LocalDate.parse(it.recordDate, formatter) }
                val defaultBalance = nearest?.balance ?: 1.0
                withContext(Dispatchers.IO) {
                    model.upsertBalance(
                        BalanceRecord(
                            sourceType = source,
                            recordDate = today,
                            balance = defaultBalance
                        )
                    )
                }
                ensured = withContext(Dispatchers.IO) { model.getBalancesBySource(source) }
            }
            val newHistories = _channelHistories.value.toMutableMap()
            newHistories[ch] = ensured
            _channelHistories.value = newHistories

            val newBalances = _channelBalances.value.toMutableMap()
            newBalances[ch] = ensured.lastOrNull()?.balance ?: 0.0
            _channelBalances.value = newBalances

            val newSpends = _channelSpends.value.toMutableMap()
            newSpends[ch] = if (ensured.size >= 2) {
                ensured[ensured.size - 2].balance - ensured[ensured.size - 1].balance
            } else 0.0
            _channelSpends.value = newSpends
        }
        refreshChartData()
    }

    suspend fun upsertBalanceForDate(
        sourceType: BalanceChannelType,
        date: String,
        balance: Double
    ) {
        withContext(Dispatchers.IO) {
            model.upsertBalance(
                BalanceRecord(
                    sourceType = sourceType.displayName,
                    recordDate = date,
                    balance = balance
                )
            )
        }
        loadBalancesBySource(sourceType)
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

private data class DonutSegment(val color: Color, val value: Double, val label: String)

private fun computeChartData(
    balances: Map<BalanceChannelType, Double>,
    channels: List<BalanceChannelType>
): List<DonutSegment> {
    // 为不同渠道分配颜色
    fun colorFor(type: BalanceChannelType): Color = when (type) {
        BalanceChannelType.ZFB -> Color(0xFF1677FF)
        BalanceChannelType.WX -> Color(0xFF07C160)
        BalanceChannelType.YSF -> Color(0xFFF41C19)
        BalanceChannelType.ALL -> Color.Gray
    }
    return channels.map { ch ->
        val value = balances[ch] ?: 0.0
        DonutSegment(colorFor(ch), value, ch.displayName)
    }
}


@Composable
fun AutoCollectBalanceCard(
    refreshAllBalanceCallBack: () -> Unit,
    refreshTick: Int,
) {
    val viewModel: AutoCollectBalanceViewModel = viewModel(
        factory = autoCollectBalanceViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    // 获取支持的余额渠道类型（可变状态，便于局部刷新）
    var supportedBalanceChannelTypes by remember { mutableStateOf(viewModel.getSupportedApp()) }
    // 是否开启自动收集余额任务
    val isScheduled by viewModel.isScheduled.collectAsStateWithLifecycle()
    // 环形图数据现在由 ViewModel 管理
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadIsScheduled()
        // 仅在界面打开时确保所有已启用渠道存在“今日”数据
        viewModel.ensureTodayForAllSupported()
        viewModel.refreshChartData()
        // ensureTodayForAllSupported 是会导致数据变更的，需要刷新父组件和其他组件的数据
        refreshAllBalanceCallBack()
    }
    // 父组件触发刷新：监听 refreshTick
    LaunchedEffect(refreshTick) {
        viewModel.ensureTodayForAllSupported()
        viewModel.refreshChartData()
    }

    var showSettings by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier
                .padding(start = 10.dp, top = 0.dp, bottom = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "余额来源统计",
                    style = typography.titleMedium,
                    color = colorScheme.onSurface,
                    modifier = Modifier
                )
                Button(
                    onClick = { showSettings = true },
                    colors = ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = colorScheme.primary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("配置")
                }
            }

            // 环形图（放在标题下、列表上方）
            Spacer(modifier = Modifier.height(8.dp))
            PieChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 8.dp),
                data = chartData,
                onPieClick = {
                    val pieIndex = chartData.indexOf(it)
                    viewModel.setSelectedPie(pieIndex)
                },
                selectedScale = 1.2f,
                selectedPaddingDegree = 4f,
                scaleAnimEnterSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                colorAnimEnterSpec = tween(300),
                colorAnimExitSpec = tween(300),
                scaleAnimExitSpec = tween(300),
                spaceDegreeAnimExitSpec = tween(300),
                style = Pie.Style.Stroke(30.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(), // 或者是 wrapContentHeight
                verticalArrangement = Arrangement.spacedBy(8.dp) // 对应你原来的 verticalArrangement
            ) {
                // 既然只有 1 列，直接遍历数据生成组件即可
                supportedBalanceChannelTypes.forEach { balanceChannelType ->
                    BalanceItem(balanceChannelType, refreshAllBalanceCallBack)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

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
                        viewModel.resetCacheBalance(BalanceChannelType.ALL)

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
    }

    if (showSettings) {
        SettingsBottomSheet(
            onDismiss = { showSettings = false },
            onChanged = {
                // 只刷新卡片内支持的渠道列表
                supportedBalanceChannelTypes = viewModel.getSupportedApp()
                viewModel.refreshChartData()
            },
            isScheduled = isScheduled,
            onToggleSchedule = { enabled -> viewModel.setDailyTaskEnabled(enabled) }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    isScheduled: Boolean,
    onToggleSchedule: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ds = MainActivity.ds

    val allChannels = listOf(BalanceChannelType.ZFB, BalanceChannelType.WX, BalanceChannelType.YSF)

    fun isInstalled(type: BalanceChannelType): Boolean {
        val pkg = type.packageName ?: return true
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "余额来源配置", style = typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "定时获取",
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isScheduled, onCheckedChange = { onToggleSchedule(it) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "选择需要统计的应用", style = typography.labelSmall)
            Spacer(modifier = Modifier.height(12.dp))

            allChannels.forEach { ch ->
                val enabledKey = when (ch) {
                    BalanceChannelType.YSF -> YSF_ENABLED
                    BalanceChannelType.WX -> WX_ENABLED
                    BalanceChannelType.ZFB -> ZFB_ENABLED
                    BalanceChannelType.ALL -> null
                }

                if (enabledKey != null) {
                    val installed = isInstalled(ch)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row() {
                            Box(
                                modifier = Modifier
                                    .size((typography.bodyMedium.fontSize.value + 8).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = ch.iconRes),
                                    contentDescription = ch.displayName,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size((typography.bodyMedium.fontSize.value + 10).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = ch.displayName + if (!installed) "（未安装）" else "",
                                style = typography.bodyMedium,
                                color = if (installed) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        }
                        var current = remember(enabledKey) {
                            mutableStateOf(
                                ds.getString(enabledKey)?.toBoolean() == true
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (current.value) "已开启" else "未开启",
                                style = typography.bodySmall
                            )
                            Switch(
                                checked = current.value,
                                onCheckedChange = { checked ->
                                    if (!installed) return@Switch
                                    ds.putString(enabledKey, checked.toString())
                                    current.value = checked
                                    onChanged()
                                },
                                enabled = installed
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BalanceItem(
    balanceType: BalanceChannelType,
    refreshAllBalanceCallBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val parentVm: AutoCollectBalanceViewModel = viewModel(
        factory = autoCollectBalanceViewModelFactory(context.applicationContext as Application)
    )
    val balances by parentVm.channelBalances.collectAsStateWithLifecycle()
    val spends by parentVm.channelSpends.collectAsStateWithLifecycle()
    val histories by parentVm.channelHistories.collectAsStateWithLifecycle()
    val balance = balances[balanceType] ?: 0.0
    val spend = spends[balanceType] ?: 0.0
    val history = histories[balanceType] ?: emptyList()
    var showHistorySheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BalanceRecord?>(null) }
    var inputValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        parentVm.loadAllCacheBalance()
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { _ ->
                        // 单击打开历史 Sheet
                        scope.launch { showHistorySheet = true }
                    },
                    onDoubleTap = { _ -> },
                    onLongPress = { _ ->
                        balanceItemLongPressHandler(
                            balanceType,
                            haptic,
                            context,
                            parentVm
                        )
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧组：图标 + 渠道名（靠左）
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = balanceType.iconRes),
                    contentDescription = balanceType.displayName,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = balanceType.displayName,
                style = typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        // 右侧组：花费（右对齐） + 余额（左对齐）
        Row(
            modifier = Modifier
                .width(160.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            val (spendColor: Color, ico: ImageVector) = when {
                spend < 0 -> Pair(Color(0xFF18A656), Icons.Filled.ArrowDropUp)
                spend == 0.0 -> Pair(Color.Gray, Icons.Filled.HorizontalRule)
                else -> Pair(Color.Red, Icons.Filled.ArrowDropDown)
            }


            // 花费（右对齐）
            Row(
                modifier = Modifier.width(80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = ico,
                    contentDescription = balanceType.displayName + "趋势",
                    tint = spendColor,
                    modifier = Modifier.size((typography.bodySmall.fontSize.value + 4).dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = formatSpend(abs(spend)),
                    style = typography.bodySmall,
                    color = spendColor
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 余额（左对齐）
            Row(
                modifier = Modifier.width(80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "¥${formatBalance(balance)}",
                    style = typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        }

        // 紧挨右组右侧的可点击提示符图标（不放进右组）
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "更多",
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size((typography.bodySmall.fontSize.value + 6).dp)
                .padding(end = 8.dp)
        )
    }

    if (showHistorySheet) {
        BalanceHistorySheet(
            channel = balanceType.displayName,
            todayBalance = balance,
            history = history,
            onClose = { showHistorySheet = false },
            onEditItem = { item ->
                editingItem = item
                inputValue = item.balance.toString()
            },
            onEditToday = {
                // 标题右侧今日余额被点击后编辑
                val today = getTodayString()
                val todayItem = history.find { it.recordDate == today }
                editingItem = todayItem ?: BalanceRecord(
                    sourceType = balanceType.displayName,
                    recordDate = today,
                    balance = balance
                )
                inputValue = editingItem!!.balance.toString()
            }
        )
    }

    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("请输入余额数字") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("日期：${editingItem!!.recordDate}")
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = {
                            // 允许输入 Double（最多一个小数点，且保留两位小数）
                            var s = it
                            // 只保留数字和小数点
                            s = s.filter { ch -> ch.isDigit() || ch == '.' }
                            // 只保留第一个小数点
                            val firstDot = s.indexOf('.')
                            if (firstDot != -1) {
                                val before = s.substring(0, firstDot + 1)
                                val after = s.substring(firstDot + 1).replace(".", "")
                                // 限制两位小数
                                s = before + after.take(2)
                            }
                            // 去除前导多余的 0（但保留 "0" 和 "0." 形式）
                            s = when {
                                s.isEmpty() -> ""
                                s.startsWith('.') -> "0$s"
                                else -> s
                            }
                            inputValue = s
                        },
                        singleLine = true,
                        label = { Text("余额") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val num = inputValue.toDoubleOrNull()
                    if (num != null) {
                        scope.launch {
                            parentVm.upsertBalanceForDate(
                                balanceType,
                                editingItem!!.recordDate,
                                num
                            )
                            editingItem = null
                            refreshAllBalanceCallBack()
                        }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("取消") }
            }
        )
    }
}

private fun formatBalance(value: Double): String {
    val intDigits = value.toLong().toString().length
    val decimals = when {
        intDigits >= 9 -> 0
        intDigits >= 8 -> 1
        else -> 2
    }
    return String.format("%.${decimals}f", value)
}

private fun formatSpend(value: Double): String {
    val intDigits = value.toLong().toString().length
    val decimals = when {
        intDigits >= 8 -> 0
        intDigits >= 7 -> 1
        else -> 2
    }
    return String.format("%.${decimals}f", value)
}


// BalanceItem 长按触发函数
fun balanceItemLongPressHandler(
    channelType: BalanceChannelType,
    haptic: HapticFeedback,
    context: Context,
    viewModel: AutoCollectBalanceViewModel,
) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

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

    viewModel.resetCacheBalance(channelType)
    Log.d(
        "获取余额是否需要更新",
        "${channelType.displayName}：${viewModel.channelBalances.value[channelType] ?: 0.0} 需要更新：" + MainActivity.ds.shouldUpdateBalance(
            channelType
        )
    )

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
            viewModel.loadAllCacheBalance()
            Log.i("自动化完成", "Result: $result")
        } else {
            Log.e("自动化出错", "Error: $throwable")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceHistorySheet(
    channel: String,
    todayBalance: Double,
    history: List<BalanceRecord>,
    onClose: () -> Unit,
    onEditItem: (BalanceRecord) -> Unit,
    onEditToday: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(indication = null, interactionSource = null) { onEditToday() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$channel 今日余额",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "¥${formatBalance(todayBalance)}",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            HorizontalDivider()
            val todayStr = getTodayString()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    history
                        .filter { it.recordDate != todayStr }
                        .sortedByDescending { java.time.LocalDate.parse(it.recordDate) }
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditItem(item) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.recordDate,
                            style = typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = "¥${formatBalance(item.balance)}", style = typography.bodyLarge)
                    }
                }
            }
        }
    }
}

private fun getTodayString(): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return java.time.LocalDate.now().format(formatter)
}