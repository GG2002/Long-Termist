package com.cyc.yearlymemoir.ui.personalbanlance

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.TransactionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// 假设应用级的 DataStore 入口
private const val TAGS_KEY = "custom_tags"
private fun loadTags(): MutableList<String> {
    // 从 MainApplication.ds 读取，用反引号拼接的字符串
    return try {
        val raw = MainApplication.ds.getString(TAGS_KEY) ?: ""
        raw.split('`').filter { it.isNotBlank() }.toMutableList()
    } catch (e: Exception) {
        mutableListOf("餐饮", "交通", "购物", "收入", "房租")
    }
}

private fun saveTags(tags: List<String>) {
    val serialized = tags.joinToString(separator = "`")
    MainApplication.ds.putString(TAGS_KEY, serialized)
}

private enum class NestedSheetType { Asset, Bookkeeping }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordSheet(
    onDismiss: () -> Unit,
    onSelect: (NestedSheetType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "添加记录",
                style = typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = SpaceBetween
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    logo = Icons.Filled.AttachMoney,
                    title = "录入资产",
                    subtitle = "管理资产与持仓",
                    color = Color(0xFFFFCA28),
                    onClick = { onSelect(NestedSheetType.Asset) }
                )
                Spacer(Modifier.width(12.dp))
                ActionCard(
                    modifier = Modifier.weight(1f),
                    logo = Icons.Filled.Wallet,
                    title = "记账",
                    subtitle = "快速记录收支",
                    color = Color(0xFF108B46),
                    onClick = { onSelect(NestedSheetType.Bookkeeping) }
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    logo: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = logo,
                contentDescription = title,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        colorScheme.surface,
                        shape = CircleShape
                    )
                    .padding(8.dp),
                tint = color
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = title,
                style = typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetInputSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "资产录入", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "这里将展示资产账户、币种、持仓等表单（占位）",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookkeepingSheet(
    onDismiss: () -> Unit,
    initial: TransactionRecord? = null
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "记一笔", style = typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // 初始化
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = run {
                    if (initial?.recordDate != null) {
                        try {
                            LocalDate.parse(
                                initial.recordDate,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            )
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli()
                        } catch (e: Exception) {
                            LocalDate.now(ZoneId.systemDefault())
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli()
                        }
                    } else {
                        LocalDate.now(ZoneId.systemDefault())
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli()
                    }
                }
            )
            // 当 initial 改变时，同步 DatePicker 的选中值，确保能正确回显
            LaunchedEffect(initial) {
                val millis = if (initial?.recordDate != null) {
                    try {
                        LocalDate.parse(
                            initial.recordDate,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        )
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli()
                    } catch (e: Exception) {
                        LocalDate.now(ZoneId.systemDefault())
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli()
                    }
                } else {
                    LocalDate.now(ZoneId.systemDefault())
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }
                // 兼容 API：通过 setSelection 更新选中值
                try {
                    dateState.selectedDateMillis = millis
                } catch (_: Throwable) {
                    // 部分旧版可能暴露可变属性，作为兜底
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val field = dateState::class.java.getDeclaredField("selectedDateMillis")
                        field.isAccessible = true
                        field.set(dateState, millis)
                    } catch (_: Throwable) {
                    }
                }
            }
            val selectedDateText = remember(dateState.selectedDateMillis) {
                dateState.selectedDateMillis?.let { millis ->
                    // 与 DatePicker 内部保持一致，按 UTC 解析为 LocalDate，避免跨时区偏移
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                } ?: "选择日期"
            }

            // 1. 金额输入（￥ 前缀）
            var amount by remember { mutableStateOf("") }
            var isExpense by remember { mutableStateOf(true) }
            val amountInitial = remember(initial) {
                initial?.let { kotlin.math.abs(it.amount).toString() }
            }
            var isExpenseInitial = remember(initial) {
                initial?.let { it.amount < 0 } ?: true
            }
            AmountCalculatorInput(
                onResultChange = { amount = it },
                onConfirmResult = { amount = it },
                onTypeChange = { expense -> isExpense = expense },
                initialValue = amountInitial,
                initialIsExpense = isExpenseInitial
            )
            Spacer(Modifier.height(12.dp))

            // 2. 备注
            var remark by remember(initial) {
                mutableStateOf(initial?.let { it.remark } ?: "")
            }
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))

            // 3. 日期选择器
            var openDatePicker by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDateText,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("日期") }
                )
                // 覆盖层点击：去除水波纹（indication = null），保留点击逻辑
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { openDatePicker = true }
                )
            }
            if (openDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { openDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { openDatePicker = false }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { openDatePicker = false }) { Text("取消") }
                    }
                ) {
                    DatePicker(state = dateState)
                }
            }
            Spacer(Modifier.height(12.dp))

            // 4. 标签输入（单选）
            val selectedTag = remember { mutableStateOf(initial?.let { it.tag }) }
            TagOneSelect(
                initialSelected = selectedTag.value,
                onSelectedChange = { selectedTag.value = it }
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    // 将字符串金额解析为 Double，并根据支出/收入设置正负号
                    println(amount?.trim())
                    println(amount?.trim()?.toDoubleOrNull())
                    val parsed = amount?.trim()?.toDoubleOrNull() ?: return@Button
                    val finalAmount =
                        if (isExpense) -kotlin.math.abs(parsed) else kotlin.math.abs(parsed)

                    val date =
                        if (selectedDateText.isNotBlank() && selectedDateText != "选择日期") {
                            selectedDateText
                        } else {
                            // 兜底使用今天（与上方 DatePicker 规则一致，按 UTC）
                            LocalDate.now(ZoneOffset.UTC)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        }

                    val tag = selectedTag.value ?: ""
                    val rec = TransactionRecord(
                        id = initial?.id ?: 0,
                        amount = finalAmount,
                        tag = tag,
                        remark = remark.trim(),
                        recordDate = date
                    )

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                MainApplication.repository.upsertTransaction(rec)
                                Log.d(
                                    "BookingSheet",
                                    "已保存：${if (isExpense) "支出" else "收入"} $finalAmount，日期：$date，备注：${rec.remark}，标签：$tag"
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                Log.e("BookingSheet", "保存失败：${e.message}")
                            }
                        }
                    }
                }) { Text("保存") }
            }
        }
    }
}

