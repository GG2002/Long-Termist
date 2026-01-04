package com.cyc.yearlymemoir.ui.personalbanlance

import AutoCollectBalanceCard
import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PersonalBalanceViewModel(application: Application) : AndroidViewModel(application) {
    private val model = MainApplication.repository

    private val _allBalance = MutableStateFlow(0.0)
    val allBalance: StateFlow<Double> = _allBalance

    private val _lastUpdated = MutableStateFlow(0.0)
    val lastUpdated = _lastUpdated

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
                emptyList<com.cyc.yearlymemoir.domain.model.BalanceRecord>()
            }
            val total = balances.sumOf { it.balance }
            _allBalance.value = total
            _lastUpdated.value = System.currentTimeMillis().toDouble()
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

    fun importBalancesFromText(input: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            // Each line: source,date,balance
            val lines = input.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            for (line in lines) {
                val parts = line.split(",")
                Log.e("i", parts.toString())
                if (parts.size >= 3) {
                    val source = parts[0].trim()
                    val date = parts[1].trim()
                    val balanceStr = parts[2].trim()
                    val balance = balanceStr.toDoubleOrNull()
                    println(balance)
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
            computeAllBalance()
            onDone()
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
    val lastUpdate by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val refreshTick by viewModel.refreshTick.collectAsStateWithLifecycle()

    var menuExpanded by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

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
                                .format(Date(lastUpdate.toLong()))
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
                                    text = { Text("导出（占位）") },
                                    onClick = {
                                        menuExpanded = false
                                        // Export placeholder: no-op for now
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("导入") },
                                    onClick = {
                                        menuExpanded = false
                                        showImportSheet = true
                                    }
                                )
                            }
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
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
            Spacer(Modifier.height(10.dp))

            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    BanlanceChartCard(refreshTick = refreshTick)
                }
            }

            Spacer(Modifier.height(20.dp))

            AutoCollectBalanceCard(
                refreshAllBalanceCallBack = { viewModel.refreshAll() },
                refreshTick = refreshTick
            )
        }
    }

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            dragHandle = null
        ) {
            // Nearly full-screen: use a large height but keep some margin
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "批量导入余额（每行：来源，日期 (yyyy-MM-dd),余额）",
                    style = typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    placeholder = { Text("例如：\n微信，2026-01-04,1289649.56\n支付宝，2026-01-04,21540.0") },
                    maxLines = 20
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showImportSheet = false }) { Text("取消") }
                    Spacer(Modifier.height(0.dp))
                    Button(onClick = {
                        viewModel.importBalancesFromText(importText) {
                            showImportSheet = false
                            importText = ""
                        }
                    }) { Text("导入") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

