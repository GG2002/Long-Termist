package com.cyc.yearlymemoir.ui.personalbanlance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cyc.yearlymemoir.data.local.db.TmpFinanceDataBase
import com.cyc.yearlymemoir.data.local.entity.FinanceAsset
import ir.ehsannarmani.compose_charts.extensions.format
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 列表项 UI 模型：仅展示资产名与其最新金额
private data class UiAssetItem(
    val assetName: String,
    val latestAmount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAssetListScreen(
    onBack: () -> Unit,
    onEditAsset: (FinanceAsset) -> Unit,
    onDeleteAsset: (FinanceAsset) -> Unit
) {
    val scope = rememberCoroutineScope()
    // 仅展示每个资产名的最新金额
    val itemsState = remember { mutableStateOf<List<UiAssetItem>>(emptyList()) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { TmpFinanceDataBase.get(context) }

    fun refresh() {
        scope.launch {
            val latest = db.financeAssetDao().getLatestPerAsset()
            itemsState.value = latest.map { UiAssetItem(it.asset_name, it.asset_amount) }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    // 编辑弹窗状态
    val editOpen = remember { mutableStateOf(false) }
    // originalName: 用于在保存时判断是否需要重命名历史记录
    val editOriginalName = remember { mutableStateOf<String?>(null) }
    val editName = remember { mutableStateOf("") }
    val editAmountText = remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("金融资产") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        })
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            items(itemsState.value, key = { it.assetName }) { item ->
                FinanceAssetItemCard(
                    item = item,
                    onEdit = {
                        editOriginalName.value = item.assetName
                        editName.value = item.assetName
                        editAmountText.value = item.latestAmount.format(2)
                        editOpen.value = true
                    },
                    onDelete = {
                        scope.launch {
                            db.financeAssetDao().deleteByAssetName(item.assetName)
                            onDeleteAsset(
                                FinanceAsset(
                                    asset_name = item.assetName,
                                    asset_amount = 0.0,
                                    record_date = nowDate()
                                )
                            )
                            refresh()
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (editOpen.value) {
        AlertDialog(
            onDismissRequest = { editOpen.value = false },
            title = { Text("编辑资产（重命名将影响全部历史；金额为今日）") },
            text = {
                androidx.compose.foundation.layout.Column {
                    OutlinedTextField(
                        value = editName.value,
                        onValueChange = { editName.value = it },
                        label = { Text("名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editAmountText.value,
                        onValueChange = { editAmountText.value = it },
                        label = { Text("金额") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = editAmountText.value.replace(",", "").toDoubleOrNull()
                    val name = editName.value.trim()
                    val original = editOriginalName.value
                    if (value != null && name.isNotBlank()) {
                        scope.launch {
                            // 如名称发生变化，先重命名全部历史记录
                            if (!original.isNullOrBlank() && original != name) {
                                db.financeAssetDao().renameAsset(original, name)
                            }
                            val asset = FinanceAsset(
                                asset_name = name,
                                asset_amount = value,
                                record_date = nowDate()
                            )
                            db.financeAssetDao().insert(asset)
                            onEditAsset(asset)
                            refresh()
                        }
                    }
                    editOpen.value = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editOpen.value = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun FinanceAssetItemCard(
    item: UiAssetItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.assetName, style = typography.bodyLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(String.format("%,.2f", item.latestAmount), style = typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除"
                    )
                }
            }
        }
    }
}

private fun nowDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)