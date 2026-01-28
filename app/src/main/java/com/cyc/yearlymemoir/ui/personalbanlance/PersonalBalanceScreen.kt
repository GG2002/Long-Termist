package com.cyc.yearlymemoir.ui.personalbanlance

import AutoCollectBalanceCard
import LedgerChartCard
import android.annotation.SuppressLint
import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.data.local.db.TmpFinanceDataBase
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.model.TransactionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// 底部导航数据结构：标题、图标、点击事件（由父组件传入，写死即可，方便后续扩展）
data class NavItem(
    val title: String,
    val icon: ImageVector,
    val isSelected: () -> Boolean,
    val onClick: () -> Unit,
)

// 页签枚举，避免使用易出错的下标
enum class BottomTab { BILL, CENTER, ASSET }


class PersonalBalanceViewModel(application: Application) : AndroidViewModel(application) {
    private val model = MainApplication.repository
    private val ds = MainApplication.ds

    private val _allBalance = MutableStateFlow(0.0)
    val allBalance: StateFlow<Double> = _allBalance

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated

    // 子组件刷新的触发计数（父组件控制）
    private val _refreshTick = MutableStateFlow(0)
    val refreshTick: StateFlow<Int> = _refreshTick

    fun computeAllBalance() {
        viewModelScope.launch {
            // 获取今天的所有 balance 并相加
            val today = java.time.LocalDate.now().toString()
            val balances = try {
                model.getBalancesByDate(today)
            } catch (e: Exception) {
                emptyList()
            }
            // 加上 Finance Asset 的最新总额
            val assetTotal = runCatching {
                TmpFinanceDataBase.get(MainApplication.instance).financeAssetDao()
                    .getLatestTotalAmount()
            }.getOrElse { 0.0 }
            val total = balances.sumOf { it.balance } + assetTotal
            _allBalance.value = total
            _lastUpdated.value = ds.getLong("last_update_time")
        }
    }

    // 父组件刷新：先触发子组件，再刷新父组件数据
    fun refreshAll() {
        viewModelScope.launch {
            // 递增 tick 以触发所有子组件监听的 LaunchedEffect
            _refreshTick.value += 1
            // 然后刷新父组件的总余额
            computeAllBalance()
        }
    }

