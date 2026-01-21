package com.cyc.yearlymemoir.ui.yearlycalendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.YearlyDetail
import kotlinx.coroutines.runBlocking

@Composable
fun DetailsManagementScreen(
    viewModel: DetailsManagerViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editingDetailText by remember { mutableStateOf("") }
    var editingDetailIsYearly by remember { mutableStateOf(false) }
    var editingDetail: Any? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        colors = CardColors(
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface,
            disabledContainerColor = colorScheme.surfaceVariant,
            disabledContentColor = colorScheme.onSurfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("详情管理", style = typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.uiState.items) { item ->
                    DetailRow(
                        item = item,
                        onEdit = {
                            editingDetailText = it.detail
                            editingDetailIsYearly = it.isYearly
                            editingDetail = it
                            showEditDialog = true
                        },
                        onDelete = {
                            viewModel.delete(it)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑详情") },
            text = {
                Column {
                    Text(text = "内容")
                    OutlinedTextField(
                        value = editingDetailText,
                        onValueChange = { editingDetailText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val item = editingDetail as DetailsManagerItem
                    viewModel.update(item.copy(detail = editingDetailText))
                    showEditDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("取消") }
            }
        )
    }
}

data class DetailsManagerItem(
    val fieldId: Int,
    val fieldName: String,
    val isYearly: Boolean,
    val year: Int?,            // 非 yearly 有 year，yearly 为 null
    val mdDateString: String,  // 通用的 md_date 字符串（例如 02-29 或 W2D3 等）
    val detail: String,
)

data class DetailsManagerUiState(
    val items: List<DetailsManagerItem> = emptyList()
)

class DetailsManagerViewModel : ViewModel() {
    var uiState by mutableStateOf(DetailsManagerUiState())
        private set

    fun loadAll() {
        // 拉取 fields 以建立 id -> name 的映射
        val fields: List<Field> = runBlocking { MainApplication.repository.getAllFields() }
        val nameById = fields.associate { it.fieldId to it.fieldName }

        // 拉取非 yearly details
        val details: List<Detail> = runBlocking { MainApplication.repository.getAllDetails() }
        val normalItems = details.map { d ->
            DetailsManagerItem(
                fieldId = d.fieldId,
                fieldName = nameById[d.fieldId] ?: "#${d.fieldId}",
                isYearly = false,
                year = d.year,
                mdDateString = d.mdDate.getRawMDDate().toString(),
                detail = d.detail
            )
        }

        // 拉取 yearly details
        val yds: List<YearlyDetail> =
            runBlocking { MainApplication.repository.getAllYearlyDetails() }
        val yearlyItems = yds.map { yd ->
            DetailsManagerItem(
                fieldId = yd.fieldId,
                fieldName = nameById[yd.fieldId] ?: "#${yd.fieldId}",
                isYearly = true,
                year = null,
                mdDateString = yd.mdDate.getRawMDDate().toString(),
                detail = yd.detail
            )
        }

        uiState = DetailsManagerUiState(items = (normalItems + yearlyItems).sortedBy { it.fieldId })
    }

    fun delete(item: DetailsManagerItem) {
        if (item.isYearly) {
            val yd = YearlyDetail(
                fieldId = item.fieldId,
                mdDate = UniversalDate.parse(1, item.mdDateString)!!,
                detail = item.detail
            )
            runBlocking { MainApplication.repository.deleteYearlyDetail(yd) }
        } else {
            val d = Detail(
                year = item.year!!,
                mdDate = UniversalDate.parse(item.year!!, item.mdDateString)!!,
                fieldId = item.fieldId,
                detail = item.detail
            )
            runBlocking { MainApplication.repository.deleteDetail(d) }
        }
        loadAll()
    }

    fun update(item: DetailsManagerItem) {
        if (item.isYearly) {
            val yd = YearlyDetail(
                fieldId = item.fieldId,
                mdDate = UniversalDate.parse(1, item.mdDateString)!!,
                detail = item.detail
            )
            runBlocking { MainApplication.repository.upsertYearlyDetail(yd) }
        } else {
            val date = UniversalDate.parse(item.year!!, item.mdDateString)!!
            runBlocking {
                MainApplication.repository.insertOrUpdateDetail(
                    fieldName = item.fieldName,
                    detail = item.detail,
                    date = date
                )
            }
        }
        loadAll()
    }
}

@Composable
private fun DetailRow(
    item: DetailsManagerItem,
    onEdit: (DetailsManagerItem) -> Unit,
    onDelete: (DetailsManagerItem) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.1.dp,
                color = colorScheme.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors().copy(
            containerColor = colorScheme.onSurface
                .copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "[" + item.fieldName + "] " + item.detail,
                        style = typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (item.isYearly) {
                            "Yearly: " + item.mdDateString
                        } else {
                            "Date: " + item.year + "-" + item.mdDateString
                        },
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(min = 80.dp)
                        .height(40.dp)
                ) {
                    IconButton(
                        onClick = { onEdit(item) },
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除",
                            tint = colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
