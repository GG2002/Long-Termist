package com.cyc.yearlymemoir.ui.personalbanlance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.TransactionRecord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<TransactionRecord>>(emptyList()) }
    var editing by remember { mutableStateOf<TransactionRecord?>(null) }

    LaunchedEffect(Unit) {
        items = MainApplication.repository.getAllTransactionsDesc()
    }

    fun refresh() {
        scope.launch { items = MainApplication.repository.getAllTransactionsDesc() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("所有收支") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { rec ->
                    TransactionItemCard(
                        record = rec,
                        onEdit = { editing = rec },
                        onDelete = { id ->
                            scope.launch {
                                if (id != 0) MainApplication.repository.deleteTransaction(id)
                                refresh()
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    // 编辑弹出：复用记账弹窗，传入初始值
    if (editing != null) {
        BookkeepingSheetWithInitial(
            initial = editing,
            onDismiss = { editing = null; refresh() }
        )
    }
}

@Composable
private fun TransactionItemCard(
    record: TransactionRecord,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isExpense = record.amount < 0
    val amountAbs = kotlin.math.abs(record.amount)
    val textColor = if (isExpense) Color(0xFFFFA000) else Color(0xFF1E88E5)
    val bgColor = textColor.copy(alpha = 0.15f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.1.dp,
                color = colorScheme.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(shape = RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors().copy(
            containerColor = colorScheme.onSurface
                .copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧 出/入 圆形标
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(bgColor, shape = RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isExpense) "出" else "入",
                    color = textColor,
                    style = typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.remark.ifBlank { "无备注" },
                        style = typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .border(
                                0.5.dp,
                                color = colorScheme.onSurface.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(7.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            record.tag.ifBlank { "未分类" },
                            modifier = Modifier.padding(horizontal = 6.dp),
                            style = typography.labelSmall
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = record.recordDate,
                    style = typography.bodySmall.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(Modifier.width(12.dp))
            Text(
                text = (if (isExpense) "-" else "+") + String.format("%,.2f", amountAbs),
                color = textColor,
                style = typography.titleLarge.copy(
                    fontSize = (typography.titleLarge.fontSize.value - 4).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (expanded) {
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onEdit
                ) {
                    Text("编辑", color = colorScheme.primary)
                }
                VerticalDivider(
                    Modifier
                        .width(8.dp)
                        .height(48.dp)
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onDelete(record.id) }
                ) {
                    Text("删除", color = colorScheme.error)
                }
            }
        }
    }
}