    private fun getDownloadsFile(): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(dir, "LongTermistBalance.txt")
    }

    private fun getDownloadsTransactionsFile(): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(dir, "LongTermistTransactions.txt")
    }

    private fun getDownloadsTagsFile(): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(dir, "LongTermistTags.txt")
    }

    fun exportDataToFile(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Fetch all balances; if repository has only by date, we may need a method to get all
                val all = try {
                    model.getAllBalances()
                } catch (_: Exception) {
                    emptyList()
                }
                val content = all.joinToString(separator = "\n") { r ->
                    "${r.sourceType},${r.recordDate},${r.balance}"
                }
                val file = getDownloadsFile()
                file.parentFile?.mkdirs()
                file.writeText(content)

                // 额外导出：所有 transactions（舍去 id）到单独文件
                val transactions = try {
                    model.getAllTransactionsDesc()
                } catch (_: Exception) {
                    emptyList()
                }
                val txContent = transactions.joinToString(separator = "\n") { t ->
                    // amount,tag,remark,record_date（不含 id）
                    "${t.amount},${t.tag},${t.remark},${t.recordDate}"
                }
                val txFile = getDownloadsTransactionsFile()
                txFile.parentFile?.mkdirs()
                txFile.writeText(txContent)

                // 额外导出：所有 tags（DataStore 里的反引号拼接字符串）到单独文件
                val rawTags = ds.getString("custom_tags") ?: ""
                val tagsFile = getDownloadsTagsFile()
                tagsFile.parentFile?.mkdirs()
                tagsFile.writeText(rawTags)

                onSuccess()
            } catch (e: IOException) {
                onError("写入失败：${e.message}")
            } catch (e: Exception) {
                onError("导出失败：${e.message}")
            }
        }
    }

    fun importDataFromFile(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // 导入 balances
                val baFile = getDownloadsFile()
                if (!baFile.exists()) {
                    onError("文件不存在：${baFile.absolutePath}")
                    return@launch
                }
                val baText = baFile.readText()
                // Each line: source,date,balance
                var lines = baText.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val source = parts[0].trim()
                        val date = parts[1].trim()
                        val balanceStr = parts[2].trim()
                        val balance = balanceStr.toDoubleOrNull()
                        if (balance != null) {
                            try {
                                model.upsertBalance(
                                    BalanceRecord(
                                        sourceType = source,
                                        recordDate = date,
                                        balance = balance
                                    )
                                )
                            } catch (e: Exception) {
                                // ignore a single bad insert
                                Log.e("i", "$source $date $balance $e")
                            }
                        }
                    }
                }

                // 导入 transactions
                val txFile = getDownloadsTransactionsFile()
                if (!txFile.exists()) {
                    onError("文件不存在：${txFile.absolutePath}")
                    return@launch
                }
                val txText = txFile.readText()
                // 每行 -> amount,tag,remark,record_date
                lines = txText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 4) {
                        val amount = parts[0].trim().toDoubleOrNull()
                        val tag = parts[1].trim()
                        val remark = parts[2].trim()
                        val recordDate = parts[3].trim()
                        if (amount != null) {
                            try {
                                model.upsertTransaction(
                                    TransactionRecord(
                                        id = 0,
                                        amount = amount,
                                        tag = tag,
                                        remark = remark,
                                        recordDate = recordDate
                                    )
                                )
                            } catch (_: Exception) {
                                // ignore single bad row
                            }
                        }
                    }
                }

                // 导入 tags（独立文件）
                val tagsFile = getDownloadsTagsFile()
                if (tagsFile.exists()) {
                    val raw = tagsFile.readText()
                    // 直接写回 DataStore 原始反引号拼接字符串
                    ds.putString("custom_tags", raw)
                }

                // 完成后刷新
                computeAllBalance()
                onSuccess()
            } catch (e: IOException) {
                onError("读取失败：${e.message}")
            } catch (e: Exception) {
                onError("导入失败：${e.message}")
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun PersonalBalanceScreen(
    viewModel: PersonalBalanceViewModel = viewModel()
) {
    val allBalance by viewModel.allBalance.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val refreshTick by viewModel.refreshTick.collectAsStateWithLifecycle()

    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.computeAllBalance()
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer,
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val formattedTime =
                            SimpleDateFormat("MM 月 dd 日 HH:mm", Locale.getDefault())
                                .format(Date(lastUpdated))
                        Column {
                            Text(
                                "总余额：${String.format(Locale.getDefault(), "%.2f", allBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                            Text(
                                "最后更新：$formattedTime",
                                style = typography.labelSmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "更多"
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("导出到文件") },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.exportDataToFile(
                                            onSuccess = {
                                                viewModel.refreshAll()
                                            },
                                            onError = {
                                                viewModel.refreshAll()
                                            }
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("从文件导入") },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.importDataFromFile(
                                            onSuccess = {
                                                viewModel.refreshAll()
                                            },
                                            onError = {
                                                viewModel.refreshAll()
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        // 页面选择用枚举，避免下标耦合
        var selectedTab by remember { mutableStateOf(BottomTab.BILL) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    start = 18.dp,
                    end = 18.dp
                )
                .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                BottomTab.BILL -> { // 账单页
                    LedgerChartCard(refreshTick = refreshTick)
                    Spacer(Modifier.height(12.dp))
                    BanlanceChartCard(refreshTick = refreshTick)
                }

                BottomTab.ASSET -> { // 资产页
                    AutoCollectBalanceCard(
                        refreshAllBalanceCallBack = { viewModel.refreshAll() },
                        refreshTick = refreshTick
                    )
                    Spacer(Modifier.height(12.dp))
                    FinanceAssetCard(refreshTick = refreshTick)
                }

                BottomTab.CENTER -> {
                    // 中间按钮或其他索引暂不显示内容
                }
            }

            Spacer(Modifier.height(108.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val haptic = LocalHapticFeedback.current
            val navItems = remember(selectedTab) {
                listOf(
                    NavItem(
                        title = "账单",
                        icon = Icons.Default.Home,
                        isSelected = { selectedTab == BottomTab.BILL },
                        onClick = {
                            // 防抖
                            if (selectedTab != BottomTab.BILL) {
                                selectedTab = BottomTab.BILL
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            }
                        }
                    ),
                    // 中间按钮事件不需要考虑，这里占位：不触发页面切换
                    NavItem(
                        title = "中心",
                        icon = Icons.Default.Add,
                        isSelected = { selectedTab == BottomTab.CENTER },
                        onClick = { /* no-op */ }
                    ),
                    NavItem(
                        title = "资产",
                        icon = Icons.Default.ShoppingCart,
                        isSelected = { selectedTab == BottomTab.ASSET },
                        onClick = {
                            if (selectedTab != BottomTab.ASSET) {
                                selectedTab = BottomTab.ASSET
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            }
                        }
                    ),
                )
            }
            Box {
                WatchStyleBottomBar(
                    items = navItems,
                    onAnyActionConfirmed = { viewModel.refreshAll() }
                )
            }
        }
    }
}


@Composable
fun WatchStyleBottomBar(
    items: List<NavItem>,
    onAnyActionConfirmed: () -> Unit = {}
) {
    // 位置大小
    val bottomHeight = 40
    // 尺寸配置
    val barHeight = 60.dp        // 两侧长条的高度
    val navIconSize = 24.dp      // 侧边导航图标大小
    val fabSize = 48.dp          // 中间大按钮的大小 (直径)
    val fabIconSize = 28.dp      // 中间图标大小
    val barWidth = 220.dp        // 总宽度

    // 颜色配置
    val barColor = colorScheme.surface.copy(alpha = 0.7f)
    val fabColor = colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), // 给容器足够的高度来容纳突出的按钮
        contentAlignment = Alignment.BottomCenter
    ) {

        // -------------------------------------------------
        // 1. 底部的胶囊长条 (Background Bar)
        // -------------------------------------------------
        val tmp = Modifier
            .padding(bottom = bottomHeight.dp)
            .width(barWidth)
            .height(barHeight)
            .shadow(elevation = 10.dp, shape = CircleShape, spotColor = Color.Black)
            .background(barColor, shape = CircleShape)
        Box(
            modifier = if (!isSystemInDarkTheme()) tmp else tmp
                .border(
                    width = 1.dp,
                    color = Color.White, shape = CircleShape
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                items.getOrNull(0)?.onClick?.invoke()
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val leftItem = items.getOrNull(0)
                    NavIcon(
                        icon = leftItem?.icon ?: Icons.Default.Home,
                        text = leftItem?.title ?: "账单",
                        isSelected = leftItem?.isSelected?.invoke() ?: false,
                        size = navIconSize
                    )
                }

                Spacer(modifier = Modifier.width(fabSize))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                items.getOrNull(2)?.onClick?.invoke()
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val rightItem = items.getOrNull(2)
                    NavIcon(
                        icon = rightItem?.icon ?: Icons.Default.ShoppingCart,
                        text = rightItem?.title ?: "资产",
                        isSelected = rightItem?.isSelected?.invoke() ?: false,
                        size = navIconSize
                    )
                }
            }
        }

        // 中间的大圆形按钮 (Floating Action Button)
        val haptic = LocalHapticFeedback.current
        var showDualSheet by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .padding(bottom = bottomHeight.dp + (barHeight - fabSize) / 2)
                .size(fabSize)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = fabColor.copy(alpha = 0.5f)
                )
                .background(fabColor, CircleShape)
                .clip(CircleShape)
                .clickable {
                    showDualSheet = true
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Center Action",
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(fabIconSize)
            )
        }
        DualActionSheets(
            show = showDualSheet,
            onDismissAll = { showDualSheet = false },
            onConfirmed = {
                // 记一笔等操作完成后，通知父层刷新
                onAnyActionConfirmed()
            }
        )
    }
}

// 辅助组件：导航图标
@Composable
fun NavIcon(
    icon: ImageVector,
    text: String,
    isSelected: Boolean,
    size: androidx.compose.ui.unit.Dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) colorScheme.primary else colorScheme.onSurface,
            modifier = Modifier.size(size)
        )
        Text(
            text, style = typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
            )
        )
    }
}