@Composable
fun BookkeepingSheetWithInitial(
    initial: TransactionRecord?,
    onDismiss: () -> Unit
) {
    BookkeepingSheet(onDismiss = onDismiss, initial = initial)
}

@Composable
fun AmountCalculatorInput(
    onResultChange: (String) -> Unit = { _ -> },
    onConfirmResult: (String) -> Unit = { _ -> },
    onTypeChange: (Boolean) -> Unit = { _ -> },
    initialValue: String? = null,
    initialIsExpense: Boolean = true
) {
    // 输入的原始字符串 (例如 "100+20*3")
    var inputString by remember { mutableStateOf("") }
    // 实时计算的结果
    var calculatedResult by remember { mutableStateOf<String?>(null) }
    // 支出/收入切换：默认支出（true）
    var isExpense by remember { mutableStateOf(initialIsExpense) }

    // 初始金额随 props 变化时回显
    LaunchedEffect(initialValue) {
        if (!initialValue.isNullOrBlank()) {
            inputString = initialValue
            calculatedResult = null
            onResultChange(initialValue)
        }
    }
    // 初始“支出/收入”随 props 变化时回显并回调
    LaunchedEffect(initialIsExpense) {
        isExpense = initialIsExpense
        onTypeChange(isExpense)
    }

    // 简单的四则运算逻辑（不带括号，先乘除后加减）
    fun calculate(expression: String): String? {
        if (expression.isBlank()) return null
        // 简单的正则检查，防止非法字符导致崩溃
        if (!expression.matches(Regex("^[0-9+\\-*/.]+$"))) return null
        // 如果最后一位是运算符，暂停计算
        if (expression.last() in listOf('+', '-', '*', '/', '.')) return null

        return try {
            // 1. 分割数字和运算符
            val numbers =
                expression.split(Regex("[+\\-*/]")).map { it.toBigDecimal() }.toMutableList()
            val operators = expression.filter { it in "+-*/" }.toMutableList()

            if (numbers.size != operators.size + 1) return null

            // 2. 先处理乘除 (遍历并压缩列表)
            var i = 0
            while (i < operators.size) {
                if (operators[i] == '*' || operators[i] == '/') {
                    val a = numbers[i]
                    val b = numbers[i + 1]
                    val res = if (operators[i] == '*') a.multiply(b) else a.divide(
                        b,
                        2,
                        RoundingMode.HALF_UP
                    )

                    // 原地替换
                    numbers[i] = res
                    numbers.removeAt(i + 1)
                    operators.removeAt(i)
                    i-- // 回退一步以检查连续的乘除
                }
                i++
            }

            // 3. 再处理加减
            var result = numbers[0]
            for (j in operators.indices) {
                val nextNum = numbers[j + 1]
                result = if (operators[j] == '+') result.add(nextNum) else result.subtract(nextNum)
            }

            // 去除多余的 0 (例如 150.00 -> 150)
            result.stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            null // 计算出错（如除以 0）时不显示结果
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 金额输入框占满剩余空间
        OutlinedTextField(
            value = inputString,
            onValueChange = { input ->
                // 1. 允许输入数字、小数点和四则运算符
                val filtered = input.filter { it.isDigit() || ".+-*/".contains(it) }

                // 2. 简单的连点/连符号防护（可选，这里为了简单只防多重小数点，逻辑略）
                // 如果你希望逻辑简单，直接赋值即可，依靠 calculate 函数里的 try-catch 兜底
                inputString = filtered

                // 3. 实时计算
                calculatedResult = calculate(filtered)
                calculatedResult?.let { onResultChange(it) }
            },
            modifier = Modifier.weight(1f),
            // 【关键点 1】在 Label 中显示结果
            label = {
                if (calculatedResult != null && calculatedResult != inputString) {
                    // 如果有计算结果且不等于当前输入，显示 "金额 = 结果"
                    Text("金额 = $calculatedResult")
                } else {
                    Text("金额")
                }
            },
            prefix = { Text("￥") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    calculatedResult?.let { result ->
                        inputString = result // 将输入框内容替换为计算结果
                        onConfirmResult(result) // 将最终结果传给父层
                        calculatedResult = null // 重置预览
                    }
                    defaultKeyboardAction(ImeAction.Next)
                }
            ),
            singleLine = true
        )

        Spacer(Modifier.width(8.dp))

        // 圆形“出/入”按钮
        val expenseColor = Color(0xFFFFA000) // 橙色
        val incomeColor = Color(0xFF1E88E5) // 蓝色
        val textColor = if (isExpense) expenseColor else incomeColor
        val bgColor = textColor.copy(alpha = 0.15f)

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(bgColor, shape = CircleShape)
                .border(0.5.dp, color = textColor.copy(alpha = 0.5f), shape = CircleShape)
                .clip(CircleShape)
                .clickable { isExpense = !isExpense; onTypeChange(isExpense) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isExpense) "出" else "入",
                color = textColor,
                style = typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TagOneSelect(
    initialSelected: String? = null,
    onSelectedChange: (String?) -> Unit = {}
) {
    // 所有标签：从 DataStore 载入，修改后立刻持久化
    val allTags = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val selected = remember { mutableStateOf<String?>(null) }
    var manageOpen by remember { mutableStateOf(false) }
    var addDialogOpen by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    // 加载所有标签
    LaunchedEffect(Unit) {
        val initial = loadTags()
        allTags.clear(); allTags.addAll(initial)
        // 首次加载后，根据传入初始值设置选中
        selected.value = initialSelected?.takeIf { it in allTags }
        onSelectedChange(selected.value)
    }
    // 当外部初始选中项变化时，同步内部选中状态
    LaunchedEffect(initialSelected) {
        selected.value = initialSelected?.takeIf { it in allTags }
        onSelectedChange(selected.value)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 顶部行：将“自定义”按钮放在“标签（可选）”左侧，留出一定间距
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "标签（可选）", style = MaterialTheme.typography.labelLarge.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
            OutlinedButton(
                onClick = { manageOpen = true },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                border = BorderStroke(0.5.dp, colorScheme.secondary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colorScheme.secondary.copy(alpha = 0.2f),
                    contentColor = colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "管理标签",
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "自定义",
                    color = colorScheme.secondary,
                    style = typography.labelSmall
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // 展示所有标签，自动换行布局，点击切换选中状态并以 Primary 颜色突出
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allTags.forEach { tag ->
                val selectedNow = selected.value == tag
                AssistChip(
                    onClick = {
                        selected.value = if (selectedNow) null else tag
                        onSelectedChange(selected.value)
                    },
                    label = { Text(tag) },
                    colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedNow) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surface,
                        labelColor = if (selectedNow) colorScheme.primary else colorScheme.onSurface
                    )
                )
            }
        }

        // 管理标签 BottomSheet
        if (manageOpen) {
            ModalBottomSheet(onDismissRequest = { manageOpen = false }) {
                Column(
                    Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "管理标签", style = typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        IconButton(onClick = { addDialogOpen = true }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "添加标签")
                        }
                    }
                    HorizontalDivider(Modifier.height(12.dp))

                    // 列出所有标签，右侧删除按钮
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allTags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .border(
                                        1.dp,
                                        color = colorScheme.error.copy(0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(Modifier.width(10.dp))
                                Text(text = tag, style = typography.labelLarge)
                                IconButton(onClick = {
                                    val idx = allTags.indexOf(tag)
                                    if (idx >= 0) {
                                        allTags.removeAt(idx)
                                        saveTags(allTags)
                                        // 同步已选
                                        if (selected.value == tag) {
                                            selected.value = null
                                            onSelectedChange(null)
                                        }
                                    }
                                }) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Filled.Delete,
                                        tint = colorScheme.error,
                                        contentDescription = "删除"
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // 添加标签对话框
        if (addDialogOpen) {
            AlertDialog(
                onDismissRequest = { addDialogOpen = false },
                title = { Text("添加标签") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            label = { Text("标签名") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = newTagName.trim()
                        if (name.isNotEmpty() && name !in allTags) {
                            allTags.add(name)
                            saveTags(allTags)
                        }
                        newTagName = ""
                        addDialogOpen = false
                    }) { Text("添加") }
                },
                dismissButton = {
                    TextButton(onClick = { addDialogOpen = false }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
fun DualActionSheets(
    show: Boolean,
    onDismissAll: () -> Unit
) {
    if (!show) return
    // 管理第二层具体类型
    var current by remember { mutableStateOf<NestedSheetType?>(null) }

    when (current) {
        null -> AddRecordSheet(
            onDismiss = onDismissAll,
            onSelect = { current = it }
        )

        NestedSheetType.Asset -> AssetInputSheet(onDismiss = onDismissAll)
        NestedSheetType.Bookkeeping -> BookkeepingSheet(onDismiss = onDismissAll)
    }
}
